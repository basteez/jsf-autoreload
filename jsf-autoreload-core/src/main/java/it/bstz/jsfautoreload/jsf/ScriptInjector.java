package it.bstz.jsfautoreload.jsf;

import it.bstz.jsfautoreload.bridge.JsfBridge;
import it.bstz.jsfautoreload.logging.ReloadLogger;

public final class ScriptInjector {

    private static final String SCRIPT_TEMPLATE =
            "(function(){" +
            "if(typeof EventSource==='undefined')return;" +
            "var es=new EventSource('%s');" +
            "es.addEventListener('reload',function(e){location.reload();});" +
            "})();";

    private final JsfBridge jsfBridge;
    private final String sseEndpointPath;

    public ScriptInjector(JsfBridge jsfBridge, String sseEndpointPath) {
        this.jsfBridge = jsfBridge;
        this.sseEndpointPath = sseEndpointPath;
    }

    public void register(Object application) {
        String scriptContent = String.format(SCRIPT_TEMPLATE, sseEndpointPath);
        jsfBridge.registerScriptInjector(application, scriptContent);
        ReloadLogger.info("SCRIPT_INJECTOR", "Reload script registered for SSE endpoint: " + sseEndpointPath);
    }

    public String getScriptContent() {
        return String.format(SCRIPT_TEMPLATE, sseEndpointPath);
    }
}
