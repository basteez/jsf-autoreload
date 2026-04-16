package it.bstz.jsfautoreload.jsf;

import it.bstz.jsfautoreload.bridge.JsfBridge;
import it.bstz.jsfautoreload.logging.ReloadLogger;

public final class DevModeGuard {

    private final JsfBridge jsfBridge;
    private final String projectStageParam;

    public DevModeGuard(JsfBridge jsfBridge) {
        this.jsfBridge = jsfBridge;
        this.projectStageParam = jsfBridge.projectStageParamName();
    }

    public boolean isDevelopmentMode(Object servletContext) {
        // Check context-param directly (before JSF is fully initialized)
        if (servletContext != null) {
            try {
                java.lang.reflect.Method getInitParam = servletContext.getClass()
                        .getMethod("getInitParameter", String.class);
                String stage = (String) getInitParam.invoke(servletContext, projectStageParam);
                if ("Development".equalsIgnoreCase(stage)) {
                    ReloadLogger.info("DEV_MODE", "Development mode detected via context-param");
                    return true;
                }
                if (stage != null) {
                    ReloadLogger.info("DEV_MODE", "Project stage is '" + stage + "' — plugin inactive");
                    return false;
                }
            } catch (Exception e) {
                ReloadLogger.warning("DEV_MODE", "Failed to read project stage context-param", e);
            }
        }
        // Default: JSF defaults to Production if not set
        ReloadLogger.info("DEV_MODE", "No PROJECT_STAGE context-param found — defaulting to inactive");
        return false;
    }
}
