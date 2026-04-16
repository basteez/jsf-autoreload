# Feature Specification: JSF Hot Reload

**Feature Branch**: `001-jsf-hot-reload`
**Created**: 2026-04-16
**Status**: Draft
**Input**: User description: "This plugin must watch file changes (JSF, classes, static files) and hot reload the project as the developer is working on it. Something like Quarkus does already, but for JSF legacy projects."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - XHTML/Facelets Live Reload (Priority: P1)

A JSF developer is working on a Facelets page (`.xhtml`). They edit the template markup, save the file, and the browser automatically refreshes to show the updated page without the developer manually reloading. This is the core value proposition: eliminating the save-switch-refresh cycle for view layer changes.

**Why this priority**: View template editing is the most frequent activity during JSF development. Automating the browser refresh for XHTML changes delivers immediate, high-frequency value with the simplest implementation scope.

**Independent Test**: Can be fully tested by editing any `.xhtml` file in a running JSF project and verifying the browser refreshes within seconds, displaying the updated content.

**Acceptance Scenarios**:

1. **Given** a JSF application is running with the plugin enabled, **When** the developer saves a modified `.xhtml` file, **Then** the browser refreshes and displays the updated page within 3 seconds.
2. **Given** a JSF application is running with the plugin enabled, **When** the developer saves an `.xhtml` file that contains a syntax error, **Then** the browser refreshes and the standard JSF error page is shown (the plugin does not suppress errors).
3. **Given** a JSF application is running with the plugin enabled, **When** the developer saves a file outside the watched directories, **Then** no browser refresh occurs.

---

### User Story 2 - Static Resource Live Reload (Priority: P2)

A developer edits a CSS stylesheet, JavaScript file, or image used by the JSF application. Upon saving, the browser automatically refreshes to reflect the changes. This covers resources served from the standard JSF resource directories or web application static paths.

**Why this priority**: Static resource changes are the second most frequent edit during UI development. Supporting them alongside XHTML changes makes the plugin a complete front-end development companion.

**Independent Test**: Can be tested by modifying a CSS file in the project's resource directory and verifying the browser refreshes to show the updated styling.

**Acceptance Scenarios**:

1. **Given** a JSF application is running with the plugin enabled, **When** the developer saves a modified `.css`, `.js`, or image file in a watched resource directory, **Then** the browser refreshes and reflects the change within 3 seconds.
2. **Given** a JSF application is running with the plugin enabled, **When** the developer adds a new static resource file to a watched directory, **Then** the browser refreshes so the new resource is available on next page load.

---

### User Story 3 - Compiled Class Change Reload (Priority: P3)

A developer modifies a Java source file (managed bean, backing bean, service class), compiles it (e.g., via IDE auto-build or `mvn compile`), and the plugin detects the updated `.class` file. The browser is then triggered to refresh so the developer can see the effect of the backend change.

**Why this priority**: Class file changes are less frequent than template edits and require an external compilation step. However, detecting recompiled classes and triggering a browser refresh closes the full development loop.

**Independent Test**: Can be tested by modifying a managed bean's return value, compiling, and verifying the browser refreshes to show the updated output.

**Acceptance Scenarios**:

1. **Given** a JSF application is running with the plugin enabled, **When** a `.class` file changes in the configured output directory, **Then** the plugin triggers a servlet context reload and the browser refreshes within 5 seconds showing the effect of the new class.
2. **Given** a JSF application is running with the plugin enabled, **When** multiple `.class` files change in rapid succession (e.g., a full rebuild), **Then** the plugin debounces and triggers a single context reload and browser refresh after changes stabilize.
3. **Given** a JSF application is running with the plugin enabled, **When** a context reload is triggered, **Then** the developer's session state is reset (expected trade-off, similar to a manual redeploy).

---

### User Story 4 - Auto-Compile Mode (Priority: P4)

A developer enables auto-compile mode so they don't need to manually run `mvn compile` or rely on IDE auto-build. They edit a `.java` source file, save it, and the plugin automatically compiles it, triggers a context reload, and refreshes the browser — a Quarkus-like experience for legacy JSF projects.

**Why this priority**: This is a convenience enhancement on top of the core class reload (US3). The default workflow (developer compiles externally) already works. Auto-compile adds polish but is not essential for the core value proposition.

**Independent Test**: Can be tested by enabling auto-compile in the plugin configuration, editing a `.java` source file, and verifying the browser refreshes with the updated behavior without any manual compile step.

**Acceptance Scenarios**:

1. **Given** auto-compile is enabled in the plugin configuration, **When** the developer saves a modified `.java` file, **Then** the plugin invokes the configured build tool, reloads the context, and refreshes the browser.
2. **Given** auto-compile is enabled and the developer saves a `.java` file with a compilation error, **Then** the plugin logs the compilation error with the full compiler output and does not trigger a context reload or browser refresh.
3. **Given** auto-compile is disabled (default), **When** the developer saves a `.java` file, **Then** the plugin ignores it entirely; only `.class` file changes trigger a reload.

---

### User Story 5 - Plugin Configuration (Priority: P5)

A developer configures the plugin to customize watched directories, file patterns, debounce timing, and enable/disable behavior. The plugin provides sensible defaults so it works out of the box for standard JSF project layouts, but allows customization for non-standard setups.

**Why this priority**: Configuration enables the plugin to work across diverse project structures. However, sensible defaults mean most developers never need to configure anything, making this lower priority than core functionality.

**Independent Test**: Can be tested by adding a configuration entry that changes the watched directory, then verifying the plugin watches the new location instead of the default.

**Acceptance Scenarios**:

1. **Given** a JSF project with standard Maven layout, **When** the developer adds the plugin with no custom configuration, **Then** the plugin automatically watches `src/main/webapp` and the compiled class output directory.
2. **Given** a developer wants to exclude certain directories, **When** they add exclusion patterns to the configuration, **Then** changes in excluded directories do not trigger a browser refresh.
3. **Given** a developer wants to adjust the debounce interval, **When** they configure a custom debounce value, **Then** the plugin uses that interval to coalesce rapid changes into a single refresh.

---

### Edge Cases

- What happens when the developer saves a file while the browser is navigating? The refresh event is queued and delivered once the page stabilizes.
- What happens when the watched directory is deleted or renamed at runtime? The plugin logs a warning and stops watching that path; it does not crash the application.
- What happens when the browser tab is closed or the connection is lost? The plugin continues watching files; when the browser reconnects, it picks up the current state.
- What happens when multiple browser tabs are open on the same application? All connected tabs receive the refresh notification.
- What happens in a multi-developer environment sharing a server? Each developer's browser connection is independent; a file change triggers refresh for all connected browsers. The SSE endpoint has no authentication — the plugin is development-only and auto-disables in production, so access restriction adds no meaningful security value.
- What happens when the application is deployed in production without removing the plugin? The plugin detects it is not in development mode and disables itself completely, with zero overhead.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The plugin MUST monitor specified directories for file changes (creation, modification, deletion).
- **FR-002**: The plugin MUST detect changes to JSF view files (`.xhtml`, `.jspx`, `.jsp`) and trigger a browser refresh.
- **FR-003**: The plugin MUST detect changes to static resources (`.css`, `.js`, `.png`, `.jpg`, `.gif`, `.svg`, `.ico`, `.woff`, `.woff2`) and trigger a browser refresh.
- **FR-004**: The plugin MUST detect changes to compiled class files (`.class`) and trigger a browser refresh.
- **FR-005**: The plugin MUST debounce rapid successive file changes into a single refresh notification to avoid excessive reloading.
- **FR-006**: The plugin MUST provide a browser-side mechanism using Server-Sent Events (SSE) to receive refresh notifications and reload the page automatically. The injected JavaScript opens an SSE connection to a plugin-provided servlet endpoint; the server pushes reload events over this connection.
- **FR-007**: The plugin MUST inject its browser-side refresh mechanism into JSF-rendered pages transparently, without requiring the developer to modify their templates.
- **FR-008**: The plugin MUST be completely inert when the application is not running in development mode (i.e., `javax.faces.PROJECT_STAGE` or `jakarta.faces.PROJECT_STAGE` is not set to `Development`).
- **FR-015**: The plugin MUST support both `javax.faces` (JSF 2.0–2.3, Java EE) and `jakarta.faces` (JSF 3.0+, Jakarta EE 9+) namespaces within a single artifact, using runtime detection to determine which namespace is in use.
- **FR-009**: The plugin MUST provide sensible default configuration for standard Maven WAR project layouts (`src/main/webapp` for views/resources, `target/classes` for compiled classes).
- **FR-010**: The plugin MUST allow developers to configure: watched directories, file inclusion/exclusion patterns, debounce interval, and enable/disable toggle.
- **FR-011**: The plugin MUST log file-watch events and reload triggers with structured diagnostics (file path, event type, timestamp).
- **FR-012**: When a `.class` file change is detected, the plugin MUST trigger a servlet context reload so the new classes take effect, followed by a browser refresh. This is the default behavior. The reload mechanism is container-specific, implemented via a pluggable container adapter interface. A Tomcat adapter MUST be provided out of the box. The project MUST use a modular architecture (e.g., Maven multi-module) so that new container adapters (WildFly, Jetty, GlassFish, etc.) can be added as separate modules without modifying the core.
- **FR-013**: The plugin MUST offer an optional auto-compile mode, configurable via the build tool (Maven plugin configuration or Gradle equivalent). When enabled, the plugin watches `.java` source files in addition to `.class` files, invokes the build tool to compile changed sources, and then proceeds with the standard context reload and browser refresh cycle.
- **FR-014**: Auto-compile mode MUST be disabled by default. When disabled, the plugin only watches compiled `.class` files and the developer is responsible for triggering compilation externally (IDE auto-build, manual `mvn compile`, etc.).

### Key Entities

- **Watched Directory**: A filesystem path monitored for changes. Has attributes: path, inclusion patterns, exclusion patterns, active status.
- **File Change Event**: A detected filesystem event. Has attributes: file path, change type (created/modified/deleted), timestamp.
- **Reload Notification**: A message sent to connected browsers to trigger a refresh. Has attributes: trigger file, event type, timestamp, debounce group ID.
- **Browser Connection**: A persistent SSE connection from a browser tab to the plugin's notification servlet endpoint. Has attributes: connection ID, connected-since timestamp, last-notified timestamp.
- **Plugin Configuration**: The set of user-defined and default settings controlling plugin behavior. Has attributes: watched directories, file patterns, debounce interval, enabled flag.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers see XHTML template changes reflected in the browser within 3 seconds of saving, without manual refresh.
- **SC-002**: Developers see static resource changes reflected in the browser within 3 seconds of saving, without manual refresh.
- **SC-003**: Developers see the effect of recompiled classes reflected in the browser within 5 seconds of the `.class` file update, without manual refresh.
- **SC-004**: The plugin works out of the box with zero configuration on a standard Maven WAR project.
- **SC-005**: The plugin adds zero measurable overhead to production deployments when the application is not in Development stage.
- **SC-006**: The plugin successfully delivers reload notifications to 10 or more simultaneously connected browser tabs.
- **SC-007**: Rapid file changes (e.g., 20 saves within 2 seconds) result in at most 2 browser refreshes due to debouncing.
- **SC-008**: The plugin operates without requiring changes to the developer's existing JSF templates, `web.xml`, or build configuration beyond adding the dependency.

## Clarifications

### Session 2026-04-16

- Q: Which JSF namespace(s) must the plugin support? → A: Both `javax.faces` and `jakarta.faces` in a single artifact with runtime detection.
- Q: What browser notification transport should the plugin use? → A: Server-Sent Events (SSE) — unidirectional, HTTP-native, no special proxy configuration needed.
- Q: Which servlet container(s) should the plugin target for context reload? → A: Tomcat-first with a pluggable container adapter interface. Project uses a modular architecture (Maven multi-module) so new adapters can be added as separate modules.
- Q: What is the minimum Java version the plugin must support? → A: Java 8 (compile with `--release 8`). Single artifact runs on Java 8+. Multi-target JDK-optimized builds can be added later via CI/CD if profiling warrants it.
- Q: Should the SSE notification endpoint require any access restriction? → A: No authentication — development-only tool that auto-disables in production. No real-world security benefit to adding auth.

## Assumptions

- Developers are using a standard Maven WAR project layout. Gradle and non-standard layouts are supported via configuration but not auto-detected.
- The application server supports servlet 3.0+ (required for the async notification mechanism).
- The JSF application uses `Development` project stage during local development (standard practice).
- By default, the developer's IDE or build tool handles Java compilation; the plugin only watches for `.class` file changes, not `.java` source files directly. When auto-compile mode is enabled, the plugin also watches `.java` sources and invokes the build tool.
- The browser supports modern web standards (all current evergreen browsers).
- The plugin is added as a Maven dependency; no standalone server or external process is required.
- File system event notification is available on the target OS (Linux inotify, macOS FSEvents, Windows ReadDirectoryChanges). Polling fallback is provided for environments where native events are unavailable.
- The plugin targets the same JVM process as the application server — it is deployed inside the WAR, not as a sidecar.
- The plugin ships as a single artifact supporting both `javax.faces` (JSF 2.0–2.3) and `jakarta.faces` (JSF 3.0+) via runtime namespace detection. No separate modules or classifier variants are required.
- The project uses a modular architecture (Maven multi-module): a core module containing the file watcher, SSE infrastructure, and container adapter SPI; a Tomcat adapter module shipped out of the box; additional container adapter modules (WildFly, Jetty, GlassFish, etc.) can be added independently without modifying the core.
- The plugin targets Java 8 as the minimum version (`--release 8`). The single artifact runs on Java 8, 11, 17, 21+. Multi-target JDK-optimized builds may be introduced in a future release via CI/CD if performance profiling warrants it.
