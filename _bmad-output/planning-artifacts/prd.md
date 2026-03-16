---
stepsCompleted: ['step-01-init', 'step-02-discovery', 'step-02b-vision', 'step-02c-executive-summary', 'step-03-success', 'step-04-journeys', 'step-05-domain', 'step-06-innovation', 'step-07-project-type', 'step-08-scoping', 'step-09-functional', 'step-10-nonfunctional', 'step-11-polish', 'step-12-complete']
inputDocuments: ['_bmad-output/implementation-artifacts/tech-spec-jsf-autoreload-plugin.md']
workflowType: 'prd'
documentCounts:
  briefs: 0
  research: 0
  projectDocs: 0
  projectContext: 0
  other: 1
classification:
  projectType: developer_tool
  domain: general
  complexity: low
  projectContext: greenfield
  distribution: open_source
  targets: ['Gradle Plugin Portal', 'Maven Central']
---

# Product Requirements Document - jsf-autoreload

**Author:** Tiziano
**Date:** 2026-03-13

## Executive Summary

jsf-autoreload brings live-reload to JavaServer Faces development. Edit, save, see it live — when a developer saves an `.xhtml` template, CSS, JS, or static resource, the plugin copies the updated file into the exploded WAR output directory and triggers an automatic browser refresh via WebSocket. No server restart, no manual reload. What used to be a save-rebuild-restart-wait-refresh cycle measured in tens of seconds becomes a sub-2-second feedback loop. The target audience is Java developers maintaining JSF 2.x applications on any Java EE application server.

The tool ships as a build plugin (Gradle and Maven) and a dependency-free runtime JAR containing a Servlet Filter that auto-injects the reload script. A `ServerAdapter` abstraction makes the architecture server-agnostic from day one, with IBM Open Liberty and Apache Tomcat as the initial supported targets and a public API for community-contributed adapters. Distribution is open source via Gradle Plugin Portal and Maven Central.

### What Makes This Special

jsf-autoreload is the first and only autoreload tool purpose-built for JSF. The ecosystem has no competing solution — modern tooling has moved on from JSF, leaving thousands of active developers without the DX improvements their stack deserves.

The core insight is that JSF's template engines already support hot reload — both Mojarra and MyFaces expose a `REFRESH_PERIOD` configuration that forces template re-evaluation. jsf-autoreload connects dots the framework already provides and packages them into a seamless developer experience: file watching, output directory sync, and browser notification. The result is modern DX on a legacy stack, with no migration cost — just apply the plugin to an existing build and go.

## Project Classification

- **Project Type:** Developer Tool (build plugin + runtime library)
- **Domain:** General (developer tooling)
- **Complexity:** Low — standard software practices, no regulatory or compliance requirements
- **Project Context:** Greenfield — new open source project, no legacy constraints
- **Distribution:** Open source, Gradle Plugin Portal + Maven Central

## Success Criteria

### User Success

- Developer installs the plugin on an existing JSF project and gets live-reload working without configuration friction — "apply plugin, run, it works"
- The "aha" moment: saving an `.xhtml` file and watching the browser refresh automatically for the first time — the developer feels like they've arrived in the present century
- Success extends beyond individual adoption — teams adopt it as their standard JSF development workflow

### Business Success

- **6-month target:** 1,000+ GitHub stars, establishing jsf-autoreload as the recognized live-reload solution for JSF
- **Downloads:** Meaningful traction on both Gradle Plugin Portal and Maven Central
- **Community:** Active contributions of any kind — bug reports, PRs (especially new ServerAdapter implementations), documentation, and community support
- **Positioning:** Recognized as the first and only autoreload tool for JSF — category creator, not competitor

### Technical Success

- End-to-end feedback loop (file save → browser refresh) under 2 seconds in the typical case
- File detection and copy completes under 500ms
- Works reliably across Mojarra and MyFaces JSF implementations
- Server-agnostic architecture proven with two adapters (Liberty, Tomcat); public `ServerAdapter` API documented for community extension
- Both Gradle and Maven plugins functional and published

### Measurable Outcomes

- Zero-config experience for standard project layouts (developer applies plugin, runs one command, gets live-reload)
- No server restart required for `.xhtml`, CSS, JS, and static resource changes
- Runtime JAR is dependency-free — no conflicts with any user WAR
- Plugin does not interfere with existing build or server lifecycle

## User Journeys

### Journey 1: Marco — The Daily JSF Developer

**Who:** Marco is a mid-level Java developer at a large insurance company. He maintains a JSF 2.x application running on Liberty that handles policy management. The app has hundreds of `.xhtml` templates.

**Opening Scene:** Marco starts his morning by tweaking the layout of a policy summary page. He edits `policy-summary.xhtml`, saves, switches to the terminal, runs a rebuild, waits 30+ seconds for the server to restart, switches to the browser, manually refreshes, scrolls back to where he was — only to realize the margin is still off. He repeats this cycle eight times before lunch.

**Rising Action:** Marco finds jsf-autoreload on the Gradle Plugin Portal. The README shows a 3-line setup and a GIF of live-reload in action. A compatibility matrix at the top confirms: Liberty + Gradle + Mojarra — his exact stack. He adds `id 'it.bstz.jsf-autoreload'` to his `build.gradle`, runs `gradle jsfDev`, and opens his browser.

**Climax:** Marco edits `policy-summary.xhtml`, hits save, and the browser refreshes by itself. The updated page is there in under two seconds. He stares for a moment, then edits the CSS file. Save. Browser refreshes. He grins.

**Resolution:** Marco's edit-feedback cycle drops from 30+ seconds to under 2 seconds. He stops dreading frontend work on the JSF app. He shares the plugin with his team in the next standup. The whole team adopts it within the week.

**Capabilities revealed:** Plugin installation, build tool integration, file watching, WebSocket reload, zero-config experience, exploded WAR file copy, clear README with GIF demo and compatibility matrix.

### Journey 2: Aisha — The Evaluator

**Who:** Aisha is a senior developer and tech lead at a logistics company. She's responsible for tooling decisions on a JSF 2.x application running on Tomcat. She's skeptical of new tools — she's been burned by plugins that promise magic and deliver pain.

**Opening Scene:** Aisha is searching for ways to speed up her team's JSF development workflow. She finds jsf-autoreload on GitHub while browsing Stack Overflow. The tagline catches her eye: "Edit, save, see it live."

**Rising Action:** She reads the README. Within 30 seconds she knows: supported servers (Liberty, Tomcat — her server is there), supported build tools (Gradle, Maven), supported JSF implementations (Mojarra, MyFaces). The compatibility matrix answers her questions before she has to scroll. She checks the GitHub stars, scans the issues for red flags, reads the "How It Works" section. She sees the `ServerAdapter` architecture and thinks "this was built to last, not hacked together."

**Climax:** She clones the example project, runs it against her local Tomcat setup, and it works on the first try. No config beyond applying the plugin. She edits a template and watches the browser refresh.

**Resolution:** Aisha adds jsf-autoreload to the team's shared `pom.xml` with a dev profile. She writes a one-paragraph Confluence note for the team. Adoption is frictionless because it doesn't change any existing workflow — it only adds the live-reload on top.

**Capabilities revealed:** Clear documentation, compatibility matrix (servers x build tools x JSF implementations), example project, Maven integration, trust signals (stars, issue tracker), dev-profile-friendly configuration.

### Journey 3: Kenji — The Community Contributor

**Who:** Kenji is a senior Java developer at a consulting firm. His team uses JSF on WildFly. He discovers jsf-autoreload, loves the concept, but WildFly isn't supported yet.

**Opening Scene:** Kenji tries jsf-autoreload on his WildFly project. It doesn't work out of the box — the plugin ships with Liberty and Tomcat adapters only. He's disappointed but notices the `ServerAdapter` interface is public and documented.

**Rising Action:** Kenji reads `CONTRIBUTING.md` and finds a dedicated "How to add a new server" guide. It walks him through the `ServerAdapter` interface (three methods: `isRunning()`, `getHttpPort()`, `getContextRoot()`), explains how to wire the adapter into the build plugin DSL, documents where WildFly-like servers typically place exploded WAR output, and points to `LibertyServerAdapter` as a reference implementation.

**Climax:** Kenji writes `WildFlyServerAdapter` in a few hours, tests it against his local WildFly instance, and opens a PR. The maintainer reviews it, suggests a small change, and merges it within a week.

**Resolution:** WildFly support ships in the next release. Kenji's consulting firm adopts jsf-autoreload across all their JSF projects. He becomes a regular contributor.

**Capabilities revealed:** Public ServerAdapter API, `CONTRIBUTING.md` with "How to add a new server" guide, clear extension points, PR review process, community growth model.

### Journey 4: Marco — First-Time Setup Failure and Recovery

**Who:** Same Marco from Journey 1, but this time things don't go smoothly on the first try.

**Opening Scene:** Marco applies the plugin and runs `gradle jsfDev`. Instead of the browser refreshing, he gets an error: `[JSF Autoreload] Output directory not found: build/wlp/usr/servers/defaultServer/apps/expanded/my-app.war. Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your Liberty server name matches jsfAutoreload { serverName = '...' }`.

**Rising Action:** Marco realizes his Liberty server is named `myServer`, not `defaultServer`. He adds `jsfAutoreload { serverName = 'myServer' }` to his build file and runs again. This time the server starts, but port 35729 is already in use by another dev tool. The error is immediate and clear: `JSF Autoreload: port 35729 is already in use. Configure a different port via jsfAutoreload { port = XXXX }`. He sets `port = 35730`.

**Climax:** Third run — everything works. The error messages told him exactly what was wrong and exactly how to fix it. Total time from first failure to working setup: under 5 minutes.

**Resolution:** Marco trusts the plugin because when it failed, it failed helpfully. He doesn't need to file an issue or read source code to debug setup problems. The plugin respects his time even when things go wrong.

**Capabilities revealed:** Actionable error messages, fail-fast behavior, clear configuration guidance in errors, port conflict detection, server name configuration, developer trust through helpful failure.

### Journey Requirements Summary

| Capability | Marco (Daily Dev) | Marco (Failure) | Aisha (Evaluator) | Kenji (Contributor) |
|---|---|---|---|---|
| Zero-config plugin installation | Core | Fails, then configures | Core | Core |
| File watching + exploded WAR copy | Core | — | Validates | Validates |
| WebSocket browser refresh | Core | — | Validates | Validates |
| Gradle + Maven support | Uses | Uses | Evaluates | Uses |
| Multiple server support | Uses one | — | Key decision factor | Gap that motivates contribution |
| Public ServerAdapter API + docs | — | — | Trust signal | Core enabler |
| README with GIF + compatibility matrix | First impression | — | Key decision factor | Reference |
| CONTRIBUTING.md + "Add a server" guide | — | — | — | Core enabler |
| Actionable error messages | — | Core experience | — | — |
| Fail-fast with clear guidance | — | Core experience | — | — |
| Sub-2-second feedback loop | Core experience | Validates after fix | Validates | Validates |
| Contributor documentation | — | — | — | Core enabler |

## Developer Tool Requirements

### Language & Platform Matrix

- **Minimum Java version:** Java 11+
- **Target JSF versions:** JSF 2.0, 2.1, 2.2, 2.3 (`javax.faces` namespace)
- **JSF implementations:** Mojarra and MyFaces
- **Build tools:** Gradle 7+, Maven 3.6+
- **Application servers (v1):** IBM Open Liberty, Apache Tomcat
- **Application servers (extensible):** Any server via public `ServerAdapter` API
- **Out of scope (v1):** Jakarta Faces 3.0+ / `jakarta.servlet` namespace

### Module Architecture

Four-module multi-project build to avoid logic duplication across build tools:

1. **`jsf-autoreload-core`** — Shared logic: `FileChangeWatcher`, `DevWebSocketServer`, `ServerAdapter` interface, file copy logic. Java 11+, zero build-tool dependencies.
2. **`jsf-autoreload-plugin`** — Gradle plugin: wraps core in Gradle tasks. Depends on core.
3. **`jsf-autoreload-maven-plugin`** — Maven plugin: wraps core in Maven mojos. Depends on core.
4. **`jsf-autoreload-runtime`** — Servlet Filter + `web-fragment.xml`. Independent, dependency-free.

### Installation Methods

- **Gradle:** `plugins { id 'it.bstz.jsf-autoreload' }` — published on Gradle Plugin Portal
- **Maven:** Plugin coordinates on Maven Central (group: `it.bstz`, artifact: `jsf-autoreload-maven-plugin`)
- **Distribution automation:** GitHub Actions pipeline for publishing to both Gradle Plugin Portal and Maven Central on release

### API Surface

- **User-facing DSL (Gradle):** `jsfAutoreload { }` extension block — `port`, `serverName`, `outputDir`, `watchDirs`
- **User-facing config (Maven):** Equivalent `<configuration>` block in `pom.xml`
- **Public extension API:** `ServerAdapter` interface (3 methods: `isRunning()`, `getHttpPort()`, `getContextRoot()`) — lives in `jsf-autoreload-core`
- **Runtime:** Zero API — `DevModeFilter` auto-registers via `web-fragment.xml`, no user code required

### Documentation Strategy

- **README.md:** Quick start (3-line setup), GIF demo, compatibility matrix (servers x build tools x JSF implementations), configuration reference
- **In-repo docs:** `docs/` folder with detailed guides — architecture overview, configuration reference, troubleshooting
- **GitHub Pages:** Documentation site generated from repo docs
- **CONTRIBUTING.md:** Contributor guide including "How to add a new ServerAdapter" walkthrough
- **Javadoc:** Published for public API (`ServerAdapter` interface, extension DSL)

### Example Projects

- **`examples/` folder** in repository root with two working examples for v1:
  - `examples/liberty-gradle/` — Liberty + Gradle setup
  - `examples/tomcat-maven/` — Tomcat + Maven setup
- Each example is a minimal JSF 2.x WAR with a single `.xhtml` page, ready to clone and run
- Additional examples (Liberty + Maven, Tomcat + Gradle) added post-launch

### Implementation Considerations

- **IDE agnostic:** Works from command line; no IDE plugin required for v1
- **Build tool isolation:** Gradle and Maven plugins share core logic via `jsf-autoreload-core` module — build-tool-specific code limited to task/mojo wiring
- **Shadow/relocation:** WebSocket library relocated in Gradle plugin JAR to avoid classpath conflicts in user builds

## Product Scope & Release Strategy

### Strategy

**Approach:** Staged release — ship early to validate, expand to prove the platform.

**Resource Constraint:** Solo developer. All scope decisions account for one-person bandwidth. Phasing is critical — no parallel work streams.

### v0.1-beta — Validate the Core Assumption

Ship the existing Gradle + Liberty implementation as a public beta to validate that JSF developers want autoreload.

**Goal:** Real developers using the tool, collecting feedback, building early community.

**Feature set:**
- Gradle plugin only (two-module build: `plugin` + `runtime`)
- Liberty ServerAdapter only
- File watching for `.xhtml`, CSS, JS, static resources
- WebSocket browser refresh via auto-injected Servlet Filter
- Configurable port, server name, output dir, watch dirs
- Mojarra and MyFaces support via `REFRESH_PERIOD=0`
- Published on Gradle Plugin Portal

**Journeys supported:** Marco (daily dev), Marco (failure recovery)

**Success signal to proceed to v1:** Downloads, GitHub stars, issue reports indicating real usage.

### v1.0 — Prove the Platform

Extract shared core module, add Maven plugin and Tomcat adapter. Validates the riskiest assumption: the architecture supports multiple servers and build tools.

**Goal:** Server-agnostic, build-tool-agnostic — the platform story.

**Feature set (additive to v0.1):**
- Four-module build: `core`, `plugin`, `maven-plugin`, `runtime`
- Core module extraction: `FileChangeWatcher`, `DevWebSocketServer`, `ServerAdapter` → `jsf-autoreload-core`
- Maven plugin wrapping core in Maven mojos
- Tomcat ServerAdapter implementation
- Public, documented `ServerAdapter` API for community contributions
- Published on both Gradle Plugin Portal and Maven Central
- README with GIF demo, compatibility matrix
- `CONTRIBUTING.md` with "How to add a new ServerAdapter" guide
- Two example projects: `examples/liberty-gradle/`, `examples/tomcat-maven/`

**Journeys supported:** All four (Marco daily, Marco failure, Aisha evaluator, Kenji contributor)

### Phase 2 — Growth

- Community-contributed ServerAdapter implementations: WildFly, JBoss, GlassFish
- Attach mode (connect to already-running server)
- IDE integrations (IntelliJ, Eclipse, VS Code)
- Jakarta EE 9+ (`jakarta.servlet`) support
- GitHub Pages documentation site
- Additional example projects (Liberty + Maven, Tomcat + Gradle)

### Phase 3 — Vision

- Java class reloading / hot-swap via custom ClassLoader
- Production-mode static asset optimization pipeline
- Multi-module project support with cross-module change detection
- Community-contributed ServerAdapter ecosystem

### Risk Mitigation

**Technical Risks:**
- Core module extraction may reveal tight coupling to Gradle APIs → mitigate by extracting early in v1 development, before Maven plugin work begins
- Tomcat adapter may surface deployment model differences not covered by `ServerAdapter` interface → mitigate by studying Tomcat exploded WAR layout before committing to interface changes

**Market Risks:**
- "Nobody wants autoreload for JSF" → mitigated by v0.1-beta early release; if no traction, pivot or stop before v1 investment
- "JSF developers can't find the tool" → mitigated post-v1 via Stack Overflow answers, blog post, Gradle Plugin Portal listing

**Resource Risks:**
- Solo developer, limited bandwidth → mitigated by staged releases; v0.1-beta ships existing code with minimal additional work
- If v1 scope proves too large for one person → ship Maven or Tomcat support separately (v1.0 = one, v1.1 = the other)

## Functional Requirements

### Plugin Installation & Configuration

- **FR1:** Developer can apply the plugin to an existing Gradle build with a single plugin declaration
- **FR2:** Developer can apply the plugin to an existing Maven build with a single plugin declaration
- **FR3:** Developer can configure the WebSocket port via build tool DSL/configuration
- **FR4:** Developer can configure the application server name via build tool DSL/configuration
- **FR5:** Developer can configure the exploded WAR output directory via build tool DSL/configuration
- **FR6:** Developer can configure which directories to watch for file changes via build tool DSL/configuration
- **FR7:** Plugin can infer default output directory based on server name when not explicitly configured

### Build & Server Lifecycle

- **FR8:** Plugin can wire build task/phase dependencies so that preparation runs before server start, and the dev loop runs after server start
- **FR9:** Plugin can inject the runtime JAR into the exploded WAR's `WEB-INF/lib` before the application server starts
- **FR10:** Plugin can detect whether the application server is running
- **FR11:** Plugin can retrieve the HTTP port and context root from a running server

### Dev Loop Lifecycle

- **FR12:** Plugin can start and run a long-lived dev server (WebSocket server + file watcher) that blocks until terminated
- **FR13:** Plugin can shut down the file watcher, WebSocket server, and dev loop gracefully on termination (e.g., Ctrl+C)

### File Watching & Synchronization

- **FR14:** Plugin can detect file creation events in watched directories
- **FR15:** Plugin can detect file modification events in watched directories
- **FR16:** Plugin can detect file deletion events in watched directories
- **FR17:** Plugin can copy a changed file to the exploded WAR output directory preserving relative path structure
- **FR18:** Plugin can skip file copy on deletion events while still triggering a browser refresh
- **FR19:** Plugin can watch all files in configured watch directories (`.xhtml`, CSS, JS, images, fonts, and any other files present)

### Browser Refresh

- **FR20:** Plugin can run a WebSocket server that browser clients connect to
- **FR21:** Plugin can broadcast a reload message to all connected browsers when a file change is detected
- **FR22:** Runtime filter can buffer HTML responses and append a reload script before writing to the client
- **FR23:** Runtime filter can adjust response headers (Content-Length) to account for injected content
- **FR24:** Runtime filter can leave non-HTML responses (JSON, XML, etc.) unmodified
- **FR25:** Runtime filter can auto-register in Servlet 3.0+ containers without user configuration

### JSF Template Reload

- **FR26:** Plugin can configure the application server to force JSF template re-evaluation on every request (Mojarra `FACELETS_REFRESH_PERIOD=0`)
- **FR27:** Plugin can configure the application server to force JSF template re-evaluation on every request (MyFaces `REFRESH_PERIOD=0`)
- **FR28:** Plugin can detect existing configuration entries and skip writing duplicates on repeated runs

### Error Handling & Developer Feedback

- **FR29:** Plugin can fail fast with an actionable error message when the configured WebSocket port is already in use
- **FR30:** Plugin can fail fast with an actionable error message when the inferred output directory does not exist
- **FR31:** Plugin can warn when Liberty classloader delegation is set to `parentFirst`
- **FR32:** Plugin can display a startup message confirming the WebSocket server address and watched directories

### Server Extensibility

- **FR33:** Contributor can implement a new server adapter by implementing a public interface with three methods
- **FR34:** Contributor can reference existing adapter implementations as documentation for building new ones

### Distribution & Documentation

- **FR35:** Maintainer can publish the plugin to Gradle Plugin Portal via automated CI pipeline
- **FR36:** Maintainer can publish the plugin to Maven Central via automated CI pipeline
- **FR37:** Developer can view a compatibility matrix showing supported servers, build tools, and JSF implementations
- **FR38:** Developer can clone and run example projects for supported server/build-tool combinations
- **FR39:** Contributor can follow a documented guide to add a new ServerAdapter implementation

## Non-Functional Requirements

### Performance

- **NFR1:** End-to-end feedback loop (file save → browser refresh) completes in under 2 seconds for a single file change event (create, modify, or delete) on a file under 1MB
- **NFR2:** File system event detection and file copy completes in under 500ms as measured by unit test wall-clock timing from event callback to copy completion
- **NFR3:** WebSocket broadcast delivery to connected browsers completes in under 100ms as measured by unit test round-trip timing from broadcast call to client message receipt
- **NFR4:** Plugin startup (WebSocket server + file watcher initialization) completes in under 3 seconds
- **NFR5:** Runtime filter response buffering adds no more than 50ms latency to HTML responses
- **NFR6:** Plugin coalesces rapid successive file change events within a debounce window (default 300ms) into a single browser reload to prevent reload storms (e.g., IDE "save all", git checkout)

### Compatibility

- **NFR7:** Plugin runs on Java 11 and all subsequent LTS versions; CI matrix tests against Java 11, 17, and 21
- **NFR8:** Gradle plugin works with Gradle 7.x and 8.x
- **NFR9:** Maven plugin works with Maven 3.6+
- **NFR10:** Runtime filter works in any Servlet 3.0+ container
- **NFR11:** Runtime filter operates correctly with both Mojarra and MyFaces JSF implementations
- **NFR12:** Plugin does not conflict with the following common Gradle/Maven plugins in the user's build: `war`, `java`, `liberty-gradle-plugin`, `spring-boot-gradle-plugin`, `maven-war-plugin`, `maven-compiler-plugin`; verified by integration tests applying the plugin alongside each
- **NFR13:** Runtime JAR has zero transitive dependencies to avoid classpath conflicts in user WARs
- **NFR14:** Plugin operates correctly on macOS, Linux, and Windows

### Reliability

- **NFR15:** Dev loop runs stably for extended development sessions (8+ hours) without memory leaks — heap usage remains stable (no monotonic growth) under repeated file change events
- **NFR16:** Graceful shutdown releases all resources (file watchers, WebSocket connections, threads) within 2 seconds
- **NFR17:** File watcher recovers from transient file system errors (e.g., locked files) without crashing the dev loop
- **NFR18:** WebSocket server handles client disconnection/reconnection without intervention

### Documentation Quality

- **NFR19:** Developer can determine whether their stack is supported (server, build tool, JSF implementation) within 30 seconds of reading the README
- **NFR20:** Developer can go from zero to working live-reload by following the README in under 5 minutes
- **NFR21:** Example projects run successfully on a fresh clone with no modifications beyond prerequisite JDK and server installation; example READMEs list exact prerequisites
