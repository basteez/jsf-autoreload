package it.bstz.jsfautoreload.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class DevModeFilter implements Filter {

    private String reloadScript;

    @Override
    public void init(FilterConfig filterConfig) {
        String portStr = System.getProperty("jsf.autoreload.port", "35729");
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            port = 35729;
        }

        reloadScript = "<script>(function(){"
                + "var retries=0,maxRetries=10;"
                + "function connect(){"
                + "var ws=new WebSocket('ws://localhost:" + port + "');"
                + "ws.onmessage=function(e){if(e.data==='reload')window.location.reload();};"
                + "ws.onopen=function(){retries=0;};"
                + "ws.onclose=function(){if(retries<maxRetries){retries++;setTimeout(connect,1000*Math.min(retries,5));}};"
                + "}"
                + "connect();"
                + "}());</script>";
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        if (response.isCommitted()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        BufferingResponseWrapper wrapper = new BufferingResponseWrapper(httpResponse);

        chain.doFilter(request, wrapper);
        wrapper.flushBuffer();

        String contentType = wrapper.getContentType();
        byte[] content = wrapper.getContent();

        if (contentType != null && contentType.contains("text/html") && content.length > 0) {
            String charset = wrapper.getCharacterEncoding() != null ? wrapper.getCharacterEncoding() : "UTF-8";
            String html = new String(content, charset);

            int bodyClose = html.lastIndexOf("</body>");
            if (bodyClose >= 0) {
                html = html.substring(0, bodyClose) + reloadScript + html.substring(bodyClose);
            } else {
                html = html + reloadScript;
            }

            byte[] modifiedContent = html.getBytes(charset);
            httpResponse.setContentLength(modifiedContent.length);
            httpResponse.getOutputStream().write(modifiedContent);
        } else {
            httpResponse.getOutputStream().write(content);
        }
    }

    @Override
    public void destroy() {
    }

    private static class BufferingResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private PrintWriter writer;
        private ServletOutputStream outputStream;

        BufferingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (outputStream != null) {
                throw new IllegalStateException("getOutputStream() already called");
            }
            if (writer == null) {
                String charset = getCharacterEncoding();
                if (charset == null) charset = "UTF-8";
                writer = new PrintWriter(new OutputStreamWriter(buffer, charset));
            }
            return writer;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() already called");
            }
            if (outputStream == null) {
                outputStream = new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        buffer.write(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        buffer.write(b, off, len);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {
                    }
                };
            }
            return outputStream;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            if (outputStream != null) {
                outputStream.flush();
            }
        }

        @Override
        public void resetBuffer() {
            buffer.reset();
        }

        byte[] getContent() throws IOException {
            flushBuffer();
            return buffer.toByteArray();
        }
    }
}
