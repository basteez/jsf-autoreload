package it.bstz.jsfautoreload.init;

import it.bstz.jsfautoreload.bridge.BridgeDetector;
import it.bstz.jsfautoreload.bridge.BridgePair;
import it.bstz.jsfautoreload.config.ConfigurationReader;
import it.bstz.jsfautoreload.config.PluginConfiguration;
import it.bstz.jsfautoreload.config.WatchedDirectory;
import it.bstz.jsfautoreload.core.Debouncer;
import it.bstz.jsfautoreload.core.DirectoryWatcher;
import it.bstz.jsfautoreload.core.ReloadCoordinator;
import it.bstz.jsfautoreload.jsf.DevModeGuard;
import it.bstz.jsfautoreload.logging.ReloadLogger;
import it.bstz.jsfautoreload.sse.ConnectionManager;
import it.bstz.jsfautoreload.sse.DefaultSseHandler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoreloadInitializer {

    public void onStartup(Set<Class<?>> classes, Object servletContext) {
        ReloadLogger.info("INIT", "JSF Auto Reload initializer starting");

        // Detect namespace
        BridgePair bridges;
        try {
            bridges = BridgeDetector.detect();
        } catch (IllegalStateException e) {
            ReloadLogger.warning("INIT", "No JSF namespace found — skipping initialization");
            return;
        }

        // Read configuration from context-params
        Map<String, String> contextParams = readContextParams(servletContext);
        PluginConfiguration config = ConfigurationReader.read(contextParams);

        // Check if plugin is explicitly disabled
        if (!config.isEnabled()) {
            ReloadLogger.info("INIT", "Plugin disabled via configuration");
            return;
        }

        // Check development mode
        DevModeGuard devModeGuard = new DevModeGuard(bridges.jsf());
        if (!devModeGuard.isDevelopmentMode(servletContext)) {
            ReloadLogger.info("INIT", "Not in development mode — plugin inactive");
            return;
        }

        ReloadLogger.info("INIT", "Development mode confirmed — bootstrapping components");

        // Component wiring is completed in US1 (T036)
        // This shell sets up the detection and config framework
        bootstrap(bridges, config, servletContext);
    }

    protected void bootstrap(BridgePair bridges, PluginConfiguration config, Object servletContext) {
        // 1. ConnectionManager
        ConnectionManager connectionManager = new ConnectionManager();

        // 2. ReloadCoordinator
        ReloadCoordinator coordinator = new ReloadCoordinator(connectionManager, servletContext);

        // 3. Debouncer → ReloadCoordinator
        Debouncer debouncer = new Debouncer(
                config.getDebounceIntervalMs(),
                config.getClassDebounceIntervalMs(),
                coordinator);

        // 4. SSE Handler + register servlet
        DefaultSseHandler sseHandler = new DefaultSseHandler(bridges.servlet(), connectionManager);
        bridges.servlet().registerServlet(servletContext, config.getSseEndpointPath(), sseHandler);

        // 4b. ScriptInjector — deferred registration via ServletContext attributes
        bridges.jsf().registerDeferredScriptInjector(servletContext, config.getSseEndpointPath());

        // 5. DirectoryWatcher(s)
        List<WatchedDirectory> dirs = config.getWatchedDirectories();
        if (dirs == null || dirs.isEmpty()) {
            // Default watched directories: src/main/webapp and target/classes
            dirs = new ArrayList<>();
            Path webapp = Paths.get("src/main/webapp");
            Path classes = Paths.get("target/classes");
            dirs.add(new WatchedDirectory(webapp));
            dirs.add(new WatchedDirectory(classes));
        }
        List<DirectoryWatcher> watchers = new ArrayList<>();
        for (WatchedDirectory wd : dirs) {
            if (wd.isActive() && wd.getPath().toFile().isDirectory()) {
                DirectoryWatcher watcher = new DirectoryWatcher(
                        wd.getPath(), wd.isRecursive(),
                        wd.getInclusionPatterns(), wd.getExclusionPatterns(),
                        debouncer::submit);
                watcher.start();
                watchers.add(watcher);
            }
        }

        // 6. Shutdown listener
        bridges.servlet().registerShutdownListener(servletContext, () -> {
            ReloadLogger.info("SHUTDOWN", "Shutting down JSF Auto Reload");
            for (DirectoryWatcher w : watchers) {
                w.stop();
            }
            debouncer.shutdown();
            sseHandler.shutdown();
        });

        ReloadLogger.info("INIT", "JSF Auto Reload fully initialized — " +
                watchers.size() + " directory watcher(s) active, SSE at " + config.getSseEndpointPath());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readContextParams(Object servletContext) {
        Map<String, String> params = new HashMap<>();
        try {
            java.lang.reflect.Method getNames = servletContext.getClass()
                    .getMethod("getInitParameterNames");
            java.lang.reflect.Method getParam = servletContext.getClass()
                    .getMethod("getInitParameter", String.class);

            Enumeration<String> names = (Enumeration<String>) getNames.invoke(servletContext);
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = (String) getParam.invoke(servletContext, name);
                params.put(name, value);
            }
        } catch (Exception e) {
            ReloadLogger.warning("INIT", "Failed to read context-params", e);
        }
        return params;
    }
}
