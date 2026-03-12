package it.bstz.jsfautoreload.server;

public interface ServerAdapter {
    boolean isRunning();
    int getHttpPort();
    String getContextRoot();
}
