package it.bstz.jsfautoreload.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DevWebSocketServerTest {

    private DevWebSocketServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    void broadcastReloadSendsMessageToConnectedClient() throws Exception {
        int port = findFreePort();
        server = new DevWebSocketServer(port);
        server.startServer();

        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        WebSocketClient client = new WebSocketClient(new URI("ws://localhost:" + port)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
            }

            @Override
            public void onMessage(String message) {
                receivedMessage.set(message);
                messageLatch.countDown();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };

        client.connectBlocking(2, TimeUnit.SECONDS);
        assertTrue(client.isOpen(), "Client should be connected");

        server.broadcastReload();

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS), "Should receive reload message");
        assertEquals("reload", receivedMessage.get());

        client.closeBlocking();
    }

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
