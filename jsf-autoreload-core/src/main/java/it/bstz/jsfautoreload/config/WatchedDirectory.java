package it.bstz.jsfautoreload.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class WatchedDirectory {

    private static final Set<String> DEFAULT_INCLUSION_PATTERNS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "**/*.xhtml", "**/*.jspx", "**/*.jsp",
                    "**/*.css", "**/*.js",
                    "**/*.png", "**/*.jpg", "**/*.gif", "**/*.svg", "**/*.ico",
                    "**/*.woff", "**/*.woff2",
                    "**/*.class"
            )));

    private static final Set<String> DEFAULT_EXCLUSION_PATTERNS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "**/.*", "**/node_modules/**"
            )));

    private final Path path;
    private final Set<String> inclusionPatterns;
    private final Set<String> exclusionPatterns;
    private final boolean recursive;
    private final boolean active;

    public WatchedDirectory(Path path) {
        this(path, DEFAULT_INCLUSION_PATTERNS, DEFAULT_EXCLUSION_PATTERNS, true, true);
    }

    public WatchedDirectory(Path path, Set<String> inclusionPatterns, Set<String> exclusionPatterns,
                            boolean recursive, boolean active) {
        this.path = path;
        this.inclusionPatterns = inclusionPatterns;
        this.exclusionPatterns = exclusionPatterns;
        this.recursive = recursive;
        this.active = active;
    }

    public Path getPath() {
        return path;
    }

    public Set<String> getInclusionPatterns() {
        return inclusionPatterns;
    }

    public Set<String> getExclusionPatterns() {
        return exclusionPatterns;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public boolean isActive() {
        return active;
    }
}
