package it.bstz.jsfautoreload.bridge;

public interface JsfBridge {

    boolean isDevelopmentMode(Object facesContext);

    void registerScriptInjector(Object application, String scriptContent);

    String projectStageParamName();
}
