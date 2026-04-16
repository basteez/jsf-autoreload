# Implementation Plan: README & License for Maven Publishing

**Branch**: `003-readme-and-license` | **Date**: 2026-04-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-readme-and-license/spec.md`

## Summary

Create a comprehensive README.md for the jsf-autoreload project and add an Apache License 2.0 LICENSE file to prepare for Maven Central publishing. The README must cover plugin purpose, quick-start setup, module structure, compatibility matrix, configuration reference, and contributor build instructions. The parent POM must be updated with `<licenses>`, `<developers>`, `<scm>`, `<url>`, and `<description>` metadata required by Maven Central.

## Technical Context

**Language/Version**: N/A (documentation-only feature — no runtime code changes)
**Primary Dependencies**: N/A
**Storage**: N/A
**Testing**: Manual review — validate README content accuracy against source code, verify LICENSE file presence, verify POM metadata elements
**Target Platform**: GitHub repository + Maven Central metadata
**Project Type**: Maven multi-module library (documentation deliverables only)
**Performance Goals**: N/A
**Constraints**: README code snippets must be syntactically valid and copy-paste-ready (SC-002); contributor setup must be 3 steps or fewer (SC-004)
**Scale/Scope**: 3 deliverable files (README.md, LICENSE, pom.xml update)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Applicable? | Status | Notes |
|-----------|-------------|--------|-------|
| I. Plugin Modularity | No | PASS | No runtime code changes — documentation only |
| II. TDD (NON-NEGOTIABLE) | No | PASS | No implementation code introduced; TDD applies to feature/bug-fix code, not docs/config. Documentation accuracy verified by manual review against source. |
| III. JSF Specification Compliance | No | PASS | No code changes |
| IV. Zero Production Impact | No | PASS | No code changes — README/LICENSE/POM metadata have no runtime effect |
| V. Observability and Diagnostics | No | PASS | No code changes |

**Technical Constraints Check**:
- Java 8+ compile: N/A (no source changes)
- JSF versions: Documented correctly in README per spec FR-004
- Build tool: Maven — README documents Maven-only setup per assumptions
- Dependencies: No new dependencies introduced
- Artifact: No artifact changes

**Gate result**: PASS — all principles either satisfied or not applicable (documentation-only feature).

## Project Structure

### Documentation (this feature)

```text
specs/003-readme-and-license/
├── plan.md              # This file
├── research.md          # Phase 0: license research, Maven Central requirements
├── data-model.md        # Phase 1: README structure and content model
├── quickstart.md        # Phase 1: contributor quickstart for this feature
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
# Existing multi-module structure (no changes to source layout)
jsf-autoreload/
├── jsf-autoreload-core/           # Core library: SSE handler, file watcher, config, JSF bridge
│   └── src/main/java/it/bstz/jsfautoreload/
│       ├── init/                   # Servlet container initializers (javax + jakarta)
│       ├── sse/                    # SSE connection management and handler
│       ├── core/                   # DirectoryWatcher, Debouncer, ReloadCoordinator
│       ├── config/                 # PluginConfiguration, ConfigurationReader
│       ├── bridge/                 # JsfBridge, ServletBridge, BridgeDetector (javax/jakarta)
│       ├── jsf/                    # ScriptInjector
│       └── model/                  # FileCategory
├── jsf-autoreload-tomcat/         # Tomcat adapter: container-specific integration
│   └── src/main/java/it/bstz/jsfautoreload/tomcat/
│       └── TomcatAdapter.java
├── jsf-autoreload-maven-plugin/   # Maven plugin: watch + auto-compile goals
│   └── src/main/java/it/bstz/jsfautoreload/maven/
│       ├── WatchMojo.java          # `watch` goal — monitors source dir, triggers compile
│       └── AutoCompileMojo.java    # `auto-compile` goal — single compile trigger
├── jsf-autoreload-integration-tests/ # Integration tests: embedded Tomcat end-to-end
│   └── src/test/java/it/bstz/jsfautoreload/it/
│       ├── XhtmlReloadIT.java
│       ├── StaticResourceReloadIT.java
│       ├── ClassReloadIT.java
│       ├── DebouncingIT.java
│       ├── ProductionModeIT.java
│       └── MultiConnectionIT.java
├── pom.xml                         # Parent POM (to be updated with Maven Central metadata)
├── README.md                       # NEW — project documentation
└── LICENSE                         # NEW — Apache License 2.0
```

**Structure Decision**: No changes to the source code layout. This feature adds two root-level files (README.md, LICENSE) and updates the existing parent pom.xml.

## Deliverables

| File | Action | Satisfies |
|------|--------|-----------|
| `README.md` | CREATE | FR-001 through FR-006, FR-008, FR-011 |
| `LICENSE` | CREATE | FR-007, FR-010 |
| `pom.xml` | UPDATE | FR-009, FR-009a, FR-009b, FR-009c, FR-009d |

## Configuration Reference (source of truth for README)

Extracted from `ConfigurationReader.java` — all options support both `web.xml` context-param (prefix: `it.bstz.jsfautoreload.`) and system property (prefix: `jsfautoreload.`). System properties take precedence.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `enabled` | boolean | `true` | Enable/disable the plugin entirely |
| `debounceMs` | long | `500` | Debounce interval (ms) for non-class file changes |
| `classDebounceMs` | long | `1000` | Debounce interval (ms) for class file changes |
| `sseEndpointPath` | String | `/_jsf-autoreload/events` | SSE endpoint path for browser connection |
| `autoCompileEnabled` | boolean | `false` | Enable automatic compilation on Java source changes |
| `autoCompileCommand` | String | *(none)* | Shell command to execute for auto-compile |
| `sourceDirectory` | String | `src/main/java` | Java source directory to watch for auto-compile |
| `watchDirs` | String | *(none)* | Comma-separated additional directories to watch |
| `excludePatterns` | String | *(none)* | Comma-separated glob patterns to exclude from watching |

Maven plugin parameters (WatchMojo / AutoCompileMojo):

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `sourceDirectory` | `jsf-autoreload.sourceDirectory` | `src/main/java` | Source directory to monitor |
| `compileCommand` | `jsf-autoreload.compileCommand` | `mvn compile` | Compile command to execute on change |
| `autoCompile` | `jsf-autoreload.autoCompile` | `true` | Enable/disable auto-compile goal |

## Complexity Tracking

> No constitution violations — no entries needed.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *(none)* | | |
