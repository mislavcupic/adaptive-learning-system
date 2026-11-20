package hr.algebra.adaptive.learning.backend.service;

import java.util.function.Consumer;

public interface ConsoleExecutorClient {

    ConsoleConnection connect(String startMessage, Consumer<String> onMessage, Runnable onClosed);

    interface ConsoleConnection {

        void send(String message);

        void close();
    }
}
