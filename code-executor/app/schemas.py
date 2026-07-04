from pydantic import BaseModel
from typing import Optional
from enum import Enum


class LanguageType(str, Enum):
    C = "C"
    CSHARP = "CSHARP"
    PYTHON = "PYTHON"


class TestCase(BaseModel):
    input: str
    expectedOutput: str


class ExecutionRequest(BaseModel):
    code: str
    language: LanguageType
    testCases: list[TestCase] = []
    timeoutSeconds: int = 10
    memoryLimitMb: int = 256


class ExecutionResponse(BaseModel):
    success: bool
    compilerOutput: Optional[str] = None
    executionOutput: Optional[str] = None
    testsPassed: int = 0
    testsTotal: int = 0
    testResults: Optional[str] = None
    valgrindOutput: Optional[str] = None
    executionTimeMs: Optional[int] = None
    memoryUsedKb: Optional[int] = None
    error: Optional[str] = None