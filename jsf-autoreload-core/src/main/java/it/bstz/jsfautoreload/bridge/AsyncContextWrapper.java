package it.bstz.jsfautoreload.bridge;

import java.io.IOException;
import java.io.OutputStream;

public final class AsyncContextWrapper {

    private final Object asyncContext;
    private final OutputStream outputStream;
    private final Runnable completer;

    public AsyncContextWrapper(Object asyncContext, OutputStream outputStream, Runnable completer) {
        this.asyncContext = asyncContext;
        this.outputStream = outputStream;
        this.completer = completer;
    }

    public Object getRawContext() {
        return asyncContext;
    }

    public void write(String data) throws IOException {
        outputStream.write(data.getBytes("UTF-8"));
        outputStream.flush();
    }

    public void complete() {
        completer.run();
    }
}
