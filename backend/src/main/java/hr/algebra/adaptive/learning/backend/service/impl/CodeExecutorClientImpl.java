package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionRequest;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import hr.algebra.adaptive.learning.backend.service.CodeExecutorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class CodeExecutorClientImpl implements CodeExecutorClient {

    private final WebClient webClient;

    public CodeExecutorClientImpl(
            WebClient.Builder webClientBuilder,
            @Value("${code.executor.url:http://adaptive-code-executor:8001}") String codeExecutorUrl) {
        this.webClient = webClientBuilder.baseUrl(codeExecutorUrl).build();
        log.info("CodeExecutorClient initialized with URL: {}", codeExecutorUrl);
    }

    @Override
    public CodeExecutionResponse execute(CodeExecutionRequest request) {
        try {
            return webClient.post()
                    .uri("/api/execute")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CodeExecutionResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Code Executor error: {}", e.getMessage());
            CodeExecutionResponse fallback = new CodeExecutionResponse();
            fallback.setSuccess(false);
            fallback.setError("Code Executor nedostupan: " + e.getMessage());
            return fallback;
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception _) {
            return false;
        }
    }
}