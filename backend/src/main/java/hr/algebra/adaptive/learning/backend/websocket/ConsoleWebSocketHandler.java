package hr.algebra.adaptive.learning.backend.websocket;

import hr.algebra.adaptive.learning.backend.service.ConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class ConsoleWebSocketHandler extends TextWebSocketHandler {

    private final ConsoleService consoleService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        consoleService.onMessage(session, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        consoleService.onClose(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        consoleService.onClose(session);
    }
}
