# Story 4.1: Maven Mojo & PrepareStep Implementation

Status: ready-for-dev

## Story

As a developer using Maven,
I want to run `mvn jsf-autoreload:dev` to get live-reload,
So that I don't need to switch build tools to use jsf-autoreload.

## Acceptance Criteria

1. Maven `pom.xml` with `jsf-autoreload-maven-plugin` declared, `mvn jsf-autoreload:dev` executes `JsfDevMojo` which runs prepare step then enters dev loop
2. `JsfDevMojo` configuration via `<configuration>` block (port, serverName, outputDir, watchDirs) maps to `DevServerConfig` and passes to `DevServer.start()`
3. `PrepareStep` resolves runtime JAR via Maven dependency resolution, copies to `WEB-INF/lib`, writes `jsf-autoreload.properties` to `WEB-INF/classes`, calls `ServerAdapter.writeServerConfig()`
4. `JsfAutoreloadException` from core is translated to `MojoExecutionException` with same message
5. `PrepareStep` unit tested with plain JUnit (no Maven infrastructure)

## Tasks / Subtasks

- [ ] Task 1: Create `JsfDevMojo` (AC: #1, #2, #4)
  - [ ] 1.1 Create `jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/JsfDevMojo.java`
  - [ ] 1.2 Annotate with `@Mojo(name = "dev", requiresProject = true)`
  - [ ] 1.3 Declare `@Parameter` fields: port (default 35729), serverName (default "defaultServer"), outputDir, watchDirs (default ["src/main/webapp"])
  - [ ] 1.4 In `execute()`: create PrepareStep, run it, then build DevServerConfig and call DevServer.start()
  - [ ] 1.5 Construct appropriate ServerAdapter from serverName
  - [ ] 1.6 Catch `JsfAutoreloadException` -> throw `MojoExecutionException(e.getMessage(), e)`
  - [ ] 1.7 Use Maven's `getLog()` for logging
- [ ] Task 2: Create `PrepareStep` (AC: #3, #5)
  - [ ] 2.1 Create `jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/PrepareStep.java`
  - [ ] 2.2 Constructor takes: outputDir (Path), port (int), ServerAdapter, runtime JAR path
  - [ ] 2.3 `execute()` method:
    - Copy runtime JAR to `{outputDir}/WEB-INF/lib/`
    - Write `jsf-autoreload.properties` to `{outputDir}/WEB-INF/classes/` with `port={port}`
    - Call `serverAdapter.writeServerConfig(params)` with appropriate `ServerConfigParams`
  - [ ] 2.4 Use `java.nio.file` for all file operations
  - [ ] 2.5 Design for testability — no Maven API dependencies in PrepareStep itself
- [ ] Task 3: Implement runtime JAR resolution (AC: #3)
  - [ ] 3.1 In `JsfDevMojo`, resolve `jsf-autoreload-runtime` JAR from Maven dependencies
  - [ ] 3.2 Use Maven's `@Parameter(defaultValue = "${project}")` and dependency resolution
  - [ ] 3.3 Pass resolved JAR path to `PrepareStep`
- [ ] Task 4: Update build configuration (AC: #1)
  - [ ] 4.1 Ensure `jsf-autoreload-maven-plugin/build.gradle.kts` has correct Maven API dependencies
  - [ ] 4.2 Verify `de.benediktritter.maven-plugin-development` generates `META-INF/maven/plugin.xml`
  - [ ] 4.3 Add dependency on `jsf-autoreload-core`
  - [ ] 4.4 Add dependency on `jsf-autoreload-runtime` (for runtime JAR resolution)
- [ ] Task 5: Write tests (AC: #5)
  - [ ] 5.1 Create `jsf-autoreload-maven-plugin/src/test/java/it/bstz/jsfautoreload/PrepareStepTest.java`
  - [ ] 5.2 Test: PrepareStep copies runtime JAR to correct location
  - [ ] 5.3 Test: PrepareStep writes properties file with correct port
  - [ ] 5.4 Test: PrepareStep calls writeServerConfig on adapter
  - [ ] 5.5 All tests use plain JUnit + `@TempDir` — NO Maven test infrastructure

## Dev Notes

### Architecture Decision: Single Goal with Internal PrepareStep

Users type ONE command: `mvn jsf-autoreload:dev`. The mojo internally:
1. Runs `PrepareStep` (copies JAR, writes config)
2. Constructs `DevServerConfig`
3. Calls `DevServer.start()` (blocking dev loop)

`PrepareStep` is a separate class for testability but NOT a separate Maven goal.

### Maven Plugin Development via Gradle

The `de.benediktritter.maven-plugin-development` Gradle plugin generates Maven plugin descriptors from `@Mojo`/`@Parameter` annotations. This means:
- The maven-plugin module is built by Gradle
- Contributors don't need Maven installed
- End users consume the published artifact normally via `pom.xml`

### Key Maven API Classes

```java
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
```

### PrepareStep Design: Build-Tool Agnostic

`PrepareStep` should NOT import Maven API. It receives:
- `Path outputDir` — where to write files
- `int port` — WebSocket port
- `ServerAdapter adapter` — for server config
- `Path runtimeJarPath` — already resolved by the mojo

This enables testing with plain JUnit and `@TempDir`.

### Exception Translation Pattern

```java
try {
    devServer.start();
} catch (JsfAutoreloadException e) {
    throw new MojoExecutionException(e.getMessage(), e);
}
```

### Project Structure Notes

- New: `jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/JsfDevMojo.java`
- New: `jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/PrepareStep.java`
- New: `jsf-autoreload-maven-plugin/src/test/java/it/bstz/jsfautoreload/PrepareStepTest.java`
- Modified: `jsf-autoreload-maven-plugin/build.gradle.kts`
- Depends on: Epic 1 (all core), Epic 3 (Tomcat validates interface)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Maven Lifecycle Integration]
- [Source: _bmad-output/planning-artifacts/architecture.md#Maven Plugin Module Boundary]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 4.1]
- [Source: _bmad-output/planning-artifacts/prd.md#FR2 — Maven plugin declaration]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
