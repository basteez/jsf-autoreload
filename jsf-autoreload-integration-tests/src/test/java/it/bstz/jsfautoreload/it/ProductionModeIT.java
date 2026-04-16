package it.bstz.jsfautoreload.it;

import it.bstz.jsfautoreload.config.ConfigurationReader;
import it.bstz.jsfautoreload.config.PluginConfiguration;
import it.bstz.jsfautoreload.jsf.DevModeGuard;
import it.bstz.jsfautoreload.bridge.BridgeDetector;
import it.bstz.jsfautoreload.bridge.BridgePair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductionModeIT {

    @Test
    void productionMode_pluginStaysInactive() {
        // Simulate production mode — no Development stage set
        BridgePair bridges = BridgeDetector.detect();
        DevModeGuard guard = new DevModeGuard(bridges.jsf());

        // With null context (no servlet context), should default to inactive
        assertFalse(guard.isDevelopmentMode(null),
                "FR-008: Plugin must be inactive when not in Development mode");
    }

    @Test
    void explicitlyDisabled_pluginDoesNotActivate() {
        Map<String, String> params = new HashMap<>();
        params.put("it.bstz.jsfautoreload.enabled", "false");

        PluginConfiguration config = ConfigurationReader.read(params);

        assertFalse(config.isEnabled(),
                "SC-005: Plugin must respect explicit disable configuration");
    }

    @Test
    void productionStage_pluginDoesNotActivate() {
        // When PROJECT_STAGE is Production, the DevModeGuard should reject
        BridgePair bridges = BridgeDetector.detect();
        DevModeGuard guard = new DevModeGuard(bridges.jsf());

        // Mock a context with Production stage via a simple object
        MockServletContext ctx = new MockServletContext("Production", bridges.jsf().projectStageParamName());
        assertFalse(guard.isDevelopmentMode(ctx),
                "FR-008: Plugin must be inactive when PROJECT_STAGE is Production");
    }

    private static class MockServletContext {
        private final String stage;
        private final String paramName;

        MockServletContext(String stage, String paramName) {
            this.stage = stage;
            this.paramName = paramName;
        }

        public String getInitParameter(String name) {
            if (paramName.equals(name)) {
                return stage;
            }
            return null;
        }
    }
}
