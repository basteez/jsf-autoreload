package it.bstz.jsfautoreload.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReloadLogger {

    private static final Logger LOGGER = Logger.getLogger("it.bstz.jsfautoreload");
    private static final String PREFIX = "[JSF-AUTORELOAD]";

    private ReloadLogger() {
    }

    public static void fine(String eventType, String details) {
        log(Level.FINE, eventType, null, details);
    }

    public static void fine(String eventType, String filePath, String details) {
        log(Level.FINE, eventType, filePath, details);
    }

    public static void info(String eventType, String details) {
        log(Level.INFO, eventType, null, details);
    }

    public static void info(String eventType, String filePath, String details) {
        log(Level.INFO, eventType, filePath, details);
    }

    public static void warning(String eventType, String details) {
        log(Level.WARNING, eventType, null, details);
    }

    public static void warning(String eventType, String details, Throwable t) {
        LOGGER.log(Level.WARNING, format(Level.WARNING, eventType, null, details), t);
    }

    public static void severe(String eventType, String details) {
        log(Level.SEVERE, eventType, null, details);
    }

    public static void severe(String eventType, String details, Throwable t) {
        LOGGER.log(Level.SEVERE, format(Level.SEVERE, eventType, null, details), t);
    }

    private static void log(Level level, String eventType, String filePath, String details) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, format(level, eventType, filePath, details));
        }
    }

    private static String format(Level level, String eventType, String filePath, String details) {
        StringBuilder sb = new StringBuilder(PREFIX);
        sb.append(' ').append(level.getName());
        sb.append(" | ").append(eventType);
        sb.append(" | ").append(filePath != null ? filePath : "-");
        sb.append(" | ").append(details);
        return sb.toString();
    }
}
