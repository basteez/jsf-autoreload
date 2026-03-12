package it.bstz.jsfautoreload;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;

import java.io.File;

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
        });

        project.getTasks().register("jsfDev", JsfDevTask.class, task -> {
            task.setGroup("JSF Autoreload");
            task.setDescription("Starts the JSF autoreload dev server with file watching");
            task.getPort().set(extension.getPort());
            task.getOutputDir().set(resolvedOutputDir);
            task.getWatchDirs().set(extension.getWatchDirs());
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
