package it.bstz.jsfautoreload.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Mojo(name = "auto-compile")
public class AutoCompileMojo extends AbstractMojo {

    @Parameter(property = "jsf-autoreload.sourceDirectory", defaultValue = "src/main/java")
    private File sourceDirectory;

    @Parameter(property = "jsf-autoreload.compileCommand", defaultValue = "mvn compile")
    private String compileCommand;

    @Parameter(property = "jsf-autoreload.autoCompile", defaultValue = "true")
    private boolean autoCompile;

    @Override
    public void execute() throws MojoExecutionException {
        if (!autoCompile) {
            getLog().info("[JSF-AUTORELOAD] Auto-compile disabled");
            return;
        }

        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException("Source directory does not exist: " + sourceDirectory);
        }

        getLog().info("[JSF-AUTORELOAD] Auto-compile trigger — invoking: " + compileCommand);
        try {
            int exitCode = runCommand(compileCommand);
            if (exitCode != 0) {
                getLog().warn("[JSF-AUTORELOAD] Compilation failed with exit code: " + exitCode);
            } else {
                getLog().info("[JSF-AUTORELOAD] Compilation succeeded");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute compile command", e);
        }
    }

    private int runCommand(String command) throws Exception {
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
        return process.waitFor();
    }

    public static boolean isJavaFile(String fileName) {
        return fileName != null && fileName.endsWith(".java");
    }

    public void setCompileCommand(String compileCommand) {
        this.compileCommand = compileCommand;
    }

    public String getCompileCommand() {
        return compileCommand;
    }
}
