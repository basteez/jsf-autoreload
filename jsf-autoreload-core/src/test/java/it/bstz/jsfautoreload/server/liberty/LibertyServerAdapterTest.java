package it.bstz.jsfautoreload.server.liberty;

import it.bstz.jsfautoreload.server.ServerConfigParams;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibertyServerAdapterTest {

    private MockWebServer mockServer;

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null) {
            mockServer.shutdown();
        }
    }

    @Test
    void isRunningReturnsTrueWhenServerReturns200() throws IOException {
        mockServer = new MockWebServer();
        mockServer.enqueue(new MockResponse().setResponseCode(200));
        mockServer.start();

        LibertyServerAdapter adapter = new LibertyServerAdapter(mockServer.getPort(), "/", "defaultServer", Path.of("."));
        assertTrue(adapter.isRunning());
    }

    @Test
    void isRunningReturnsTrueWhenServerReturns404() throws IOException {
        mockServer = new MockWebServer();
        mockServer.enqueue(new MockResponse().setResponseCode(404));
        mockServer.start();

        LibertyServerAdapter adapter = new LibertyServerAdapter(mockServer.getPort(), "/", "defaultServer", Path.of("."));
        assertTrue(adapter.isRunning());
    }

    @Test
    void isRunningReturnsFalseWhenNothingListening() throws Exception {
        int port = findFreePort();
        LibertyServerAdapter adapter = new LibertyServerAdapter(port, "/", "defaultServer", Path.of("."));
        assertFalse(adapter.isRunning());
    }

    @Test
    void getHttpPortReturnsConfiguredValue() {
        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/myapp", "defaultServer", Path.of("."));
        assertEquals(9080, adapter.getHttpPort());
    }

    @Test
    void getContextRootReturnsConfiguredValue() {
        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/myapp", "defaultServer", Path.of("."));
        assertEquals("/myapp", adapter.getContextRoot());
    }

    @Test
    void resolveOutputDirReturnsLibertyConventionPath(@TempDir Path projectDir) {
        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/", "defaultServer", projectDir);
        Path outputDir = adapter.resolveOutputDir("defaultServer", projectDir);

        String projectName = projectDir.getFileName().toString();
        Path expected = projectDir.resolve("build/wlp/usr/servers/defaultServer/apps/expanded/" + projectName + ".war");
        assertEquals(expected, outputDir);
    }

    @Test
    void resolveOutputDirUsesProvidedServerName(@TempDir Path projectDir) {
        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/", "myServer", projectDir);
        Path outputDir = adapter.resolveOutputDir("myServer", projectDir);

        String projectName = projectDir.getFileName().toString();
        assertTrue(outputDir.toString().contains("myServer"));
        Path expected = projectDir.resolve("build/wlp/usr/servers/myServer/apps/expanded/" + projectName + ".war");
        assertEquals(expected, outputDir);
    }

    @Test
    void writeServerConfigCreatesBootstrapProperties(@TempDir Path projectDir) throws IOException {
        Path serverDir = projectDir.resolve("build/wlp/usr/servers/defaultServer");
        Files.createDirectories(serverDir);

        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/", "defaultServer", projectDir);
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(projectDir.resolve("output"))
                .mojarraRefreshPeriod(0)
                .myfacesRefreshPeriod(0)
                .port(35729)
                .build();

        adapter.writeServerConfig(params);

        Path bootstrapProps = serverDir.resolve("bootstrap.properties");
        assertTrue(Files.exists(bootstrapProps));
        List<String> lines = Files.readAllLines(bootstrapProps, StandardCharsets.UTF_8);
        assertTrue(lines.contains("javax.faces.FACELETS_REFRESH_PERIOD=0"));
        assertTrue(lines.contains("org.apache.myfaces.REFRESH_PERIOD=0"));
    }

    @Test
    void writeServerConfigIsIdempotent(@TempDir Path projectDir) throws IOException {
        Path serverDir = projectDir.resolve("build/wlp/usr/servers/defaultServer");
        Files.createDirectories(serverDir);

        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/", "defaultServer", projectDir);
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(projectDir.resolve("output"))
                .mojarraRefreshPeriod(0)
                .myfacesRefreshPeriod(0)
                .port(35729)
                .build();

        adapter.writeServerConfig(params);
        adapter.writeServerConfig(params);

        Path bootstrapProps = serverDir.resolve("bootstrap.properties");
        List<String> lines = Files.readAllLines(bootstrapProps, StandardCharsets.UTF_8);
        long mojarraCount = lines.stream()
                .filter(l -> l.startsWith("javax.faces.FACELETS_REFRESH_PERIOD="))
                .count();
        long myfacesCount = lines.stream()
                .filter(l -> l.startsWith("org.apache.myfaces.REFRESH_PERIOD="))
                .count();
        assertEquals(1, mojarraCount, "Mojarra entry should appear exactly once");
        assertEquals(1, myfacesCount, "MyFaces entry should appear exactly once");
    }

    @Test
    void writeServerConfigPreservesExistingProperties(@TempDir Path projectDir) throws IOException {
        Path serverDir = projectDir.resolve("build/wlp/usr/servers/defaultServer");
        Files.createDirectories(serverDir);
        Path bootstrapProps = serverDir.resolve("bootstrap.properties");
        Files.write(bootstrapProps, List.of("some.other.property=value"), StandardCharsets.UTF_8);

        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/", "defaultServer", projectDir);
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(projectDir.resolve("output"))
                .mojarraRefreshPeriod(0)
                .myfacesRefreshPeriod(0)
                .port(35729)
                .build();

        adapter.writeServerConfig(params);

        List<String> lines = Files.readAllLines(bootstrapProps, StandardCharsets.UTF_8);
        assertTrue(lines.contains("some.other.property=value"));
        assertTrue(lines.contains("javax.faces.FACELETS_REFRESH_PERIOD=0"));
        assertTrue(lines.contains("org.apache.myfaces.REFRESH_PERIOD=0"));
    }

    @Test
    void writeServerConfigLogsWarningForParentFirst(@TempDir Path projectDir) throws IOException {
        Path serverDir = projectDir.resolve("build/wlp/usr/servers/defaultServer");
        Files.createDirectories(serverDir);

        Path serverXml = projectDir.resolve("src/main/liberty/config/server.xml");
        Files.createDirectories(serverXml.getParent());
        Files.write(serverXml, List.of(
                "<server>",
                "  <classloader delegation=\"parentFirst\"/>",
                "</server>"), StandardCharsets.UTF_8);

        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger(LibertyServerAdapter.class.getName());
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);

        try {
            LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/", "defaultServer", projectDir);
            ServerConfigParams params = ServerConfigParams.builder()
                    .outputDir(projectDir.resolve("output"))
                    .mojarraRefreshPeriod(0)
                    .myfacesRefreshPeriod(0)
                    .port(35729)
                    .build();

            adapter.writeServerConfig(params);

            boolean warningLogged = logHandler.records.stream()
                    .anyMatch(r -> r.getLevel() == Level.WARNING
                            && r.getMessage().contains("parentFirst"));
            assertTrue(warningLogged, "Should log warning about parentFirst classloader delegation");
        } finally {
            logger.removeHandler(logHandler);
        }
    }

    @Test
    void writeServerConfigNoWarningWithoutParentFirst(@TempDir Path projectDir) throws IOException {
        Path serverDir = projectDir.resolve("build/wlp/usr/servers/defaultServer");
        Files.createDirectories(serverDir);

        Path serverXml = projectDir.resolve("src/main/liberty/config/server.xml");
        Files.createDirectories(serverXml.getParent());
        Files.write(serverXml, List.of(
                "<server>",
                "  <classloader delegation=\"parentLast\"/>",
                "</server>"), StandardCharsets.UTF_8);

        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger(LibertyServerAdapter.class.getName());
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);

        try {
            LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/", "defaultServer", projectDir);
            ServerConfigParams params = ServerConfigParams.builder()
                    .outputDir(projectDir.resolve("output"))
                    .mojarraRefreshPeriod(0)
                    .myfacesRefreshPeriod(0)
                    .port(35729)
                    .build();

            adapter.writeServerConfig(params);

            boolean warningLogged = logHandler.records.stream()
                    .anyMatch(r -> r.getLevel() == Level.WARNING
                            && r.getMessage().contains("parentFirst"));
            assertFalse(warningLogged, "Should not log parentFirst warning");
        } finally {
            logger.removeHandler(logHandler);
        }
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static class TestLogHandler extends Handler {
        final List<LogRecord> records = new java.util.ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
