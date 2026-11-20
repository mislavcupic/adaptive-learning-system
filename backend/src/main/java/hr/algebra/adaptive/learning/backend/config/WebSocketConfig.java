package hr.algebra.adaptive.learning.backend.config;

import hr.algebra.adaptive.learning.backend.websocket.ConsoleWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String CONSOLE_PATH = "/api/ws/run";

    private final ConsoleWebSocketHandler consoleWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(consoleWebSocketHandler, CONSOLE_PATH)
                .setAllowedOriginPatterns("*");
    }
}
