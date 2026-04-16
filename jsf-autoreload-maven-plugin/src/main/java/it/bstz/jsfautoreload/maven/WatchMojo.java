package it.bstz.jsfautoreload.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.StandardWatchEventKinds.*;

@Mojo(name = "watch")
public class WatchMojo extends AbstractMojo {

    @Parameter(property = "jsf-autoreload.sourceDirectory", defaultValue = "src/main/java")
    private File sourceDirectory;

    @Parameter(property = "jsf-autoreload.compileCommand", defaultValue = "mvn compile")
    private String compileCommand;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException("Source directory does not exist: " + sourceDirectory);
        }

        getLog().info("[JSF-AUTORELOAD] Watch mode started — monitoring: " + sourceDirectory);
        getLog().info("[JSF-AUTORELOAD] Compile command: " + compileCommand);
        getLog().info("[JSF-AUTORELOAD] Press Ctrl+C to stop");

        try {
            watchAndCompile();
        } catch (Exception e) {
            throw new MojoExecutionException("Watch mode failed", e);
        }
    }

    private void watchAndCompile() throws Exception {
        Path root = sourceDirectory.toPath();
        WatchService watchService = root.getFileSystem().newWatchService();

        // Register all directories recursively
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws java.io.IOException {
                dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });

        while (true) {
            WatchKey key = watchService.take();
            boolean javaChanged = false;

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == OVERFLOW) continue;

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                String fileName = pathEvent.context().toString();

                if (AutoCompileMojo.isJavaFile(fileName)) {
                    getLog().info("[JSF-AUTORELOAD] Detected change: " + fileName);
                    javaChanged = true;
                }
            }

            key.reset();

            if (javaChanged) {
                getLog().info("[JSF-AUTORELOAD] Compiling...");
                try {
                    runCommand(compileCommand);
                } catch (Exception e) {
                    getLog().warn("[JSF-AUTORELOAD] Compilation failed: " + e.getMessage());
                }
            }
        }
    }

    private void runCommand(String command) throws Exception {
        ProcessBuilder pb;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", command);
        } else {
            pb = new ProcessBuilder("sh", "-c", command);
        }
        pb.directory(sourceDirectory.getParentFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            getLog().warn("[JSF-AUTORELOAD] Compile exited with code: " + exitCode);
        } else {
            getLog().info("[JSF-AUTORELOAD] Compilation succeeded");
        }
    }
}
