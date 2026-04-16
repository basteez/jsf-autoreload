package it.bstz.jsfautoreload.sse;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;
import it.bstz.jsfautoreload.bridge.ServletBridge;
import it.bstz.jsfautoreload.logging.ReloadLogger;
import it.bstz.jsfautoreload.model.BrowserConnection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultSseHandler implements SseHandler {

    private final ServletBridge servletBridge;
    private final ConnectionManager connectionManager;
    private final ScheduledExecutorService heartbeatScheduler;

    public DefaultSseHandler(ServletBridge servletBridge, ConnectionManager connectionManager) {
        this.servletBridge = servletBridge;
        this.connectionManager = connectionManager;
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jsf-autoreload-heartbeat");
            t.setDaemon(true);
            return t;
        });
        startHeartbeat();
    }

    @Override
    public void handleRequest(Object request, Object response) {
        try {
            setResponseHeaders(response);
            AsyncContextWrapper asyncContext = servletBridge.startAsync(request, response);
            BrowserConnection connection = new BrowserConnection(asyncContext);
            connectionManager.add(connection);

            // Register AsyncListener for proactive connection cleanup
            Runnable removeCallback = () -> {
                connectionManager.remove(connection);
                ReloadLogger.fine("SSE_ASYNC_LISTENER",
                        "Connection removed via AsyncListener: " + connection.getConnectionId());
            };
            servletBridge.addAsyncListener(asyncContext, removeCallback, removeCallback, removeCallback);

            // Send initial :ok comment
            connectionManager.sendComment(connection, "ok");

            ReloadLogger.info("SSE_HANDLER", "New SSE connection established: " + connection.getConnectionId());
        } catch (Exception e) {
            ReloadLogger.warning("SSE_HANDLER", "Failed to handle SSE request", e);
        }
    }

    public void shutdown() {
        heartbeatScheduler.shutdownNow();
        connectionManager.closeAll();
    }

    private void setResponseHeaders(Object response) {
        try {
            java.lang.reflect.Method setContentType = response.getClass()
                    .getMethod("setContentType", String.class);
            java.lang.reflect.Method setHeader = response.getClass()
                    .getMethod("setHeader", String.class, String.class);
            java.lang.reflect.Method setCharEnc = response.getClass()
                    .getMethod("setCharacterEncoding", String.class);

            setContentType.invoke(response, "text/event-stream");
            setCharEnc.invoke(response, "UTF-8");
            setHeader.invoke(response, "Cache-Control", "no-cache");
            setHeader.invoke(response, "Connection", "keep-alive");
        } catch (Exception e) {
            ReloadLogger.warning("SSE_HANDLER", "Failed to set response headers", e);
        }
    }

    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            connectionManager.sendHeartbeatToAll();
        }, 30, 30, TimeUnit.SECONDS);
    }
}
