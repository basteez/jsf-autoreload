package it.bstz.jsfautoreload.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DevModeFilterTest {

    private DevModeFilter filter;
    private String originalPort;

    @BeforeEach
    void setUp() throws ServletException {
        originalPort = System.getProperty("jsf.autoreload.port");
        System.setProperty("jsf.autoreload.port", "35729");
        filter = new DevModeFilter();
        filter.init(mock(FilterConfig.class));
    }

    @AfterEach
    void tearDown() {
        if (originalPort != null) {
            System.setProperty("jsf.autoreload.port", originalPort);
        } else {
            System.clearProperty("jsf.autoreload.port");
        }
    }

    private HttpServletResponse createMockResponse(ByteArrayOutputStream output) throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(false);
        when(response.getCharacterEncoding()).thenReturn("UTF-8");

        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public void write(int b) { output.write(b); }
            @Override
            public void write(byte[] b, int off, int len) { output.write(b, off, len); }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener listener) {}
        };
        when(response.getOutputStream()).thenReturn(sos);

        return response;
    }

    @Test
    void htmlResponseGetsReloadScriptInsertedBeforeBodyClose() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = createMockResponse(output);
        when(response.getContentType()).thenReturn("text/html");

        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setContentType("text/html");
            res.getWriter().write("<html><body>Hello</body></html>");
        };

        filter.doFilter(request, response, chain);

        String result = output.toString("UTF-8");
        assertTrue(result.contains("<script>"), "Should contain script tag");
        assertTrue(result.contains("ws://localhost:35729"), "Should contain WebSocket URL");
        assertTrue(result.indexOf("<script>") < result.indexOf("</body>"), "Script should be before </body>");
    }

    @Test
    void contentLengthSetCorrectlyForHtmlResponse() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = createMockResponse(output);
        when(response.getContentType()).thenReturn("text/html");

        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setContentType("text/html");
            res.getWriter().write("<html><body></body></html>");
        };

        filter.doFilter(request, response, chain);

        String result = output.toString("UTF-8");
        verify(response).setContentLength(result.getBytes("UTF-8").length);
    }

    @Test
    void jsonResponseNotModified() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = createMockResponse(output);
        when(response.getContentType()).thenReturn("application/json");

        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setContentType("application/json");
            res.getWriter().write("{\"key\":\"value\"}");
        };

        filter.doFilter(request, response, chain);

        String result = output.toString("UTF-8");
        assertEquals("{\"key\":\"value\"}", result);
        assertFalse(result.contains("<script>"));
    }

    @Test
    void committedResponseNotModified() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).getOutputStream();
    }

    @Test
    void portReadFromSystemProperty() throws Exception {
        System.setProperty("jsf.autoreload.port", "9999");
        DevModeFilter customFilter = new DevModeFilter();
        customFilter.init(mock(FilterConfig.class));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = createMockResponse(output);
        when(response.getContentType()).thenReturn("text/html");

        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setContentType("text/html");
            res.getWriter().write("<html><body></body></html>");
        };

        customFilter.doFilter(request, response, chain);

        assertTrue(output.toString("UTF-8").contains("ws://localhost:9999"));
    }

    @Test
    void htmlResponseViaOutputStreamGetsScriptInjected() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = createMockResponse(output);
        when(response.getContentType()).thenReturn("text/html");
        when(response.getCharacterEncoding()).thenReturn("UTF-8");

        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setContentType("text/html");
            byte[] content = "<html><body>Hello</body></html>".getBytes("UTF-8");
            res.getOutputStream().write(content);
        };

        filter.doFilter(request, response, chain);

        String result = output.toString("UTF-8");
        assertTrue(result.contains("<script>"), "Should contain script tag even when output stream was used");
        assertTrue(result.contains("Hello"));
    }

    @Test
    void nonHttpResponsePassedThrough() throws Exception {
        javax.servlet.ServletRequest request = mock(javax.servlet.ServletRequest.class);
        javax.servlet.ServletResponse response = mock(javax.servlet.ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void reloadScriptUsesReconnectNotReload() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = createMockResponse(output);
        when(response.getContentType()).thenReturn("text/html");

        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setContentType("text/html");
            res.getWriter().write("<html><body></body></html>");
        };

        filter.doFilter(request, response, chain);

        String result = output.toString("UTF-8");
        assertTrue(result.contains("function connect()"), "Should use reconnect function");
        assertTrue(result.contains("maxRetries"), "Should have retry limit");
        assertFalse(result.contains("location.reload();};}());"), "Should NOT reload on close");
    }
}
