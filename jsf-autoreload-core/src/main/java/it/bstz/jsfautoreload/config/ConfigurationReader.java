package it.bstz.jsfautoreload.config;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigurationReader {

    private static final String CONTEXT_PARAM_PREFIX = "it.bstz.jsfautoreload.";
    private static final String SYSTEM_PROP_PREFIX = "jsfautoreload.";

    private ConfigurationReader() {
    }

    public static PluginConfiguration read(Map<String, String> contextParams) {
        PluginConfiguration.Builder builder = PluginConfiguration.builder();

        builder.enabled(resolveBoolean(contextParams, "enabled", true));
        builder.debounceIntervalMs(resolveLong(contextParams, "debounceMs", 500L));
        builder.classDebounceIntervalMs(resolveLong(contextParams, "classDebounceMs", 1000L));
        builder.sseEndpointPath(resolveString(contextParams, "sseEndpointPath", "/_jsf-autoreload/events"));
        builder.autoCompileEnabled(resolveBoolean(contextParams, "autoCompileEnabled", false));
        builder.autoCompileCommand(resolveString(contextParams, "autoCompileCommand", null));
        builder.sourceDirectory(resolveString(contextParams, "sourceDirectory", "src/main/java"));

        // Custom watched directories
        String watchDirs = resolveString(contextParams, "watchDirs", null);
        if (watchDirs != null && !watchDirs.trim().isEmpty()) {
            List<WatchedDirectory> dirs = new ArrayList<>();
            for (String dir : watchDirs.split(",")) {
                String trimmed = dir.trim();
                if (!trimmed.isEmpty()) {
                    dirs.add(new WatchedDirectory(Paths.get(trimmed)));
                }
            }
            builder.watchedDirectories(dirs);
        }

        // Custom exclusion patterns
        String excludePatterns = resolveString(contextParams, "excludePatterns", null);
        if (excludePatterns != null && !excludePatterns.trim().isEmpty()) {
            Set<String> patterns = new LinkedHashSet<>();
            for (String pattern : excludePatterns.split(",")) {
                String trimmed = pattern.trim();
                if (!trimmed.isEmpty()) {
                    patterns.add(trimmed);
                }
            }
            builder.excludePatterns(patterns);
        }

        return builder.build();
    }

    private static String resolveString(Map<String, String> contextParams, String key, String defaultValue) {
        // System property takes precedence
        String sysProp = System.getProperty(SYSTEM_PROP_PREFIX + key);
        if (sysProp != null) {
            return sysProp;
        }
        // Then context-param
        String ctxParam = contextParams.get(CONTEXT_PARAM_PREFIX + key);
        if (ctxParam != null) {
            return ctxParam;
        }
        return defaultValue;
    }

    private static boolean resolveBoolean(Map<String, String> contextParams, String key, boolean defaultValue) {
        String value = resolveString(contextParams, key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private static long resolveLong(Map<String, String> contextParams, String key, long defaultValue) {
        String value = resolveString(contextParams, key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
