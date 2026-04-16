package it.bstz.jsfautoreload.it;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;
import it.bstz.jsfautoreload.model.BrowserConnection;
import it.bstz.jsfautoreload.model.ChangeType;
import it.bstz.jsfautoreload.model.FileCategory;
import it.bstz.jsfautoreload.model.ReloadNotification;
import it.bstz.jsfautoreload.sse.ConnectionManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiConnectionIT {

    @Test
    void tenSimultaneousConnectionsReceiveBroadcast() throws Exception {
        ConnectionManager connectionManager = new ConnectionManager();
        List<ByteArrayOutputStream> outputs = new ArrayList<>();
        List<BrowserConnection> connections = new ArrayList<>();

        // Open 10+ simultaneous connections
        for (int i = 0; i < 12; i++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AsyncContextWrapper wrapper = new AsyncContextWrapper(new Object(), out, () -> {});
            BrowserConnection conn = new BrowserConnection(wrapper);
            connectionManager.add(conn);
            outputs.add(out);
            connections.add(conn);
        }

        assertEquals(12, connectionManager.getConnectionCount());

        // Trigger a file change broadcast
        ReloadNotification notification = new ReloadNotification(
                Paths.get("test.xhtml"), ChangeType.MODIFIED, FileCategory.VIEW,
                Instant.now(), false);

        connectionManager.broadcast(notification);

        // All connections should receive the event (SC-006)
        for (int i = 0; i < outputs.size(); i++) {
            String content = outputs.get(i).toString("UTF-8");
            assertTrue(content.contains("event: reload"),
                    "Connection " + i + " should receive the reload event");
        }
    }
}
