package hr.algebra.adaptive.learning.backend.config;

import hr.algebra.adaptive.learning.backend.websocket.ConsoleWebSocketHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WebSocketConfig")
class WebSocketConfigTest {

    @Test
    @DisplayName("registrira konzolu na /api/ws/run")
    void registersConsoleHandler() {
        ConsoleWebSocketHandler handler = mock(ConsoleWebSocketHandler.class);
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(handler, "/api/ws/run")).thenReturn(registration);

        WebSocketConfig config = new WebSocketConfig(handler);
        ReflectionTestUtils.setField(config, "consolePath", "/api/ws/run");
        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/api/ws/run");
        verify(registration).setAllowedOriginPatterns("*");
    }
}