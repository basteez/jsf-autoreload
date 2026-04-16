package it.bstz.jsfautoreload.core;

import it.bstz.jsfautoreload.logging.ReloadLogger;
import it.bstz.jsfautoreload.model.FileCategory;
import it.bstz.jsfautoreload.model.FileChangeEvent;
import it.bstz.jsfautoreload.model.ReloadNotification;
import it.bstz.jsfautoreload.spi.ContainerAdapter;
import it.bstz.jsfautoreload.spi.ReloadException;
import it.bstz.jsfautoreload.sse.ConnectionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class ReloadCoordinator implements Consumer<ReloadNotification> {

    private final ConnectionManager connectionManager;
    private final Object servletContext;
    private ContainerAdapter containerAdapter;
    private String autoCompileCommand;

    public ReloadCoordinator(ConnectionManager connectionManager, Object servletContext) {
        this.connectionManager = connectionManager;
        this.servletContext = servletContext;
        discoverContainerAdapter();
    }

    public Consumer<FileChangeEvent> asFileChangeListener(Debouncer debouncer) {
        return debouncer::submit;
    }

    @Override
    public void accept(ReloadNotification notification) {
        // Broadcast SSE notification to all connected browsers FIRST.
        // For class changes, context reload destroys the SSE servlet and all connections,
        // so browsers must receive the reload event before the context restarts.
        connectionManager.broadcast(notification);

        // If class change, reload the application context to pick up new classes.
        // The context reload destroys all SSE connections. The client-side script detects
        // the connection loss and triggers a page reload after a brief delay, so browsers
        // refresh even if the SSE event didn't arrive before the connection was torn down.
        if (notification.isRequiresContextReload()) {
            handleContextReload(notification);
        }
    }

    private void handleContextReload(ReloadNotification notification) {
        if (containerAdapter == null) {
            ReloadLogger.warning("RELOAD", "No container adapter available — " +
                    "class reload not possible, browser refresh only");
            return;
        }

        try {
            ReloadLogger.info("RELOAD", notification.getTriggerFile().toString(),
                    "Triggering context reload via " + containerAdapter.containerName());
            containerAdapter.reload(servletContext);
            ReloadLogger.info("RELOAD", "Context reload completed");
        } catch (ReloadException e) {
            ReloadLogger.severe("RELOAD", "Context reload failed", e);
        }
    }

    private void discoverContainerAdapter() {
        ServiceLoader<ContainerAdapter> loader = ServiceLoader.load(ContainerAdapter.class);
        ContainerAdapter best = null;

        for (ContainerAdapter adapter : loader) {
            if (adapter.supports()) {
                if (best == null || adapter.priority() < best.priority()) {
                    best = adapter;
                }
            }
        }

        if (best != null) {
            this.containerAdapter = best;
            ReloadLogger.info("ADAPTER", "Container adapter discovered: " + best.containerName());
        } else {
            ReloadLogger.warning("ADAPTER", "No container adapter found — " +
                    "class reload will not trigger context reload");
        }
    }

    public void setAutoCompileCommand(String command) {
        this.autoCompileCommand = command;
    }

    public void handleSourceChange(FileChangeEvent event) {
        if (event.getFileCategory() != FileCategory.SOURCE) {
            return;
        }

        if (autoCompileCommand == null || autoCompileCommand.isEmpty()) {
            ReloadLogger.warning("AUTO_COMPILE", "SOURCE file changed but autoCompileCommand not configured");
            return;
        }

        ReloadLogger.info("AUTO_COMPILE", event.getFilePath().toString(), "Invoking compile: " + autoCompileCommand);
        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", autoCompileCommand);
            } else {
                pb = new ProcessBuilder("sh", "-c", autoCompileCommand);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ReloadLogger.fine("AUTO_COMPILE", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                ReloadLogger.warning("AUTO_COMPILE", "Compilation failed (exit code " + exitCode + ") — skipping reload");
                return;
            }
            ReloadLogger.info("AUTO_COMPILE", "Compilation succeeded — delegating to CLASS flow");
            // CLASS flow will be triggered by the DirectoryWatcher detecting the new .class files
        } catch (Exception e) {
            ReloadLogger.severe("AUTO_COMPILE", "Failed to execute compile command", e);
        }
    }
}
