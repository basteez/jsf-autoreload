package it.bstz.jsfautoreload.watcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FileChangeWatcherTest {

    @TempDir
    Path tempDir;

    private FileChangeWatcher watcher;

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    @Test
    void createEventTriggersOnChanged() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Path> changedPath = new AtomicReference<>();

        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> {
                    changedPath.set(path);
                    latch.countDown();
                },
                path -> {}
        );
        watcher.start();

        // Small delay to let watcher initialize
        Thread.sleep(500);

        Path newFile = tempDir.resolve("test.xhtml");
        Files.writeString(newFile, "<html/>");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "onChanged should be called for CREATE event");
        assertEquals(newFile, changedPath.get());
    }

    @Test
    void modifyEventTriggersOnChanged() throws Exception {
        Path existingFile = tempDir.resolve("existing.xhtml");
        Files.writeString(existingFile, "<html/>");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Path> changedPath = new AtomicReference<>();

        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> {
                    changedPath.set(path);
                    latch.countDown();
                },
                path -> {}
        );
        watcher.start();

        Thread.sleep(500);

        Files.writeString(existingFile, "<html>modified</html>");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "onChanged should be called for MODIFY event");
        assertEquals(existingFile, changedPath.get());
    }

    @Test
    void deleteEventTriggersOnDeleted() throws Exception {
        Path fileToDelete = tempDir.resolve("delete-me.xhtml");
        Files.writeString(fileToDelete, "<html/>");

        CountDownLatch deleteLatch = new CountDownLatch(1);
        AtomicReference<Path> deletedPath = new AtomicReference<>();
        CountDownLatch changeLatch = new CountDownLatch(1);

        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> changeLatch.countDown(),
                path -> {
                    deletedPath.set(path);
                    deleteLatch.countDown();
                }
        );
        watcher.start();

        Thread.sleep(500);

        Files.delete(fileToDelete);

        assertTrue(deleteLatch.await(5, TimeUnit.SECONDS), "onDeleted should be called for DELETE event");
        assertNotNull(deletedPath.get());
        assertFalse(changeLatch.await(1, TimeUnit.SECONDS), "onChanged should NOT be called for DELETE event");
    }
}
