package it.bstz.jsfautoreload.core;

import it.bstz.jsfautoreload.logging.ReloadLogger;
import it.bstz.jsfautoreload.model.*;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class Debouncer {

    private final long viewStaticDebounceMs;
    private final long classDebounceMs;
    private final Consumer<ReloadNotification> notificationListener;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<DebounceGroup, ScheduledFuture<?>> pendingFutures;
    private final ConcurrentHashMap<DebounceGroup, FileChangeEvent> latestEvents;

    public Debouncer(long viewStaticDebounceMs, long classDebounceMs,
                     Consumer<ReloadNotification> notificationListener) {
        this.viewStaticDebounceMs = viewStaticDebounceMs;
        this.classDebounceMs = classDebounceMs;
        this.notificationListener = notificationListener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jsf-autoreload-debouncer");
            t.setDaemon(true);
            return t;
        });
        this.pendingFutures = new ConcurrentHashMap<>();
        this.latestEvents = new ConcurrentHashMap<>();
    }

    public void submit(FileChangeEvent event) {
        DebounceGroup group = DebounceGroup.fromCategory(event.getFileCategory());
        latestEvents.put(group, event);

        long delayMs = (group == DebounceGroup.CLASS) ? classDebounceMs : viewStaticDebounceMs;

        // Cancel existing pending future for this group
        ScheduledFuture<?> existing = pendingFutures.get(group);
        if (existing != null) {
            existing.cancel(false);
        }

        // Schedule new notification
        ScheduledFuture<?> future = scheduler.schedule(() -> fire(group), delayMs, TimeUnit.MILLISECONDS);
        pendingFutures.put(group, future);

        ReloadLogger.fine("DEBOUNCE", event.getFilePath().toString(),
                "Scheduled for group " + group + " in " + delayMs + "ms");
    }

    private void fire(DebounceGroup group) {
        FileChangeEvent event = latestEvents.remove(group);
        pendingFutures.remove(group);

        if (event == null) {
            return;
        }

        boolean requiresContextReload = (event.getFileCategory() == FileCategory.CLASS);
        ReloadNotification notification = new ReloadNotification(
                event.getFilePath(), event.getChangeType(), event.getFileCategory(),
                event.getTimestamp(), requiresContextReload);

        ReloadLogger.info("DEBOUNCE_FIRE", event.getFilePath().toString(),
                "Notification fired for group " + group);
        notificationListener.accept(notification);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
