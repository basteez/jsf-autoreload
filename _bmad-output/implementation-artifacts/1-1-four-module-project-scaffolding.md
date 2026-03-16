# Story 1.1: Four-Module Project Scaffolding

Status: done

## Story

As a maintainer,
I want the project structured into four modules (core, gradle-plugin, maven-plugin, runtime),
So that shared logic can be extracted into core without build-tool-specific dependencies.

## Acceptance Criteria

1. `settings.gradle.kts` includes four modules: `jsf-autoreload-core`, `jsf-autoreload-gradle-plugin`, `jsf-autoreload-maven-plugin`, `jsf-autoreload-runtime`
2. The existing `jsf-autoreload-plugin` directory is renamed to `jsf-autoreload-gradle-plugin`
3. `jsf-autoreload-core/build.gradle.kts` applies `java-library` and `maven-publish` plugins with zero build-tool (Gradle API, Maven API) or Servlet API dependencies
4. `jsf-autoreload-maven-plugin/build.gradle.kts` depends on `jsf-autoreload-core` and applies the `de.benediktritter.maven-plugin-development` plugin for descriptor generation
5. `jsf-autoreload-gradle-plugin/build.gradle.kts` depends on `jsf-autoreload-core`
6. `./gradlew build` succeeds — all four modules compile and all existing tests pass

## Tasks / Subtasks

- [x] Task 1: Rename `jsf-autoreload-plugin` directory to `jsf-autoreload-gradle-plugin` (AC: #2)
  - [x] 1.1 Rename the directory on disk
  - [x] 1.2 Update `settings.gradle.kts` to reference new module name
  - [x] 1.3 Update any internal references in `jsf-autoreload-gradle-plugin/build.gradle.kts` (plugin ID, artifact name) — no changes needed, no old-name references existed
  - [x] 1.4 Update the runtime JAR embedding path reference in `build.gradle.kts` if it references the old module name — no changes needed, runtime reference uses `:jsf-autoreload-runtime`
- [x] Task 2: Create `jsf-autoreload-core` module shell (AC: #1, #3)
  - [x] 2.1 Create directory `jsf-autoreload-core/`
  - [x] 2.2 Create `build.gradle.kts` with `java-library` + `maven-publish` plugins
  - [x] 2.3 Set Java source/target to 11
  - [x] 2.4 Add JUnit 5 test dependency only
  - [x] 2.5 Create empty `src/main/java/it/bstz/jsfautoreload/` package directory
  - [x] 2.6 Create empty `src/test/java/it/bstz/jsfautoreload/` package directory
- [x] Task 3: Create `jsf-autoreload-maven-plugin` module shell (AC: #1, #4)
  - [x] 3.1 Create directory `jsf-autoreload-maven-plugin/`
  - [x] 3.2 Create `build.gradle.kts` with `maven-publish` plugin
  - [x] 3.3 Add dependency on `project(":jsf-autoreload-core")`
  - [x] 3.4 Apply `de.benediktritter.maven-plugin-development` plugin
  - [x] 3.5 Add Maven API compile dependencies (`maven-plugin-api`, `maven-plugin-annotations`)
  - [x] 3.6 Create empty `src/main/java/it/bstz/jsfautoreload/` package directory
  - [x] 3.7 Create empty `src/test/java/it/bstz/jsfautoreload/` package directory
- [x] Task 4: Update `settings.gradle.kts` to include all four modules (AC: #1)
  - [x] 4.1 Set includes to: `jsf-autoreload-core`, `jsf-autoreload-gradle-plugin`, `jsf-autoreload-maven-plugin`, `jsf-autoreload-runtime`
- [x] Task 5: Add `jsf-autoreload-core` dependency to gradle-plugin (AC: #5)
  - [x] 5.1 Add `implementation(project(":jsf-autoreload-core"))` to `jsf-autoreload-gradle-plugin/build.gradle.kts`
  - [x] 5.2 Ensure shadow JAR configuration includes core classes — shadow plugin auto-bundles implementation deps
- [x] Task 6: Verify full build (AC: #6)
  - [x] 6.1 Run `./gradlew build` and confirm all modules compile — BUILD SUCCESSFUL (16 tasks)
  - [x] 6.2 Confirm all existing tests pass (plugin integration tests, watcher tests, websocket tests, compiler tests, liberty adapter tests, filter tests) — all pass

## Dev Notes

### Existing Project Structure (CRITICAL - understand before changing)

Current `settings.gradle.kts`:
```kotlin
rootProject.name = "jsf-autoreload"
include("jsf-autoreload-plugin", "jsf-autoreload-runtime")
```

Current root `build.gradle.kts`:
```kotlin
allprojects {
    group = "it.bstz"
    version = "0.1.0-SNAPSHOT"
}
```

### Architecture Constraints

- Module dependency direction (HARD RULE):
  - `runtime` -> standalone, zero dependencies
  - `core` -> standalone, zero build-tool dependencies
  - `gradle-plugin` -> core
  - `maven-plugin` -> core
  - NO cross-dependencies between gradle-plugin and maven-plugin
- Core module MUST NOT import: `org.gradle.*`, `org.apache.maven.*`, `javax.servlet.*`
- This story creates EMPTY shells only — no code moves yet

### Key Implementation Details

- The `de.benediktritter.maven-plugin-development` Gradle plugin generates `META-INF/maven/plugin.xml` from `@Mojo`/`@Parameter` annotations during the Gradle build. This keeps a single build system.
- The maven-plugin module needs `maven-plugin-api` and `maven-plugin-annotations` as compileOnly dependencies for the annotations to work.
- The core module `build.gradle.kts` should mirror the Java configuration from the existing modules (Java 11 source/target, UTF-8 encoding).
- Shadow JAR in gradle-plugin must be updated to include core module classes when they are later extracted.

### Gradle Plugin Reference for Maven Descriptor

Plugin: `de.benediktritter.maven-plugin-development` — generates Maven plugin descriptors from a Gradle build. Apply to maven-plugin module. Latest stable version should be used.

### Risk: Rename may break Gradle plugin ID registration

The existing `jsf-autoreload-plugin/build.gradle.kts` registers the plugin ID `it.bstz.jsf-autoreload` with implementation class `it.bstz.jsfautoreload.JsfAutoreloadPlugin`. After rename to `jsf-autoreload-gradle-plugin`, verify:
- The `gradlePlugin` block still works
- The `META-INF/gradle-plugins/it.bstz.jsf-autoreload.properties` file is still generated
- Integration tests using the plugin ID still pass

### Project Structure Notes

- This is a create-then-extract approach: create empty module shells first, extract code in subsequent stories
- The rename from `plugin` to `gradle-plugin` provides symmetric naming with `maven-plugin`
- No code should be moved or modified in this story beyond build configuration

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Scaffolding Sequence]
- [Source: _bmad-output/planning-artifacts/architecture.md#Module Dependency Direction Rule]
- [Source: _bmad-output/planning-artifacts/architecture.md#Foundational Technical Decisions]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.1]
- [Source: jsf-autoreload-plugin/build.gradle.kts — current plugin build config]
- [Source: settings.gradle.kts — current module includes]

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6 (1M context)

### Debug Log References
- Build output: BUILD SUCCESSFUL in 42s, 16 actionable tasks (11 executed, 5 up-to-date)
- Deprecation note: LibertyServerAdapter.java uses deprecated API (pre-existing, not introduced by this story)

### Completion Notes List
- Renamed `jsf-autoreload-plugin` -> `jsf-autoreload-gradle-plugin` directory; no internal reference changes needed
- Created `jsf-autoreload-core` module with `java-library` + `maven-publish` plugins, Java 11, JUnit 5 test dependency only, zero build-tool dependencies
- Created `jsf-autoreload-maven-plugin` module with `de.benediktritter.maven-plugin-development` v0.4.3 plugin, dependency on core, Maven API compileOnly dependencies
- Updated `settings.gradle.kts` to include all four modules
- Added `implementation(project(":jsf-autoreload-core"))` to gradle-plugin; shadow JAR auto-includes via implementation configuration
- Full build passes: all four modules compile, all existing tests pass
- [Code Review Fix] Added `.gitkeep` files to 4 empty package directories so they persist in git
- [Code Review Fix] Changed maven-plugin from `java-library` to `java` plugin per architecture spec
- [Code Review Fix] Added UTF-8 encoding config (`tasks.withType<JavaCompile>`) to core, maven-plugin, and gradle-plugin
- [Code Review Fix] Updated File List to accurately reflect git operations (deletes, new files vs "modified")

### File List
- `settings.gradle.kts` (modified — updated module includes)
- `jsf-autoreload-plugin/` (deleted — entire directory removed, superseded by jsf-autoreload-gradle-plugin rename)
- `jsf-autoreload-gradle-plugin/build.gradle.kts` (new — based on former jsf-autoreload-plugin/build.gradle.kts with core dependency added, UTF-8 encoding)
- `jsf-autoreload-gradle-plugin/src/` (new — all source files carried over from jsf-autoreload-plugin)
- `jsf-autoreload-core/build.gradle.kts` (new)
- `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/.gitkeep` (new — preserves empty package in git)
- `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/.gitkeep` (new — preserves empty package in git)
- `jsf-autoreload-maven-plugin/build.gradle.kts` (new)
- `jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/.gitkeep` (new — preserves empty package in git)
- `jsf-autoreload-maven-plugin/src/test/java/it/bstz/jsfautoreload/.gitkeep` (new — preserves empty package in git)

## Change Log
- 2026-03-16: Story 1.1 implemented — four-module project scaffolding complete. Renamed plugin module, created core and maven-plugin empty shells, updated settings and dependencies. Full build passes.
- 2026-03-16: Code review — fixed 6 findings (3 HIGH, 3 MEDIUM): added .gitkeep for empty dirs, corrected maven-plugin plugin type, added UTF-8 encoding, updated File List accuracy. Build verified.
