package it.bstz.jsfautoreload.server.liberty;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class LibertyServerAdapterTest {

    private MockWebServer mockServer;

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null) {
            mockServer.shutdown();
        }
    }

    @Test
    void isRunningReturnsTrueWhenServerReturns200() throws IOException {
        mockServer = new MockWebServer();
        mockServer.enqueue(new MockResponse().setResponseCode(200));
        mockServer.start();

        LibertyServerAdapter adapter = new LibertyServerAdapter(mockServer.getPort(), "/");
        assertTrue(adapter.isRunning());
    }

    @Test
    void isRunningReturnsTrueWhenServerReturns404() throws IOException {
        mockServer = new MockWebServer();
        mockServer.enqueue(new MockResponse().setResponseCode(404));
        mockServer.start();

        LibertyServerAdapter adapter = new LibertyServerAdapter(mockServer.getPort(), "/");
        assertTrue(adapter.isRunning());
    }

    @Test
    void isRunningReturnsFalseWhenNothingListening() throws Exception {
        int port = findFreePort();
        LibertyServerAdapter adapter = new LibertyServerAdapter(port, "/");
        assertFalse(adapter.isRunning());
    }

    @Test
    void getHttpPortReturnsConfiguredValue() {
        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/myapp");
        assertEquals(9080, adapter.getHttpPort());
    }

    @Test
    void getContextRootReturnsConfiguredValue() {
        LibertyServerAdapter adapter = new LibertyServerAdapter(9080, "/myapp");
        assertEquals("/myapp", adapter.getContextRoot());
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
