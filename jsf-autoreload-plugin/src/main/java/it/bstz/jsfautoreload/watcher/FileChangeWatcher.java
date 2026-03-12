package it.bstz.jsfautoreload.watcher;

import io.methvin.watcher.DirectoryChangeEvent.EventType;
import io.methvin.watcher.DirectoryWatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileChangeWatcher {

    private static final Logger LOGGER = Logger.getLogger(FileChangeWatcher.class.getName());

    private final List<Path> watchDirs;
    private final Consumer<Path> onChanged;
    private final Consumer<Path> onDeleted;
    private volatile DirectoryWatcher watcher;

    public FileChangeWatcher(List<Path> watchDirs, Consumer<Path> onChanged, Consumer<Path> onDeleted) {
        this.watchDirs = watchDirs;
        this.onChanged = onChanged;
        this.onDeleted = onDeleted;
    }

    public void start() {
        try {
            watcher = DirectoryWatcher.builder()
                    .paths(watchDirs)
                    .listener(event -> {
                        if (event.eventType() == EventType.CREATE || event.eventType() == EventType.MODIFY) {
                            onChanged.accept(event.path());
                        } else if (event.eventType() == EventType.DELETE) {
                            onDeleted.accept(event.path());
                        }
                    })
                    .build();
            watcher.watchAsync();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start file watcher", e);
        }
    }

    public void stop() {
        DirectoryWatcher w = watcher;
        if (w != null) {
            try {
                w.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing file watcher", e);
            }
        }
    }
}
