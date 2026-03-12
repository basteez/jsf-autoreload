package it.bstz.jsfautoreload;

import it.bstz.jsfautoreload.watcher.FileChangeWatcher;
import it.bstz.jsfautoreload.websocket.DevWebSocketServer;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

import org.gradle.api.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@UntrackedTask(because = "Dev server runs indefinitely")
public abstract class JsfDevTask extends DefaultTask {

    @Input
    public abstract Property<Integer> getPort();

    @Input
    public abstract Property<String> getOutputDir();

    @Input
    public abstract ListProperty<String> getWatchDirs();

    @TaskAction
    public void execute() throws InterruptedException {
        int port = getPort().get();
        String outputDirPath = getOutputDir().get();

        DevWebSocketServer wsServer = new DevWebSocketServer(port);
        wsServer.startServer();

        List<Path> watchPaths = getWatchDirs().get().stream()
                .map(dir -> getProject().getProjectDir().toPath().resolve(dir))
                .collect(Collectors.toList());

        Path outputDir = Paths.get(outputDirPath);

        FileChangeWatcher watcher = new FileChangeWatcher(watchPaths,
                changedFile -> {
                    boolean copied = false;
                    for (Path watchPath : watchPaths) {
                        if (changedFile.startsWith(watchPath)) {
                            Path relativePath = watchPath.relativize(changedFile);
                            Path targetPath = outputDir.resolve(relativePath);
                            try {
                                Files.createDirectories(targetPath.getParent());
                                Files.copy(changedFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                getLogger().lifecycle("[JSF Autoreload] Copied: {}", relativePath);
                                copied = true;
                            } catch (IOException e) {
                                getLogger().error("[JSF Autoreload] Failed to copy {}: {}", relativePath, e.getMessage());
                            }
                            break;
                        }
                    }
                    if (copied) {
                        wsServer.broadcastReload();
                    }
                },
                deletedFile -> {
                    for (Path watchPath : watchPaths) {
                        if (deletedFile.startsWith(watchPath)) {
                            Path relativePath = watchPath.relativize(deletedFile);
                            Path targetPath = outputDir.resolve(relativePath);
                            try {
                                Files.deleteIfExists(targetPath);
                                getLogger().lifecycle("[JSF Autoreload] Deleted from output: {}", relativePath);
                            } catch (IOException e) {
                                getLogger().error("[JSF Autoreload] Failed to delete {}: {}", relativePath, e.getMessage());
                            }
                            break;
                        }
                    }
                    wsServer.broadcastReload();
                });

        watcher.start();

        CountDownLatch latch = new CountDownLatch(1);
        Thread shutdownHook = new Thread(() -> latch.countDown());
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        getLogger().lifecycle("[JSF Autoreload] Dev server started on ws://localhost:{}. Watching: {}",
                port, getWatchDirs().get());

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            getLogger().lifecycle("[JSF Autoreload] Shutting down...");
            watcher.stop();
            wsServer.stopServer();
            stopLibertyServer();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is shutting down, cannot remove hook
            }
        }
    }

    private void stopLibertyServer() {
        Task libertyStop = getProject().getTasks().findByName("libertyStop");
        if (libertyStop != null) {
            try {
                getLogger().lifecycle("[JSF Autoreload] Stopping Liberty server...");
                libertyStop.getActions().forEach(action -> action.execute(libertyStop));
                getLogger().lifecycle("[JSF Autoreload] Liberty server stopped.");
            } catch (Exception e) {
                getLogger().warn("[JSF Autoreload] Failed to stop Liberty: {}", e.getMessage());
            }
        }
    }
}
