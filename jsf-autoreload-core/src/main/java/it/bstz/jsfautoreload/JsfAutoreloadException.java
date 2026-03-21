package it.bstz.jsfautoreload;

/**
 * Base runtime exception for all jsf-autoreload errors.
 * Message format convention: "[JSF Autoreload] {what went wrong}. {how to fix it}."
 */
public class JsfAutoreloadException extends RuntimeException {

    public JsfAutoreloadException(String message) {
        super(message);
    }

    public JsfAutoreloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
