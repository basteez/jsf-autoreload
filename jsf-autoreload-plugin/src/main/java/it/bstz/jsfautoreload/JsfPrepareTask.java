package it.bstz.jsfautoreload;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    @TaskAction
    public void prepare() throws IOException {
        String serverName = getServerName().get();
        int port = getPort().get();
        String outputDirPath = getOutputDir().get();

        File rootDir = getProject().getRootDir();

        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            throw new GradleException("[JSF Autoreload] Output directory not found: " + outputDirPath
                    + ". Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your Liberty server name matches jsfAutoreload { serverName = '...' }");
        }

        // Copy runtime JAR to WEB-INF/lib
        File webInfLib = new File(outputDir, "WEB-INF/lib");
        webInfLib.mkdirs();
        copyRuntimeJar(webInfLib);

        // Inject facelets refresh context-params into web.xml
        File webXml = new File(outputDir, "WEB-INF/web.xml");
        injectFaceletsRefreshParams(webXml);

        // Write to jvm.options (port only)
        File serverDir = new File(rootDir, "build/wlp/usr/servers/" + serverName);
        File jvmOptionsFile = new File(serverDir, "jvm.options");
        writeJvmOptions(jvmOptionsFile, port);

        // Check server.xml for parentFirst
        File serverXml = new File(rootDir, "src/main/liberty/config/server.xml");
        checkParentFirst(serverXml);
    }

    private void copyRuntimeJar(File webInfLib) throws IOException {
        File targetJar = new File(webInfLib, "jsf-autoreload-runtime.jar");
        FileCollection files = getRuntimeJarFiles().get();

        if (!files.isEmpty()) {
            Files.copy(files.getSingleFile().toPath(), targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            // Extract from plugin classpath (bundled in plugin JAR)
            try (InputStream is = getClass().getResourceAsStream("/META-INF/jsf-autoreload/jsf-autoreload-runtime.jar")) {
                if (is == null) {
                    throw new GradleException("[JSF Autoreload] Runtime JAR not found in runtimeJar configuration or plugin classpath. "
                            + "Ensure the plugin is correctly installed.");
                }
                Files.copy(is, targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        getLogger().lifecycle("[JSF Autoreload] Copied runtime JAR to {}", webInfLib);
    }

    private void injectFaceletsRefreshParams(File webXml) throws IOException {
        if (!webXml.exists()) {
            getLogger().warn("[JSF Autoreload] web.xml not found at {}. Cannot inject facelets refresh params.", webXml);
            return;
        }

        String content = new String(Files.readAllBytes(webXml.toPath()), StandardCharsets.UTF_8);

        String[][] params = {
                {"javax.faces.FACELETS_REFRESH_PERIOD", "0"},
                {"org.apache.myfaces.CONFIG_REFRESH_PERIOD", "0"},
                {"facelets.REFRESH_PERIOD", "0"}
        };

        boolean modified = false;
        for (String[] param : params) {
            if (!content.contains("<param-name>" + param[0] + "</param-name>")) {
                String contextParam = "\n    <context-param>\n"
                        + "        <param-name>" + param[0] + "</param-name>\n"
                        + "        <param-value>" + param[1] + "</param-value>\n"
                        + "    </context-param>";
                // Insert after <web-app ...> opening tag
                int insertPos = content.indexOf(">", content.indexOf("<web-app"));
                if (insertPos >= 0) {
                    content = content.substring(0, insertPos + 1) + contextParam + content.substring(insertPos + 1);
                    modified = true;
                }
            }
        }

        if (modified) {
            Files.write(webXml.toPath(), content.getBytes(StandardCharsets.UTF_8));
            getLogger().lifecycle("[JSF Autoreload] Injected facelets refresh context-params into web.xml");
        }
    }

    private void writeJvmOptions(File file, int port) throws IOException {
        String portLine = "-Djsf.autoreload.port=" + port;
        List<String> lines = new ArrayList<>();
        boolean found = false;

        if (file.exists()) {
            lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
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

        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        getLogger().lifecycle("[JSF Autoreload] Updated jvm.options with port={}", port);
    }

    private void checkParentFirst(File serverXml) {
        if (!serverXml.exists()) {
            return;
        }
        try {
            String content = new String(Files.readAllBytes(serverXml.toPath()), StandardCharsets.UTF_8);
            if (content.contains("delegation=\"parentFirst\"")) {
                getLogger().warn("[JSF Autoreload] WARNING: Liberty classloader delegation is set to 'parentFirst'. "
                        + "DevModeFilter may not register correctly. Switch to 'parentLast' (the default).");
            }
        } catch (IOException e) {
            getLogger().warn("[JSF Autoreload] Could not read server.xml: {}", e.getMessage());
        }
    }
}
