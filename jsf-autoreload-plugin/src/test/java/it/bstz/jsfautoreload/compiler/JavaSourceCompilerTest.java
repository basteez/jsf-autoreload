package it.bstz.jsfautoreload.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JavaSourceCompilerTest {

    @TempDir
    Path tempDir;

    @Test
    void compileValidJavaSource() throws Exception {
        Path sourceDir = tempDir.resolve("src");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("Hello.java"),
                "public class Hello {\n" +
                "    public String greet() { return \"hello\"; }\n" +
                "}\n");

        Path classesDir = tempDir.resolve("classes");

        JavaSourceCompiler compiler = new JavaSourceCompiler(
                Collections.singletonList(sourceDir), "", "11", classesDir);
        JavaSourceCompiler.CompileResult result = compiler.compile();

        assertTrue(result.isSuccess(), "Compilation should succeed: " + result.getOutput());
        assertTrue(Files.exists(classesDir.resolve("Hello.class")), "Class file should be generated");
    }

    @Test
    void compileInvalidJavaSourceReturnsFailed() throws Exception {
        Path sourceDir = tempDir.resolve("src");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("Bad.java"),
                "public class Bad {\n" +
                "    this is not valid java\n" +
                "}\n");

        Path classesDir = tempDir.resolve("classes");

        JavaSourceCompiler compiler = new JavaSourceCompiler(
                Collections.singletonList(sourceDir), "", "11", classesDir);
        JavaSourceCompiler.CompileResult result = compiler.compile();

        assertFalse(result.isSuccess(), "Compilation should fail for invalid source");
        assertFalse(result.getOutput().isEmpty(), "Should have error output");
    }

    @Test
    void compileWithNoSourcesReturnsSuccess() throws Exception {
        Path sourceDir = tempDir.resolve("empty-src");
        Files.createDirectories(sourceDir);
        Path classesDir = tempDir.resolve("classes");

        JavaSourceCompiler compiler = new JavaSourceCompiler(
                Collections.singletonList(sourceDir), "", "11", classesDir);
        JavaSourceCompiler.CompileResult result = compiler.compile();

        assertTrue(result.isSuccess(), "No sources should return success");
        assertEquals("No source files found", result.getOutput());
    }

    @Test
    void compileWithNonexistentSourceDirReturnsSuccess() throws Exception {
        Path sourceDir = tempDir.resolve("does-not-exist");
        Path classesDir = tempDir.resolve("classes");

        JavaSourceCompiler compiler = new JavaSourceCompiler(
                Collections.singletonList(sourceDir), "", "11", classesDir);
        JavaSourceCompiler.CompileResult result = compiler.compile();

        assertTrue(result.isSuccess(), "Nonexistent source dir should return success (no sources)");
    }

    @Test
    void compileWithPackagedSource() throws Exception {
        Path sourceDir = tempDir.resolve("src");
        Path pkgDir = sourceDir.resolve("com/example");
        Files.createDirectories(pkgDir);
        Files.writeString(pkgDir.resolve("Greeting.java"),
                "package com.example;\n" +
                "public class Greeting {\n" +
                "    public String say() { return \"hi\"; }\n" +
                "}\n");

        Path classesDir = tempDir.resolve("classes");

        JavaSourceCompiler compiler = new JavaSourceCompiler(
                Collections.singletonList(sourceDir), "", "11", classesDir);
        JavaSourceCompiler.CompileResult result = compiler.compile();

        assertTrue(result.isSuccess(), "Compilation should succeed: " + result.getOutput());
        assertTrue(Files.exists(classesDir.resolve("com/example/Greeting.class")),
                "Packaged class file should be in correct directory");
    }
}
