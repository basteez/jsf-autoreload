# Tasks: README & License for Maven Publishing

**Input**: Design documents from `/specs/003-readme-and-license/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Not requested — this is a documentation-only feature with no runtime code. Verification is manual (review content accuracy against source code).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No project initialization needed — this feature adds files to an existing repository. Phase 1 is intentionally empty.

**Checkpoint**: Nothing to set up — proceed to Foundational phase.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the LICENSE file and update POM metadata. These are shared deliverables that multiple user stories depend on (US3 needs LICENSE and POM; US1/US2/US4 reference them in README content).

**CRITICAL**: The LICENSE file and POM metadata must exist before the README can accurately reference them.

- [X] T001 [P] Create Apache License 2.0 file at /LICENSE with copyright "2026 Tiziano Basile" and full standard license text ([#91](https://github.com/basteez/jsf-autoreload/issues/91))
- [X] T002 [P] Add `<url>` element to /pom.xml after `<description>` pointing to `https://github.com/basteez/jsf-autoreload` ([#92](https://github.com/basteez/jsf-autoreload/issues/92))
- [X] T003 [P] Add `<licenses>` section to /pom.xml with name "Apache License, Version 2.0", URL `https://www.apache.org/licenses/LICENSE-2.0`, and `<distribution>repo</distribution>` ([#93](https://github.com/basteez/jsf-autoreload/issues/93))
- [X] T004 [P] Add `<developers>` section to /pom.xml with id `basteez`, name `Tiziano Basile`, email `tiziano.basile@nearform.com` ([#94](https://github.com/basteez/jsf-autoreload/issues/94))
- [X] T005 [P] Add `<scm>` section to /pom.xml with connection (`scm:git:git://github.com/basteez/jsf-autoreload.git`), developerConnection (`scm:git:ssh://github.com:basteez/jsf-autoreload.git`), and url (`https://github.com/basteez/jsf-autoreload`) ([#95](https://github.com/basteez/jsf-autoreload/issues/95))

**Checkpoint**: LICENSE file exists at repo root; pom.xml contains all Maven Central required metadata (`<url>`, `<licenses>`, `<developers>`, `<scm>`). Foundation ready — README content can now reference these accurately.

---

## Phase 3: User Story 1 — Developer Discovers and Understands the Plugin (Priority: P1) MVP

**Goal**: Create the README.md with introductory content — title, features list, compatibility matrix, and module structure — so a developer can understand the plugin's purpose and capabilities.

**Independent Test**: A developer unfamiliar with the project reads the README and can describe the plugin's purpose, supported environments, and module structure without looking at source code.

### Implementation for User Story 1

- [X] T006 [US1] Create /README.md with title "jsf-autoreload", one-line description ("JSF hot-reload plugin — monitors file changes and automatically refreshes the browser via SSE"), and features bullet list covering: XHTML page reload, CSS/JS static resource reload, class file reload with configurable debounce, auto-compile on Java source changes, SSE-based browser refresh (no polling) ([#96](https://github.com/basteez/jsf-autoreload/issues/96))
- [X] T007 [US1] Add compatibility table to /README.md listing: Java 8+, JSF 2.3+ (javax.faces), Jakarta Faces 3.0+ (jakarta.faces), Servlet API 3.0+, Tomcat (officially supported), and a note that other Servlet 3.0+ containers (e.g., Jetty, WildFly) may work but are untested ([#97](https://github.com/basteez/jsf-autoreload/issues/97))
- [X] T008 [US1] Add module structure table to /README.md covering all 4 modules: `jsf-autoreload-core` (core library — SSE handler, file watcher, config, JSF bridge), `jsf-autoreload-tomcat` (Tomcat container adapter), `jsf-autoreload-maven-plugin` (Maven plugin — watch and auto-compile goals), `jsf-autoreload-integration-tests` (end-to-end tests with embedded Tomcat) ([#98](https://github.com/basteez/jsf-autoreload/issues/98))

**Checkpoint**: README.md exists with intro, features, compatibility table, and module structure. User Story 1 acceptance scenarios are satisfied — a developer can understand the plugin from the README alone.

---

## Phase 4: User Story 2 — Developer Adds the Plugin to Their Project (Priority: P2)

**Goal**: Add quick-start setup instructions, javax vs jakarta guidance, and a complete configuration reference to the README so a developer can integrate the plugin into their project.

**Independent Test**: A developer follows the README instructions to add the plugin to a fresh JSF project, and the code snippets are syntactically valid and copy-paste-ready.

### Implementation for User Story 2

- [X] T009 [US2] Add quick-start section to /README.md with copy-paste-ready Maven dependency XML for `jsf-autoreload-core` (groupId `it.bstz`, version `0.1.0-SNAPSHOT`) + `jsf-autoreload-tomcat`, plugin configuration XML for `jsf-autoreload-maven-plugin` with `watch` goal, and run command (`mvn jsf-autoreload:watch`); all XML must be syntactically valid ([#99](https://github.com/basteez/jsf-autoreload/issues/99))
- [X] T010 [US2] Add javax vs jakarta section to /README.md explaining: javax.faces 2.3+ projects use `javax.faces-api` and `javax.servlet-api` dependencies; jakarta.faces 3.0+ projects use `jakarta.faces-api` and `jakarta.servlet-api`; the core library auto-detects the namespace at runtime via BridgeDetector (FR-011) ([#100](https://github.com/basteez/jsf-autoreload/issues/100))
- [X] T011 [US2] Add configuration reference section to /README.md with two sub-tables: (1) web.xml context-params (prefix `it.bstz.jsfautoreload.`, system property prefix `jsfautoreload.`) — `enabled` (boolean, default `true`), `debounceMs` (long, default `500`), `classDebounceMs` (long, default `1000`), `sseEndpointPath` (String, default `/_jsf-autoreload/events`), `autoCompileEnabled` (boolean, default `false`), `autoCompileCommand` (String, no default), `sourceDirectory` (String, default `src/main/java`), `watchDirs` (String, comma-separated, no default), `excludePatterns` (String, comma-separated globs, no default); (2) Maven plugin params — `sourceDirectory` (property `jsf-autoreload.sourceDirectory`, default `src/main/java`), `compileCommand` (property `jsf-autoreload.compileCommand`, default `mvn compile`), `autoCompile` (property `jsf-autoreload.autoCompile`, default `true`). All defaults must exactly match ConfigurationReader.java, PluginConfiguration.java, WatchMojo.java, and AutoCompileMojo.java ([#101](https://github.com/basteez/jsf-autoreload/issues/101))

**Checkpoint**: README.md contains quick-start, javax/jakarta guidance, and configuration reference. User Story 2 acceptance scenarios are satisfied — a developer can set up the plugin by following the README within 10 minutes (SC-001).

---

## Phase 5: User Story 3 — Developer Understands the License Terms (Priority: P3)

**Goal**: Add license section to the README that names the license and links to the LICENSE file, completing the licensing story alongside the LICENSE file and POM metadata from Phase 2.

**Independent Test**: The LICENSE file exists at repo root, the README states the license name and links to it, and the POM `<licenses>` element matches.

### Implementation for User Story 3

- [X] T012 [US3] Add license section to /README.md stating "Apache License 2.0" and linking to the LICENSE file at repository root (FR-008) ([#102](https://github.com/basteez/jsf-autoreload/issues/102))
- [X] T013 [US3] Verify consistency: LICENSE file copyright matches spec ("2026 Tiziano Basile"), POM `<licenses>` name matches LICENSE file ("Apache License, Version 2.0"), and README license section matches both ([#103](https://github.com/basteez/jsf-autoreload/issues/103))

**Checkpoint**: License is documented in all three locations (LICENSE file, pom.xml, README.md) with consistent naming. User Story 3 acceptance scenarios are satisfied.

---

## Phase 6: User Story 4 — Contributor Wants to Help (Priority: P4)

**Goal**: Add build-from-source and test-running instructions to the README so a contributor can build and test the project in 3 steps or fewer.

**Independent Test**: A developer clones the repo, follows the build instructions, and the project compiles and all tests pass.

### Implementation for User Story 4

- [X] T014 [US4] Add "Building from Source" section to /README.md with 3-step instructions: (1) `git clone https://github.com/basteez/jsf-autoreload.git`, (2) `mvn clean install`, (3) `mvn verify` to run unit and integration tests — per SC-004 constraint of 3 steps or fewer ([#104](https://github.com/basteez/jsf-autoreload/issues/104))
- [X] T015 [US4] Add "Development Workflow" subsection to /README.md explaining the project uses [Speckit](https://github.com/basteez/speckit) for feature planning with the full workflow (spec -> plan -> tasks -> implement); clarify that the full workflow is expected for new features but optional for small contributions like bug fixes and doc improvements (FR-012) ([#105](https://github.com/basteez/jsf-autoreload/issues/105))

**Checkpoint**: README.md contains contributor build instructions and development workflow guidance. User Story 4 acceptance scenarios are satisfied.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all deliverables to ensure consistency and completeness.

- [X] T016 [P] Review all README code snippets in /README.md for syntactic validity — all XML must be well-formed and copy-paste-ready (SC-002) ([#106](https://github.com/basteez/jsf-autoreload/issues/106))
- [X] T017 [P] Validate README configuration defaults against source files: jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/ConfigurationReader.java and jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/PluginConfiguration.java and jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/maven/WatchMojo.java and jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/maven/AutoCompileMojo.java ([#107](https://github.com/basteez/jsf-autoreload/issues/107))
- [X] T018 Run quickstart.md verification checklist against all deliverables (README.md, LICENSE, pom.xml) ([#108](https://github.com/basteez/jsf-autoreload/issues/108))

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Empty — no setup needed
- **Foundational (Phase 2)**: No dependencies — can start immediately. Tasks T001-T005 are all parallel (different files or independent POM sections)
- **User Story 1 (Phase 3)**: Depends on Phase 2 (README references POM metadata for accuracy)
- **User Story 2 (Phase 4)**: Depends on Phase 3 (builds on existing README structure)
- **User Story 3 (Phase 5)**: Depends on Phase 2 (LICENSE and POM must exist) + Phase 3 (README must exist to add license section)
- **User Story 4 (Phase 6)**: Depends on Phase 3 (README must exist to add build section)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2)
- **User Story 2 (P2)**: Depends on US1 (adds sections to the README created in US1)
- **User Story 3 (P3)**: Can start after US1 (adds license section to README); Foundational provides LICENSE + POM
- **User Story 4 (P4)**: Can start after US1 (adds build section to README)

### Within Each User Story

- Tasks within a story are sequential (each adds to the same README.md file)
- Exception: Foundational tasks (T001-T005) are fully parallel — T001 creates LICENSE, T002-T005 update different POM sections

### Parallel Opportunities

- T001-T005 (Foundational): All parallel — LICENSE file + 4 independent POM sections
- T007 and T008 (US1): Could be parallel if README skeleton is created first in T006
- T016 and T017 (Polish): Parallel — different validation concerns
- US3 (Phase 5) and US4 (Phase 6) can run in parallel after US1 completes

---

## Parallel Example: Foundational Phase

```text
# Launch all foundational tasks together (different files / independent POM sections):
Task T001: "Create Apache License 2.0 file at /LICENSE"
Task T002: "Add <url> element to /pom.xml"
Task T003: "Add <licenses> section to /pom.xml"
Task T004: "Add <developers> section to /pom.xml"
Task T005: "Add <scm> section to /pom.xml"
```

## Parallel Example: After User Story 1

```text
# US3 and US4 can run in parallel (different README sections, no cross-dependency):
Task T012: "Add license section to /README.md" (US3)
Task T014: "Add Building from Source section to /README.md" (US4)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (LICENSE + POM metadata)
2. Complete Phase 3: User Story 1 (README intro, features, compatibility, modules)
3. **STOP and VALIDATE**: README exists and a developer can understand the plugin
4. This alone satisfies the most critical need — project discoverability

### Incremental Delivery

1. Foundational (Phase 2) -> LICENSE + POM metadata ready
2. Add User Story 1 -> README with intro content -> Validate independently (MVP!)
3. Add User Story 2 -> Quick-start + config reference -> Validate setup flow
4. Add User Story 3 -> License section in README -> Validate consistency across LICENSE/POM/README
5. Add User Story 4 -> Build instructions -> Validate contributor workflow
6. Polish (Phase 7) -> Final validation pass
7. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files or independent sections, no dependencies
- [Story] label maps task to specific user story for traceability
- This is a documentation-only feature — no compiled code, no tests beyond manual review
- README content must be validated against source code (ConfigurationReader.java, PluginConfiguration.java, WatchMojo.java, AutoCompileMojo.java)
- Commit after each phase or logical group
- Stop at any checkpoint to validate story independently
