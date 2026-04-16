package it.bstz.jsfautoreload.bridge.jakarta;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;
import it.bstz.jsfautoreload.bridge.ServletBridge;
import it.bstz.jsfautoreload.sse.SseHandler;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JakartaServletBridge implements ServletBridge {

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
        async.setTimeout(0);
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
                public void contextDestroyed(ServletContextEvent sce) {
                    onShutdown.run();
                }
            });
        }
    }

    @Override
    public void addAsyncListener(AsyncContextWrapper asyncContext, Runnable onComplete, Runnable onError, Runnable onTimeout) {
        Object raw = asyncContext.getRawContext();
        if (raw instanceof AsyncContext) {
            ((AsyncContext) raw).addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    onComplete.run();
                }

                @Override
                public void onError(AsyncEvent event) {
                    onError.run();
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    onTimeout.run();
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                    // no-op
                }
            });
        }
    }
}
