package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.service.ConsoleExecutorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.function.Consumer;

@Slf4j
@Service
public class ConsoleExecutorClientImpl implements ConsoleExecutorClient {

    private final WebSocketClient client;
    private final URI uri;

    @Autowired
    public ConsoleExecutorClientImpl(
            @Value("${code.executor.ws-url:ws://adaptive-code-executor:8001/ws/run}") String url) {
        this(new ReactorNettyWebSocketClient(), url);
    }

    ConsoleExecutorClientImpl(WebSocketClient client, String url) {
        this.client = client;
        this.uri = URI.create(url);
    }

    @Override
    public ConsoleConnection connect(String startMessage, Consumer<String> onMessage, Runnable onClosed) {
        Sinks.Many<String> outgoing = Sinks.many().unicast().onBackpressureBuffer();
        outgoing.tryEmitNext(startMessage);

        Disposable subscription = client.execute(uri, session -> {
                    Mono<Void> send = session.send(outgoing.asFlux().map(session::textMessage));
                    Mono<Void> receive = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(onMessage)
                            .then();
                    return Mono.firstWithSignal(receive, send);
                })
                .doOnError(e -> log.warn("Veza s konzolom izvrsitelja prekinuta: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .doFinally(signal -> onClosed.run())
                .subscribe();

        return new ConsoleConnection() {
            @Override
            public void send(String message) {
                outgoing.tryEmitNext(message);
            }

            @Override
            public void close() {
                outgoing.tryEmitComplete();
                subscription.dispose();
            }
        };
    }
}
