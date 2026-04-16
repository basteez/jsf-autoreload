package it.bstz.jsfautoreload.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public final class ReloadNotification {

    private final String id;
    private final Path triggerFile;
    private final ChangeType eventType;
    private final FileCategory fileCategory;
    private final Instant timestamp;
    private final boolean requiresContextReload;

    public ReloadNotification(Path triggerFile, ChangeType eventType, FileCategory fileCategory,
                              Instant timestamp, boolean requiresContextReload) {
        this.id = UUID.randomUUID().toString();
        this.triggerFile = triggerFile;
        this.eventType = eventType;
        this.fileCategory = fileCategory;
        this.timestamp = timestamp;
        this.requiresContextReload = requiresContextReload;
    }

    public String getId() {
        return id;
    }

    public Path getTriggerFile() {
        return triggerFile;
    }

    public ChangeType getEventType() {
        return eventType;
    }

    public FileCategory getFileCategory() {
        return fileCategory;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isRequiresContextReload() {
        return requiresContextReload;
    }

    public String toSseData() {
        return "{\"file\":\"" + triggerFile + "\",\"type\":\"" + eventType +
                "\",\"category\":\"" + fileCategory +
                "\",\"contextReload\":" + requiresContextReload + "}";
    }

    @Override
    public String toString() {
        return "ReloadNotification{id='" + id + "', triggerFile=" + triggerFile +
                ", eventType=" + eventType + ", fileCategory=" + fileCategory + '}';
    }
}
