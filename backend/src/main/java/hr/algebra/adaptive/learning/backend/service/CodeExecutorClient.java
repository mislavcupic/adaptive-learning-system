package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionRequest;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;

public interface CodeExecutorClient {
    CodeExecutionResponse execute(CodeExecutionRequest request);
    boolean isHealthy();
}