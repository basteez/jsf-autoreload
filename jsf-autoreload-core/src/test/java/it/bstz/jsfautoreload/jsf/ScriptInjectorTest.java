package it.bstz.jsfautoreload.jsf;

import it.bstz.jsfautoreload.bridge.JsfBridge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScriptInjectorTest {

    @Test
    void getScriptContentIncludesContextPathAndEndpoint() {
        JsfBridge bridge = mock(JsfBridge.class);
        ScriptInjector injector = new ScriptInjector(bridge, "/myapp/_jsf-autoreload/events");

        String script = injector.getScriptContent();

        assertTrue(script.contains("new EventSource('/myapp/_jsf-autoreload/events')"),
                "Script should include context path in EventSource URL, got: " + script);
    }

    @Test
    void getScriptContentWorksWithRootContextPath() {
        JsfBridge bridge = mock(JsfBridge.class);
        ScriptInjector injector = new ScriptInjector(bridge, "/_jsf-autoreload/events");

        String script = injector.getScriptContent();

        assertTrue(script.contains("new EventSource('/_jsf-autoreload/events')"),
                "Script should work with root context path, got: " + script);
    }
}
