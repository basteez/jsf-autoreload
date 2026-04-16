package it.bstz.jsfautoreload.spi;

public interface ContainerAdapter {

    boolean supports();

    void reload(Object context) throws ReloadException;

    String containerName();

    default int priority() {
        return 100;
    }
}
