import asyncio
import codecs
import json
import os
import pty
import shutil
import signal
import subprocess
import uuid
from typing import Optional

from fastapi import WebSocket, WebSocketDisconnect


class InteractiveSession:

    TEMP_DIR = "/tmp/code-execution"
    START_TIMEOUT_SECONDS = 10
    COMPILE_TIMEOUT_SECONDS = 30
    DEFAULT_SECONDS = 120
    MAX_SECONDS = 120
    MAX_OUTPUT_BYTES = 64 * 1024
    MAX_INPUT_CHARS = 4096
    READ_CHUNK = 4096

    def __init__(self, websocket: WebSocket):
        self.websocket = websocket
        self.work_dir = os.path.join(self.TEMP_DIR, str(uuid.uuid4()))
        self.process: Optional[subprocess.Popen] = None
        self.master_fd: Optional[int] = None
        self.closed = False

    async def run(self) -> None:
        os.makedirs(self.work_dir, exist_ok=True)
        try:
            start = await self._receive_start()
            if start is None:
                return
            command = await self._prepare(start)
            if command is None:
                return
            await self._execute(command, self._session_seconds(start))
        except WebSocketDisconnect:
            self.closed = True
        finally:
            self._kill()
            self._close_master()
            shutil.rmtree(self.work_dir, ignore_errors=True)
            await self._close()

    async def _receive_start(self) -> Optional[dict]:
        try:
            raw = await asyncio.wait_for(self.websocket.receive_text(), self.START_TIMEOUT_SECONDS)
            message = json.loads(raw)
        except (asyncio.TimeoutError, ValueError):
            await self._send({"type": "error", "message": "Konzola nije dobila program za pokretanje."})
            return None

        if not isinstance(message, dict) or message.get("type") != "start" or not isinstance(message.get("code"), str):
            await self._send({"type": "error", "message": "Neispravan početak sesije."})
            return None
        return message

    def _session_seconds(self, start: dict) -> int:
        try:
            seconds = int(start.get("timeoutSeconds", self.DEFAULT_SECONDS))
        except (TypeError, ValueError):
            seconds = self.DEFAULT_SECONDS
        return max(1, min(seconds, self.MAX_SECONDS))

    async def _prepare(self, start: dict) -> Optional[list]:
        language = start.get("language", "C")
        code = start["code"]

        if language == "C":
            source = os.path.join(self.work_dir, "main.c")
            executable = os.path.join(self.work_dir, "main")
            with open(source, "w") as f:
                f.write(code)
            await self._send({"type": "status", "state": "compiling"})
            ok, output = await self._run_tool(["gcc", "-o", executable, source, "-Wall", "-Wextra", "-lm"])
            if not ok:
                await self._send({"type": "compile_error", "output": self._strip_paths(output)})
                return None
            if output.strip():
                await self._send({"type": "compiler_output", "output": self._strip_paths(output)})
            return [executable]

        if language == "PYTHON":
            source = os.path.join(self.work_dir, "main.py")
            with open(source, "w") as f:
                f.write(code)
            await self._send({"type": "status", "state": "compiling"})
            ok, output = await self._run_tool(["python3", "-m", "py_compile", source])
            if not ok:
                await self._send({"type": "compile_error", "output": self._strip_paths(output)})
                return None
            return ["python3", "-u", source]

        if language == "CSHARP":
            await self._send({"type": "error", "message": "C# nije podržan na ovom poslužitelju jer .NET SDK nije instaliran."})
            return None

        await self._send({"type": "error", "message": f"Nepodržani jezik: {language}"})
        return None

    async def _run_tool(self, command: list) -> tuple:
        proc = await asyncio.create_subprocess_exec(
            *command,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
            cwd=self.work_dir,
        )
        try:
            out, _ = await asyncio.wait_for(proc.communicate(), self.COMPILE_TIMEOUT_SECONDS)
        except asyncio.TimeoutError:
            proc.kill()
            await proc.wait()
            return False, "Prevođenje je trajalo predugo."
        return proc.returncode == 0, out.decode("utf-8", errors="replace")

    def _strip_paths(self, text: str) -> str:
        return text.replace(self.work_dir + os.sep, "")

    async def _execute(self, command: list, seconds: int) -> None:
        master_fd, slave_fd = pty.openpty()
        self.master_fd = master_fd
        env = {
            "PATH": "/usr/local/bin:/usr/bin:/bin",
            "HOME": self.work_dir,
            "TERM": "dumb",
            "LANG": "C.UTF-8",
            "PYTHONIOENCODING": "utf-8",
        }
        try:
            self.process = subprocess.Popen(
                command,
                stdin=slave_fd,
                stdout=slave_fd,
                stderr=slave_fd,
                cwd=self.work_dir,
                env=env,
                start_new_session=True,
                close_fds=True,
            )
        finally:
            os.close(slave_fd)

        await self._send({"type": "status", "state": "running", "seconds": seconds})

        loop = asyncio.get_running_loop()
        queue: asyncio.Queue = asyncio.Queue()
        loop.add_reader(master_fd, self._on_readable, master_fd, queue, loop)

        output_task = asyncio.create_task(self._pump_output(queue))
        input_task = asyncio.create_task(self._pump_input(master_fd))
        done, _ = await asyncio.wait(
            {output_task, input_task},
            timeout=seconds,
            return_when=asyncio.FIRST_COMPLETED,
        )

        if output_task in done:
            reason = output_task.result()
        elif input_task in done:
            reason = "stopped"
        else:
            reason = "timeout"

        self._kill()
        if output_task not in done:
            try:
                await asyncio.wait_for(output_task, 1)
            except (asyncio.TimeoutError, asyncio.CancelledError):
                output_task.cancel()
        if input_task.done() and not input_task.cancelled():
            if isinstance(input_task.exception(), WebSocketDisconnect):
                self.closed = True
        else:
            input_task.cancel()
        try:
            loop.remove_reader(master_fd)
        except (ValueError, OSError):
            pass

        exit_code = self._exit_code()
        await self._send({"type": "exit", "reason": reason, "exitCode": exit_code})

    def _on_readable(self, master_fd: int, queue: asyncio.Queue, loop) -> None:
        try:
            data = os.read(master_fd, self.READ_CHUNK)
        except OSError:
            data = b""
        if not data:
            try:
                loop.remove_reader(master_fd)
            except (ValueError, OSError):
                pass
            queue.put_nowait(None)
            return
        queue.put_nowait(data)

    async def _pump_output(self, queue: asyncio.Queue) -> str:
        decoder = codecs.getincrementaldecoder("utf-8")("replace")
        total = 0
        while True:
            data = await queue.get()
            if data is None:
                tail = decoder.decode(b"", final=True)
                if tail:
                    await self._send({"type": "output", "data": tail.replace("\r\n", "\n")})
                return "finished"
            total += len(data)
            if total > self.MAX_OUTPUT_BYTES:
                await self._send({"type": "output", "data": "\n[Ispis je predugačak, program je zaustavljen.]\n"})
                return "output_limit"
            text = decoder.decode(data)
            if text:
                await self._send({"type": "output", "data": text.replace("\r\n", "\n")})

    async def _pump_input(self, master_fd: int) -> None:
        while True:
            raw = await self.websocket.receive_text()
            try:
                message = json.loads(raw)
            except ValueError:
                continue
            if not isinstance(message, dict):
                continue
            kind = message.get("type")
            if kind == "stop":
                return
            if kind == "input" and isinstance(message.get("data"), str):
                self._write(master_fd, message["data"][: self.MAX_INPUT_CHARS])
            elif kind == "eof":
                self._write(master_fd, "\x04")

    def _write(self, master_fd: int, text: str) -> None:
        try:
            os.write(master_fd, text.encode("utf-8"))
        except OSError:
            pass

    def _kill(self) -> None:
        if self.process is None or self.process.poll() is not None:
            return
        try:
            os.killpg(self.process.pid, signal.SIGKILL)
        except (ProcessLookupError, PermissionError):
            pass
        try:
            self.process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            pass

    def _exit_code(self) -> Optional[int]:
        if self.process is None:
            return None
        try:
            return self.process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            return None

    def _close_master(self) -> None:
        if self.master_fd is not None:
            try:
                os.close(self.master_fd)
            except OSError:
                pass
            self.master_fd = None

    async def _send(self, message: dict) -> None:
        if self.closed:
            return
        try:
            await self.websocket.send_text(json.dumps(message, ensure_ascii=False))
        except Exception:
            self.closed = True

    async def _close(self) -> None:
        if self.closed:
            return
        self.closed = True
        try:
            await self.websocket.close()
        except Exception:
            pass
