package it.bstz.jsfautoreload;

import it.bstz.jsfautoreload.compiler.JavaSourceCompiler;
import it.bstz.jsfautoreload.watcher.FileChangeWatcher;
import it.bstz.jsfautoreload.websocket.DevWebSocketServer;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UntrackedTask(because = "Dev server runs indefinitely")
public abstract class JsfDevTask extends DefaultTask {

    @Input
    public abstract Property<Integer> getPort();

    @Input
    public abstract Property<String> getOutputDir();

    @Input
    public abstract ListProperty<String> getWatchDirs();

    @Input
    @Optional
    public abstract Property<Boolean> getWatchClasses();

    @Internal
    public abstract Property<String> getProjectDir();

    @Internal
    public abstract Property<String> getCompileClasspath();

    @Internal
    public abstract Property<String> getSourceCompatibility();

    @Internal
    public abstract ListProperty<String> getSourceDirs();

    @Internal
    public abstract Property<String> getClassesOutputDir();

    @TaskAction
    public void execute() throws InterruptedException {
        int port = getPort().get();
        Path outputDir = Paths.get(getOutputDir().get());
        Path projectDir = Paths.get(getProjectDir().get());

        DevWebSocketServer wsServer = new DevWebSocketServer(port);
        wsServer.startServer();

        // Webapp file watcher
        List<Path> watchPaths = getWatchDirs().get().stream()
                .map(dir -> projectDir.resolve(dir))
                .collect(Collectors.toList());

        FileChangeWatcher webappWatcher = createWebappWatcher(watchPaths, outputDir, wsServer);
        webappWatcher.start();

        // Java source watcher (optional)
        FileChangeWatcher sourceWatcher = null;
        ScheduledExecutorService compileDebouncer = null;

        boolean classWatchEnabled = getWatchClasses().getOrElse(false)
                && getCompileClasspath().isPresent()
                && getCompileClasspath().getOrElse("").length() > 0
                && getSourceDirs().isPresent()
                && !getSourceDirs().get().isEmpty()
                && getClassesOutputDir().isPresent();

        if (classWatchEnabled) {
            List<Path> sourcePaths = getSourceDirs().get().stream()
                    .map(Paths::get)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());

            if (!sourcePaths.isEmpty()) {
                compileDebouncer = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "jsf-autoreload-compiler");
                    t.setDaemon(true);
                    return t;
                });

                sourceWatcher = createSourceWatcher(sourcePaths, outputDir, wsServer, compileDebouncer);
                sourceWatcher.start();

                getLogger().lifecycle("[JSF Autoreload] Watching Java sources: {}", getSourceDirs().get());
            }
        }

        getLogger().lifecycle("[JSF Autoreload] Dev server started on ws://localhost:{}. Watching: {}",
                port, getWatchDirs().get());

        // Shutdown hook performs cleanup directly for robustness during JVM shutdown
        CountDownLatch latch = new CountDownLatch(1);
        FileChangeWatcher finalSourceWatcher = sourceWatcher;
        ScheduledExecutorService finalCompileDebouncer = compileDebouncer;
        Thread shutdownHook = new Thread(() -> {
            webappWatcher.stop();
            if (finalSourceWatcher != null) {
                finalSourceWatcher.stop();
            }
            if (finalCompileDebouncer != null) {
                finalCompileDebouncer.shutdownNow();
            }
            wsServer.stopServer();
            latch.countDown();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            getLogger().lifecycle("[JSF Autoreload] Shutting down...");
            // Idempotent cleanup — may already be done by shutdown hook
            webappWatcher.stop();
            if (sourceWatcher != null) {
                sourceWatcher.stop();
            }
            if (compileDebouncer != null) {
                compileDebouncer.shutdownNow();
            }
            wsServer.stopServer();
            stopLibertyServer();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is shutting down, cannot remove hook
            }
        }
    }

    private FileChangeWatcher createWebappWatcher(List<Path> watchPaths, Path outputDir,
                                                   DevWebSocketServer wsServer) {
        return new FileChangeWatcher(watchPaths,
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
    }

    private FileChangeWatcher createSourceWatcher(List<Path> sourcePaths, Path outputDir,
                                                   DevWebSocketServer wsServer,
                                                   ScheduledExecutorService debouncer) {
        AtomicReference<ScheduledFuture<?>> pendingCompile = new AtomicReference<>();

        return new FileChangeWatcher(sourcePaths,
                changedFile -> {
                    if (changedFile.toString().endsWith(".java")) {
                        getLogger().lifecycle("[JSF Autoreload] Java source changed: {}", changedFile.getFileName());
                        scheduleCompile(pendingCompile, debouncer, outputDir, wsServer);
                    }
                },
                deletedFile -> {
                    if (deletedFile.toString().endsWith(".java")) {
                        getLogger().lifecycle("[JSF Autoreload] Java source deleted: {}", deletedFile.getFileName());
                        scheduleCompile(pendingCompile, debouncer, outputDir, wsServer);
                    }
                });
    }

    private void scheduleCompile(AtomicReference<ScheduledFuture<?>> pendingCompile,
                                  ScheduledExecutorService debouncer,
                                  Path outputDir, DevWebSocketServer wsServer) {
        ScheduledFuture<?> prev = pendingCompile.getAndSet(
                debouncer.schedule(() -> compileAndSync(outputDir, wsServer),
                        800, TimeUnit.MILLISECONDS));
        if (prev != null) {
            prev.cancel(false);
        }
    }

    private void compileAndSync(Path outputDir, DevWebSocketServer wsServer) {
        try {
            List<Path> sourcePaths = getSourceDirs().get().stream()
                    .map(Paths::get)
                    .collect(Collectors.toList());
            Path classesDir = Paths.get(getClassesOutputDir().get());

            JavaSourceCompiler compiler = new JavaSourceCompiler(
                    sourcePaths,
                    getCompileClasspath().get(),
                    getSourceCompatibility().getOrElse("11"),
                    classesDir);

            getLogger().lifecycle("[JSF Autoreload] Compiling Java sources...");
            JavaSourceCompiler.CompileResult result = compiler.compile();

            if (!result.isSuccess()) {
                getLogger().error("[JSF Autoreload] Compilation failed:\n{}", result.getOutput());
                return;
            }

            getLogger().lifecycle("[JSF Autoreload] Compilation successful");

            // Sync classes to WEB-INF/classes in the exploded WAR
            Path targetClassesDir = outputDir.resolve("WEB-INF/classes");
            syncDirectory(classesDir, targetClassesDir);

            getLogger().lifecycle("[JSF Autoreload] Synced classes to exploded WAR");
            wsServer.broadcastReload();

        } catch (Exception e) {
            getLogger().error("[JSF Autoreload] Recompile failed: {}", e.getMessage());
        }
    }

    private void syncDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            walk.filter(Files::isRegularFile)
                .forEach(file -> {
                    Path rel = source.relativize(file);
                    Path dest = target.resolve(rel);
                    try {
                        Files.createDirectories(dest.getParent());
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        getLogger().error("[JSF Autoreload] Failed to sync {}: {}", rel, e.getMessage());
                    }
                });
        }
    }

    private void stopLibertyServer() {
        // TODO: This uses getProject() at execution time, which is incompatible with
        // Gradle configuration cache. Acceptable for @UntrackedTask dev server but
        // should be refactored if configuration cache support is required.
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
