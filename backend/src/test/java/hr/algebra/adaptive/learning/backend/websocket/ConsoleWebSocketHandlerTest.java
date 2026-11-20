package hr.algebra.adaptive.learning.backend.websocket;

import hr.algebra.adaptive.learning.backend.service.ConsoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("ConsoleWebSocketHandler")
class ConsoleWebSocketHandlerTest {

    private final ConsoleService consoleService = mock(ConsoleService.class);
    private final WebSocketSession session = mock(WebSocketSession.class);
    private final ConsoleWebSocketHandler handler = new ConsoleWebSocketHandler(consoleService);

    @Test
    @DisplayName("tekstualnu poruku predaje servisu")
    void delegatesMessages() {
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"stop\"}"));

        verify(consoleService).onMessage(session, "{\"type\":\"stop\"}");
    }

    @Test
    @DisplayName("zatvaranje veze javlja servisu")
    void delegatesClose() {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(consoleService).onClose(session);
    }

    @Test
    @DisplayName("greska prijenosa zatvara sesiju u servisu")
    void delegatesTransportError() {
        handler.handleTransportError(session, new IOException("prekid"));

        verify(consoleService).onClose(session);
    }
}
