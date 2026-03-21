package it.bstz.jsfautoreload.watcher;

import io.methvin.watcher.DirectoryChangeEvent.EventType;
import io.methvin.watcher.DirectoryWatcher;

import it.bstz.jsfautoreload.JsfAutoreloadException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches directories for file changes and routes events to callbacks.
 *
 * <p>CREATE and MODIFY events invoke {@code onChange}. DELETE events invoke {@code onDelete}.
 * All file types are watched. Transient file system errors are logged and do not crash the watcher.</p>
 *
 * <p>Implements {@link Closeable} for use with try-with-resources.</p>
 */
public class FileChangeWatcher implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(FileChangeWatcher.class.getName());

    private final List<Path> watchDirs;
    private final Consumer<Path> onChange;
    private final Consumer<Path> onDelete;
    private volatile DirectoryWatcher watcher;
    private volatile boolean closed;

    public FileChangeWatcher(List<Path> watchDirs, Consumer<Path> onChange, Consumer<Path> onDelete) {
        this.watchDirs = watchDirs;
        this.onChange = onChange;
        this.onDelete = onDelete;
    }

    public void start() {
        try {
            watcher = DirectoryWatcher.builder()
                    .paths(watchDirs)
                    .listener(event -> {
                        try {
                            if (event.eventType() == EventType.CREATE || event.eventType() == EventType.MODIFY) {
                                onChange.accept(event.path());
                            } else if (event.eventType() == EventType.DELETE) {
                                onDelete.accept(event.path());
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING,
                                    "[JSF Autoreload] Error processing file event for " + event.path()
                                            + ". Continuing to watch.", e);
                        }
                    })
                    .build();
            watcher.watchAsync();
        } catch (IOException e) {
            throw new JsfAutoreloadException(
                    "[JSF Autoreload] Failed to start file watcher for dirs: " + watchDirs
                            + ". Verify the directories exist and are readable.", e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        DirectoryWatcher w = watcher;
        if (w != null) {
            try {
                w.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,
                        "[JSF Autoreload] Error closing file watcher.", e);
            }
        }
    }

}
