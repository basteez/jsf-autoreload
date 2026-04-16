package it.bstz.jsfautoreload.core;

import it.bstz.jsfautoreload.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DebouncerTest {

    private Debouncer debouncer;
    private CopyOnWriteArrayList<ReloadNotification> notifications;

    @BeforeEach
    void setUp() {
        notifications = new CopyOnWriteArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (debouncer != null) {
            debouncer.shutdown();
        }
    }

    @Test
    void singleEventFiresAfterDebounceWindow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        debouncer = new Debouncer(200, 500, notification -> {
            notifications.add(notification);
            latch.countDown();
        });

        FileChangeEvent event = new FileChangeEvent(
                Paths.get("test.xhtml"), ChangeType.MODIFIED, Instant.now(), FileCategory.VIEW);

        debouncer.submit(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Notification should fire after debounce window");
        assertEquals(1, notifications.size());
        assertEquals(FileCategory.VIEW, notifications.get(0).getFileCategory());
    }

    @Test
    void rapidEventsResetTimer() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        debouncer = new Debouncer(300, 500, notification -> {
            notifications.add(notification);
            latch.countDown();
        });

        // Send rapid events — only the last should trigger notification
        for (int i = 0; i < 5; i++) {
            debouncer.submit(new FileChangeEvent(
                    Paths.get("test" + i + ".xhtml"), ChangeType.MODIFIED, Instant.now(), FileCategory.VIEW));
            Thread.sleep(100); // less than debounce window
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, notifications.size(), "Multiple rapid events should produce single notification");
    }

    @Test
    void independentDebounceGroups() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        debouncer = new Debouncer(200, 200, notification -> {
            notifications.add(notification);
            latch.countDown();
        });

        // VIEW_STATIC group
        debouncer.submit(new FileChangeEvent(
                Paths.get("test.xhtml"), ChangeType.MODIFIED, Instant.now(), FileCategory.VIEW));
        // CLASS group
        debouncer.submit(new FileChangeEvent(
                Paths.get("Test.class"), ChangeType.MODIFIED, Instant.now(), FileCategory.CLASS));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(2, notifications.size(), "Different debounce groups should fire independently");
    }
}
