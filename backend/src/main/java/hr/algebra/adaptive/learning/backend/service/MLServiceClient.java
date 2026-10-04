package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.ml.*;


public interface MLServiceClient {

    MLFeedbackResponse generateFeedback(MLFeedbackRequest request);

    boolean isHealthy();

    MLAncovaResponse runAncova(MLAncovaRequest request);
    MlBktResponse updateBkt(MlBktRequest request);
}