package it.bstz.jsfautoreload.server;

import java.nio.file.Path;

import it.bstz.jsfautoreload.JsfAutoreloadException;

/**
 * Abstraction over an application server (Liberty, Tomcat, etc.) so that
 * the core dev-loop engine can query server state and write configuration
 * without knowing which server is in use.
 *
 * <p>Plugin modules construct the concrete adapter and pass it into the core.
 * Each implementation must document which files it creates or modifies.</p>
 */
public interface ServerAdapter {

    /**
     * Checks whether the application server process is currently running.
     *
     * @return {@code true} if the server is running, {@code false} otherwise
     * @throws JsfAutoreloadException if the server state cannot be determined
     */
    boolean isRunning();

    /**
     * Returns the HTTP port on which the application server is listening.
     *
     * @return the HTTP port number
     * @throws JsfAutoreloadException if the port cannot be determined
     */
    int getHttpPort();

    /**
     * Returns the context root of the deployed web application.
     *
     * @return the context root (e.g. {@code "/myapp"} or {@code "/"})
     * @throws JsfAutoreloadException if the context root cannot be determined
     */
    String getContextRoot();

    /**
     * Resolves the exploded WAR output directory for the given server name
     * and project directory.
     *
     * @param serverName the logical server name (e.g. {@code "defaultServer"} for Liberty)
     * @param projectDir the root directory of the project
     * @return the absolute path to the exploded WAR output directory
     * @throws JsfAutoreloadException if the output directory cannot be resolved
     */
    Path resolveOutputDir(String serverName, Path projectDir);

    /**
     * Writes server-specific JSF configuration files (e.g. web.xml context-params,
     * jvm.options, bootstrap.properties) using the provided parameters.
     *
     * <p>Each implementation must document which files it creates or modifies.</p>
     *
     * @param params the configuration values to write
     * @throws JsfAutoreloadException if the configuration cannot be written
     */
    void writeServerConfig(ServerConfigParams params);
}
