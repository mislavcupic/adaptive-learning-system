package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.service.ConsoleExecutorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ConsoleExecutorClientImpl")
class ConsoleExecutorClientImplTest {

    private static final String URL = "ws://executor-test:8001/ws/run";

    private final WebSocketClient client = mock(WebSocketClient.class);
    private final WebSocketSession executorSession = mock(WebSocketSession.class);
    private final List<String> sentToExecutor = new CopyOnWriteArrayList<>();
    private final List<String> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger closed = new AtomicInteger();

    private static WebSocketMessage message(String text) {
        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn(text);
        return message;
    }

    @BeforeEach
    void setUp() {
        when(executorSession.textMessage(anyString())).thenAnswer(inv -> message(inv.getArgument(0)));
        when(executorSession.send(any())).thenAnswer(inv -> {
            Publisher<WebSocketMessage> outgoing = inv.getArgument(0);
            return Flux.from(outgoing).doOnNext(m -> sentToExecutor.add(m.getPayloadAsText())).then();
        });
        when(client.execute(any(URI.class), any(WebSocketHandler.class)))
                .thenAnswer(inv -> inv.<WebSocketHandler>getArgument(1).handle(executorSession));
    }

    private ConsoleExecutorClient.ConsoleConnection connect() {
        return new ConsoleExecutorClientImpl(client, URL)
                .connect("{\"type\":\"start\"}", received::add, closed::incrementAndGet);
    }

    @Test
    @DisplayName("salje pocetnu poruku i kasniji unos izvrsitelju")
    void sendsStartAndInput() {
        when(executorSession.receive()).thenReturn(Flux.never());

        ConsoleExecutorClient.ConsoleConnection connection = connect();
        connection.send("{\"type\":\"input\",\"data\":\"5\\n\"}");

        assertThat(sentToExecutor).containsExactly("{\"type\":\"start\"}", "{\"type\":\"input\",\"data\":\"5\\n\"}");

        connection.close();
        assertThat(closed.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("poruke izvrsitelja predaje pozivatelju i javlja kraj")
    void relaysExecutorMessages() {
        WebSocketMessage output = message("{\"type\":\"output\",\"data\":\"Unesi broj: \"}");
        WebSocketMessage exit = message("{\"type\":\"exit\",\"reason\":\"finished\"}");
        when(executorSession.receive()).thenReturn(Flux.just(output, exit));

        connect();

        assertThat(received).containsExactly(
                "{\"type\":\"output\",\"data\":\"Unesi broj: \"}",
                "{\"type\":\"exit\",\"reason\":\"finished\"}");
        assertThat(closed.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("nedostupan izvrsitelj ne baca iznimku nego javlja kraj")
    void handlesConnectionFailure() {
        doReturn(Mono.error(new IllegalStateException("connection refused")))
                .when(client).execute(any(URI.class), any(WebSocketHandler.class));

        assertThatCode(this::connect).doesNotThrowAnyException();
        assertThat(closed.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("javni konstruktor ne otvara vezu")
    void publicConstructorDoesNotConnect() {
        assertThatCode(() -> new ConsoleExecutorClientImpl(URL)).doesNotThrowAnyException();
    }
}