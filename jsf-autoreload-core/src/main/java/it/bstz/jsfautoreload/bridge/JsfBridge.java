package it.bstz.jsfautoreload.bridge;

public interface JsfBridge {

    boolean isDevelopmentMode(Object facesContext);

    void registerScriptInjector(Object application, String scriptContent);

    void registerDeferredScriptInjector(Object servletContext, String sseEndpointPath);

    String projectStageParamName();
}
