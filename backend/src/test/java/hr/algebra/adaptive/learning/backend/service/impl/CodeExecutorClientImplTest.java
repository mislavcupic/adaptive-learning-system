package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionRequest;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodeExecutorClientImpl")
class CodeExecutorClientImplTest {

    private static final String BASE_URL = "http://executor-test:8001";

    private final AtomicReference<ClientRequest> sent = new AtomicReference<>();

    @Nested
    @DisplayName("Izvrsavanje koda")
    class Execute {

        @Test
        @DisplayName("salje zahtjev na rutu izvrsavanja")
        void callsExecuteEndpoint() {
            CodeExecutorClientImpl client = clientReturning(HttpStatus.OK, "{}");

            client.execute(request());

            assertThat(sent.get().method()).isEqualTo(HttpMethod.POST);
            assertThat(sent.get().url()).hasToString(BASE_URL + "/api/execute");        }

        @Test
        @DisplayName("vraca odgovor izvrsitelja")
        void returnsResponse() {
            CodeExecutorClientImpl client = clientReturning(HttpStatus.OK,
                    "{\"success\":true,\"testsPassed\":3,\"testsTotal\":3}");

            CodeExecutionResponse response = client.execute(request());

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getTestsPassed()).isEqualTo(3);
        }

        @Test
        @DisplayName("kad je izvrsitelj nedostupan vraca odgovor s greskom")
        void fallsBackOnFailure() {
            CodeExecutorClientImpl client = clientFailing("connection refused");

            CodeExecutionResponse response = client.execute(request());

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError()).contains("Code Executor nedostupan");
        }

        @Test
        @DisplayName("neuspjeh ne izbacuje iznimku prema pozivatelju")
        void doesNotThrow() {
            CodeExecutorClientImpl client = clientFailing("timeout");

            assertThat(client.execute(request())).isNotNull();
        }
    }

    @Nested
    @DisplayName("Provjera dostupnosti")
    class Health {

        @Test
        @DisplayName("odgovor na health znaci da je dostupan")
        void healthyWhenEndpointResponds() {
            CodeExecutorClientImpl client = clientReturning(HttpStatus.OK, "ok");

            assertThat(client.isHealthy()).isTrue();
            assertThat(sent.get().url()).hasToString(BASE_URL + "/health");        }

        @Test
        @DisplayName("greska na health znaci da nije dostupan")
        void unhealthyWhenCallFails() {
            CodeExecutorClientImpl client = clientFailing("host unreachable");

            assertThat(client.isHealthy()).isFalse();
        }
    }

    private CodeExecutionRequest request() {
        return CodeExecutionRequest.builder()
                .code("int main(void) { return 0; }")
                .language("C")
                .timeoutSeconds(5)
                .build();
    }

    private CodeExecutorClientImpl clientReturning(HttpStatus status, String body) {
        return build(request -> {
            sent.set(request);
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        });
    }

    private CodeExecutorClientImpl clientFailing(String message) {
        return build(request -> {
            sent.set(request);
            return Mono.error(new RuntimeException(message));
        });
    }

    private CodeExecutorClientImpl build(ExchangeFunction exchangeFunction) {
        return new CodeExecutorClientImpl(
                WebClient.builder().exchangeFunction(exchangeFunction),
                BASE_URL);
    }
}