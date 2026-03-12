package it.bstz.jsfautoreload;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsfAutoreloadPluginIntegrationTest {

    @TempDir
    Path testProjectDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(testProjectDir.resolve("settings.gradle"), "");
    }

    @Test
    void pluginRegistersJsfDevAndJsfPrepareTasks() throws IOException {
        Files.writeString(testProjectDir.resolve("build.gradle"),
                "plugins {\n" +
                "    id 'it.bstz.jsf-autoreload'\n" +
                "}\n");

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("tasks", "--all")
                .withPluginClasspath()
                .build();

        String output = result.getOutput();
        assertTrue(output.contains("jsfDev"), "jsfDev task should be registered");
        assertTrue(output.contains("jsfPrepare"), "jsfPrepare task should be registered");
    }

    @Test
    void extensionPortPropertyResolves() throws IOException {
        Files.writeString(testProjectDir.resolve("build.gradle"),
                "plugins {\n" +
                "    id 'it.bstz.jsf-autoreload'\n" +
                "}\n" +
                "jsfAutoreload {\n" +
                "    port = 35730\n" +
                "}\n" +
                "tasks.register('printPort') {\n" +
                "    doLast {\n" +
                "        println \"PORT=\" + jsfAutoreload.port.get()\n" +
                "    }\n" +
                "}\n");

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("printPort")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PORT=35730"));
    }
}
