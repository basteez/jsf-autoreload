# Data Model: JSF Hot Reload Plugin

**Feature Branch**: `001-jsf-hot-reload`
**Date**: 2026-04-16

## Entity Definitions

### 1. PluginConfiguration

Represents the complete set of user-defined and default settings controlling plugin behavior.

| Field | Type | Default | Validation |
|-------|------|---------|------------|
| `enabled` | `boolean` | `true` | — |
| `watchedDirectories` | `List<WatchedDirectory>` | Auto-detected from project layout | At least one directory if enabled |
| `debounceIntervalMs` | `long` | `500` | Must be >= 0 |
| `classDebounceIntervalMs` | `long` | `1000` | Must be >= 0 |
| `sseEndpointPath` | `String` | `/_jsf-autoreload/events` | Must start with `/` |
| `autoCompileEnabled` | `boolean` | `false` | — |
| `autoCompileCommand` | `String` | `null` | Required if autoCompileEnabled is true |
| `sourceDirectory` | `String` | `src/main/java` | Required if autoCompileEnabled is true |

**Sources** (read in order, later overrides earlier):
1. Built-in defaults
2. `web.xml` context-params (prefix: `com.jsfautoreload.`)
3. System properties (prefix: `jsfautoreload.`)

**Relationships**: Contains one or more `WatchedDirectory` entries.

### 2. WatchedDirectory

A filesystem path monitored for changes, with inclusion/exclusion filtering.

| Field | Type | Default | Validation |
|-------|------|---------|------------|
| `path` | `Path` | — | Must exist and be a directory |
| `inclusionPatterns` | `Set<String>` | `["**/*.xhtml", "**/*.css", "**/*.js", "**/*.class"]` | Valid glob patterns |
| `exclusionPatterns` | `Set<String>` | `["**/.*", "**/node_modules/**"]` | Valid glob patterns |
| `recursive` | `boolean` | `true` | — |
| `active` | `boolean` | `true` | — |

**Default directories** (standard Maven WAR layout):
- `src/main/webapp` — XHTML views, static resources
- `target/classes` — compiled class files

**Relationships**: Belongs to `PluginConfiguration`. Produces `FileChangeEvent` instances.

### 3. FileChangeEvent

A detected filesystem event emitted by the file watcher.

| Field | Type | Description |
|-------|------|-------------|
| `filePath` | `Path` | Absolute path of the changed file |
| `changeType` | `ChangeType` | `CREATED`, `MODIFIED`, or `DELETED` |
| `timestamp` | `Instant` | When the change was detected |
| `fileCategory` | `FileCategory` | Derived from file extension |

**Enum: ChangeType**
```
CREATED, MODIFIED, DELETED
```

**Enum: FileCategory**
```
VIEW       — .xhtml, .jspx, .jsp
STATIC     — .css, .js, .png, .jpg, .gif, .svg, .ico, .woff, .woff2
CLASS      — .class
SOURCE     — .java (only when auto-compile enabled)
OTHER      — unrecognized (ignored)
```

**Derived behavior**: `fileCategory` determines the debounce group:
- `VIEW` or `STATIC` → `DebounceGroup.VIEW_STATIC`
- `CLASS` → `DebounceGroup.CLASS`
- `SOURCE` → triggers auto-compile, then CLASS flow

**Relationships**: Produced by `DirectoryWatcher`. Consumed by `Debouncer`.

### 4. ReloadNotification

A message sent to connected browsers to trigger a refresh. Produced after debouncing.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique notification ID (UUID) |
| `triggerFile` | `Path` | File that initiated the reload |
| `eventType` | `ChangeType` | Type of file change |
| `fileCategory` | `FileCategory` | Category of the changed file |
| `timestamp` | `Instant` | When the notification was emitted |
| `requiresContextReload` | `boolean` | True if CLASS category (container must reload) |

**SSE format** (sent to browser):
```
id: {id}
event: reload
data: {"file":"{triggerFile}","type":"{eventType}","category":"{fileCategory}","contextReload":{requiresContextReload}}
```

**Relationships**: Produced by `Debouncer`. Consumed by `ConnectionManager` for broadcast.

### 5. BrowserConnection

A persistent SSE connection from a browser tab to the plugin's notification endpoint.

| Field | Type | Description |
|-------|------|-------------|
| `connectionId` | `String` | Unique connection ID (UUID) |
| `asyncContext` | `AsyncContext` | Servlet async context for this connection |
| `connectedSince` | `Instant` | When the connection was established |
| `lastNotifiedAt` | `Instant` | When the last notification was sent (nullable) |
| `state` | `ConnectionState` | Current connection state |

**Enum: ConnectionState**
```
CONNECTED, DISCONNECTED
```

**State transitions**:
```
CONNECTED ──(timeout/error/close)──> DISCONNECTED
```

**Relationships**: Managed by `ConnectionManager`. Receives `ReloadNotification` broadcasts.

### 6. ContainerAdapter (SPI)

Interface for container-specific operations. Implemented by adapter modules.

| Method | Description |
|--------|-------------|
| `boolean supports()` | Returns true if this adapter can handle the current container |
| `void reload(ServletContext)` | Triggers a webapp context reload |
| `String containerName()` | Human-readable container name for logging |

**Discovery**: Via `ServiceLoader<ContainerAdapter>`. The first adapter where `supports()` returns `true` is selected.

**Known implementations**:
- `TomcatAdapter` (in `jsf-autoreload-tomcat` module)

**Relationships**: Used by `ReloadCoordinator` when `requiresContextReload` is true.

## State Machine: Plugin Lifecycle

```
                  ┌──────────────┐
                  │ INITIALIZING │
                  └──────┬───────┘
                         │ SCI.onStartup()
                         │ (dev mode detected)
                         v
                  ┌──────────────┐
                  │    ACTIVE    │◄───── watches files, serves SSE
                  └──────┬───────┘
                         │ context shutdown
                         v
                  ┌──────────────┐
                  │ SHUTTING_DOWN│───── stops watcher, closes connections
                  └──────┬───────┘
                         │
                         v
                  ┌──────────────┐
                  │   INACTIVE   │
                  └──────────────┘
```

If dev mode is **not** detected during INITIALIZING, the plugin transitions directly to INACTIVE with zero overhead.

## State Machine: File Change Processing

```
  FileChangeEvent
       │
       v
  ┌──────────┐     debounce timer     ┌───────────┐
  │ RECEIVED │─────────────────────────│ DEBOUNCING│
  └──────────┘                         └─────┬─────┘
                                             │ timer expires
                                             v
                                     ┌───────────────┐
                                     │ NOTIFYING      │
                                     │ (context reload │
                                     │  if CLASS)      │
                                     └───────┬────────┘
                                             │
                                             v
                                     ┌───────────────┐
                                     │   BROADCAST    │──── SSE to all browsers
                                     └───────────────┘
```

If a new event arrives during DEBOUNCING for the same group, the timer resets (classic debounce).

## Component Interaction Diagram

```
 ┌─────────────────────────────────────────────────────────────────────┐
 │                        jsf-autoreload-core                         │
 │                                                                     │
 │  ┌──────────────┐    FileChangeEvent    ┌───────────┐              │
 │  │DirectoryWatch│──────────────────────>│ Debouncer  │              │
 │  │   er         │                       └─────┬──────┘              │
 │  └──────────────┘                             │                     │
 │                                    ReloadNotification               │
 │                                               │                     │
 │  ┌──────────────┐                             v                     │
 │  │ ScriptInject │    ┌────────────────┐  ┌──────────────┐          │
 │  │   or (JSF)   │    │ReloadCoordinat │  │ConnectionMgr │          │
 │  └──────────────┘    │   or           │  │  (SSE)       │          │
 │         │            └───────┬────────┘  └──────┬───────┘          │
 │         │ injects script     │ reload()         │ broadcast        │
 │         v                    v                  v                   │
 │    ┌─────────┐     ┌────────────────┐   ┌──────────────┐          │
 │    │ Browser │     │ContainerAdapter│   │  Browser     │          │
 │    │  (head) │     │     (SPI)      │   │  Connections │          │
 │    └─────────┘     └────────────────┘   └──────────────┘          │
 │                           │                                        │
 └───────────────────────────┼────────────────────────────────────────┘
                             │
                    ┌────────┴─────────┐
                    │jsf-autoreload-   │
                    │   tomcat         │
                    │ (TomcatAdapter)  │
                    └──────────────────┘
```
