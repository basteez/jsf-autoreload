package it.bstz.jsfautoreload;

import it.bstz.jsfautoreload.server.ServerAdapter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Immutable configuration for the dev-loop server.
 *
 * <p>Use the {@link Builder} to construct instances:</p>
 * <pre>
 * DevServerConfig config = DevServerConfig.builder()
 *     .port(35729)
 *     .outputDir(path)
 *     .watchDirs(dirs)
 *     .serverAdapter(adapter)
 *     .debounceMs(300)
 *     .build();
 * </pre>
 */
public final class DevServerConfig {

    private static final int DEFAULT_PORT = 35729;
    private static final long DEFAULT_DEBOUNCE_MS = 300;

    private final int port;
    private final Path outputDir;
    private final List<Path> watchDirs;
    private final ServerAdapter serverAdapter;
    private final long debounceMs;
    private final ScheduledExecutorService executor;

    private DevServerConfig(Builder builder) {
        this.port = builder.port;
        this.outputDir = builder.outputDir;
        this.watchDirs = Collections.unmodifiableList(new ArrayList<>(builder.watchDirs));
        this.serverAdapter = builder.serverAdapter;
        this.debounceMs = builder.debounceMs;
        this.executor = builder.executor;
    }

    public int getPort() {
        return port;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public List<Path> getWatchDirs() {
        return watchDirs;
    }

    public ServerAdapter getServerAdapter() {
        return serverAdapter;
    }

    public long getDebounceMs() {
        return debounceMs;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int port = DEFAULT_PORT;
        private Path outputDir;
        private List<Path> watchDirs;
        private ServerAdapter serverAdapter;
        private long debounceMs = DEFAULT_DEBOUNCE_MS;
        private ScheduledExecutorService executor;

        private Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder outputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder watchDirs(List<Path> watchDirs) {
            this.watchDirs = new ArrayList<>(watchDirs);
            return this;
        }

        public Builder serverAdapter(ServerAdapter serverAdapter) {
            this.serverAdapter = serverAdapter;
            return this;
        }

        public Builder debounceMs(long debounceMs) {
            this.debounceMs = debounceMs;
            return this;
        }

        public Builder executor(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public DevServerConfig build() {
            if (outputDir == null) {
                throw new JsfAutoreloadConfigException(
                        "[JSF Autoreload] Missing required configuration: outputDir. "
                                + "Set it via jsfAutoreload { outputDir = '...' }.");
            }
            if (watchDirs == null || watchDirs.isEmpty()) {
                throw new JsfAutoreloadConfigException(
                        "[JSF Autoreload] Missing required configuration: watchDirs. "
                                + "Set it via jsfAutoreload { watchDirs = ['src/main/webapp'] }.");
            }
            if (serverAdapter == null) {
                throw new JsfAutoreloadConfigException(
                        "[JSF Autoreload] Missing required configuration: serverAdapter. "
                                + "Ensure a server adapter is configured.");
            }
            return new DevServerConfig(this);
        }
    }
}
