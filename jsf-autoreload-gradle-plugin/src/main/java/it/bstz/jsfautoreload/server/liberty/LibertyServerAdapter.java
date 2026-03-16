package it.bstz.jsfautoreload.server.liberty;

import it.bstz.jsfautoreload.server.ServerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LibertyServerAdapter implements ServerAdapter {

    private final int httpPort;
    private final String contextRoot;

    public LibertyServerAdapter(int httpPort, String contextRoot) {
        this.httpPort = httpPort;
        this.contextRoot = contextRoot;
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
        } catch (IOException ignored) {
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
}
