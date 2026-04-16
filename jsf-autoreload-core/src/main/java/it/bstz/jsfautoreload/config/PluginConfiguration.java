package it.bstz.jsfautoreload.config;

import java.util.List;
import java.util.Set;

public final class PluginConfiguration {

    private final boolean enabled;
    private final List<WatchedDirectory> watchedDirectories;
    private final Set<String> excludePatterns;
    private final long debounceIntervalMs;
    private final long classDebounceIntervalMs;
    private final String sseEndpointPath;
    private final boolean autoCompileEnabled;
    private final String autoCompileCommand;
    private final String sourceDirectory;

    private PluginConfiguration(Builder builder) {
        this.enabled = builder.enabled;
        this.watchedDirectories = builder.watchedDirectories;
        this.excludePatterns = builder.excludePatterns;
        this.debounceIntervalMs = builder.debounceIntervalMs;
        this.classDebounceIntervalMs = builder.classDebounceIntervalMs;
        this.sseEndpointPath = builder.sseEndpointPath;
        this.autoCompileEnabled = builder.autoCompileEnabled;
        this.autoCompileCommand = builder.autoCompileCommand;
        this.sourceDirectory = builder.sourceDirectory;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<WatchedDirectory> getWatchedDirectories() {
        return watchedDirectories;
    }

    public Set<String> getExcludePatterns() {
        return excludePatterns;
    }

    public long getDebounceIntervalMs() {
        return debounceIntervalMs;
    }

    public long getClassDebounceIntervalMs() {
        return classDebounceIntervalMs;
    }

    public String getSseEndpointPath() {
        return sseEndpointPath;
    }

    public boolean isAutoCompileEnabled() {
        return autoCompileEnabled;
    }

    public String getAutoCompileCommand() {
        return autoCompileCommand;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean enabled = true;
        private List<WatchedDirectory> watchedDirectories;
        private Set<String> excludePatterns;
        private long debounceIntervalMs = 500L;
        private long classDebounceIntervalMs = 1000L;
        private String sseEndpointPath = "/_jsf-autoreload/events";
        private boolean autoCompileEnabled = false;
        private String autoCompileCommand;
        private String sourceDirectory = "src/main/java";

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder watchedDirectories(List<WatchedDirectory> watchedDirectories) {
            this.watchedDirectories = watchedDirectories;
            return this;
        }

        public Builder excludePatterns(Set<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
            return this;
        }

        public Builder debounceIntervalMs(long debounceIntervalMs) {
            this.debounceIntervalMs = debounceIntervalMs;
            return this;
        }

        public Builder classDebounceIntervalMs(long classDebounceIntervalMs) {
            this.classDebounceIntervalMs = classDebounceIntervalMs;
            return this;
        }

        public Builder sseEndpointPath(String sseEndpointPath) {
            this.sseEndpointPath = sseEndpointPath;
            return this;
        }

        public Builder autoCompileEnabled(boolean autoCompileEnabled) {
            this.autoCompileEnabled = autoCompileEnabled;
            return this;
        }

        public Builder autoCompileCommand(String autoCompileCommand) {
            this.autoCompileCommand = autoCompileCommand;
            return this;
        }

        public Builder sourceDirectory(String sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
            return this;
        }

        public PluginConfiguration build() {
            return new PluginConfiguration(this);
        }
    }
}
