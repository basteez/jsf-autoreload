package it.bstz.jsfautoreload;

import it.bstz.jsfautoreload.server.ServerAdapter;
import it.bstz.jsfautoreload.server.ServerConfigParams;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevServerConfigTest {

    private final ServerAdapter stubAdapter = new ServerAdapter() {
        @Override
        public boolean isRunning() { return false; }
        @Override
        public int getHttpPort() { return 9080; }
        @Override
        public String getContextRoot() { return "/"; }
        @Override
        public Path resolveOutputDir(String serverName, Path projectDir) { return projectDir; }
        @Override
        public void writeServerConfig(ServerConfigParams params) { }
    };

    @Test
    void buildsWithAllRequiredFields() {
        DevServerConfig config = DevServerConfig.builder()
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                .serverAdapter(stubAdapter)
                .build();

        assertNotNull(config);
        assertEquals(Paths.get("/tmp/output"), config.getOutputDir());
        assertEquals(1, config.getWatchDirs().size());
        assertEquals(stubAdapter, config.getServerAdapter());
    }

    @Test
    void defaultPortIs35729() {
        DevServerConfig config = DevServerConfig.builder()
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                .serverAdapter(stubAdapter)
                .build();

        assertEquals(35729, config.getPort());
    }

    @Test
    void defaultDebounceMsIs300() {
        DevServerConfig config = DevServerConfig.builder()
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                .serverAdapter(stubAdapter)
                .build();

        assertEquals(300, config.getDebounceMs());
    }

    @Test
    void customPortOverridesDefault() {
        DevServerConfig config = DevServerConfig.builder()
                .port(8080)
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                .serverAdapter(stubAdapter)
                .build();

        assertEquals(8080, config.getPort());
    }

    @Test
    void customDebounceMsOverridesDefault() {
        DevServerConfig config = DevServerConfig.builder()
                .debounceMs(500)
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                .serverAdapter(stubAdapter)
                .build();

        assertEquals(500, config.getDebounceMs());
    }

    @Test
    void throwsExceptionWhenOutputDirMissing() {
        JsfAutoreloadConfigException exception = assertThrows(JsfAutoreloadConfigException.class, () ->
                DevServerConfig.builder()
                        .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                        .serverAdapter(stubAdapter)
                        .build());

        assertTrue(exception.getMessage().contains("outputDir"));
    }

    @Test
    void throwsExceptionWhenWatchDirsMissing() {
        JsfAutoreloadConfigException exception = assertThrows(JsfAutoreloadConfigException.class, () ->
                DevServerConfig.builder()
                        .outputDir(Paths.get("/tmp/output"))
                        .serverAdapter(stubAdapter)
                        .build());

        assertTrue(exception.getMessage().contains("watchDirs"));
    }

    @Test
    void throwsExceptionWhenServerAdapterMissing() {
        JsfAutoreloadConfigException exception = assertThrows(JsfAutoreloadConfigException.class, () ->
                DevServerConfig.builder()
                        .outputDir(Paths.get("/tmp/output"))
                        .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                        .build());

        assertTrue(exception.getMessage().contains("serverAdapter"));
    }

    @Test
    void injectableExecutorIsStoredAndRetrievable() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            DevServerConfig config = DevServerConfig.builder()
                    .outputDir(Paths.get("/tmp/output"))
                    .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                    .serverAdapter(stubAdapter)
                    .executor(executor)
                    .build();

            assertEquals(executor, config.getExecutor());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void executorIsNullByDefault() {
        DevServerConfig config = DevServerConfig.builder()
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(Arrays.asList(Paths.get("src/main/webapp")))
                .serverAdapter(stubAdapter)
                .build();

        assertNull(config.getExecutor());
    }

    @Test
    void watchDirsListIsImmutable() {
        List<Path> dirs = new ArrayList<>(Arrays.asList(Paths.get("src/main/webapp")));
        DevServerConfig config = DevServerConfig.builder()
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(dirs)
                .serverAdapter(stubAdapter)
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                config.getWatchDirs().add(Paths.get("another")));
    }

    @Test
    void externalListMutationDoesNotAffectConfig() {
        List<Path> dirs = new ArrayList<>(Arrays.asList(Paths.get("src/main/webapp")));
        DevServerConfig config = DevServerConfig.builder()
                .outputDir(Paths.get("/tmp/output"))
                .watchDirs(dirs)
                .serverAdapter(stubAdapter)
                .build();

        dirs.add(Paths.get("another"));
        assertEquals(1, config.getWatchDirs().size());
    }
}
