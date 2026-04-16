# Tasks: JSF Hot Reload

**Input**: Design documents from `/specs/001-jsf-hot-reload/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: TDD is mandatory per project constitution. Test tasks are included and MUST be completed (red) before their corresponding implementation tasks (green).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Maven multi-module**: `{module}/src/main/java/it/bstz/jsfautoreload/` and `{module}/src/test/java/it/bstz/jsfautoreload/`
- Base package: `it.bstz.jsfautoreload`
- Modules: `jsf-autoreload-core`, `jsf-autoreload-tomcat`, `jsf-autoreload-maven-plugin`, `jsf-autoreload-integration-tests`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create Maven multi-module project structure with all module POMs, dependency management, and compiler settings

- [X] T001 (#2) Create parent POM (packaging: pom) with module declarations, Java 8 `--release 8` compiler settings, and dependencyManagement for JSF/Servlet/JUnit versions in pom.xml
- [X] T002 (#3) Create jsf-autoreload-core module POM with provided-scope dependencies on javax.faces-api 2.3, jakarta.faces-api 3.0, javax.servlet-api 3.1, jakarta.servlet-api 5.0, and test-scope JUnit 5 in jsf-autoreload-core/pom.xml
- [X] T003 (#4) [P] Create jsf-autoreload-tomcat module POM with compile-scope dependency on jsf-autoreload-core and provided-scope dependency on tomcat-catalina in jsf-autoreload-tomcat/pom.xml
- [X] T004 (#5) [P] Create jsf-autoreload-maven-plugin module POM (packaging: maven-plugin) with dependencies on maven-plugin-api and maven-project in jsf-autoreload-maven-plugin/pom.xml
- [X] T005 (#6) [P] Create jsf-autoreload-integration-tests module POM with test-scope dependencies on jsf-autoreload-core, jsf-autoreload-tomcat, embedded Tomcat, and JUnit 5 in jsf-autoreload-integration-tests/pom.xml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Models, bridge abstraction layer, SPI, logging, configuration, and initialization framework that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

### Tests for Foundational Phase

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T006 (#7) [P] Unit test: FileCategoryTest — verify file extension to category mapping for all extensions (.xhtml, .css, .js, .class, .java, unknown) in jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/model/FileCategoryTest.java
- [ ] T007 (#8) [P] Unit test: BridgeDetectorTest — verify jakarta-first detection order, javax fallback, and IllegalStateException when neither namespace is present in jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/bridge/BridgeDetectorTest.java
- [ ] T008 (#9) [P] Unit test: ConfigurationReaderTest — verify default values, context-param overrides, and system property overrides in jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/config/ConfigurationReaderTest.java

### Implementation for Foundational Phase

#### Models and Enums

- [ ] T009 (#10) [P] Create ChangeType enum (CREATED, MODIFIED, DELETED) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/model/ChangeType.java
- [ ] T010 (#11) [P] Create FileCategory enum (VIEW, STATIC, CLASS, SOURCE, OTHER) with static `fromExtension(String)` method mapping file extensions to categories in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/model/FileCategory.java
- [ ] T011 (#12) [P] Create DebounceGroup enum (VIEW_STATIC, CLASS) with static `fromCategory(FileCategory)` derivation in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/model/DebounceGroup.java
- [ ] T012 (#13) [P] Create FileChangeEvent model with fields filePath (Path), changeType (ChangeType), timestamp (Instant), fileCategory (FileCategory) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/model/FileChangeEvent.java
- [ ] T013 (#14) [P] Create ReloadNotification model with fields id (String/UUID), triggerFile (Path), eventType (ChangeType), fileCategory (FileCategory), timestamp (Instant), requiresContextReload (boolean) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/model/ReloadNotification.java
- [ ] T013a (#15) [P] Create ConnectionState enum (CONNECTED, DISCONNECTED) and BrowserConnection model with fields connectionId (String/UUID), asyncContext (AsyncContextWrapper), connectedSince (Instant), lastNotifiedAt (Instant), state (ConnectionState) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/model/BrowserConnection.java

#### Bridge Abstraction Layer

- [ ] T014 (#16) [P] Create JsfBridge interface (isDevelopmentMode, registerScriptInjector, projectStageParamName), ServletBridge interface (registerServlet, startAsync, registerShutdownListener), AsyncContextWrapper class, and BridgePair holder in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/
- [ ] T015 (#17) [P] Create JavaxJsfBridge and JavaxServletBridge implementations importing javax.faces.* and javax.servlet.* in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/javax/
- [ ] T016 (#18) [P] Create JakartaJsfBridge and JakartaServletBridge implementations importing jakarta.faces.* and jakarta.servlet.* in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/jakarta/
- [ ] T017 (#19) Create BridgeDetector with jakarta-first detection via Class.forName and javax fallback in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/BridgeDetector.java

#### SPI, Logging, and Config

- [ ] T018 (#20) [P] Create ContainerAdapter SPI interface (supports, reload, containerName, priority) and ReloadException checked exception in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/spi/
- [ ] T019 (#21) [P] Create ReloadLogger with structured JUL format `[JSF-AUTORELOAD] {level} | {event_type} | {file_path} | {details}` in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/logging/ReloadLogger.java
- [ ] T020 (#22) [P] Create PluginConfiguration (enabled, debounceIntervalMs, classDebounceIntervalMs, sseEndpointPath, autoCompileEnabled, autoCompileCommand, sourceDirectory) and WatchedDirectory (path, inclusionPatterns, exclusionPatterns, recursive, active) models with default values per data-model.md in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/
- [ ] T021 (#23) Create ConfigurationReader to read config from context-params (prefix: it.bstz.jsfautoreload.) and system properties (prefix: jsfautoreload.) with override precedence in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/ConfigurationReader.java

#### Initialization and JSF Integration

- [ ] T022 (#24) Create DevModeGuard to check PROJECT_STAGE via JsfBridge — returns true only when Development stage is active in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/jsf/DevModeGuard.java
- [ ] T023 (#25) Create AutoreloadInitializer shell — called by SCI bridges, checks dev mode, reads config, prepares to bootstrap components (wiring completed in US1) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/init/AutoreloadInitializer.java

#### META-INF Resources

- [ ] T024 (#26) [P] Create META-INF/faces-config.xml as an empty auto-discovered marker (no listener registration — ScriptInjector is registered programmatically via JsfBridge.registerScriptInjector in AutoreloadInitializer to avoid namespace-specific imports in core) in jsf-autoreload-core/src/main/resources/META-INF/faces-config.xml
- [ ] T025 (#27) [P] Create SCI service files for both javax.servlet.ServletContainerInitializer and jakarta.servlet.ServletContainerInitializer in jsf-autoreload-core/src/main/resources/META-INF/services/
- [ ] T026 (#28) [P] Create META-INF/web-fragment.xml with async-supported=true for the plugin's servlets in jsf-autoreload-core/src/main/resources/META-INF/web-fragment.xml

**Checkpoint**: Foundation ready — all models, bridges, SPI, config, and initialization framework in place. User story implementation can now begin.

---

## Phase 3: User Story 1 — XHTML/Facelets Live Reload (Priority: P1) :dart: MVP

**Goal**: Edit an `.xhtml` file, save, and the browser refreshes automatically within 3 seconds — no manual refresh needed

**Independent Test**: Edit any `.xhtml` file in a running JSF project and verify the browser refreshes within seconds displaying the updated content

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T027 (#29) [P] [US1] Unit test: DirectoryWatcherTest — verify recursive directory registration, file change detection, inclusion/exclusion pattern filtering, and new subdirectory registration in jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/core/DirectoryWatcherTest.java
- [ ] T028 (#30) [P] [US1] Unit test: DebouncerTest — verify time-window debounce, timer reset on new events, independent debounce groups, and callback invocation in jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/core/DebouncerTest.java
- [ ] T029 (#31) [P] [US1] Unit test: ConnectionManagerTest — verify connection add/remove, broadcast to all connections, dead connection cleanup, and concurrent access safety in jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/sse/ConnectionManagerTest.java

### Implementation for User Story 1

- [ ] T030 (#32) [US1] Implement DirectoryWatcher wrapping java.nio.file.WatchService with recursive directory registration, file pattern filtering, SensitivityWatchEventModifier.HIGH for macOS, FileChangeEvent emission, and graceful handling of watched directory deletion at runtime (log warning, stop watching that path, do not crash) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/core/DirectoryWatcher.java
- [ ] T031 (#33) [US1] Implement Debouncer using ScheduledExecutorService — maintain ScheduledFuture per DebounceGroup, cancel and reschedule on new events, emit ReloadNotification when timer fires in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/core/Debouncer.java
- [ ] T032 (#34) [P] [US1] Implement ConnectionManager with CopyOnWriteArraySet of BrowserConnection, add/remove/broadcast operations, and AsyncListener for timeout/error cleanup in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/ConnectionManager.java
- [ ] T033 (#35) [US1] Implement SseHandler for SSE endpoint at /_jsf-autoreload/events — start async context via ServletBridge, send initial `:ok` comment, schedule periodic `:heartbeat` comments every 30 seconds (per SSE contract), register connection with ConnectionManager in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/SseHandler.java
- [ ] T034 (#36) [US1] Implement ScriptInjector — defines the reload script content and delegates registration to JsfBridge.registerScriptInjector() (bridge creates the namespace-specific SystemEventListener for PostAddToViewEvent). ScriptInjector itself does NOT import javax/jakarta in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/jsf/ScriptInjector.java
- [ ] T035 (#37) [US1] Implement ReloadCoordinator to wire DirectoryWatcher → Debouncer → ConnectionManager pipeline — register as FileChangeEvent listener, feed events to Debouncer, broadcast ReloadNotification to SSE on debounce callback in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/core/ReloadCoordinator.java
- [ ] T036 (#38) [US1] Wire AutoreloadInitializer to bootstrap all US1 components — instantiate DirectoryWatcher, Debouncer, ConnectionManager, SseHandler, ScriptInjector, ReloadCoordinator; register SSE servlet via ServletBridge; start file watcher; register shutdown listener in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/init/AutoreloadInitializer.java
- [ ] T037 (#39) [US1] Create minimal test webapp (web.xml with Development stage, index.xhtml Facelets page) for integration tests in jsf-autoreload-integration-tests/src/test/resources/test-webapp/
- [ ] T038 (#40) [US1] Integration test: XhtmlReloadIT — start embedded Tomcat with test webapp, open SSE connection, modify .xhtml file, assert reload event received within 3 seconds in jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/XhtmlReloadIT.java

**Checkpoint**: XHTML live reload works end-to-end. The MVP is deliverable.

---

## Phase 4: User Story 2 — Static Resource Live Reload (Priority: P2)

**Goal**: Edit a CSS, JS, or image file and the browser refreshes automatically within 3 seconds

**Independent Test**: Modify a `.css` file in the project's resource directory and verify the browser refreshes to show the updated styling

### Tests for User Story 2

- [X] T039 (#41) [US2] Integration test: StaticResourceReloadIT — modify .css and .js files in test webapp's resource directory, assert reload events received within 3 seconds in jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/StaticResourceReloadIT.java

### Implementation for User Story 2

- [X] T040 (#42) [US2] Ensure default WatchedDirectory inclusion patterns cover all FR-002 view extensions (.xhtml, .jspx, .jsp) and all FR-003 static resource extensions (.css, .js, .png, .jpg, .gif, .svg, .ico, .woff, .woff2) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/PluginConfiguration.java
- [X] T041 (#43) [US2] Add static resource files (sample CSS, JS) to test webapp for integration tests in jsf-autoreload-integration-tests/src/test/resources/test-webapp/

**Checkpoint**: XHTML and static resource live reload both work independently.

---

## Phase 5: User Story 3 — Compiled Class Change Reload (Priority: P3)

**Goal**: Recompile a Java class, and the plugin detects the `.class` file change, triggers a Tomcat context reload, and refreshes the browser within 5 seconds

**Independent Test**: Modify a managed bean's return value, compile, and verify the browser refreshes showing the updated output

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T042 (#44) [P] [US3] Unit test: TomcatAdapterTest — verify supports() detects Tomcat classpath, reload() traverses to StandardContext and invokes reload, containerName() returns "Apache Tomcat" in jsf-autoreload-tomcat/src/test/java/it/bstz/jsfautoreload/tomcat/TomcatAdapterTest.java
- [ ] T043 (#45) [P] [US3] Integration test: ClassReloadIT — deploy test webapp, change a .class file, assert context reload occurs and SSE reload event is received within 5 seconds in jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/ClassReloadIT.java

### Implementation for User Story 3

- [ ] T044 (#46) [US3] Implement TomcatAdapter — supports() checks for org.apache.catalina.Context, reload() traverses ApplicationContextFacade → ApplicationContext → StandardContext and calls reload(), containerName() returns "Apache Tomcat" in jsf-autoreload-tomcat/src/main/java/it/bstz/jsfautoreload/tomcat/TomcatAdapter.java
- [ ] T045 (#47) [US3] Create ServiceLoader registration file listing TomcatAdapter in jsf-autoreload-tomcat/src/main/resources/META-INF/services/it.bstz.jsfautoreload.spi.ContainerAdapter
- [ ] T046 (#48) [US3] Add container adapter discovery via ServiceLoader and context reload logic to ReloadCoordinator — when CLASS debounce fires, invoke ContainerAdapter.reload() before SSE broadcast. If no adapter supports the running container, log WARNING and skip context reload (browser refresh still works for view/static changes) in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/core/ReloadCoordinator.java
- [ ] T047 (#49) [US3] Integration test: DebouncingIT — trigger 20 rapid .class file changes, assert at most 2 reload events received (SC-007) in jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/DebouncingIT.java

**Checkpoint**: Full development loop closed — XHTML, static, and class file reload all work. Context reload is operational via Tomcat adapter.

---

## Phase 6: User Story 4 — Auto-Compile Mode (Priority: P4)

**Goal**: Edit a `.java` source file, save, and the plugin automatically compiles it, reloads context, and refreshes the browser — a Quarkus-like experience

**Independent Test**: Enable auto-compile in plugin config, edit a `.java` source file, and verify the browser refreshes with updated behavior without any manual compile step

**Design Note — Two Auto-Compile Paths**: Auto-compile can be triggered two ways: (1) **In-WAR mode** — the core plugin watches `.java` files via DirectoryWatcher; when a SOURCE event fires, ReloadCoordinator invokes the configured `autoCompileCommand` (T051). Configured via `web.xml` context-params. (2) **Maven goal mode** — the developer runs `mvn jsf-autoreload:watch` which starts a standalone process that watches `.java` files and invokes the compiler (T049/T050). The compiled `.class` files are then detected by the in-WAR core plugin via the normal CLASS flow. Path 1 is self-contained; path 2 is useful when the servlet container doesn't allow spawning external processes.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T048 (#50) [US4] Unit test: AutoCompileMojoTest — verify .java file detection triggers compilation, compilation errors are logged without triggering reload, and disabled auto-compile ignores .java files in jsf-autoreload-maven-plugin/src/test/java/it/bstz/jsfautoreload/maven/AutoCompileMojoTest.java

### Implementation for User Story 4

- [ ] T049 (#51) [US4] Implement AutoCompileMojo — watch configured sourceDirectory for .java changes, invoke Maven compiler via ProcessBuilder, log compilation output, skip reload on compile failure in jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/maven/AutoCompileMojo.java
- [ ] T050 (#52) [US4] Implement WatchMojo — Maven goal that starts the file watcher and auto-compile loop as a long-running process in jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/maven/WatchMojo.java
- [ ] T051 (#53) [US4] Add SOURCE file category handling to ReloadCoordinator — when SOURCE event is detected and autoCompile is enabled, invoke compile command then delegate to CLASS flow in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/core/ReloadCoordinator.java

**Checkpoint**: Full Quarkus-like developer experience available when auto-compile is enabled.

---

## Phase 7: User Story 5 — Plugin Configuration (Priority: P5)

**Goal**: Customize watched directories, file patterns, debounce timing, and enable/disable behavior via `web.xml` context-params or system properties

**Independent Test**: Add a context-param that changes the watched directory, then verify the plugin watches the new location instead of the default

### Tests for User Story 5

- [ ] T052 (#54) [US5] Extend ConfigurationReaderTest to cover all configurable options — custom watchDirs, excludePatterns, debounceMs, classDebounceMs, sseEndpointPath, enabled toggle, and property override precedence in jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/config/ConfigurationReaderTest.java

### Implementation for User Story 5

- [ ] T053 (#55) [US5] Implement custom watched directory parsing from comma-separated it.bstz.jsfautoreload.watchDirs context-param in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/ConfigurationReader.java
- [ ] T054 (#56) [US5] Implement file exclusion pattern parsing from comma-separated it.bstz.jsfautoreload.excludePatterns context-param in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/ConfigurationReader.java
- [ ] T055 (#57) [US5] Implement debounce interval configuration from it.bstz.jsfautoreload.debounceMs and it.bstz.jsfautoreload.classDebounceMs context-params in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/ConfigurationReader.java
- [ ] T056 (#58) [US5] Implement enable/disable toggle from it.bstz.jsfautoreload.enabled context-param — when false, AutoreloadInitializer skips all registration in jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/ConfigurationReader.java

**Checkpoint**: Plugin is fully configurable for non-standard project layouts.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration tests and validation that span multiple user stories

- [ ] T057 (#59) [P] Integration test: MultiConnectionIT — open 10+ simultaneous SSE connections, trigger file change, assert all connections receive reload event (SC-006) in jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/MultiConnectionIT.java
- [ ] T058 (#60) [P] Integration test: ProductionModeIT — start app with PROJECT_STAGE=Production, verify no SSE servlet registered, no watcher started, no script injected (FR-008, SC-005) in jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/ProductionModeIT.java
- [ ] T059 (#61) Run quickstart.md validation — build project, deploy test webapp, execute all quickstart steps, verify documented behavior matches implementation
- [ ] T060 (#62) Final code review — verify no core module imports javax.*/jakarta.* directly, all logging uses ReloadLogger, no unused dependencies, clean `mvn verify` passes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3–7)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4 → P5)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories. This IS the MVP.
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) — Shares infrastructure with US1 but is independently testable. Very lightweight if US1 is complete.
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) — Adds TomcatAdapter and context reload logic. Extends ReloadCoordinator from US1 but the SPI contract is defined in Phase 2.
- **User Story 4 (P4)**: Depends on US3 (context reload flow) — Auto-compile delegates to CLASS flow after compilation.
- **User Story 5 (P5)**: Can start after Foundational (Phase 2) — Extends ConfigurationReader already created in Phase 2.

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD per constitution)
- Models/enums before services
- Services before coordinators/handlers
- Core implementation before integration wiring
- Integration test after implementation complete
- Story complete before moving to next priority

### Parallel Opportunities

- T003, T004, T005 can run in parallel (independent module POMs)
- T006, T007, T008 can run in parallel (independent test files)
- T009–T013 can run in parallel (independent model files)
- T014–T016 can run in parallel (independent bridge packages)
- T018–T020 can run in parallel (independent foundational components)
- T024–T026 can run in parallel (independent META-INF files)
- T027–T029 can run in parallel (independent US1 test files)
- T032 can run in parallel with T030/T031 (ConnectionManager has no dependency on DirectoryWatcher/Debouncer)
- T042, T043 can run in parallel (independent US3 test files)
- T057, T058 can run in parallel (independent integration test files)
- Once Phase 2 is complete: US1, US2, US3, US5 can all start in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together (TDD red phase):
Task T027: "DirectoryWatcherTest in .../core/DirectoryWatcherTest.java"
Task T028: "DebouncerTest in .../core/DebouncerTest.java"
Task T029: "ConnectionManagerTest in .../sse/ConnectionManagerTest.java"

# Then launch independent US1 implementations:
Task T030: "DirectoryWatcher in .../core/DirectoryWatcher.java"  # sequential (T031 depends on watcher output)
Task T032: "ConnectionManager in .../sse/ConnectionManager.java" # parallel (independent file)

# Then sequential pipeline wiring:
Task T031: "Debouncer" → Task T033: "SseHandler" → Task T034: "ScriptInjector" → Task T035: "ReloadCoordinator" → Task T036: "Wire AutoreloadInitializer"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (5 tasks)
2. Complete Phase 2: Foundational (21 tasks) — CRITICAL, blocks all stories
3. Complete Phase 3: User Story 1 (12 tasks)
4. **STOP and VALIDATE**: Test XHTML live reload end-to-end
5. Deploy/demo if ready — this is the core value proposition

### Incremental Delivery

1. Setup + Foundational → Foundation ready (26 tasks)
2. Add User Story 1 → Test independently → **MVP!** (38 tasks cumulative)
3. Add User Story 2 → Test independently → Front-end companion complete (41 tasks)
4. Add User Story 3 → Test independently → Full development loop closed (47 tasks)
5. Add User Story 4 → Test independently → Quarkus-like experience (51 tasks)
6. Add User Story 5 → Test independently → Fully configurable (56 tasks)
7. Polish → Production-quality release (60 tasks)

### Parallel Team Strategy

With multiple developers after Phase 2 is complete:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (core pipeline — the critical path)
   - Developer B: User Story 3 (Tomcat adapter — independent module)
   - Developer C: User Story 5 (configuration — extends existing code)
3. After US1: Developer A picks up US2 (thin, builds on US1)
4. After US3: Developer B picks up US4 (builds on context reload flow)
5. Stories integrate cleanly — shared interfaces defined in Phase 2

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- TDD: Write tests (red) → implement (green) → refactor
- Core module MUST NOT import javax.* or jakarta.* directly — only through bridge interfaces
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Total: 60 tasks across 8 phases
