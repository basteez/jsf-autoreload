package it.bstz.jsfautoreload.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaSourceCompiler {

    private final List<Path> sourceDirs;
    private final String classpath;
    private final String sourceCompatibility;
    private final Path classesOutputDir;

    public JavaSourceCompiler(List<Path> sourceDirs, String classpath,
                              String sourceCompatibility, Path classesOutputDir) {
        this.sourceDirs = sourceDirs;
        this.classpath = classpath;
        this.sourceCompatibility = sourceCompatibility;
        this.classesOutputDir = classesOutputDir;
    }

    public CompileResult compile() throws IOException, InterruptedException {
        List<String> sourceFiles = collectSourceFiles();
        if (sourceFiles.isEmpty()) {
            return new CompileResult(true, "No source files found");
        }

        Path javac = resolveJavac();
        if (javac == null) {
            return new CompileResult(false, "javac not found. Ensure a JDK (not JRE) is installed.");
        }

        Files.createDirectories(classesOutputDir);

        // Write source file list to an argfile to avoid command line length limits
        Path argFile = Files.createTempFile("jsf-autoreload-sources", ".txt");
        try {
            Files.write(argFile, sourceFiles, StandardCharsets.UTF_8);

            List<String> cmd = new ArrayList<>();
            cmd.add(javac.toString());
            cmd.add("-classpath");
            cmd.add(classpath);
            cmd.add("-d");
            cmd.add(classesOutputDir.toString());
            cmd.add("-source");
            cmd.add(sourceCompatibility);
            cmd.add("-target");
            cmd.add(sourceCompatibility);
            cmd.add("@" + argFile);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = readOutput(proc);
            int exitCode = proc.waitFor();

            return new CompileResult(exitCode == 0, output);
        } finally {
            Files.deleteIfExists(argFile);
        }
    }

    private List<String> collectSourceFiles() throws IOException {
        List<String> files = new ArrayList<>();
        for (Path sourceDir : sourceDirs) {
            if (Files.isDirectory(sourceDir)) {
                try (Stream<Path> walk = Files.walk(sourceDir)) {
                    walk.filter(p -> p.toString().endsWith(".java"))
                        .map(Path::toString)
                        .forEach(files::add);
                }
            }
        }
        return files;
    }

    private Path resolveJavac() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            return null;
        }

        // JDK 9+: java.home points to JDK root
        String ext = System.getProperty("os.name", "").toLowerCase().contains("win") ? ".exe" : "";
        Path javac = Paths.get(javaHome, "bin", "javac" + ext);
        if (Files.isExecutable(javac)) {
            return javac;
        }

        // JDK 8 fallback: java.home might point to jre/ subdirectory
        javac = Paths.get(javaHome).getParent().resolve("bin").resolve("javac" + ext);
        if (Files.isExecutable(javac)) {
            return javac;
        }

        return null;
    }

    private String readOutput(Process proc) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    public static class CompileResult {
        private final boolean success;
        private final String output;

        public CompileResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }
    }
}
