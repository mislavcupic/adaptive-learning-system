package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.dto.ml.*;
import hr.algebra.adaptive.learning.backend.service.MLServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Map;

import static org.springframework.web.reactive.function.client.WebClient.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MLServiceClientImpl implements MLServiceClient {

    private final Builder webClientBuilder;

    @Value("${ml.service.url:http://adaptive-ml:8000}")
    private String mlServiceUrl;

    @Override
    public MLFeedbackResponse generateFeedback(MLFeedbackRequest request) {
        log.info("Calling ML service for submission: {}", request.getSubmissionId());

        try {
            MLFeedbackResponse response = webClientBuilder.build()
                    .post()
                    .uri(mlServiceUrl + "/api/feedback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MLFeedbackResponse.class)
                    .block();

            log.info("ML service returned feedback for submission: {}", request.getSubmissionId());
            return response;

        } catch (Exception e) {
            log.error("Error calling ML service: {}", e.getMessage());
            return MLFeedbackResponse.builder()
                    .submissionId(request.getSubmissionId())
                    .aiFeedback("Automatski feedback trenutno nije dostupan.")
                    .aiScore(0)
                    .skillsUpdated(new ArrayList<>())
                    .build();
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            Map<String, Object> health = webClientBuilder.build()
                    .get()
                    .uri(mlServiceUrl + "/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return health != null && "healthy".equals(health.get("status"));
        } catch (Exception e) {
            log.warn("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public MLAncovaResponse runAncova(MLAncovaRequest request) {
        log.info("Calling ML service for ANCOVA with {} records",
                request.getRecords() != null ? request.getRecords().size() : 0);

        return webClientBuilder.build()
                .post()
                .uri(mlServiceUrl + "/api/statistics/ancova")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MLAncovaResponse.class)
                .block();
    }

    @Override
    public MlBktResponse updateBkt(MlBktRequest request) {
        log.info("Calling ML service for BKT update: student {}, skill {}",
                request.getStudentId(), request.getSkillName());

        try {
            MlBktResponse response = webClientBuilder.build()
                    .post()
                    .uri(mlServiceUrl + "/api/bkt/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MlBktResponse.class)
                    .block();

            if (response != null) {
                log.info("BKT mastery: {} -> {}",
                        response.getPreviousLevel(), response.getMasteryLevel());
            }
            return response;

        } catch (Exception e) {
            // Neuspjeh ne smije srusiti predaju koja je vec spremljena
            log.error("BKT update failed: {}", e.getMessage());
            return null;
        }
    }
}