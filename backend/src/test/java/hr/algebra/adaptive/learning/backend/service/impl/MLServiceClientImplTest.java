package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.dto.ml.MLAncovaRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackResponse;
import hr.algebra.adaptive.learning.backend.dto.ml.MlBktRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MlBktResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MLServiceClientImpl")
class MLServiceClientImplTest {

    private static final String ML_URL = "http://ml-test:8000";

    private final AtomicReference<ClientRequest> sent = new AtomicReference<>();

    @Nested
    @DisplayName("Generiranje povratne informacije")
    class Feedback {

        @Test
        @DisplayName("salje zahtjev na rutu za feedback")
        void callsFeedbackEndpoint() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK, "{}");

            client.generateFeedback(feedbackRequest());

            assertThat(sent.get().method()).isEqualTo(HttpMethod.POST);
            assertThat(sent.get().url().toString()).isEqualTo(ML_URL + "/api/feedback");
        }

        @Test
        @DisplayName("vraca odgovor ML servisa")
        void returnsResponse() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK, "{}");

            assertThat(client.generateFeedback(feedbackRequest())).isNotNull();
        }

        @Test
        @DisplayName("kad ML servis padne vraca zamjenski feedback")
        void fallsBackOnFailure() {
            MLServiceClientImpl client = clientFailing("ML servis ne odgovara");
            MLFeedbackRequest request = feedbackRequest();

            MLFeedbackResponse response = client.generateFeedback(request);

            assertThat(response).isNotNull();
            assertThat(response.getSubmissionId()).isEqualTo(request.getSubmissionId());
            assertThat(response.getAiScore()).isZero();
            assertThat(response.getAiFeedback()).contains("nije dostupan");
            assertThat(response.getSkillsUpdated()).isEmpty();
        }

        @Test
        @DisplayName("neuspjeh ne izbacuje iznimku prema pozivatelju")
        void doesNotThrow() {
            MLServiceClientImpl client = clientFailing("timeout");

            assertThat(client.generateFeedback(feedbackRequest())).isNotNull();
        }
    }

    @Nested
    @DisplayName("Provjera dostupnosti")
    class Health {

        @Test
        @DisplayName("status healthy znaci da je servis dostupan")
        void healthyStatus() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK, "{\"status\":\"healthy\"}");

            assertThat(client.isHealthy()).isTrue();
            assertThat(sent.get().url().toString()).isEqualTo(ML_URL + "/health");
        }

        @Test
        @DisplayName("drugi status znaci da servis nije dostupan")
        void otherStatus() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK, "{\"status\":\"degraded\"}");

            assertThat(client.isHealthy()).isFalse();
        }

        @Test
        @DisplayName("odgovor bez statusa znaci da servis nije dostupan")
        void missingStatus() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK, "{}");

            assertThat(client.isHealthy()).isFalse();
        }

        @Test
        @DisplayName("greska na provjeri znaci da servis nije dostupan")
        void failedCheck() {
            MLServiceClientImpl client = clientFailing("connection refused");

            assertThat(client.isHealthy()).isFalse();
        }
    }

    @Nested
    @DisplayName("ANCOVA")
    class Ancova {

        @Test
        @DisplayName("salje zahtjev na statisticku rutu")
        void callsAncovaEndpoint() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK, "{}");

            client.runAncova(MLAncovaRequest.builder().build());

            assertThat(sent.get().method()).isEqualTo(HttpMethod.POST);
            assertThat(sent.get().url().toString()).isEqualTo(ML_URL + "/api/statistics/ancova");
        }

        @Test
        @DisplayName("greska se propusta pozivatelju")
        void propagatesFailure() {
            MLServiceClientImpl client = clientFailing("statsmodels pukao");

            assertThatThrownBy(() -> client.runAncova(MLAncovaRequest.builder().build()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("BKT azuriranje")
    class Bkt {

        @Test
        @DisplayName("salje zahtjev na rutu za BKT")
        void callsBktEndpoint() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK, "{}");

            client.updateBkt(MlBktRequest.builder().build());

            assertThat(sent.get().method()).isEqualTo(HttpMethod.POST);
            assertThat(sent.get().url().toString()).isEqualTo(ML_URL + "/api/bkt/update");
        }

        @Test
        @DisplayName("vraca procjenu znanja")
        void returnsMastery() {
            MLServiceClientImpl client = clientReturning(HttpStatus.OK,
                    "{\"masteryLevel\":0.68,\"previousLevel\":0.3}");

            MlBktResponse response = client.updateBkt(MlBktRequest.builder().build());

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("neuspjeh vraca null i ne rusi predaju")
        void returnsNullOnFailure() {
            MLServiceClientImpl client = clientFailing("ML servis ne odgovara");

            assertThat(client.updateBkt(MlBktRequest.builder().build())).isNull();
        }
    }

    private MLFeedbackRequest feedbackRequest() {
        return MLFeedbackRequest.builder()
                .submissionId(UUID.randomUUID().toString())
                .build();
    }

    private MLServiceClientImpl clientReturning(HttpStatus status, String body) {
        return build(request -> {
            sent.set(request);
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        });
    }

    private MLServiceClientImpl clientFailing(String message) {
        return build(request -> {
            sent.set(request);
            return Mono.error(new RuntimeException(message));
        });
    }

    private MLServiceClientImpl build(ExchangeFunction exchangeFunction) {
        MLServiceClientImpl client =
                new MLServiceClientImpl(WebClient.builder().exchangeFunction(exchangeFunction));
        ReflectionTestUtils.setField(client, "mlServiceUrl", ML_URL);
        return client;
    }
}
