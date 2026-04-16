package it.bstz.jsfautoreload.init;

import it.bstz.jsfautoreload.bridge.BridgePair;
import it.bstz.jsfautoreload.bridge.JsfBridge;
import it.bstz.jsfautoreload.bridge.ServletBridge;
import it.bstz.jsfautoreload.config.PluginConfiguration;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AutoreloadInitializerTest {

    @Test
    void bootstrapCallsRegisterDeferredScriptInjector() {
        JsfBridge jsfBridge = mock(JsfBridge.class);
        ServletBridge servletBridge = mock(ServletBridge.class);
        BridgePair bridges = new BridgePair(jsfBridge, servletBridge);

        PluginConfiguration config = PluginConfiguration.builder()
                .sseEndpointPath("/_jsf-autoreload/events")
                .build();

        Object mockServletContext = new Object();

        AutoreloadInitializer initializer = new AutoreloadInitializer();
        initializer.bootstrap(bridges, config, mockServletContext);

        verify(jsfBridge).registerDeferredScriptInjector(mockServletContext, "/_jsf-autoreload/events");
    }
}
