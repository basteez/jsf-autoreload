package it.bstz.jsfautoreload.bridge;

import it.bstz.jsfautoreload.sse.SseHandler;

public interface ServletBridge {

    void registerServlet(Object servletContext, String path, SseHandler handler);

    AsyncContextWrapper startAsync(Object request, Object response);

    void registerShutdownListener(Object servletContext, Runnable onShutdown);

    void addAsyncListener(AsyncContextWrapper asyncContext, Runnable onComplete, Runnable onError, Runnable onTimeout);
}
