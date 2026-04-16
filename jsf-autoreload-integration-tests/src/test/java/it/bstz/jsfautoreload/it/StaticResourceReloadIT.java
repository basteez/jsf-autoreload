package it.bstz.jsfautoreload.it;

import it.bstz.jsfautoreload.config.WatchedDirectory;
import it.bstz.jsfautoreload.core.DirectoryWatcher;
import it.bstz.jsfautoreload.model.FileCategory;
import it.bstz.jsfautoreload.model.FileChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StaticResourceReloadIT {

    private Path tempDir;
    private DirectoryWatcher watcher;
    private CopyOnWriteArrayList<FileChangeEvent> events;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("static-reload-it");
        events = new CopyOnWriteArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    @Test
    void cssFileChangeDetected() throws Exception {
        WatchedDirectory wd = new WatchedDirectory(tempDir);
        CountDownLatch latch = new CountDownLatch(1);

        watcher = new DirectoryWatcher(tempDir, true,
                wd.getInclusionPatterns(), wd.getExclusionPatterns(),
                event -> {
                    events.add(event);
                    latch.countDown();
                });
        watcher.start();

        Files.write(tempDir.resolve("style.css"), "body { color: red; }".getBytes());

        assertTrue(latch.await(10, TimeUnit.SECONDS), "CSS file change should be detected");
        assertEquals(1, events.size());
        assertEquals(FileCategory.STATIC, events.get(0).getFileCategory());
    }

    @Test
    void jsFileChangeDetected() throws Exception {
        WatchedDirectory wd = new WatchedDirectory(tempDir);
        CountDownLatch latch = new CountDownLatch(1);

        watcher = new DirectoryWatcher(tempDir, true,
                wd.getInclusionPatterns(), wd.getExclusionPatterns(),
                event -> {
                    events.add(event);
                    latch.countDown();
                });
        watcher.start();

        Files.write(tempDir.resolve("app.js"), "console.log('hi')".getBytes());

        assertTrue(latch.await(10, TimeUnit.SECONDS), "JS file change should be detected");
        assertEquals(1, events.size());
        assertEquals(FileCategory.STATIC, events.get(0).getFileCategory());
    }
}
