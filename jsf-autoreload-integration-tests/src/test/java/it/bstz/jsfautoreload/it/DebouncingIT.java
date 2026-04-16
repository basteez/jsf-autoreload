package it.bstz.jsfautoreload.it;

import it.bstz.jsfautoreload.config.WatchedDirectory;
import it.bstz.jsfautoreload.core.Debouncer;
import it.bstz.jsfautoreload.core.DirectoryWatcher;
import it.bstz.jsfautoreload.model.ReloadNotification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DebouncingIT {

    private Path tempDir;
    private DirectoryWatcher watcher;
    private Debouncer debouncer;
    private CopyOnWriteArrayList<ReloadNotification> notifications;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("debouncing-it");
        notifications = new CopyOnWriteArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (watcher != null) watcher.stop();
        if (debouncer != null) debouncer.shutdown();
    }

    @Test
    void rapidClassChangesProduceAtMostTwoReloads() throws Exception {
        debouncer = new Debouncer(200, 1000, notifications::add);

        WatchedDirectory wd = new WatchedDirectory(tempDir);
        watcher = new DirectoryWatcher(tempDir, true,
                wd.getInclusionPatterns(), wd.getExclusionPatterns(),
                debouncer::submit);
        watcher.start();

        // Write 20 rapid .class file changes
        for (int i = 0; i < 20; i++) {
            Files.write(tempDir.resolve("Change" + i + ".class"),
                    new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) i});
            Thread.sleep(50);
        }

        // Wait for debounce to settle
        Thread.sleep(3000);

        assertTrue(notifications.size() <= 2,
                "SC-007: 20 rapid changes should produce at most 2 reload events, got: " + notifications.size());
        assertFalse(notifications.isEmpty(), "Should have at least 1 notification");
    }
}
