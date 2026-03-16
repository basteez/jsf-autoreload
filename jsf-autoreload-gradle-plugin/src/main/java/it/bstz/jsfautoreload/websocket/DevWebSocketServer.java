package it.bstz.jsfautoreload.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DevWebSocketServer extends WebSocketServer {

    private static final Logger LOGGER = Logger.getLogger(DevWebSocketServer.class.getName());

    private final int port;
    private final CountDownLatch startLatch = new CountDownLatch(1);
    private volatile Exception startError;

    public DevWebSocketServer(int port) {
        super(new InetSocketAddress("localhost", port));
        this.port = port;
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn == null) {
            startError = ex;
            startLatch.countDown();
        }
        LOGGER.log(Level.WARNING, "WebSocket error", ex);
    }

    @Override
    public void onStart() {
        startLatch.countDown();
    }

    public void broadcastReload() {
        broadcast("reload");
    }

    public void startServer() {
        start();
        try {
            if (!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("WebSocket server failed to start within 5 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting WebSocket server", e);
        }
        if (startError != null) {
            if (startError instanceof java.net.BindException
                    || (startError.getCause() instanceof java.net.BindException)) {
                throw new IllegalStateException(
                        "JSF Autoreload: port " + port + " is already in use. Configure a different port via jsfAutoreload { port = XXXX }",
                        startError);
            }
            throw new RuntimeException("Failed to start WebSocket server", startError);
        }
    }

    public void stopServer() {
        try {
            stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
