package it.bstz.jsfautoreload.sse;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;
import it.bstz.jsfautoreload.model.BrowserConnection;
import it.bstz.jsfautoreload.model.ChangeType;
import it.bstz.jsfautoreload.model.FileCategory;
import it.bstz.jsfautoreload.model.ReloadNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionManagerTest {

    private ConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
    }

    @Test
    void addAndRemoveConnection() {
        BrowserConnection conn = createTestConnection();
        connectionManager.add(conn);
        assertEquals(1, connectionManager.getConnectionCount());

        connectionManager.remove(conn);
        assertEquals(0, connectionManager.getConnectionCount());
    }

    @Test
    void broadcastSendsToAllConnections() throws IOException {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();

        BrowserConnection conn1 = createTestConnection(out1);
        BrowserConnection conn2 = createTestConnection(out2);

        connectionManager.add(conn1);
        connectionManager.add(conn2);

        ReloadNotification notification = new ReloadNotification(
                Paths.get("test.xhtml"), ChangeType.MODIFIED, FileCategory.VIEW,
                Instant.now(), false);

        connectionManager.broadcast(notification);

        assertTrue(out1.toString("UTF-8").contains("event: reload"),
                "First connection should receive broadcast");
        assertTrue(out2.toString("UTF-8").contains("event: reload"),
                "Second connection should receive broadcast");
    }

    @Test
    void broadcastRemovesDeadConnections() {
        // Create a connection with a broken output stream
        BrowserConnection deadConn = createDeadConnection();
        BrowserConnection goodConn = createTestConnection();

        connectionManager.add(deadConn);
        connectionManager.add(goodConn);
        assertEquals(2, connectionManager.getConnectionCount());

        ReloadNotification notification = new ReloadNotification(
                Paths.get("test.xhtml"), ChangeType.MODIFIED, FileCategory.VIEW,
                Instant.now(), false);

        connectionManager.broadcast(notification);

        assertEquals(1, connectionManager.getConnectionCount(),
                "Dead connections should be removed after failed broadcast");
    }

    @Test
    void concurrentAccessIsSafe() throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                BrowserConnection conn = createTestConnection();
                connectionManager.add(conn);
                connectionManager.remove(conn);
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(0, connectionManager.getConnectionCount());
    }

    private BrowserConnection createTestConnection() {
        return createTestConnection(new ByteArrayOutputStream());
    }

    private BrowserConnection createTestConnection(ByteArrayOutputStream out) {
        AsyncContextWrapper wrapper = new AsyncContextWrapper(new Object(), out, () -> {});
        return new BrowserConnection(wrapper);
    }

    private BrowserConnection createDeadConnection() {
        java.io.OutputStream failingStream = new java.io.OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Connection closed");
            }
        };
        AsyncContextWrapper wrapper = new AsyncContextWrapper(new Object(), failingStream, () -> {});
        return new BrowserConnection(wrapper);
    }
}
