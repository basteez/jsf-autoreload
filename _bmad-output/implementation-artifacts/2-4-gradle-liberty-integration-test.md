# Story 2.4: Gradle + Liberty Integration Test

Status: ready-for-dev

## Story

As a developer using Gradle + Liberty,
I want the full end-to-end live-reload pipeline verified,
So that I'm confident the plugin works correctly on my stack.

## Acceptance Criteria

1. Gradle TestKit test: after `jsfPrepare`, runtime JAR in exploded WAR's `WEB-INF/lib`, `jsf-autoreload.properties` in `WEB-INF/classes`, `bootstrap.properties` contains JSF refresh entries
2. Plugin applied alongside `war` and `liberty-gradle-plugin` — no task conflicts or classpath errors
3. Integration tests pass on both Gradle 7.x and 8.x

## Tasks / Subtasks

- [ ] Task 1: Create/update comprehensive integration test (AC: #1)
  - [ ] 1.1 Update or create `JsfAutoreloadPluginIntegrationTest.java` in gradle-plugin
  - [ ] 1.2 Test: jsfPrepare produces runtime JAR in `WEB-INF/lib`
  - [ ] 1.3 Test: jsfPrepare produces `jsf-autoreload.properties` in `WEB-INF/classes`
  - [ ] 1.4 Test: jsfPrepare produces JSF refresh entries in `bootstrap.properties`
  - [ ] 1.5 Test: extension properties (port, serverName, outputDir, watchDirs) are respected
- [ ] Task 2: Test plugin compatibility (AC: #2)
  - [ ] 2.1 Test: plugin applies alongside `war` plugin without conflicts
  - [ ] 2.2 Test: plugin applies alongside `liberty-gradle-plugin` without task conflicts
  - [ ] 2.3 Verify task dependency graph: jsfPrepare runs before libertyStart
- [ ] Task 3: Test Gradle version compatibility (AC: #3)
  - [ ] 3.1 Run tests with Gradle 7.x distribution
  - [ ] 3.2 Run tests with Gradle 8.x distribution
  - [ ] 3.3 Use `GradleRunner.withGradleVersion()` for version-specific tests

## Dev Notes

### Existing Integration Tests

Location: `jsf-autoreload-gradle-plugin/src/test/java/it/bstz/jsfautoreload/JsfAutoreloadPluginIntegrationTest.java` (97 lines)

Current tests:
- Plugin registers jsfDev and jsfPrepare tasks
- Task dependency wiring with libertyStart task
- Extension port property resolution

These need to be updated for the new architecture (core delegation, properties file, bootstrap.properties).

### Gradle TestKit Pattern

```java
GradleRunner.create()
    .withProjectDir(testProjectDir)
    .withArguments("jsfPrepare")
    .withPluginClasspath()
    .build();

// Verify outputs
assertTrue(Files.exists(testProjectDir.toPath()
    .resolve("build/wlp/usr/servers/defaultServer/apps/expanded/test.war/WEB-INF/lib/jsf-autoreload-runtime.jar")));
assertTrue(Files.exists(testProjectDir.toPath()
    .resolve("build/wlp/usr/servers/defaultServer/apps/expanded/test.war/WEB-INF/classes/jsf-autoreload.properties")));
```

### Test Naming Convention

`{Feature}IntegrationTest.java` for integration tests. Method names: camelCase behavior descriptions.

### Project Structure Notes

- Modified: `jsf-autoreload-gradle-plugin/src/test/java/it/bstz/jsfautoreload/JsfAutoreloadPluginIntegrationTest.java`
- Depends on: Stories 2.1, 2.2, 2.3 (all Gradle+Liberty wiring complete)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Test Organization & Conventions]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 2.4]
- [Source: jsf-autoreload-plugin/src/test/java/it/bstz/jsfautoreload/JsfAutoreloadPluginIntegrationTest.java — existing 97-line tests]
- [Source: _bmad-output/planning-artifacts/prd.md#NFR8 — Gradle 7.x and 8.x]
- [Source: _bmad-output/planning-artifacts/prd.md#NFR12 — no conflicts with common plugins]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
