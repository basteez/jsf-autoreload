package it.bstz.jsfautoreload.bridge.javax;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;
import it.bstz.jsfautoreload.bridge.ServletBridge;
import it.bstz.jsfautoreload.sse.SseHandler;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JavaxServletBridge implements ServletBridge {

    @Override
    public void registerServlet(Object servletContext, String path, SseHandler handler) {
        if (servletContext instanceof ServletContext) {
            ServletContext ctx = (ServletContext) servletContext;
            HttpServlet servlet = new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                    handler.handleRequest(req, resp);
                }
            };
            ServletRegistration.Dynamic reg = ctx.addServlet("jsf-autoreload-sse", servlet);
            reg.addMapping(path);
            reg.setAsyncSupported(true);
        }
    }

    @Override
    public AsyncContextWrapper startAsync(Object request, Object response) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        AsyncContext async = req.startAsync(req, resp);
        async.setTimeout(0); // no timeout
        try {
            return new AsyncContextWrapper(async, resp.getOutputStream(), async::complete);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get output stream", e);
        }
    }

    @Override
    public void registerShutdownListener(Object servletContext, Runnable onShutdown) {
        if (servletContext instanceof ServletContext) {
            ((ServletContext) servletContext).addListener(new ServletContextListener() {
                @Override
                public void contextInitialized(ServletContextEvent sce) {
                    // no-op
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                    onShutdown.run();
                }
            });
        }
    }
}
