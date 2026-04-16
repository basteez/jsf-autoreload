# Tasks: Fix Reload Bugs

**Input**: Design documents from `/specs/002-fix-reload-bugs/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Required by Constitution II (Test-Driven Development — NON-NEGOTIABLE). All bug fixes require tests first.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Verify existing codebase builds and tests pass before introducing changes

- [X] T001 Verify baseline build and test suite pass with `mvn clean verify` from repository root ([#64](https://github.com/basteez/jsf-autoreload/issues/64))

---

## Phase 2: Foundational (Bridge Interface Extensions)

**Purpose**: Extend the bridge interfaces and implementations with new methods required by multiple user stories. These changes enable deferred ScriptInjector registration (US1) and AsyncListener-based cleanup (US4).

**CRITICAL**: No user story work can begin until this phase is complete

### AsyncListener support (needed by US4)

- [X] T002 Add `addAsyncListener(AsyncContextWrapper, Runnable onComplete, Runnable onError, Runnable onTimeout)` method to `ServletBridge` interface in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/ServletBridge.java` ([#65](https://github.com/basteez/jsf-autoreload/issues/65))
- [X] T003 [P] Implement `addAsyncListener()` in `JavaxServletBridge` — create anonymous `javax.servlet.AsyncListener`, delegate `onComplete`/`onError`/`onTimeout` to the provided `Runnable` callbacks in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/javax/JavaxServletBridge.java` ([#66](https://github.com/basteez/jsf-autoreload/issues/66))
- [X] T004 [P] Implement `addAsyncListener()` in `JakartaServletBridge` — create anonymous `jakarta.servlet.AsyncListener`, delegate callbacks to provided `Runnable`s in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/jakarta/JakartaServletBridge.java` ([#67](https://github.com/basteez/jsf-autoreload/issues/67))

### Deferred ScriptInjector registration (needed by US1)

- [X] T005 Add `registerDeferredScriptInjector(Object servletContext, String sseEndpointPath)` method to `JsfBridge` interface in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/JsfBridge.java` ([#68](https://github.com/basteez/jsf-autoreload/issues/68))
- [X] T006 [P] Implement `registerDeferredScriptInjector()` in `JavaxJsfBridge` — programmatically add a `javax.servlet.ServletContextListener` via `servletContext.addListener()` that retrieves the JSF `Application` via `FactoryFinder` in `contextInitialized()` and calls `ScriptInjector.register(application)` (faces-config.xml `PostConstructApplicationEvent` approach was abandoned — Mojarra 2.3.9 fatally crashes on unresolvable jakarta listener classes) in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/javax/JavaxJsfBridge.java` ([#69](https://github.com/basteez/jsf-autoreload/issues/69))
- [X] T007 [P] Implement `registerDeferredScriptInjector()` in `JakartaJsfBridge` — equivalent programmatic `jakarta.servlet.ServletContextListener` implementation for jakarta namespace in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/bridge/jakarta/JakartaJsfBridge.java` ([#70](https://github.com/basteez/jsf-autoreload/issues/70))

### DefaultSseHandler constructor update (needed by US4)

- [X] T008 Pass `ServletBridge` reference to `DefaultSseHandler` constructor — add `servletBridge` field, update constructor signature, and update instantiation site in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/DefaultSseHandler.java` and `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/init/AutoreloadInitializer.java` ([#71](https://github.com/basteez/jsf-autoreload/issues/71))

**Checkpoint**: Foundation ready — bridge interfaces extended, user story implementation can begin

---

## Phase 3: User Story 1 — Browser auto-refreshes on view/static file change (Priority: P1) MVP

**Goal**: Fix Bug 1 (CRITICAL) — Wire `ScriptInjector` into the initialization sequence so the client-side EventSource script is injected into JSF pages, enabling browsers to establish SSE connections and auto-refresh on file changes.

**Independent Test**: Start a JSF application with the plugin in development mode, open a page in the browser, edit an XHTML file, and observe that the browser reloads automatically within a few seconds.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T009 [P] [US1] Write unit test verifying `AutoreloadInitializer.bootstrap()` calls `JsfBridge.registerDeferredScriptInjector()` with correct SSE endpoint path in new file `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/init/AutoreloadInitializerTest.java` ([#72](https://github.com/basteez/jsf-autoreload/issues/72))
- [X] T010 [P] [US1] Write unit test verifying `ScriptInjector.getScriptContent()` returns script with context-aware EventSource URL (`contextPath + endpointPath`) in new file `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/jsf/ScriptInjectorTest.java` ([#73](https://github.com/basteez/jsf-autoreload/issues/73))

### Implementation for User Story 1

- [X] T011 [US1] Update `ScriptInjector` to accept context path parameter and format EventSource URL as `contextPath + endpointPath` (handles root context path as empty string) in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/jsf/ScriptInjector.java` ([#74](https://github.com/basteez/jsf-autoreload/issues/74))
- [X] T012 [US1] Wire ScriptInjector deferred registration in `AutoreloadInitializer.bootstrap()` — call `bridges.jsf().registerDeferredScriptInjector(servletContext, config.getSseEndpointPath())` after SSE handler servlet registration in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/init/AutoreloadInitializer.java` ([#75](https://github.com/basteez/jsf-autoreload/issues/75))
- [X] T013 [US1] Update `XhtmlReloadIT` integration test to verify SSE connection is established (script injected) and page auto-refreshes on XHTML file change in `jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/XhtmlReloadIT.java` ([#76](https://github.com/basteez/jsf-autoreload/issues/76))

**Checkpoint**: At this point, browsers should auto-refresh when XHTML or static files change. User Story 1 should be fully functional and testable independently.

---

## Phase 4: User Story 2 — Browser auto-refreshes after Java class recompilation (Priority: P1)

**Goal**: Validate the end-to-end Java class change flow — `.class` file change triggers context reload AND SSE broadcast reaches connected browsers. Bug 1 fix (US1) enables the SSE connection; this story validates the full class-reload path works.

**Independent Test**: Start a JSF application with the plugin, open a page, recompile a managed bean (`.class` file changes in `target/classes`), and observe that the application context reloads and the browser refreshes.

### Tests for User Story 2

> **NOTE: Write/update these tests FIRST, ensure they FAIL before implementation**

- [X] T014 [US2] Update `ClassReloadIT` to verify end-to-end flow — `.class` file change triggers context reload AND SSE broadcast reaches at least 1 connected client (logs confirm both events) in `jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/ClassReloadIT.java` ([#77](https://github.com/basteez/jsf-autoreload/issues/77))

### Implementation for User Story 2

- [X] T015 [US2] Fix `ReloadCoordinator` ordering — broadcast SSE event BEFORE context reload (context reload destroys connections) and add client-side `onerror` handler for connection-loss-triggered reload in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/core/ReloadCoordinator.java` and `ScriptInjector.java` ([#78](https://github.com/basteez/jsf-autoreload/issues/78))

**Checkpoint**: At this point, both XHTML/static and Java class changes should trigger browser auto-refresh. User Stories 1 AND 2 should both work independently.

---

## Phase 5: User Story 3 — SSE connection stays alive during idle periods (Priority: P2)

**Goal**: Fix Bug 2 (HIGH) — Implement a working heartbeat that sends SSE comments (`:heartbeat\n\n`) to all connections every 30 seconds, preventing proxy/browser idle-timeout drops. Failed heartbeat writes trigger dead connection removal.

**Independent Test**: Open a page, wait longer than the heartbeat interval without making changes, then edit a file and confirm the browser still auto-refreshes.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T016 [P] [US3] Write unit test for `ConnectionManager.sendHeartbeatToAll()` — verify `:heartbeat\n\n` comment sent to all connections and dead connections removed on `IOException` in `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/sse/ConnectionManagerTest.java` ([#79](https://github.com/basteez/jsf-autoreload/issues/79))
- [X] T017 [P] [US3] Write unit test for `DefaultSseHandler.startHeartbeat()` — verify heartbeat task calls `connectionManager.sendHeartbeatToAll()` at configured interval in new file `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/sse/DefaultSseHandlerTest.java` ([#80](https://github.com/basteez/jsf-autoreload/issues/80))

### Implementation for User Story 3

- [X] T018 [US3] Add `sendHeartbeatToAll()` method to `ConnectionManager` — iterate `CopyOnWriteArraySet` snapshot, call `sendComment(connection, "heartbeat")` for each, catch `IOException` and call `remove(connection)` for dead connections in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/ConnectionManager.java` ([#81](https://github.com/basteez/jsf-autoreload/issues/81))
- [X] T019 [US3] Fix `startHeartbeat()` in `DefaultSseHandler` — replace no-op lambda body with call to `connectionManager.sendHeartbeatToAll()` in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/DefaultSseHandler.java` ([#82](https://github.com/basteez/jsf-autoreload/issues/82))
- [X] T020 [US3] Update `MultiConnectionIT` to verify SSE connections survive idle period longer than heartbeat interval (wait >35s, then trigger reload and assert all connections receive event) in `jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/MultiConnectionIT.java` ([#83](https://github.com/basteez/jsf-autoreload/issues/83))

**Checkpoint**: At this point, SSE connections should remain alive during extended idle periods. User Story 3 should be independently verifiable.

---

## Phase 6: User Story 4 — Disconnected browsers are cleaned up (Priority: P3)

**Goal**: Fix Bug 3 (MEDIUM) — Register an `AsyncListener` on each connection's `AsyncContext` to detect `onComplete`/`onTimeout`/`onError` events from the servlet container, enabling immediate stale-connection removal without waiting for a broadcast or heartbeat write failure.

**Independent Test**: Open a page, verify the connection count is 1, close the tab, then trigger a file change and verify the broadcast log shows the connection was removed.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T021 [P] [US4] Write unit test verifying `AsyncListener` is registered on `AsyncContext` when `DefaultSseHandler.handleRequest()` is called — mock `servletBridge.addAsyncListener()` and verify invocation in `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/sse/DefaultSseHandlerTest.java` ([#84](https://github.com/basteez/jsf-autoreload/issues/84))
- [X] T022 [P] [US4] Write unit test verifying `connectionManager.remove()` is called when any `AsyncListener` callback (`onComplete`/`onError`/`onTimeout`) fires in `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/sse/ConnectionManagerTest.java` ([#85](https://github.com/basteez/jsf-autoreload/issues/85))

### Implementation for User Story 4

- [X] T023 [US4] Register `AsyncListener` in `DefaultSseHandler.handleRequest()` — after adding `BrowserConnection` to `ConnectionManager`, call `servletBridge.addAsyncListener(asyncContext, ...)` with callbacks that invoke `connectionManager.remove(connection)` on disconnect events in `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/DefaultSseHandler.java` ([#86](https://github.com/basteez/jsf-autoreload/issues/86))

**Checkpoint**: All four user stories should now be independently functional. Dead connections are cleaned up proactively via AsyncListener callbacks.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validate all fixes work together, ensure observability, and run end-to-end scenarios

- [X] T024 [P] Run full test suite (`mvn clean verify`) and fix any regressions across all modules ([#87](https://github.com/basteez/jsf-autoreload/issues/87))
- [X] T025 [P] Verify all new code paths include appropriate `ReloadLogger` messages per Constitution V (Observability and Diagnostics) — review ScriptInjector wiring logs, heartbeat logs, and AsyncListener cleanup logs ([#88](https://github.com/basteez/jsf-autoreload/issues/88))
- [X] T026 Run quickstart.md validation scenarios against the test project at `/Users/tizianobasile/workspace/me/jsf-autoreload-test-prj` — verify manual testing steps for all three bugs ([#89](https://github.com/basteez/jsf-autoreload/issues/89))

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (T005-T007 bridge methods)
- **User Story 2 (Phase 4)**: Depends on User Story 1 (SSE connections must work)
- **User Story 3 (Phase 5)**: Depends on Foundational only — can run in parallel with US1/US2
- **User Story 4 (Phase 6)**: Depends on Foundational (T002-T004, T008 bridge methods) — can run in parallel with US1/US2/US3
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Requires Foundational — no dependencies on other stories
- **User Story 2 (P1)**: Requires User Story 1 (SSE connections must be established for end-to-end validation)
- **User Story 3 (P2)**: Requires Foundational only — independent of US1/US2
- **User Story 4 (P3)**: Requires Foundational only — independent of US1/US2/US3

### Within Each User Story

- Tests MUST be written and FAIL before implementation (Constitution II)
- Unit tests before integration tests
- Core behavior before edge cases
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 2**: T003 || T004 (javax/jakarta AsyncListener); T006 || T007 (javax/jakarta deferred registration)
- **Phase 3**: T009 || T010 (unit tests for different classes)
- **Phase 5**: T016 || T017 (unit tests for different classes)
- **Phase 6**: T021 || T022 (unit tests for different classes)
- **Phase 7**: T024 || T025 (test suite and observability review)
- **Cross-phase**: US3 and US4 can run in parallel with US1/US2 after Foundational completes

---

## Parallel Example: User Story 3

```bash
# Launch both unit tests in parallel (different files):
Task: "T016 — unit test for ConnectionManager.sendHeartbeatToAll()"
Task: "T017 — unit test for DefaultSseHandler.startHeartbeat()"

# Then implement sequentially (T018 before T019, since handler calls manager):
Task: "T018 — Add sendHeartbeatToAll() to ConnectionManager"
Task: "T019 — Fix startHeartbeat() in DefaultSseHandler"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup (verify baseline)
2. Complete Phase 2: Foundational (bridge extensions)
3. Complete Phase 3: User Story 1 (script injection fix)
4. Complete Phase 4: User Story 2 (class reload validation)
5. **STOP and VALIDATE**: Browser auto-refresh works for both XHTML and Java class changes
6. Deploy/demo if ready — core plugin functionality restored

### Incremental Delivery

1. Setup + Foundational -> Bridge extensions ready
2. Add US1 -> Script injection works, SSE connections established -> Test independently (MVP!)
3. Add US2 -> Java class reload end-to-end validated -> Test independently
4. Add US3 -> Heartbeat keeps connections alive during idle -> Test independently
5. Add US4 -> Stale connections cleaned up proactively -> Test independently
6. Polish -> Full validation and observability audit

### Parallel Execution Strategy

With concurrent implementation capacity:

1. Complete Setup + Foundational together
2. Once Foundational is done:
   - Stream A: User Story 1 -> User Story 2 (sequential, US2 depends on US1)
   - Stream B: User Story 3 (independent)
   - Stream C: User Story 4 (independent)
3. All streams converge at Polish phase

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Constitution II (TDD) is NON-NEGOTIABLE — write failing tests before implementation
- All bug fixes are within existing files in `jsf-autoreload-core` — no new modules
- AsyncContext timeout is already set to 0 in both bridge implementations (verified in existing code)
- `CopyOnWriteArraySet` provides thread-safe iteration for heartbeat and broadcast
- The javax/jakarta bridge pattern must be maintained for all new methods
