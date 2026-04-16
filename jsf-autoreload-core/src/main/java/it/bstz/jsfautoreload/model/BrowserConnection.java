package it.bstz.jsfautoreload.model;

import it.bstz.jsfautoreload.bridge.AsyncContextWrapper;

import java.time.Instant;
import java.util.UUID;

public final class BrowserConnection {

    public enum ConnectionState {
        CONNECTED,
        DISCONNECTED
    }

    private final String connectionId;
    private final AsyncContextWrapper asyncContext;
    private final Instant connectedSince;
    private volatile Instant lastNotifiedAt;
    private volatile ConnectionState state;

    public BrowserConnection(AsyncContextWrapper asyncContext) {
        this.connectionId = UUID.randomUUID().toString();
        this.asyncContext = asyncContext;
        this.connectedSince = Instant.now();
        this.state = ConnectionState.CONNECTED;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public AsyncContextWrapper getAsyncContext() {
        return asyncContext;
    }

    public Instant getConnectedSince() {
        return connectedSince;
    }

    public Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(Instant lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrowserConnection)) return false;
        BrowserConnection that = (BrowserConnection) o;
        return connectionId.equals(that.connectionId);
    }

    @Override
    public int hashCode() {
        return connectionId.hashCode();
    }
}
