package it.bstz.jsfautoreload.core;

import it.bstz.jsfautoreload.logging.ReloadLogger;
import it.bstz.jsfautoreload.model.ChangeType;
import it.bstz.jsfautoreload.model.FileCategory;
import it.bstz.jsfautoreload.model.FileChangeEvent;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher {

    private final Path rootDir;
    private final boolean recursive;
    private final Set<String> inclusionPatterns;
    private final Set<String> exclusionPatterns;
    private final Consumer<FileChangeEvent> listener;
    private volatile boolean running;
    private WatchService watchService;
    private Thread watchThread;

    public DirectoryWatcher(Path rootDir, boolean recursive,
                            Set<String> inclusionPatterns, Set<String> exclusionPatterns,
                            Consumer<FileChangeEvent> listener) {
        this.rootDir = rootDir;
        this.recursive = recursive;
        this.inclusionPatterns = inclusionPatterns;
        this.exclusionPatterns = exclusionPatterns;
        this.listener = listener;
    }

    public void start() {
        try {
            watchService = rootDir.getFileSystem().newWatchService();
            registerDirectory(rootDir);
            if (recursive) {
                registerRecursive(rootDir);
            }
        } catch (IOException e) {
            ReloadLogger.severe("WATCHER_INIT", "Failed to create watch service", e);
            return;
        }

        running = true;
        watchThread = new Thread(this::pollLoop, "jsf-autoreload-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
        ReloadLogger.info("WATCHER_START", rootDir.toString(), "Watching directory");
    }

    public void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                ReloadLogger.warning("WATCHER_STOP", "Error closing watch service", e);
            }
        }
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    private void pollLoop() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            Path dir = (Path) key.watchable();

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path fileName = pathEvent.context();
                Path fullPath = dir.resolve(fileName);

                // Register new subdirectories
                if (kind == ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    if (recursive) {
                        try {
                            registerDirectory(fullPath);
                            registerRecursive(fullPath);
                        } catch (IOException e) {
                            ReloadLogger.warning("WATCHER_REGISTER",
                                    "Failed to register new subdirectory: " + fullPath);
                        }
                    }
                    continue;
                }

                if (!matchesPatterns(fullPath)) {
                    continue;
                }

                ChangeType changeType = toChangeType(kind);
                String extension = getExtension(fullPath);
                FileCategory category = FileCategory.fromExtension(extension);

                FileChangeEvent changeEvent = new FileChangeEvent(
                        fullPath, changeType, Instant.now(), category);

                ReloadLogger.fine("FILE_CHANGE", fullPath.toString(),
                        changeType + " [" + category + "]");
                listener.accept(changeEvent);
            }

            boolean valid = key.reset();
            if (!valid) {
                ReloadLogger.warning("WATCHER_INVALID",
                        "Watch key no longer valid for: " + dir);
            }
        }
    }

    private void registerDirectory(Path dir) throws IOException {
        try {
            WatchEvent.Modifier[] modifiers = getModifiers();
            if (modifiers.length > 0) {
                dir.register(watchService,
                        new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE},
                        modifiers);
            } else {
                dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            }
        } catch (IOException e) {
            ReloadLogger.warning("WATCHER_REGISTER", "Failed to register: " + dir);
            throw e;
        }
    }

    private void registerRecursive(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root)) {
                    registerDirectory(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                ReloadLogger.warning("WATCHER_WALK", "Failed to visit: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private WatchEvent.Modifier[] getModifiers() {
        // Use SensitivityWatchEventModifier.HIGH on macOS for faster detection
        try {
            Class<?> modClass = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier");
            @SuppressWarnings("unchecked")
            WatchEvent.Modifier high = (WatchEvent.Modifier) Enum.valueOf(
                    (Class<Enum>) modClass, "HIGH");
            return new WatchEvent.Modifier[]{high};
        } catch (Exception e) {
            return new WatchEvent.Modifier[0];
        }
    }

    private boolean matchesPatterns(Path path) {
        String fileName = path.getFileName().toString();
        String pathStr = path.toString().replace('\\', '/');

        // Check exclusion patterns first
        for (String pattern : exclusionPatterns) {
            if (matchGlob(pattern, pathStr, fileName)) {
                return false;
            }
        }

        // Check inclusion patterns
        for (String pattern : inclusionPatterns) {
            if (matchGlob(pattern, pathStr, fileName)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchGlob(String pattern, String pathStr, String fileName) {
        // Simple glob matching: **/*.ext matches any file with that extension
        if (pattern.startsWith("**/")) {
            String subPattern = pattern.substring(3);
            if (subPattern.startsWith("*.")) {
                // Extension match
                String ext = subPattern.substring(1);
                return fileName.endsWith(ext);
            }
            if (subPattern.equals(".*")) {
                // Hidden file pattern
                return fileName.startsWith(".");
            }
            // Recursive directory match
            if (subPattern.endsWith("/**")) {
                String dirName = subPattern.substring(0, subPattern.length() - 3);
                return pathStr.contains("/" + dirName + "/");
            }
        }
        return false;
    }

    private static ChangeType toChangeType(WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_CREATE) return ChangeType.CREATED;
        if (kind == ENTRY_MODIFY) return ChangeType.MODIFIED;
        if (kind == ENTRY_DELETE) return ChangeType.DELETED;
        return ChangeType.MODIFIED;
    }

    private static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}
