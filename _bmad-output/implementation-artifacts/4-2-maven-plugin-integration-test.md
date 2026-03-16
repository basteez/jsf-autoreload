# Story 4.2: Maven Plugin Integration Test

Status: ready-for-dev

## Story

As a developer using Maven,
I want the Maven plugin verified against real Maven builds,
So that I'm confident it works with standard Maven project layouts.

## Acceptance Criteria

1. `maven-invoker-plugin` test project with `jsf-autoreload-maven-plugin` configured: `mvn jsf-autoreload:dev` prepare step completes — runtime JAR in `WEB-INF/lib`, properties file in `WEB-INF/classes`, server config written
2. Maven plugin applied alongside `maven-war-plugin` and `maven-compiler-plugin` — no conflicts or classpath errors
3. Integration tests pass on Maven 3.6+
4. Built artifact contains valid `META-INF/maven/plugin.xml` descriptor

## Tasks / Subtasks

- [ ] Task 1: Create maven-invoker test project (AC: #1, #2)
  - [ ] 1.1 Set up test infrastructure in `jsf-autoreload-maven-plugin/src/test/`
  - [ ] 1.2 Create minimal test Maven project with `pom.xml` applying the plugin
  - [ ] 1.3 Include `maven-war-plugin` and `maven-compiler-plugin` in test project
  - [ ] 1.4 Write verify script to check outputs (runtime JAR, properties file, server config)
- [ ] Task 2: Verify plugin descriptor (AC: #4)
  - [ ] 2.1 After build, inspect the plugin JAR for `META-INF/maven/plugin.xml`
  - [ ] 2.2 Verify descriptor contains `dev` goal definition
  - [ ] 2.3 Verify parameters are listed (port, serverName, outputDir, watchDirs)
- [ ] Task 3: Test Maven version compatibility (AC: #3)
  - [ ] 3.1 Verify tests pass with Maven 3.6+
  - [ ] 3.2 Document any version-specific considerations

## Dev Notes

### Testing Strategy: maven-invoker-plugin

The architecture specifies `maven-invoker-plugin` for integration testing (NOT `maven-plugin-testing-harness` which is brittle and poorly maintained).

However, since the maven-plugin module is built by Gradle, the integration test setup needs adaptation:
- Option A: Create a Gradle test task that invokes Maven on a test project
- Option B: Use a functional test approach — build the plugin JAR, install to local repo, run Maven against test project
- Option C: Write the integration tests as Gradle TestKit-style tests that verify the plugin descriptor and PrepareStep behavior

Given the project uses Gradle as its build system, consider the most pragmatic approach. The `PrepareStep` is already unit tested (Story 4.1). The integration test mainly needs to verify the Maven descriptor is valid and the mojo wiring works.

### Plugin Descriptor Validation

The `de.benediktritter.maven-plugin-development` Gradle plugin generates `META-INF/maven/plugin.xml` during build. Verify:
- Goal `dev` is registered
- Parameters have correct names and defaults
- `requiresProject` is true

### Compatibility with Common Maven Plugins

The plugin should not conflict with:
- `maven-war-plugin` — WAR packaging
- `maven-compiler-plugin` — Java compilation
- `maven-surefire-plugin` — test execution

### Project Structure Notes

- New: integration test infrastructure in maven-plugin module
- Depends on: Story 4.1 (JsfDevMojo and PrepareStep exist)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Maven Lifecycle Integration — testing strategy]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 4.2]
- [Source: _bmad-output/planning-artifacts/prd.md#NFR9 — Maven 3.6+]
- [Source: _bmad-output/planning-artifacts/prd.md#NFR12 — no conflicts with common plugins]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
