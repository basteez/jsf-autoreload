package it.bstz.jsfautoreload.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AutoCompileMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsJavaFileExtension() {
        assertTrue(AutoCompileMojo.isJavaFile("MyBean.java"));
        assertFalse(AutoCompileMojo.isJavaFile("MyBean.class"));
        assertFalse(AutoCompileMojo.isJavaFile("style.css"));
    }

    @Test
    void compileCommandIsConfigurable() {
        AutoCompileMojo mojo = new AutoCompileMojo();
        mojo.setCompileCommand("mvn compile -pl core");
        assertEquals("mvn compile -pl core", mojo.getCompileCommand());
    }
}
