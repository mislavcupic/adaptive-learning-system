import os
import subprocess
import shutil
import time
import uuid
from typing import Optional
from .schemas import LanguageType, ExecutionRequest, ExecutionResponse


class CodeExecutor:

    TEMP_DIR = "/tmp/code-execution"

    def __init__(self):
        os.makedirs(self.TEMP_DIR, exist_ok=True)

    def execute(self, request: ExecutionRequest) -> ExecutionResponse:
        execution_id = str(uuid.uuid4())
        work_dir = os.path.join(self.TEMP_DIR, execution_id)
        os.makedirs(work_dir, exist_ok=True)

        try:
            if request.language == LanguageType.C:
                return self._execute_c(request, work_dir)
            elif request.language == LanguageType.CSHARP:
                return self._execute_csharp(request, work_dir)
            elif request.language == LanguageType.PYTHON:
                return self._execute_python(request, work_dir)
            else:
                return ExecutionResponse(
                    success=False,
                    error=f"Nepodržani jezik: {request.language}"
                )
        finally:
            shutil.rmtree(work_dir, ignore_errors=True)

    def _execute_c(self, request: ExecutionRequest, work_dir: str) -> ExecutionResponse:
        source_file = os.path.join(work_dir, "main.c")
        executable = os.path.join(work_dir, "main")

        with open(source_file, "w") as f:
            f.write(request.code)

        compile_result = subprocess.run(
            ["gcc", "-o", executable, source_file, "-Wall", "-Wextra"],
            capture_output=True,
            text=True,
            timeout=30
        )

        compiler_output = compile_result.stderr or compile_result.stdout

        if compile_result.returncode != 0:
            return ExecutionResponse(
                success=False,
                compilerOutput=compiler_output,
                error="Greška prilikom kompilacije"
            )

        tests_passed = 0
        tests_total = len(request.testCases)
        test_results = []
        execution_output = ""

        start_time = time.time()

        for i, test_case in enumerate(request.testCases):
            try:
                result = subprocess.run(
                    [executable],
                    input=test_case.input,
                    capture_output=True,
                    text=True,
                    timeout=request.timeoutSeconds
                )

                actual_output = result.stdout.strip()
                expected_output = test_case.expectedOutput.strip()

                passed = actual_output == expected_output
                if passed:
                    tests_passed += 1

                test_results.append(
                    f"Test {i+1}: {'PASSED' if passed else 'FAILED'}\n"
                    f"  Input: {test_case.input.strip()}\n"
                    f"  Expected: {expected_output}\n"
                    f"  Actual: {actual_output}"
                )

                if i == 0:
                    execution_output = actual_output

            except subprocess.TimeoutExpired:
                test_results.append(f"Test {i+1}: TIMEOUT")
            except Exception as e:
                test_results.append(f"Test {i+1}: ERROR - {str(e)}")

        execution_time = int((time.time() - start_time) * 1000)

        valgrind_output = None
        if request.testCases:
            valgrind_output = self._run_valgrind(executable, request.testCases[0].input)

        return ExecutionResponse(
            success=True,
            compilerOutput=compiler_output if compiler_output else "Kompilacija uspješna.",
            executionOutput=execution_output,
            testsPassed=tests_passed,
            testsTotal=tests_total,
            testResults="\n\n".join(test_results),
            valgrindOutput=valgrind_output,
            executionTimeMs=execution_time
        )

    def _execute_csharp(self, request: ExecutionRequest, work_dir: str) -> ExecutionResponse:
        project_dir = os.path.join(work_dir, "CSharpProject")
        os.makedirs(project_dir, exist_ok=True)

        csproj_content = """<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>Exe</OutputType>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>
</Project>"""

        with open(os.path.join(project_dir, "CSharpProject.csproj"), "w") as f:
            f.write(csproj_content)

        with open(os.path.join(project_dir, "Program.cs"), "w") as f:
            f.write(request.code)

        compile_result = subprocess.run(
            ["dotnet", "build", "--configuration", "Release", "--verbosity", "quiet"],
            cwd=project_dir,
            capture_output=True,
            text=True,
            timeout=60
        )

        compiler_output = compile_result.stderr or compile_result.stdout

        if compile_result.returncode != 0:
            return ExecutionResponse(
                success=False,
                compilerOutput=compiler_output,
                error="Greška prilikom kompilacije"
            )

        tests_passed = 0
        tests_total = len(request.testCases)
        test_results = []
        execution_output = ""

        start_time = time.time()

        for i, test_case in enumerate(request.testCases):
            try:
                result = subprocess.run(
                    ["dotnet", "run", "--configuration", "Release", "--no-build"],
                    cwd=project_dir,
                    input=test_case.input,
                    capture_output=True,
                    text=True,
                    timeout=request.timeoutSeconds
                )

                actual_output = result.stdout.strip()
                expected_output = test_case.expectedOutput.strip()

                passed = actual_output == expected_output
                if passed:
                    tests_passed += 1

                test_results.append(
                    f"Test {i+1}: {'PASSED' if passed else 'FAILED'}\n"
                    f"  Input: {test_case.input.strip()}\n"
                    f"  Expected: {expected_output}\n"
                    f"  Actual: {actual_output}"
                )

                if i == 0:
                    execution_output = actual_output

            except subprocess.TimeoutExpired:
                test_results.append(f"Test {i+1}: TIMEOUT")
            except Exception as e:
                test_results.append(f"Test {i+1}: ERROR - {str(e)}")

        execution_time = int((time.time() - start_time) * 1000)

        return ExecutionResponse(
            success=True,
            compilerOutput="Kompilacija uspješna.",
            executionOutput=execution_output,
            testsPassed=tests_passed,
            testsTotal=tests_total,
            testResults="\n\n".join(test_results),
            executionTimeMs=execution_time
        )

    def _execute_python(self, request: ExecutionRequest, work_dir: str) -> ExecutionResponse:
        """Izvršava Python kod."""
        source_file = os.path.join(work_dir, "main.py")

        with open(source_file, "w") as f:
            f.write(request.code)

        # Python nema kompilaciju, ali provjerimo sintaksu
        syntax_check = subprocess.run(
            ["python3", "-m", "py_compile", source_file],
            capture_output=True,
            text=True,
            timeout=10
        )

        if syntax_check.returncode != 0:
            return ExecutionResponse(
                success=False,
                compilerOutput=syntax_check.stderr,
                error="Sintaksna greška u Python kodu"
            )

        tests_passed = 0
        tests_total = len(request.testCases)
        test_results = []
        execution_output = ""

        start_time = time.time()

        for i, test_case in enumerate(request.testCases):
            try:
                result = subprocess.run(
                    ["python3", source_file],
                    input=test_case.input,
                    capture_output=True,
                    text=True,
                    timeout=request.timeoutSeconds
                )

                actual_output = result.stdout.strip()
                expected_output = test_case.expectedOutput.strip()

                # Provjeri stderr za runtime greške
                if result.stderr:
                    test_results.append(
                        f"Test {i+1}: ERROR\n"
                        f"  Input: {test_case.input.strip()}\n"
                        f"  Error: {result.stderr.strip()}"
                    )
                    if i == 0:
                        execution_output = result.stderr.strip()
                    continue

                passed = actual_output == expected_output
                if passed:
                    tests_passed += 1

                test_results.append(
                    f"Test {i+1}: {'PASSED' if passed else 'FAILED'}\n"
                    f"  Input: {test_case.input.strip()}\n"
                    f"  Expected: {expected_output}\n"
                    f"  Actual: {actual_output}"
                )

                if i == 0:
                    execution_output = actual_output

            except subprocess.TimeoutExpired:
                test_results.append(f"Test {i+1}: TIMEOUT")
            except Exception as e:
                test_results.append(f"Test {i+1}: ERROR - {str(e)}")

        execution_time = int((time.time() - start_time) * 1000)

        return ExecutionResponse(
            success=True,
            compilerOutput="Python - nema kompilacije, sintaksa OK.",
            executionOutput=execution_output,
            testsPassed=tests_passed,
            testsTotal=tests_total,
            testResults="\n\n".join(test_results),
            executionTimeMs=execution_time
        )

    def _run_valgrind(self, executable: str, input_data: str) -> Optional[str]:
        try:
            result = subprocess.run(
                ["valgrind", "--leak-check=full", "--error-exitcode=1", executable],
                input=input_data,
                capture_output=True,
                text=True,
                timeout=30
            )
            return result.stderr
        except Exception as e:
            return f"Valgrind error: {str(e)}"
    