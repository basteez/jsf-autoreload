package it.bstz.jsfautoreload.bridge;

import it.bstz.jsfautoreload.bridge.jakarta.JakartaJsfBridge;
import it.bstz.jsfautoreload.bridge.jakarta.JakartaServletBridge;
import it.bstz.jsfautoreload.bridge.javax.JavaxJsfBridge;
import it.bstz.jsfautoreload.bridge.javax.JavaxServletBridge;
import it.bstz.jsfautoreload.logging.ReloadLogger;

public final class BridgeDetector {

    private BridgeDetector() {
    }

    public static BridgePair detect() {
        // Jakarta-first detection order
        if (isClassAvailable("jakarta.faces.context.FacesContext")) {
            ReloadLogger.info("BRIDGE_DETECT", "Detected Jakarta Faces namespace");
            return new BridgePair(new JakartaJsfBridge(), new JakartaServletBridge());
        }

        if (isClassAvailable("javax.faces.context.FacesContext")) {
            ReloadLogger.info("BRIDGE_DETECT", "Detected javax.faces namespace");
            return new BridgePair(new JavaxJsfBridge(), new JavaxServletBridge());
        }

        throw new IllegalStateException(
                "Neither jakarta.faces nor javax.faces found on classpath. " +
                "JSF Auto Reload requires a JSF implementation.");
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
