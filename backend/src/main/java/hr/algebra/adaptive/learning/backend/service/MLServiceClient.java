package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackResponse;

public interface MLServiceClient {

    MLFeedbackResponse generateFeedback(MLFeedbackRequest request);

    boolean isHealthy();
}
