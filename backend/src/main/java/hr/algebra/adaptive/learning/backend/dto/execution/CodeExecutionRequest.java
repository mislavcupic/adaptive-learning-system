package hr.algebra.adaptive.learning.backend.dto.execution;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CodeExecutionRequest {
    private String code;
    private String language;
    private List<TestCase> testCases;
    private Integer timeoutSeconds;

    @Data
    @Builder
    public static class TestCase {
        private String input;
        private String expectedOutput;
    }
}
