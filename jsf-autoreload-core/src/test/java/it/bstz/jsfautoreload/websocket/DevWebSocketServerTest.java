package it.bstz.jsfautoreload.websocket;

import it.bstz.jsfautoreload.JsfAutoreloadException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevWebSocketServerTest {

    private DevWebSocketServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void broadcastReloadSendsMessageToConnectedClient() throws Exception {
        int port = findFreePort();
        server = new DevWebSocketServer(port);
        server.startServer();

        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        WebSocketClient client = createClient(port, messageLatch, receivedMessage);
        client.connectBlocking(2, TimeUnit.SECONDS);
        assertTrue(client.isOpen(), "Client should be connected");

        server.broadcastReload();

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS), "Should receive reload message");
        assertEquals("reload", receivedMessage.get());

        client.closeBlocking();
    }

    @Test
    void portInUseThrowsJsfAutoreloadException() throws Exception {
        int port = findFreePort();
        server = new DevWebSocketServer(port);
        server.startServer();

        DevWebSocketServer secondServer = new DevWebSocketServer(port);
        JsfAutoreloadException exception = assertThrows(JsfAutoreloadException.class,
                secondServer::startServer);
        assertTrue(exception.getMessage().contains(String.valueOf(port)));
        assertTrue(exception.getMessage().contains("[JSF Autoreload]"));
    }

    @Test
    void clientDisconnectionAndReconnection() throws Exception {
        int port = findFreePort();
        server = new DevWebSocketServer(port);
        server.startServer();

        // First connection
        CountDownLatch firstLatch = new CountDownLatch(1);
        WebSocketClient firstClient = createClient(port, firstLatch, new AtomicReference<>());
        firstClient.connectBlocking(2, TimeUnit.SECONDS);
        assertTrue(firstClient.isOpen());
        firstClient.closeBlocking();

        // Reconnection
        CountDownLatch secondLatch = new CountDownLatch(1);
        AtomicReference<String> secondMessage = new AtomicReference<>();
        WebSocketClient secondClient = createClient(port, secondLatch, secondMessage);
        secondClient.connectBlocking(2, TimeUnit.SECONDS);
        assertTrue(secondClient.isOpen(), "Reconnected client should be connected");

        server.broadcastReload();
        assertTrue(secondLatch.await(2, TimeUnit.SECONDS), "Reconnected client should receive message");
        assertEquals("reload", secondMessage.get());

        secondClient.closeBlocking();
    }

    @Test
    void closeReleasesPort() throws Exception {
        int port = findFreePort();
        server = new DevWebSocketServer(port);
        server.startServer();
        server.close();

        // Should be able to rebind
        DevWebSocketServer secondServer = new DevWebSocketServer(port);
        secondServer.startServer();
        secondServer.close();
        server = null; // already closed
    }

    @Test
    void startupCompletesWithin3Seconds() throws Exception {
        int port = findFreePort();
        server = new DevWebSocketServer(port);

        long start = System.currentTimeMillis();
        server.startServer();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 3000, "Startup should complete in under 3 seconds, took " + elapsed + "ms");
    }

    @Test
    void closeIsIdempotent() throws Exception {
        int port = findFreePort();
        server = new DevWebSocketServer(port);
        server.startServer();
        server.close();
        server.close(); // second close should not throw
        server = null;
    }

    private WebSocketClient createClient(int port, CountDownLatch messageLatch,
                                          AtomicReference<String> receivedMessage) throws Exception {
        return new WebSocketClient(new URI("ws://localhost:" + port)) {
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
    }

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
