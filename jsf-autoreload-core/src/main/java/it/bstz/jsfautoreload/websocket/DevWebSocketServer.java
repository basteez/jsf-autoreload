package it.bstz.jsfautoreload.websocket;

import it.bstz.jsfautoreload.JsfAutoreloadException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket server that accepts browser connections and broadcasts reload notifications.
 *
 * <p>Implements {@link Closeable} — {@link #close()} closes all connections and releases the port.</p>
 */
public class DevWebSocketServer extends WebSocketServer implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(DevWebSocketServer.class.getName());

    private final int port;
    private final CountDownLatch startLatch = new CountDownLatch(1);
    private volatile Exception startError;
    private volatile boolean closed;

    public DevWebSocketServer(int port) {
        super(new InetSocketAddress("localhost", port));
        this.port = port;
        setReuseAddr(true);
        setConnectionLostTimeout(0);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.fine("[JSF Autoreload] Browser connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.fine("[JSF Autoreload] Browser disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // No inbound messages expected from browser clients
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn == null) {
            // Server-level error (e.g., bind failure) — reported via startServer() exception
            startError = ex;
            startLatch.countDown();
            return;
        }
        LOGGER.log(Level.WARNING, "[JSF Autoreload] WebSocket error.", ex);
    }

    @Override
    public void onStart() {
        startLatch.countDown();
    }

    /**
     * Broadcasts a "reload" message to all connected browser clients.
     */
    public void broadcastReload() {
        broadcast("reload");
    }

    /**
     * Starts the WebSocket server and blocks until it is ready or an error occurs.
     *
     * @throws JsfAutoreloadException if the port is already in use or startup fails
     */
    public void startServer() {
        start();
        try {
            if (!startLatch.await(3, TimeUnit.SECONDS)) {
                throw new JsfAutoreloadException(
                        "[JSF Autoreload] WebSocket server failed to start within 3 seconds. "
                                + "Check for port conflicts or system resource issues.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JsfAutoreloadException(
                    "[JSF Autoreload] Interrupted while starting WebSocket server. "
                            + "Retry the dev task.", e);
        }
        if (startError != null) {
            if (startError instanceof java.net.BindException
                    || (startError.getCause() instanceof java.net.BindException)) {
                throw new JsfAutoreloadException(
                        "[JSF Autoreload] Port " + port + " is already in use. "
                                + "Configure a different port via jsfAutoreload { port = XXXX }.",
                        startError);
            }
            throw new JsfAutoreloadException(
                    "[JSF Autoreload] Failed to start WebSocket server. "
                            + "Check logs for details.", startError);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            stop(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
