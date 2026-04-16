package it.bstz.jsfautoreload.sse;

public interface SseHandler {

    void handleRequest(Object request, Object response);
}
