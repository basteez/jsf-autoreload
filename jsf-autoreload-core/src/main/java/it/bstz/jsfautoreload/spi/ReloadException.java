package it.bstz.jsfautoreload.spi;

public class ReloadException extends Exception {

    public ReloadException(String message) {
        super(message);
    }

    public ReloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
