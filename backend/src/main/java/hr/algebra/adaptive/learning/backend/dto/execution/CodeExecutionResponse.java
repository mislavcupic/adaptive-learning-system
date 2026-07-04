package hr.algebra.adaptive.learning.backend.dto.execution;

import lombok.Data;

@Data
public class CodeExecutionResponse {
    private boolean success;
    private String compilerOutput;
    private String executionOutput;
    private int testsPassed;
    private int testsTotal;
    private String testResults;
    private String valgrindOutput;
    private Integer executionTimeMs;
    private String error;
}