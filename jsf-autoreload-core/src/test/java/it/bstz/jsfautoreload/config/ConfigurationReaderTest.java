package it.bstz.jsfautoreload.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationReaderTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("jsfautoreload.enabled");
        System.clearProperty("jsfautoreload.debounceMs");
        System.clearProperty("jsfautoreload.classDebounceMs");
        System.clearProperty("jsfautoreload.sseEndpointPath");
        System.clearProperty("jsfautoreload.watchDirs");
        System.clearProperty("jsfautoreload.excludePatterns");
    }

    @Test
    void read_noParams_returnsDefaults() {
        Map<String, String> contextParams = new HashMap<>();
        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertTrue(config.isEnabled());
        assertEquals(500L, config.getDebounceIntervalMs());
        assertEquals(1000L, config.getClassDebounceIntervalMs());
        assertEquals("/_jsf-autoreload/events", config.getSseEndpointPath());
        assertFalse(config.isAutoCompileEnabled());
        assertNull(config.getAutoCompileCommand());
    }

    @Test
    void read_contextParamOverrides_appliedCorrectly() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.enabled", "false");
        contextParams.put("it.bstz.jsfautoreload.debounceMs", "300");
        contextParams.put("it.bstz.jsfautoreload.classDebounceMs", "2000");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertFalse(config.isEnabled());
        assertEquals(300L, config.getDebounceIntervalMs());
        assertEquals(2000L, config.getClassDebounceIntervalMs());
    }

    @Test
    void read_systemPropertyOverrides_takePrecedenceOverContextParams() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.debounceMs", "300");

        System.setProperty("jsfautoreload.debounceMs", "100");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertEquals(100L, config.getDebounceIntervalMs());
    }

    @Test
    void read_customWatchDirs_parsedFromCommaSeparated() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.watchDirs", "src/main/webapp,src/main/resources");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertNotNull(config.getWatchedDirectories());
        assertEquals(2, config.getWatchedDirectories().size());
    }

    @Test
    void read_customExcludePatterns_parsedFromCommaSeparated() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.excludePatterns", "**/.git/**,**/target/**");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertNotNull(config.getExcludePatterns());
        assertEquals(2, config.getExcludePatterns().size());
        assertTrue(config.getExcludePatterns().contains("**/.git/**"));
        assertTrue(config.getExcludePatterns().contains("**/target/**"));
    }

    @Test
    void read_customDebounceValues() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.debounceMs", "250");
        contextParams.put("it.bstz.jsfautoreload.classDebounceMs", "3000");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertEquals(250L, config.getDebounceIntervalMs());
        assertEquals(3000L, config.getClassDebounceIntervalMs());
    }

    @Test
    void read_customSseEndpointPath() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.sseEndpointPath", "/_custom/sse");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertEquals("/_custom/sse", config.getSseEndpointPath());
    }

    @Test
    void read_enabledToggle_disablesPlugin() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.enabled", "false");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertFalse(config.isEnabled());
    }

    @Test
    void read_enabledToggle_viaSystemProperty() {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("it.bstz.jsfautoreload.enabled", "true");
        System.setProperty("jsfautoreload.enabled", "false");

        PluginConfiguration config = ConfigurationReader.read(contextParams);

        assertFalse(config.isEnabled(), "System property should override context-param");
    }
}
