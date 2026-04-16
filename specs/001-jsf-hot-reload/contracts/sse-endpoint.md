# Contract: SSE Reload Endpoint

**Type**: HTTP Server-Sent Events endpoint
**Module**: `jsf-autoreload-core`
**Audience**: Browser-side injected JavaScript (internal to the plugin)

## Endpoint

```
GET /_jsf-autoreload/events
```

**Content-Type**: `text/event-stream`
**Cache-Control**: `no-cache`
**Connection**: `keep-alive`

## Connection Lifecycle

1. **Client opens connection**: Browser's `EventSource` sends GET request
2. **Server accepts**: Starts async context, stores connection, sends initial comment (`:ok\n\n`) as heartbeat
3. **Server pushes events**: On file changes (after debounce), sends reload events to all connections
4. **Client disconnects**: Server detects via `AsyncListener.onError()` / `onTimeout()`, removes connection
5. **Client reconnects**: `EventSource` auto-reconnects per SSE spec (default ~3s); server sends current state

## Event Format

### reload

Sent when a file change is detected and debounce window has elapsed.

```
id: 550e8400-e29b-41d4-a716-446655440000
event: reload
data: {"file":"src/main/webapp/index.xhtml","type":"MODIFIED","category":"VIEW","contextReload":false}

```

Fields in `data` JSON:
| Field | Type | Description |
|-------|------|-------------|
| `file` | string | Relative path of the file that triggered the reload |
| `type` | string | `CREATED`, `MODIFIED`, or `DELETED` |
| `category` | string | `VIEW`, `STATIC`, `CLASS`, or `SOURCE` |
| `contextReload` | boolean | Whether a servlet context reload was triggered |

### heartbeat

Sent every 30 seconds to keep the connection alive through proxies/load balancers.

```
:heartbeat

```

## Error Responses

| Status | Condition |
|--------|-----------|
| 404 | Plugin is disabled or not in development mode |
| 503 | Plugin is shutting down |

## Client Implementation (Injected Script)

```javascript
(function() {
  if (typeof EventSource === 'undefined') return;
  var es = new EventSource('/_jsf-autoreload/events');
  es.addEventListener('reload', function(e) {
    location.reload();
  });
})();
```

The `EventSource` API handles:
- Automatic reconnection on connection loss
- Last-Event-ID header for resumption (server can use this for dedup)

## Concurrency

- The endpoint supports multiple simultaneous connections (target: 10+, per SC-006)
- Each connection is an independent `AsyncContext` — no shared mutable state between connections
- Broadcast is performed by iterating the connection set with copy-on-write semantics

## Security

- No authentication — development-only endpoint
- Endpoint is not registered when the application is not in Development stage
- The `/_jsf-autoreload/` path prefix is reserved by the plugin
