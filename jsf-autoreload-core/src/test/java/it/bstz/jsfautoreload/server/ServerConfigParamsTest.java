package it.bstz.jsfautoreload.server;

import it.bstz.jsfautoreload.JsfAutoreloadConfigException;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigParamsTest {

    @Test
    void builderSetsAllFields() {
        Path outputDir = Paths.get("/tmp/webapp");
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(outputDir)
                .mojarraRefreshPeriod(0)
                .myfacesRefreshPeriod(0)
                .port(35729)
                .build();

        assertEquals(outputDir, params.getOutputDir());
        assertEquals(0, params.getMojarraRefreshPeriod());
        assertEquals(0, params.getMyfacesRefreshPeriod());
        assertEquals(35729, params.getPort());
    }

    @Test
    void defaultValuesAreZero() {
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(Paths.get("/tmp/webapp"))
                .build();

        assertEquals(0, params.getMojarraRefreshPeriod());
        assertEquals(0, params.getMyfacesRefreshPeriod());
        assertEquals(0, params.getPort());
    }

    @Test
    void missingOutputDirThrowsConfigException() {
        JsfAutoreloadConfigException exception = assertThrows(JsfAutoreloadConfigException.class, () ->
                ServerConfigParams.builder().build());
        assertTrue(exception.getMessage().contains("outputDir"));
    }

    @Test
    void builderAllowsCustomRefreshPeriods() {
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(Paths.get("/tmp/webapp"))
                .mojarraRefreshPeriod(5)
                .myfacesRefreshPeriod(10)
                .build();

        assertEquals(5, params.getMojarraRefreshPeriod());
        assertEquals(10, params.getMyfacesRefreshPeriod());
    }

    @Test
    void builderAllowsCustomPort() {
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(Paths.get("/tmp/webapp"))
                .port(8080)
                .build();

        assertEquals(8080, params.getPort());
    }
}
