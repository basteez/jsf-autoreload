package it.bstz.jsfautoreload;

import it.bstz.jsfautoreload.watcher.FileChangeWatcher;
import it.bstz.jsfautoreload.websocket.DevWebSocketServer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates the dev-loop: watches source files, copies changes to the
 * exploded WAR output directory, and broadcasts reload notifications to
 * connected browsers via WebSocket.
 *
 * <p>Plugin modules build a {@link DevServerConfig} and call
 * {@link #start(DevServerConfig)}. They should NOT directly interact
 * with {@link FileChangeWatcher} or {@link DevWebSocketServer}.</p>
 *
 * <p>Implements {@link Closeable} for try-with-resources in tests.</p>
 */
public class DevServer implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(DevServer.class.getName());

    private volatile DevWebSocketServer wsServer;
    private volatile FileChangeWatcher watcher;
    private volatile ScheduledExecutorService executor;
    private volatile boolean ownsExecutor;
    private volatile boolean closed;
    private final AtomicReference<ScheduledFuture<?>> pendingReload = new AtomicReference<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * Starts the dev server: validates configuration, initializes components,
     * and enters a blocking dev loop. Returns when the server is shut down.
     *
     * @param config the dev server configuration
     * @throws JsfAutoreloadConfigException if the output directory does not exist
     * @throws JsfAutoreloadException if any component fails to start
     */
    public void start(DevServerConfig config) {
        startNonBlocking(config);

        // Register shutdown hook
        Thread shutdownHook = new Thread(() -> {
            close();
            shutdownLatch.countDown();
        }, "jsf-autoreload-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // Block until shutdown
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            close();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is shutting down, cannot remove hook
            }
        }
    }

    /**
     * Starts the dev server components without blocking.
     * Useful for testing — call {@link #close()} to shut down.
     *
     * @param config the dev server configuration
     * @throws JsfAutoreloadConfigException if the output directory does not exist
     * @throws JsfAutoreloadException if any component fails to start
     */
    public void startNonBlocking(DevServerConfig config) {
        Path outputDir = config.getOutputDir();
        if (!Files.isDirectory(outputDir)) {
            throw new JsfAutoreloadConfigException(
                    "[JSF Autoreload] Output directory not found: " + outputDir
                            + ". Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your server name.");
        }

        if (config.getExecutor() != null) {
            executor = config.getExecutor();
            ownsExecutor = false;
        } else {
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "jsf-autoreload-debounce");
                t.setDaemon(true);
                return t;
            });
            ownsExecutor = true;
        }

        watcher = new FileChangeWatcher(
                config.getWatchDirs(),
                changedFile -> onFileChanged(changedFile, config),
                deletedFile -> onFileDeleted(deletedFile, config)
        );
        watcher.start();

        wsServer = new DevWebSocketServer(config.getPort());
        wsServer.startServer();

        LOGGER.info("[JSF Autoreload] WebSocket server listening on ws://localhost:" + config.getPort()
                + ". Watching: " + config.getWatchDirs());
    }

    private void onFileChanged(Path changedFile, DevServerConfig config) {
        for (Path watchDir : config.getWatchDirs()) {
            if (changedFile.startsWith(watchDir)) {
                Path relativePath = watchDir.relativize(changedFile);
                Path targetPath = config.getOutputDir().resolve(relativePath);
                try {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(changedFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.fine("[JSF Autoreload] Copied: " + relativePath);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "[JSF Autoreload] Failed to copy " + relativePath + ".", e);
                }
                break;
            }
        }
        scheduleReload(config);
    }

    private void onFileDeleted(Path deletedFile, DevServerConfig config) {
        LOGGER.fine("[JSF Autoreload] File deleted: " + deletedFile.getFileName());
        scheduleReload(config);
    }

    private void scheduleReload(DevServerConfig config) {
        ScheduledFuture<?> prev = pendingReload.get();
        if (prev != null) {
            prev.cancel(false);
        }
        pendingReload.set(executor.schedule(() -> {
            DevWebSocketServer ws = wsServer;
            if (ws != null) {
                ws.broadcastReload();
            }
        }, config.getDebounceMs(), TimeUnit.MILLISECONDS));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        // Cancel pending debounce
        ScheduledFuture<?> pending = pendingReload.getAndSet(null);
        if (pending != null) {
            pending.cancel(false);
        }

        // Shutdown in reverse order: executor -> WebSocket server -> file watcher
        if (ownsExecutor && executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (wsServer != null) {
            wsServer.close();
        }

        if (watcher != null) {
            watcher.close();
        }

        shutdownLatch.countDown();
        LOGGER.info("[JSF Autoreload] Dev server stopped.");
    }
}
