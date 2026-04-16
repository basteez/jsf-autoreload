package it.bstz.jsfautoreload.sse;

import it.bstz.jsfautoreload.logging.ReloadLogger;
import it.bstz.jsfautoreload.model.BrowserConnection;
import it.bstz.jsfautoreload.model.ReloadNotification;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConnectionManager {

    private final CopyOnWriteArraySet<BrowserConnection> connections = new CopyOnWriteArraySet<>();

    public void add(BrowserConnection connection) {
        connections.add(connection);
        ReloadLogger.fine("SSE_CONNECT", "Connection added: " + connection.getConnectionId()
                + " (total: " + connections.size() + ")");
    }

    public void remove(BrowserConnection connection) {
        connection.setState(BrowserConnection.ConnectionState.DISCONNECTED);
        connections.remove(connection);
        ReloadLogger.fine("SSE_DISCONNECT", "Connection removed: " + connection.getConnectionId()
                + " (total: " + connections.size() + ")");
    }

    public void broadcast(ReloadNotification notification) {
        String sseMessage = formatSseEvent(notification);
        ReloadLogger.info("SSE_BROADCAST", notification.getTriggerFile().toString(),
                "Broadcasting to " + connections.size() + " connections");

        Iterator<BrowserConnection> it = connections.iterator();
        while (it.hasNext()) {
            BrowserConnection conn = it.next();
            try {
                conn.getAsyncContext().write(sseMessage);
                conn.setLastNotifiedAt(Instant.now());
            } catch (IOException e) {
                ReloadLogger.fine("SSE_WRITE_FAIL",
                        "Removing dead connection: " + conn.getConnectionId());
                conn.setState(BrowserConnection.ConnectionState.DISCONNECTED);
                connections.remove(conn);
            }
        }
    }

    public void sendComment(BrowserConnection connection, String comment) {
        try {
            connection.getAsyncContext().write(":" + comment + "\n\n");
        } catch (IOException e) {
            remove(connection);
        }
    }

    public void closeAll() {
        for (BrowserConnection conn : connections) {
            try {
                conn.getAsyncContext().complete();
            } catch (Exception e) {
                // ignore during shutdown
            }
        }
        connections.clear();
    }

    public int getConnectionCount() {
        return connections.size();
    }

    private String formatSseEvent(ReloadNotification notification) {
        return "id: " + notification.getId() + "\n" +
                "event: reload\n" +
                "data: " + notification.toSseData() + "\n\n";
    }
}
