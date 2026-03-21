package it.bstz.jsfautoreload;

/**
 * Exception for configuration and validation errors.
 * Message format convention: "[JSF Autoreload] {what went wrong}. {how to fix it}."
 */
public class JsfAutoreloadConfigException extends JsfAutoreloadException {

    public JsfAutoreloadConfigException(String message) {
        super(message);
    }

    public JsfAutoreloadConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
