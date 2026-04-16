package it.bstz.jsfautoreload.core;

import it.bstz.jsfautoreload.model.ChangeType;
import it.bstz.jsfautoreload.model.FileCategory;
import it.bstz.jsfautoreload.model.FileChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryWatcherTest {

    @TempDir
    Path tempDir;

    private DirectoryWatcher watcher;
    private CopyOnWriteArrayList<FileChangeEvent> receivedEvents;

    private static final Set<String> INCLUDE = new LinkedHashSet<>(Arrays.asList("**/*.xhtml", "**/*.css"));
    private static final Set<String> EXCLUDE = new LinkedHashSet<>(Collections.singletonList("**/.*"));

    @BeforeEach
    void setUp() {
        receivedEvents = new CopyOnWriteArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    @Test
    void detectsFileCreation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        watcher = new DirectoryWatcher(tempDir, true, INCLUDE, EXCLUDE, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        watcher.start();

        Files.write(tempDir.resolve("test.xhtml"), "hello".getBytes());

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Should detect file creation");
        assertEquals(1, receivedEvents.size());
        assertEquals(FileCategory.VIEW, receivedEvents.get(0).getFileCategory());
    }

    @Test
    void detectsFileModification() throws Exception {
        Path file = tempDir.resolve("style.css");
        Files.write(file, "body {}".getBytes());

        CountDownLatch latch = new CountDownLatch(1);
        watcher = new DirectoryWatcher(tempDir, true, INCLUDE, EXCLUDE, event -> {
            if (event.getChangeType() == ChangeType.MODIFIED) {
                receivedEvents.add(event);
                latch.countDown();
            }
        });
        watcher.start();

        Thread.sleep(200); // let watcher register
        Files.write(file, "body { color: red; }".getBytes());

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Should detect file modification");
        assertFalse(receivedEvents.isEmpty());
        assertEquals(FileCategory.STATIC, receivedEvents.get(0).getFileCategory());
    }

    @Test
    void exclusionPatternFiltersFiles() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        watcher = new DirectoryWatcher(tempDir, true, INCLUDE, EXCLUDE, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        watcher.start();

        // Hidden file should be excluded
        Files.write(tempDir.resolve(".hidden.xhtml"), "hidden".getBytes());
        // Non-matching extension should be excluded
        Files.write(tempDir.resolve("readme.txt"), "text".getBytes());

        assertFalse(latch.await(3, TimeUnit.SECONDS), "Excluded files should not trigger events");
        assertTrue(receivedEvents.isEmpty());
    }

    @Test
    void recursiveDirectoryRegistration() throws Exception {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);

        CountDownLatch latch = new CountDownLatch(1);
        watcher = new DirectoryWatcher(tempDir, true, INCLUDE, EXCLUDE, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        watcher.start();

        Files.write(subDir.resolve("nested.xhtml"), "nested".getBytes());

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Should detect files in subdirectories");
        assertEquals(1, receivedEvents.size());
    }

    @Test
    void newSubdirectoryIsAutoRegistered() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        watcher = new DirectoryWatcher(tempDir, true, INCLUDE, EXCLUDE, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        watcher.start();

        // Create new subdirectory after watcher started
        Path newSubDir = tempDir.resolve("newdir");
        Files.createDirectories(newSubDir);
        Thread.sleep(3000); // macOS WatchService needs time to detect and register new dirs

        Files.write(newSubDir.resolve("dynamic.xhtml"), "dynamic".getBytes());

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Should detect files in dynamically created subdirectories");
        assertEquals(1, receivedEvents.size());
    }
}
