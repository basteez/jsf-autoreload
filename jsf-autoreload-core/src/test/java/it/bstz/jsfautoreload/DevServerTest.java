package it.bstz.jsfautoreload;

import it.bstz.jsfautoreload.server.ServerAdapter;
import it.bstz.jsfautoreload.server.ServerConfigParams;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevServerTest {

    private DevServer devServer;
    private ScheduledExecutorService testExecutor;

    @AfterEach
    void tearDown() {
        if (devServer != null) {
            devServer.close();
        }
        if (testExecutor != null) {
            testExecutor.shutdownNow();
        }
    }

    @Test
    void fileModifyCopiedToOutputAndBroadcast(@TempDir Path tempDir) throws Exception {
        Path watchDir = tempDir.resolve("src");
        Files.createDirectories(watchDir);
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        int port = findFreePort();

        DevServerConfig config = DevServerConfig.builder()
                .port(port)
                .outputDir(outputDir)
                .watchDirs(Collections.singletonList(watchDir))
                .serverAdapter(stubAdapter())
                .debounceMs(50)
                .build();

        devServer = new DevServer();
        devServer.startNonBlocking(config);

        // Connect a WS client
        CountDownLatch messageLatch = new CountDownLatch(1);
        WebSocketClient client = createClient(port, messageLatch);
        client.connectBlocking(2, TimeUnit.SECONDS);

        // FileChangeWatcher starts asynchronously; pause for native FS listener registration
        Thread.sleep(500); // let watcher initialize

        // Create a file in watch dir
        Path sourceFile = watchDir.resolve("page.xhtml");
        Files.writeString(sourceFile, "<html>hello</html>");

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive reload broadcast");

        // Verify file was copied
        Path copied = outputDir.resolve("page.xhtml");
        assertTrue(Files.exists(copied), "File should be copied to output dir");
        assertEquals("<html>hello</html>", Files.readString(copied, StandardCharsets.UTF_8));

        client.closeBlocking();
    }

    @Test
    void fileDeleteDoesNotCopyButBroadcasts(@TempDir Path tempDir) throws Exception {
        Path watchDir = tempDir.resolve("src");
        Files.createDirectories(watchDir);
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        int port = findFreePort();

        // Pre-create a file
        Path sourceFile = watchDir.resolve("deleteme.xhtml");
        Files.writeString(sourceFile, "<html/>");

        DevServerConfig config = DevServerConfig.builder()
                .port(port)
                .outputDir(outputDir)
                .watchDirs(Collections.singletonList(watchDir))
                .serverAdapter(stubAdapter())
                .debounceMs(50)
                .build();

        devServer = new DevServer();
        devServer.startNonBlocking(config);

        CountDownLatch messageLatch = new CountDownLatch(1);
        WebSocketClient client = createClient(port, messageLatch);
        client.connectBlocking(2, TimeUnit.SECONDS);

        // FileChangeWatcher starts asynchronously; pause for native FS listener registration
        Thread.sleep(500);

        // Delete the file
        Files.delete(sourceFile);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive reload on delete");

        // Verify file was NOT copied to output (it was deleted, not changed)
        Path notCopied = outputDir.resolve("deleteme.xhtml");
        assertTrue(!Files.exists(notCopied), "Deleted file should not appear in output dir");

        client.closeBlocking();
    }

    @Test
    void debounceCoalescesRapidChanges(@TempDir Path tempDir) throws Exception {
        Path watchDir = tempDir.resolve("src");
        Files.createDirectories(watchDir);
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        int port = findFreePort();

        DevServerConfig config = DevServerConfig.builder()
                .port(port)
                .outputDir(outputDir)
                .watchDirs(Collections.singletonList(watchDir))
                .serverAdapter(stubAdapter())
                .debounceMs(500) // long debounce to test coalescing
                .build();

        devServer = new DevServer();
        devServer.startNonBlocking(config);

        AtomicInteger reloadCount = new AtomicInteger(0);
        CountDownLatch firstReload = new CountDownLatch(1);
        WebSocketClient client = createCountingClient(port, reloadCount, firstReload);
        client.connectBlocking(2, TimeUnit.SECONDS);

        // FileChangeWatcher starts asynchronously; pause for native FS listener registration
        Thread.sleep(500);

        // Rapid successive changes
        for (int i = 0; i < 5; i++) {
            Files.writeString(watchDir.resolve("rapid.xhtml"), "<html>change " + i + "</html>");
            // Less than 500ms debounce — triggers coalescing
        Thread.sleep(50); // much less than 500ms debounce
        }

        // Wait for debounce to fire
        assertTrue(firstReload.await(5, TimeUnit.SECONDS), "Should eventually receive a reload");
        // Allow time for any additional debounced reloads to fire
        Thread.sleep(200); // give time for any extra reloads

        // Should have received very few reloads (ideally 1, but FS events may batch differently)
        assertTrue(reloadCount.get() <= 3,
                "Debounce should coalesce rapid changes, got " + reloadCount.get() + " reloads");

        client.closeBlocking();
    }

    @Test
    void missingOutputDirThrowsConfigException(@TempDir Path tempDir) throws Exception {
        Path watchDir = tempDir.resolve("src");
        Files.createDirectories(watchDir);
        Path nonExistent = tempDir.resolve("doesnotexist");
        int port = findFreePort();

        DevServerConfig config = DevServerConfig.builder()
                .port(port)
                .outputDir(nonExistent)
                .watchDirs(Collections.singletonList(watchDir))
                .serverAdapter(stubAdapter())
                .build();

        devServer = new DevServer();
        JsfAutoreloadConfigException exception = assertThrows(JsfAutoreloadConfigException.class,
                () -> devServer.startNonBlocking(config));
        assertTrue(exception.getMessage().contains("Output directory not found"));
        assertTrue(exception.getMessage().contains("[JSF Autoreload]"));
    }

    @Test
    void closeReleasesAllResources(@TempDir Path tempDir) throws Exception {
        Path watchDir = tempDir.resolve("src");
        Files.createDirectories(watchDir);
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        int port = findFreePort();

        DevServerConfig config = DevServerConfig.builder()
                .port(port)
                .outputDir(outputDir)
                .watchDirs(Collections.singletonList(watchDir))
                .serverAdapter(stubAdapter())
                .build();

        devServer = new DevServer();
        devServer.startNonBlocking(config);
        // FileChangeWatcher starts asynchronously; pause for native FS listener registration
        Thread.sleep(500);

        long start = System.currentTimeMillis();
        devServer.close();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 2000, "Shutdown should complete within 2 seconds, took " + elapsed + "ms");
        devServer = null; // already closed
    }

    @Test
    void tryWithResourcesPattern(@TempDir Path tempDir) throws Exception {
        Path watchDir = tempDir.resolve("src");
        Files.createDirectories(watchDir);
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        int port = findFreePort();

        DevServerConfig config = DevServerConfig.builder()
                .port(port)
                .outputDir(outputDir)
                .watchDirs(Collections.singletonList(watchDir))
                .serverAdapter(stubAdapter())
                .build();

        try (DevServer server = new DevServer()) {
            server.startNonBlocking(config);
            // FileChangeWatcher starts asynchronously; pause for native FS listener registration
        Thread.sleep(300);
        }
        // No exception means successful close via try-with-resources
    }

    private ServerAdapter stubAdapter() {
        return new ServerAdapter() {
            @Override
            public boolean isRunning() { return false; }
            @Override
            public int getHttpPort() { return 9080; }
            @Override
            public String getContextRoot() { return "/"; }
            @Override
            public Path resolveOutputDir(String serverName, Path projectDir) { return projectDir; }
            @Override
            public void writeServerConfig(ServerConfigParams params) { }
        };
    }

    private WebSocketClient createClient(int port, CountDownLatch messageLatch) throws Exception {
        return new WebSocketClient(new URI("ws://localhost:" + port)) {
            @Override
            public void onOpen(ServerHandshake handshake) { }
            @Override
            public void onMessage(String message) { messageLatch.countDown(); }
            @Override
            public void onClose(int code, String reason, boolean remote) { }
            @Override
            public void onError(Exception ex) { }
        };
    }

    private WebSocketClient createCountingClient(int port, AtomicInteger counter,
                                                   CountDownLatch firstReload) throws Exception {
        return new WebSocketClient(new URI("ws://localhost:" + port)) {
            @Override
            public void onOpen(ServerHandshake handshake) { }
            @Override
            public void onMessage(String message) {
                counter.incrementAndGet();
                firstReload.countDown();
            }
            @Override
            public void onClose(int code, String reason, boolean remote) { }
            @Override
            public void onError(Exception ex) { }
        };
    }

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
