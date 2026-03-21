package it.bstz.jsfautoreload.watcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileChangeWatcherTest {

    @TempDir
    Path tempDir;

    private FileChangeWatcher watcher;

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.close();
        }
    }

    @Test
    void createEventTriggersOnChange() throws Exception {
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

        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        Path newFile = tempDir.resolve("test.xhtml");
        Files.writeString(newFile, "<html/>");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "onChange should be called for CREATE event");
        assertEquals(newFile, changedPath.get());
    }

    @Test
    void modifyEventTriggersOnChange() throws Exception {
        Path existingFile = tempDir.resolve("existing.css");
        Files.writeString(existingFile, "body {}");

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

        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        Files.writeString(existingFile, "body { color: red; }");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "onChange should be called for MODIFY event");
        assertEquals(existingFile, changedPath.get());
    }

    @Test
    void deleteEventTriggersOnDelete() throws Exception {
        Path fileToDelete = tempDir.resolve("delete-me.js");
        Files.writeString(fileToDelete, "console.log('hi');");

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

        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        Files.delete(fileToDelete);

        assertTrue(deleteLatch.await(5, TimeUnit.SECONDS), "onDelete should be called for DELETE event");
        assertNotNull(deletedPath.get());
        assertFalse(changeLatch.await(1, TimeUnit.SECONDS), "onChange should NOT be called for DELETE event");
    }

    @Test
    void closeStopsWatching() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> latch.countDown(),
                path -> {}
        );
        watcher.start();
        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        watcher.close();
        // Allow time for close() to propagate to the underlying watcher thread
        Thread.sleep(200);

        Files.writeString(tempDir.resolve("after-close.xhtml"), "<html/>");
        assertFalse(latch.await(2, TimeUnit.SECONDS), "onChange should NOT fire after close()");
    }

    @Test
    void closeIsIdempotent() throws Exception {
        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> {},
                path -> {}
        );
        watcher.start();
        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        watcher.close();
        watcher.close(); // second close should not throw
    }

    @Test
    void watchesXhtmlFiles() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> latch.countDown(),
                path -> {}
        );
        watcher.start();
        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        Files.writeString(tempDir.resolve("page.xhtml"), "<html/>");
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should detect .xhtml files");
    }

    @Test
    void watchesCssFiles() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> latch.countDown(),
                path -> {}
        );
        watcher.start();
        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        Files.writeString(tempDir.resolve("style.css"), "body {}");
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should detect .css files");
    }

    @Test
    void watchesJsFiles() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        watcher = new FileChangeWatcher(
                Collections.singletonList(tempDir),
                path -> latch.countDown(),
                path -> {}
        );
        watcher.start();
        // DirectoryWatcher.watchAsync() starts asynchronously with no ready signal;
        // a brief pause ensures the native file system listener is registered
        Thread.sleep(500);

        Files.writeString(tempDir.resolve("app.js"), "var x = 1;");
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should detect .js files");
    }
}
