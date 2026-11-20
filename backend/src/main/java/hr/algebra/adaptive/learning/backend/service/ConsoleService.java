package hr.algebra.adaptive.learning.backend.service;

import org.springframework.web.socket.WebSocketSession;

public interface ConsoleService {

    void onMessage(WebSocketSession session, String payload);

    void onClose(WebSocketSession session);
}
