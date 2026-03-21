package it.bstz.jsfautoreload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsfAutoreloadExceptionTest {

    @Test
    void isRuntimeException() {
        JsfAutoreloadException exception = new JsfAutoreloadException("[JSF Autoreload] Something failed. Fix it.");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void messageIncludesPrefix() {
        String message = "[JSF Autoreload] Output directory not found: /path. Configure it explicitly.";
        JsfAutoreloadException exception = new JsfAutoreloadException(message);
        assertEquals(message, exception.getMessage());
        assertTrue(exception.getMessage().startsWith("[JSF Autoreload]"));
    }

    @Test
    void preservesCauseWhenProvided() {
        Throwable cause = new IllegalStateException("root cause");
        JsfAutoreloadException exception = new JsfAutoreloadException(
                "[JSF Autoreload] Wrapped error. Check logs.", cause);
        assertEquals(cause, exception.getCause());
    }

    @Test
    void causeIsNullWhenNotProvided() {
        JsfAutoreloadException exception = new JsfAutoreloadException("[JSF Autoreload] No cause. Fix it.");
        assertNull(exception.getCause());
    }

    @Test
    void configExceptionIsSubtypeOfJsfAutoreloadException() {
        JsfAutoreloadConfigException configException = new JsfAutoreloadConfigException(
                "[JSF Autoreload] Bad config. Fix your settings.");
        assertTrue(configException instanceof JsfAutoreloadException);
    }

    @Test
    void configExceptionPreservesCause() {
        Throwable cause = new NumberFormatException("not a number");
        JsfAutoreloadConfigException configException = new JsfAutoreloadConfigException(
                "[JSF Autoreload] Invalid port value. Use a number between 1024 and 65535.", cause);
        assertEquals(cause, configException.getCause());
    }

    @Test
    void configExceptionMessageIncludesPrefix() {
        String message = "[JSF Autoreload] Port 35729 is already in use. Configure a different port.";
        JsfAutoreloadConfigException configException = new JsfAutoreloadConfigException(message);
        assertEquals(message, configException.getMessage());
        assertTrue(configException.getMessage().startsWith("[JSF Autoreload]"));
    }

    @Test
    void canBeCaughtAsRuntimeException() {
        assertThrows(RuntimeException.class, () -> {
            throw new JsfAutoreloadException("[JSF Autoreload] Test. Fix.");
        });
    }

    @Test
    void configExceptionCanBeCaughtAsJsfAutoreloadException() {
        assertThrows(JsfAutoreloadException.class, () -> {
            throw new JsfAutoreloadConfigException("[JSF Autoreload] Config error. Fix config.");
        });
    }
}
