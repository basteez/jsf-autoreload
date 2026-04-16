# Feature Specification: Fix Reload Bugs

**Feature Branch**: `002-fix-reload-bugs`  
**Created**: 2026-04-16  
**Status**: Draft  
**Input**: User description: "Browser refresh does not work (SSE broadcasting to 0 connections) and Java files reload does not work at all"

## Clarifications

### Session 2026-04-16

- Q: When the SSE connection drops, should the client script automatically reconnect? → A: Yes, rely on the browser's native `EventSource` auto-reconnect (default behavior, no extra client code needed).
- Q: What should the default SSE heartbeat interval be? → A: 30 seconds.
- Q: What should the default debounce interval for file change events be? → A: 500ms.
- Q: Should the SSE endpoint have access restrictions beyond the dev-mode gate? → A: No, the dev-mode gate (FR-008) is sufficient; no additional restrictions needed.
- Q: What data should the SSE reload event carry? → A: Simple reload command only (e.g., `event: reload`, no payload); client always does a full page refresh.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browser auto-refreshes on view/static file change (Priority: P1)

A developer edits an XHTML template or a static resource (CSS, JS, image) in their JSF project. The browser, which has an open page from the running application, automatically refreshes to reflect the change without the developer needing to manually reload.

**Why this priority**: This is the core value proposition of the plugin. Without browser auto-refresh, the plugin delivers no visible benefit to the developer. The root cause is that the client-side script that establishes the SSE connection is never injected into rendered pages, so no browsers are listening for reload events.

**Independent Test**: Start a JSF application with the plugin enabled, open a page in the browser, edit an XHTML file, and observe that the browser reloads automatically within a few seconds.

**Acceptance Scenarios**:

1. **Given** a JSF application running with the plugin in development mode and a page open in the browser, **When** the developer modifies an `.xhtml` file, **Then** the browser automatically refreshes within the configured debounce interval.
2. **Given** a JSF application running with the plugin and a page open in the browser, **When** the developer modifies a static resource (`.css`, `.js`), **Then** the browser automatically refreshes.
3. **Given** a JSF application running with the plugin and a page open in the browser, **When** the developer opens the browser's developer tools network tab, **Then** an active SSE connection to the plugin's event endpoint is visible.

---

### User Story 2 - Browser auto-refreshes after Java class recompilation (Priority: P1)

A developer changes a Java source file (e.g., a managed bean), recompiles (producing updated `.class` files in the output directory), and the running application picks up the new class definitions and the browser refreshes to show the updated behavior.

**Why this priority**: Java bean changes are the most common code changes during JSF development. The server-side context reload already works (logs confirm "Context reload completed"), but the browser never refreshes because no SSE connections exist. Fixing Story 1 will also fix the browser-refresh aspect of this story, but this story additionally validates the end-to-end Java class change flow.

**Independent Test**: Start a JSF application with the plugin, open a page, recompile a managed bean (so a new `.class` file appears in `target/classes`), and observe that the application context reloads and the browser refreshes.

**Acceptance Scenarios**:

1. **Given** a running JSF application with the plugin and a page open in the browser, **When** the developer recompiles a managed bean (`.class` file changes in the watched directory), **Then** the application context reloads and the browser refreshes automatically.
2. **Given** a running JSF application with the plugin, **When** a `.class` file change is detected, **Then** the plugin logs confirm both context reload and a successful SSE broadcast to at least 1 connection.

---

### User Story 3 - SSE connection stays alive during idle periods (Priority: P2)

A developer has a page open in the browser and does not make any file changes for an extended period. The SSE connection remains active and ready to deliver reload events when the developer resumes editing.

**Why this priority**: Without connection keepalive, the SSE connection may be silently dropped by proxies or the browser after a period of inactivity, making the next reload event invisible to the developer. This degrades trust in the plugin.

**Independent Test**: Open a page, wait for longer than the heartbeat interval without making changes, then edit a file and confirm the browser still auto-refreshes.

**Acceptance Scenarios**:

1. **Given** a browser with an open SSE connection to the plugin, **When** no file changes occur for longer than the heartbeat interval, **Then** the SSE connection remains active because periodic heartbeat messages are sent.
2. **Given** a browser with an open SSE connection, **When** the developer edits a file after a period of inactivity, **Then** the browser refreshes just as quickly as it would after a fresh connection.

---

### User Story 4 - Disconnected browsers are cleaned up (Priority: P3)

When a developer closes a browser tab or the browser disconnects, the plugin detects the disconnection and removes the stale connection from its tracking, keeping resource usage minimal.

**Why this priority**: Stale connections waste memory and may cause errors during broadcast. Proper cleanup ensures the plugin remains stable during long development sessions with many tab opens/closes.

**Independent Test**: Open a page, verify the connection count is 1, close the tab, then trigger a file change and verify the broadcast log shows the connection was removed.

**Acceptance Scenarios**:

1. **Given** a browser connected via SSE, **When** the browser tab is closed, **Then** the plugin detects the disconnection and removes the connection from tracking within a reasonable time.
2. **Given** multiple browser tabs connected via SSE, **When** one tab is closed and a file change triggers a broadcast, **Then** only the remaining active connections receive the event.

---

### Edge Cases

- What happens when the browser does not support Server-Sent Events (e.g., very old browsers)?
- How does the plugin behave when the developer has multiple browser tabs open on the same application?
- If the SSE endpoint becomes unreachable, the browser relies on the native `EventSource` auto-reconnect mechanism (no custom retry logic needed).
- What happens if the application context reload fails (e.g., due to a compilation error in the bean)?
- Burst file changes (e.g., build tool writing many `.class` files at once) are coalesced via a 500ms debounce interval before triggering a single reload event.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The plugin MUST inject a client-side script into every rendered JSF page that establishes an SSE connection to the plugin's event endpoint.
- **FR-002**: The SSE event endpoint MUST accept and maintain persistent connections from browser clients.
- **FR-003**: When a watched file changes, the plugin MUST broadcast a simple reload event (`event: reload`, no payload) to all connected browsers. The client script responds by performing a full page refresh. File change events MUST be debounced with a default interval of 500ms to coalesce rapid writes (e.g., build tool output).
- **FR-004**: When a `.class` file change is detected, the plugin MUST trigger a server-side application context reload before broadcasting the reload event to browsers.
- **FR-005**: The plugin MUST send periodic heartbeat messages (default interval: 30 seconds) over SSE connections to prevent proxy/browser timeouts during idle periods.
- **FR-006**: The plugin MUST detect and remove stale browser connections (e.g., closed tabs, network disconnections) from its connection tracking.
- **FR-007**: The plugin MUST function correctly when multiple browser tabs are simultaneously connected.
- **FR-008**: The plugin MUST only activate when the application is running in development mode.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When a developer edits an XHTML or static file, the browser refreshes automatically within 3 seconds.
- **SC-002**: When a developer recompiles a Java class, the application context reloads and the browser refreshes automatically within 5 seconds.
- **SC-003**: SSE connections survive idle periods of at least 5 minutes without dropping.
- **SC-004**: Stale connections (closed tabs) are cleaned up within 1 minute of disconnection.
- **SC-005**: The plugin correctly broadcasts to all connected browser tabs simultaneously.

## Assumptions

- The developer's application server supports asynchronous servlet processing (Servlet 3.0+).
- The developer is using a supported container (e.g., Tomcat) with an available container adapter for context reload.
- The browser supports the `EventSource` API (all modern browsers do; the plugin gracefully degrades if unavailable).
- File changes are detected via the file system's native watch service (Java NIO WatchService); the OS must support this.
- The developer recompiles Java source files externally (e.g., via IDE or build tool); the plugin watches for the resulting `.class` file changes, not `.java` source changes.
