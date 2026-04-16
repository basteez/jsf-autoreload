package it.bstz.jsfautoreload.sse;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;
import it.bstz.jsfautoreload.bridge.ServletBridge;
import it.bstz.jsfautoreload.model.BrowserConnection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultSseHandlerTest {

    @Test
    void heartbeatSendsCommentToConnections() throws Exception {
        ServletBridge servletBridge = mock(ServletBridge.class);
        ConnectionManager connectionManager = new ConnectionManager();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsyncContextWrapper wrapper = new AsyncContextWrapper(new Object(), out, () -> {});
        BrowserConnection conn = new BrowserConnection(wrapper);
        connectionManager.add(conn);

        DefaultSseHandler handler = new DefaultSseHandler(servletBridge, connectionManager);

        // Trigger heartbeat manually via connectionManager
        connectionManager.sendHeartbeatToAll();

        String content = out.toString("UTF-8");
        assertTrue(content.contains(":heartbeat"),
                "Heartbeat should send :heartbeat comment, got: " + content);

        handler.shutdown();
    }

    @Test
    void handleRequestRegistersAsyncListener() throws Exception {
        ServletBridge servletBridge = mock(ServletBridge.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsyncContextWrapper asyncWrapper = new AsyncContextWrapper(new Object(), out, () -> {});
        when(servletBridge.startAsync(any(), any())).thenReturn(asyncWrapper);

        ConnectionManager connectionManager = new ConnectionManager();
        DefaultSseHandler handler = new DefaultSseHandler(servletBridge, connectionManager);

        Object mockRequest = new Object();
        Object mockResponse = createMockResponse();

        handler.handleRequest(mockRequest, mockResponse);

        // Verify addAsyncListener was called with the AsyncContextWrapper
        verify(servletBridge).addAsyncListener(eq(asyncWrapper), any(Runnable.class), any(Runnable.class), any(Runnable.class));

        handler.shutdown();
    }

    @Test
    void asyncListenerCallbackRemovesConnection() throws Exception {
        ServletBridge servletBridge = mock(ServletBridge.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsyncContextWrapper asyncWrapper = new AsyncContextWrapper(new Object(), out, () -> {});
        when(servletBridge.startAsync(any(), any())).thenReturn(asyncWrapper);

        ConnectionManager connectionManager = new ConnectionManager();
        DefaultSseHandler handler = new DefaultSseHandler(servletBridge, connectionManager);

        Object mockRequest = new Object();
        Object mockResponse = createMockResponse();

        handler.handleRequest(mockRequest, mockResponse);
        assertEquals(1, connectionManager.getConnectionCount());

        // Capture the onComplete callback
        ArgumentCaptor<Runnable> onCompleteCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(servletBridge).addAsyncListener(any(), onCompleteCaptor.capture(), any(), any());

        // Simulate onComplete event (browser closed tab)
        onCompleteCaptor.getValue().run();
        assertEquals(0, connectionManager.getConnectionCount(),
                "Connection should be removed when AsyncListener.onComplete fires");

        handler.shutdown();
    }

    private Object createMockResponse() {
        // Create a mock that has setContentType, setHeader, setCharacterEncoding methods
        // (reflection-based in DefaultSseHandler)
        return mock(MockResponse.class);
    }

    // Mock class with the methods DefaultSseHandler expects via reflection
    public static abstract class MockResponse {
        public abstract void setContentType(String type);
        public abstract void setHeader(String name, String value);
        public abstract void setCharacterEncoding(String encoding);
    }
}
