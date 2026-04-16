package it.bstz.jsfautoreload.it;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;
import it.bstz.jsfautoreload.config.WatchedDirectory;
import it.bstz.jsfautoreload.core.Debouncer;
import it.bstz.jsfautoreload.core.DirectoryWatcher;
import it.bstz.jsfautoreload.model.BrowserConnection;
import it.bstz.jsfautoreload.model.FileCategory;
import it.bstz.jsfautoreload.model.ReloadNotification;
import it.bstz.jsfautoreload.sse.ConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ClassReloadIT {

    private Path tempDir;
    private DirectoryWatcher watcher;
    private Debouncer debouncer;
    private CopyOnWriteArrayList<ReloadNotification> notifications;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("class-reload-it");
        notifications = new CopyOnWriteArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (watcher != null) watcher.stop();
        if (debouncer != null) debouncer.shutdown();
    }

    @Test
    void classFileChangeTriggersReloadNotification() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        debouncer = new Debouncer(200, 500, notification -> {
            notifications.add(notification);
            latch.countDown();
        });

        WatchedDirectory wd = new WatchedDirectory(tempDir);
        watcher = new DirectoryWatcher(tempDir, true,
                wd.getInclusionPatterns(), wd.getExclusionPatterns(),
                debouncer::submit);
        watcher.start();

        Files.write(tempDir.resolve("MyBean.class"), new byte[]{(byte) 0xCA, (byte) 0xFE});

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Class file change should trigger notification");
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).isRequiresContextReload(),
                "CLASS category should require context reload");
        assertEquals(FileCategory.CLASS, notifications.get(0).getFileCategory());
    }

    @Test
    void classFileChangeBroadcastReachesConnectedClient() throws Exception {
        ConnectionManager connectionManager = new ConnectionManager();
        ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
        AsyncContextWrapper wrapper = new AsyncContextWrapper(new Object(), clientOutput, () -> {});
        BrowserConnection conn = new BrowserConnection(wrapper);
        connectionManager.add(conn);

        CountDownLatch latch = new CountDownLatch(1);
        debouncer = new Debouncer(200, 500, notification -> {
            notifications.add(notification);
            connectionManager.broadcast(notification);
            latch.countDown();
        });

        WatchedDirectory wd = new WatchedDirectory(tempDir);
        watcher = new DirectoryWatcher(tempDir, true,
                wd.getInclusionPatterns(), wd.getExclusionPatterns(),
                debouncer::submit);
        watcher.start();

        Files.write(tempDir.resolve("Service.class"), new byte[]{(byte) 0xCA, (byte) 0xFE});

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Class file change should trigger notification");

        String received = clientOutput.toString("UTF-8");
        assertTrue(received.contains("event: reload"),
                "Connected client should receive SSE reload event, got: " + received);
        assertTrue(notifications.get(0).isRequiresContextReload(),
                "CLASS notification should require context reload");
    }
}
