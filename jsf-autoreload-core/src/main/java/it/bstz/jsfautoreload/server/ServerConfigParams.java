package it.bstz.jsfautoreload.server;

import java.nio.file.Path;

/**
 * Immutable value object holding all configuration values needed by
 * {@link ServerAdapter#writeServerConfig(ServerConfigParams)}.
 *
 * <p>Use the {@link Builder} to construct instances:</p>
 * <pre>
 * ServerConfigParams params = ServerConfigParams.builder()
 *     .outputDir(path)
 *     .port(35729)
 *     .mojarraRefreshPeriod(0)
 *     .myfacesRefreshPeriod(0)
 *     .build();
 * </pre>
 */
public final class ServerConfigParams {

    private final Path outputDir;
    private final int mojarraRefreshPeriod;
    private final int myfacesRefreshPeriod;
    private final int port;

    private ServerConfigParams(Builder builder) {
        this.outputDir = builder.outputDir;
        this.mojarraRefreshPeriod = builder.mojarraRefreshPeriod;
        this.myfacesRefreshPeriod = builder.myfacesRefreshPeriod;
        this.port = builder.port;
    }

    /**
     * Returns the path to the exploded WAR output directory.
     *
     * @return the output directory path
     */
    public Path getOutputDir() {
        return outputDir;
    }

    /**
     * Returns the Mojarra {@code FACELETS_REFRESH_PERIOD} value (typically 0 for dev mode).
     *
     * @return the Mojarra refresh period in seconds
     */
    public int getMojarraRefreshPeriod() {
        return mojarraRefreshPeriod;
    }

    /**
     * Returns the MyFaces {@code REFRESH_PERIOD} value (typically 0 for dev mode).
     *
     * @return the MyFaces refresh period in seconds
     */
    public int getMyfacesRefreshPeriod() {
        return myfacesRefreshPeriod;
    }

    /**
     * Returns the WebSocket port used for live-reload communication.
     *
     * @return the WebSocket port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Creates a new {@link Builder} instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ServerConfigParams}.
     */
    public static final class Builder {

        private Path outputDir;
        private int mojarraRefreshPeriod;
        private int myfacesRefreshPeriod;
        private int port;

        private Builder() {
        }

        public Builder outputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder mojarraRefreshPeriod(int mojarraRefreshPeriod) {
            this.mojarraRefreshPeriod = mojarraRefreshPeriod;
            return this;
        }

        public Builder myfacesRefreshPeriod(int myfacesRefreshPeriod) {
            this.myfacesRefreshPeriod = myfacesRefreshPeriod;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public ServerConfigParams build() {
            if (outputDir == null) {
                throw new it.bstz.jsfautoreload.JsfAutoreloadConfigException(
                        "[JSF Autoreload] Missing required configuration: outputDir in ServerConfigParams. "
                                + "Set it via ServerConfigParams.builder().outputDir(path).");
            }
            return new ServerConfigParams(this);
        }
    }
}
