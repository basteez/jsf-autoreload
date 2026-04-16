package it.bstz.jsfautoreload.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum FileCategory {
    VIEW,
    STATIC,
    CLASS,
    SOURCE,
    OTHER;

    private static final Map<String, FileCategory> EXTENSION_MAP = new HashMap<>();

    static {
        // View files
        EXTENSION_MAP.put(".xhtml", VIEW);
        EXTENSION_MAP.put(".jspx", VIEW);
        EXTENSION_MAP.put(".jsp", VIEW);

        // Static resources
        EXTENSION_MAP.put(".css", STATIC);
        EXTENSION_MAP.put(".js", STATIC);
        EXTENSION_MAP.put(".png", STATIC);
        EXTENSION_MAP.put(".jpg", STATIC);
        EXTENSION_MAP.put(".gif", STATIC);
        EXTENSION_MAP.put(".svg", STATIC);
        EXTENSION_MAP.put(".ico", STATIC);
        EXTENSION_MAP.put(".woff", STATIC);
        EXTENSION_MAP.put(".woff2", STATIC);

        // Compiled classes
        EXTENSION_MAP.put(".class", CLASS);

        // Source files
        EXTENSION_MAP.put(".java", SOURCE);
    }

    public static FileCategory fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return OTHER;
        }
        FileCategory category = EXTENSION_MAP.get(extension.toLowerCase(Locale.ROOT));
        return category != null ? category : OTHER;
    }
}
