package it.bstz.jsfautoreload.server.liberty;

import it.bstz.jsfautoreload.JsfAutoreloadException;
import it.bstz.jsfautoreload.server.ServerAdapter;
import it.bstz.jsfautoreload.server.ServerConfigParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ServerAdapter} implementation for Open Liberty / WebSphere Liberty.
 *
 * <p>{@link #writeServerConfig(ServerConfigParams)} creates or updates
 * {@code bootstrap.properties} in the Liberty server directory with JSF
 * refresh period settings. It also checks {@code server.xml} for
 * {@code parentFirst} classloader delegation and logs a warning.</p>
 */
public class LibertyServerAdapter implements ServerAdapter {

    private static final Logger LOGGER = Logger.getLogger(LibertyServerAdapter.class.getName());

    private final int httpPort;
    private final String contextRoot;
    private final String serverName;
    private final Path projectDir;

    public LibertyServerAdapter(int httpPort, String contextRoot, String serverName, Path projectDir) {
        this.httpPort = httpPort;
        this.contextRoot = contextRoot;
        this.serverName = serverName;
        this.projectDir = projectDir;
    }

    @Override
    public boolean isRunning() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:" + httpPort + "/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(500);
            conn.setReadTimeout(500);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            drainStream(conn);
            return responseCode < 500;
        } catch (ConnectException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void drainStream(HttpURLConnection conn) {
        try {
            InputStream is = conn.getErrorStream();
            if (is == null) {
                is = conn.getInputStream();
            }
            if (is != null) {
                byte[] buf = new byte[256];
                while (is.read(buf) != -1) {
                    // drain
                }
                is.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "[JSF Autoreload] Error draining HTTP response stream.", e);
        }
    }

    @Override
    public int getHttpPort() {
        return httpPort;
    }

    @Override
    public String getContextRoot() {
        return contextRoot;
    }

    @Override
    public Path resolveOutputDir(String serverName, Path projectDir) {
        String appName = projectDir.getFileName().toString();
        return projectDir.resolve("build/wlp/usr/servers")
                .resolve(serverName)
                .resolve("apps/expanded")
                .resolve(appName + ".war");
    }

    @Override
    public void writeServerConfig(ServerConfigParams params) {
        Path serverDir = projectDir.resolve("build/wlp/usr/servers").resolve(serverName);
        Path bootstrapProps = serverDir.resolve("bootstrap.properties");

        writeBootstrapProperties(bootstrapProps, params);
        checkParentFirst();
    }

    private void writeBootstrapProperties(Path bootstrapProps, ServerConfigParams params) {
        String mojarraKey = "javax.faces.FACELETS_REFRESH_PERIOD";
        String myfacesKey = "org.apache.myfaces.REFRESH_PERIOD";
        String mojarraLine = mojarraKey + "=" + params.getMojarraRefreshPeriod();
        String myfacesLine = myfacesKey + "=" + params.getMyfacesRefreshPeriod();

        List<String> lines = new ArrayList<>();
        boolean mojarraFound = false;
        boolean myfacesFound = false;

        try {
            if (Files.exists(bootstrapProps)) {
                List<String> existing = Files.readAllLines(bootstrapProps, StandardCharsets.UTF_8);
                for (String line : existing) {
                    if (line.startsWith(mojarraKey + "=")) {
                        lines.add(mojarraLine);
                        mojarraFound = true;
                    } else if (line.startsWith(myfacesKey + "=")) {
                        lines.add(myfacesLine);
                        myfacesFound = true;
                    } else {
                        lines.add(line);
                    }
                }
            }

            if (!mojarraFound) {
                lines.add(mojarraLine);
            }
            if (!myfacesFound) {
                lines.add(myfacesLine);
            }

            Files.createDirectories(bootstrapProps.getParent());
            Files.write(bootstrapProps, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new JsfAutoreloadException(
                    "[JSF Autoreload] Failed to write bootstrap.properties at " + bootstrapProps
                            + ". Check file permissions and that the server directory exists.", e);
        }
    }

    private void checkParentFirst() {
        Path serverXml = projectDir.resolve("src/main/liberty/config/server.xml");
        if (!Files.exists(serverXml)) {
            return;
        }
        try {
            String content = Files.readString(serverXml, StandardCharsets.UTF_8);
            if (content.contains("delegation=\"parentFirst\"")) {
                LOGGER.warning("[JSF Autoreload] Liberty classloader delegation is set to parentFirst. "
                        + "This may prevent the runtime filter from loading correctly.");
            }
        } catch (IOException e) {
            LOGGER.warning("[JSF Autoreload] Could not read server.xml: " + e.getMessage());
        }
    }
}
