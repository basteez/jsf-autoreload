package it.bstz.jsfautoreload;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Collections;
import java.util.stream.Collectors;

public class JsfAutoreloadPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        JsfAutoreloadExtension extension = project.getExtensions()
                .create("jsfAutoreload", JsfAutoreloadExtension.class);

        Configuration runtimeJarConfig = project.getConfigurations().maybeCreate("runtimeJar");

        Provider<String> resolvedOutputDir = project.provider(() -> {
            String dir = extension.getOutputDir().get();
            if (dir.isEmpty()) {
                return new File(project.getRootDir(),
                        "build/wlp/usr/servers/" + extension.getServerName().get()
                                + "/apps/expanded/" + project.getName() + ".war").getAbsolutePath();
            }
            return dir;
        });

        project.getTasks().register("jsfPrepare", JsfPrepareTask.class, task -> {
            task.setGroup("JSF Autoreload");
            task.setDescription("Prepares the project for JSF autoreload dev mode");
            task.getRuntimeJarFiles().set(runtimeJarConfig);
            task.getServerName().set(extension.getServerName());
            task.getPort().set(extension.getPort());
            task.getOutputDir().set(resolvedOutputDir);
            task.getRootDir().set(project.getRootDir().getAbsolutePath());
        });

        project.getTasks().register("jsfDev", JsfDevTask.class, task -> {
            task.setGroup("JSF Autoreload");
            task.setDescription("Starts the JSF autoreload dev server with file watching");
            task.getPort().set(extension.getPort());
            task.getOutputDir().set(resolvedOutputDir);
            task.getWatchDirs().set(extension.getWatchDirs());
            task.getWatchClasses().set(extension.getWatchClasses());
            task.getProjectDir().set(project.getProjectDir().getAbsolutePath());

            // Wire Java compilation properties from the project model
            task.getSourceDirs().set(project.provider(() -> {
                try {
                    JavaPluginExtension java = project.getExtensions()
                            .getByType(JavaPluginExtension.class);
                    return java.getSourceSets().getByName("main").getJava()
                            .getSrcDirs().stream()
                            .map(File::getAbsolutePath)
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    return Collections.singletonList(
                            new File(project.getProjectDir(), "src/main/java").getAbsolutePath());
                }
            }));

            task.getClassesOutputDir().set(
                    project.getLayout().getBuildDirectory().dir("classes/java/main")
                            .map(d -> d.getAsFile().getAbsolutePath()));

            task.getCompileClasspath().set(project.provider(() -> {
                try {
                    Configuration compileClasspath = project.getConfigurations()
                            .getByName("compileClasspath");
                    return compileClasspath.getAsPath();
                } catch (Exception e) {
                    return "";
                }
            }));

            task.getSourceCompatibility().set(project.provider(() -> {
                try {
                    JavaPluginExtension java = project.getExtensions()
                            .getByType(JavaPluginExtension.class);
                    return java.getSourceCompatibility().toString();
                } catch (Exception e) {
                    return "11";
                }
            }));
        });

        project.afterEvaluate(p -> {
            Task libertyStart = p.getTasks().findByName("libertyStart");
            if (libertyStart != null) {
                libertyStart.dependsOn("jsfPrepare");
                p.getTasks().named("jsfDev").configure(task -> task.dependsOn("libertyStart"));
            } else {
                p.getLogger().warn("[JSF Autoreload] 'libertyStart' task not found. "
                        + "Make sure io.openliberty.tools.liberty plugin is applied.");
            }
        });
    }
}
