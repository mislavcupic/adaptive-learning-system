import json

import pytest
from fastapi.testclient import TestClient

from app.interactive import InteractiveSession
from app.main import app


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setattr(InteractiveSession, "TEMP_DIR", str(tmp_path))
    return TestClient(app)


def start(ws, code, language="C", seconds=10):
    ws.send_text(json.dumps({"type": "start", "code": code, "language": language, "timeoutSeconds": seconds}))


def read_until(ws, predicate):
    messages = []
    while True:
        message = json.loads(ws.receive_text())
        messages.append(message)
        if predicate(message, messages):
            return messages


def output_of(messages):
    return "".join(m["data"] for m in messages if m["type"] == "output")


def finished(message, _):
    return message["type"] in ("exit", "error", "compile_error")


C_SQUARE = """#include <stdio.h>
int main(void) {
    int n;
    printf("Unesi broj: ");
    scanf("%d", &n);
    printf("Kvadrat je %d\\n", n * n);
    return 0;
}
"""


def test_prompt_is_shown_before_input_and_program_continues(client):
    with client.websocket_connect("/ws/run") as ws:
        start(ws, C_SQUARE)
        before = read_until(ws, lambda m, ms: "Unesi broj: " in output_of(ms))
        assert any(m == {"type": "status", "state": "compiling"} for m in before)
        assert any(m.get("state") == "running" for m in before)

        ws.send_text(json.dumps({"type": "input", "data": "5\n"}))
        after = read_until(ws, finished)

    assert "Kvadrat je 25" in output_of(after)
    assert after[-1] == {"type": "exit", "reason": "finished", "exitCode": 0}


def test_compile_error_is_reported_without_temp_paths(client):
    with client.websocket_connect("/ws/run") as ws:
        start(ws, "int main(void) { return 0 }")
        messages = read_until(ws, finished)

    assert messages[-1]["type"] == "compile_error"
    assert "error" in messages[-1]["output"]
    assert "/tmp" not in messages[-1]["output"]
    assert "main.c" in messages[-1]["output"]


def test_session_stops_after_time_limit(client):
    with client.websocket_connect("/ws/run") as ws:
        start(ws, "int main(void) { for (;;) {} }", seconds=1)
        messages = read_until(ws, finished)

    assert messages[-1]["type"] == "exit"
    assert messages[-1]["reason"] == "timeout"


def test_stop_message_ends_program(client):
    with client.websocket_connect("/ws/run") as ws:
        start(ws, C_SQUARE)
        read_until(ws, lambda m, ms: "Unesi broj: " in output_of(ms))
        ws.send_text(json.dumps({"type": "stop"}))
        messages = read_until(ws, finished)

    assert messages[-1]["type"] == "exit"
    assert messages[-1]["reason"] == "stopped"


def test_eof_ends_reading(client):
    code = """#include <stdio.h>
int main(void) {
    int x, count = 0;
    while (scanf("%d", &x) == 1) count++;
    printf("Ucitano: %d\\n", count);
    return 0;
}
"""
    with client.websocket_connect("/ws/run") as ws:
        start(ws, code)
        read_until(ws, lambda m, _: m.get("state") == "running")
        ws.send_text(json.dumps({"type": "input", "data": "4 8\n15\n"}))
        ws.send_text(json.dumps({"type": "eof"}))
        messages = read_until(ws, finished)

    assert "Ucitano: 3" in output_of(messages)
    assert messages[-1]["reason"] == "finished"


def test_python_program_runs_interactively(client):
    code = 'ime = input("Kako se zoves? ")\nprint("Bok, " + ime)\n'
    with client.websocket_connect("/ws/run") as ws:
        start(ws, code, language="PYTHON")
        read_until(ws, lambda m, ms: "Kako se zoves? " in output_of(ms))
        ws.send_text(json.dumps({"type": "input", "data": "Ana\n"}))
        messages = read_until(ws, finished)

    assert "Bok, Ana" in output_of(messages)
    assert messages[-1]["reason"] == "finished"


def test_python_syntax_error_is_reported(client):
    with client.websocket_connect("/ws/run") as ws:
        start(ws, "print(\n", language="PYTHON")
        messages = read_until(ws, finished)

    assert messages[-1]["type"] == "compile_error"


def test_output_limit_stops_program(client, monkeypatch):
    monkeypatch.setattr(InteractiveSession, "MAX_OUTPUT_BYTES", 2000)
    code = '#include <stdio.h>\nint main(void) { for (;;) printf("x"); }\n'
    with client.websocket_connect("/ws/run") as ws:
        start(ws, code)
        messages = read_until(ws, finished)

    assert messages[-1]["type"] == "exit"
    assert messages[-1]["reason"] == "output_limit"


def test_csharp_is_rejected(client):
    with client.websocket_connect("/ws/run") as ws:
        start(ws, "class P {}", language="CSHARP")
        messages = read_until(ws, finished)

    assert messages[-1]["type"] == "error"


def test_invalid_start_is_rejected(client):
    with client.websocket_connect("/ws/run") as ws:
        ws.send_text(json.dumps({"type": "input", "data": "5\n"}))
        messages = read_until(ws, finished)

    assert messages[-1]["type"] == "error"


def test_session_seconds_are_capped():
    session = InteractiveSession(websocket=None)
    assert session._session_seconds({"timeoutSeconds": 9999}) == InteractiveSession.MAX_SECONDS
    assert session._session_seconds({"timeoutSeconds": "x"}) == InteractiveSession.DEFAULT_SECONDS
    assert session._session_seconds({"timeoutSeconds": 0}) == 1
