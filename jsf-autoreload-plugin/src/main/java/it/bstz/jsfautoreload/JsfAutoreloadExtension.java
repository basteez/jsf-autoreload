package it.bstz.jsfautoreload;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.Collections;

public class JsfAutoreloadExtension {

    private final Property<Integer> port;
    private final Property<String> serverName;
    private final Property<String> outputDir;
    private final ListProperty<String> watchDirs;

    @Inject
    public JsfAutoreloadExtension(ObjectFactory objects) {
        this.port = objects.property(Integer.class).convention(35729);
        this.serverName = objects.property(String.class).convention("defaultServer");
        this.outputDir = objects.property(String.class).convention("");
        this.watchDirs = objects.listProperty(String.class).convention(Collections.singletonList("src/main/webapp"));
    }

    public Property<Integer> getPort() {
        return port;
    }

    public Property<String> getServerName() {
        return serverName;
    }

    public Property<String> getOutputDir() {
        return outputDir;
    }

    public ListProperty<String> getWatchDirs() {
        return watchDirs;
    }
}
