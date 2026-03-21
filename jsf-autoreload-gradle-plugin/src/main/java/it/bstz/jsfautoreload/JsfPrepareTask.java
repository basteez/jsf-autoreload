package it.bstz.jsfautoreload;

import it.bstz.jsfautoreload.server.ServerConfigParams;
import it.bstz.jsfautoreload.server.liberty.LibertyServerAdapter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public abstract class JsfPrepareTask extends DefaultTask {

    @InputFiles
    public abstract Property<FileCollection> getRuntimeJarFiles();

    @Input
    public abstract Property<String> getServerName();

    @Input
    public abstract Property<Integer> getPort();

    @Input
    public abstract Property<String> getOutputDir();

    @Input
    public abstract Property<String> getRootDir();

    @TaskAction
    public void prepare() throws IOException {
        String serverName = getServerName().get();
        int port = getPort().get();
        String outputDirPath = getOutputDir().get();

        Path rootDir = Path.of(getRootDir().get());
        Path outputDir = Path.of(outputDirPath);

        if (!Files.isDirectory(outputDir)) {
            throw new GradleException("[JSF Autoreload] Output directory not found: " + outputDirPath
                    + ". Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your Liberty server name matches jsfAutoreload { serverName = '...' }");
        }

        // Copy runtime JAR to WEB-INF/lib (Gradle-specific)
        Path webInfLib = outputDir.resolve("WEB-INF/lib");
        Files.createDirectories(webInfLib);
        copyRuntimeJar(webInfLib);

        // Delegate JSF config writing to the server adapter
        LibertyServerAdapter adapter = new LibertyServerAdapter(port, "/", serverName, rootDir);
        ServerConfigParams params = ServerConfigParams.builder()
                .outputDir(outputDir)
                .mojarraRefreshPeriod(0)
                .myfacesRefreshPeriod(0)
                .port(port)
                .build();

        try {
            adapter.writeServerConfig(params);
            getLogger().lifecycle("[JSF Autoreload] Server configuration written via LibertyServerAdapter");
        } catch (JsfAutoreloadException e) {
            throw new GradleException(e.getMessage(), e);
        }

        // Write port to jvm.options — will be replaced by properties file in WEB-INF/classes (Story 2-2)
        Path serverDir = rootDir.resolve("build/wlp/usr/servers").resolve(serverName);
        Path jvmOptionsFile = serverDir.resolve("jvm.options");
        writeJvmOptions(jvmOptionsFile, port);
    }

    private void copyRuntimeJar(Path webInfLib) throws IOException {
        Path targetJar = webInfLib.resolve("jsf-autoreload-runtime.jar");
        FileCollection files = getRuntimeJarFiles().get();

        if (!files.isEmpty()) {
            Files.copy(files.getSingleFile().toPath(), targetJar, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // Extract from plugin classpath (bundled in plugin JAR)
            try (InputStream is = getClass().getResourceAsStream("/META-INF/jsf-autoreload/jsf-autoreload-runtime.jar")) {
                if (is == null) {
                    throw new GradleException("[JSF Autoreload] Runtime JAR not found in runtimeJar configuration or plugin classpath. "
                            + "Ensure the plugin is correctly installed.");
                }
                Files.copy(is, targetJar, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        getLogger().lifecycle("[JSF Autoreload] Copied runtime JAR to {}", webInfLib);
    }

    private void writeJvmOptions(Path file, int port) throws IOException {
        String portLine = "-Djsf.autoreload.port=" + port;
        List<String> lines = new ArrayList<>();
        boolean found = false;

        if (Files.exists(file)) {
            lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("-Djsf.autoreload.port=")) {
                    lines.set(i, portLine);
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            lines.add(portLine);
        }

        Files.createDirectories(file.getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
        getLogger().lifecycle("[JSF Autoreload] Updated jvm.options with port={}", port);
    }
}
