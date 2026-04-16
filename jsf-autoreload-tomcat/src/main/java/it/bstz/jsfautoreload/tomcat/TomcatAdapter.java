package it.bstz.jsfautoreload.tomcat;

import it.bstz.jsfautoreload.spi.ContainerAdapter;
import it.bstz.jsfautoreload.spi.ReloadException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;

import java.lang.reflect.Field;

public class TomcatAdapter implements ContainerAdapter {

    @Override
    public boolean supports() {
        try {
            Class.forName("org.apache.catalina.Context");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void reload(Object servletContext) throws ReloadException {
        try {
            StandardContext standardContext = unwrapStandardContext(servletContext);
            if (standardContext != null) {
                standardContext.reload();
            } else {
                throw new ReloadException("Unable to obtain StandardContext from servlet context");
            }
        } catch (ReloadException e) {
            throw e;
        } catch (Exception e) {
            throw new ReloadException("Failed to reload context via Tomcat", e);
        }
    }

    @Override
    public String containerName() {
        return "Apache Tomcat";
    }

    private StandardContext unwrapStandardContext(Object servletContext) throws Exception {
        if (servletContext instanceof StandardContext) {
            return (StandardContext) servletContext;
        }

        // Traverse: ApplicationContextFacade → ApplicationContext → StandardContext
        if (servletContext instanceof ApplicationContextFacade) {
            Field contextField = ApplicationContextFacade.class.getDeclaredField("context");
            contextField.setAccessible(true);
            ApplicationContext appCtx = (ApplicationContext) contextField.get(servletContext);

            Field stdCtxField = ApplicationContext.class.getDeclaredField("context");
            stdCtxField.setAccessible(true);
            return (StandardContext) stdCtxField.get(appCtx);
        }

        // Try via generic reflection as fallback
        try {
            java.lang.reflect.Method getContext = servletContext.getClass().getMethod("getContext");
            Object ctx = getContext.invoke(servletContext);
            if (ctx instanceof StandardContext) {
                return (StandardContext) ctx;
            }
        } catch (NoSuchMethodException e) {
            // not available
        }

        return null;
    }
}
