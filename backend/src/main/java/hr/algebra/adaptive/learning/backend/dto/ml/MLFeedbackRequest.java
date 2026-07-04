package hr.algebra.adaptive.learning.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MLFeedbackRequest {
    @JsonProperty("submission_id")
    private String submissionId;

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("task_title")
    private String taskTitle;

    @JsonProperty("language_type")
    private String languageType;

    @JsonProperty("submitted_code")
    private String submittedCode;

    @JsonProperty("compiler_output")
    private String compilerOutput;

    @JsonProperty("execution_output")
    private String executionOutput;

    @JsonProperty("test_results")
    private String testResults;

    @JsonProperty("tests_passed")
    private Integer testsPassed;

    @JsonProperty("tests_total")
    private Integer testsTotal;

    @JsonProperty("research_group")
    private String researchGroup;
}