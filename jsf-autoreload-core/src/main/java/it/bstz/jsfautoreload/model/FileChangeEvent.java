package it.bstz.jsfautoreload.model;

import java.nio.file.Path;
import java.time.Instant;

public final class FileChangeEvent {

    private final Path filePath;
    private final ChangeType changeType;
    private final Instant timestamp;
    private final FileCategory fileCategory;

    public FileChangeEvent(Path filePath, ChangeType changeType, Instant timestamp, FileCategory fileCategory) {
        this.filePath = filePath;
        this.changeType = changeType;
        this.timestamp = timestamp;
        this.fileCategory = fileCategory;
    }

    public Path getFilePath() {
        return filePath;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public FileCategory getFileCategory() {
        return fileCategory;
    }

    @Override
    public String toString() {
        return "FileChangeEvent{" +
                "filePath=" + filePath +
                ", changeType=" + changeType +
                ", fileCategory=" + fileCategory +
                ", timestamp=" + timestamp +
                '}';
    }
}
