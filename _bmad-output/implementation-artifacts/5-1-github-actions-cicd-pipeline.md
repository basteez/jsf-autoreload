# Story 5.1: GitHub Actions CI/CD Pipeline

Status: ready-for-dev

## Story

As a maintainer,
I want automated build, test, and publish pipelines,
So that releases to Gradle Plugin Portal and Maven Central are reliable and repeatable.

## Acceptance Criteria

1. Push to main branch: GitHub Actions builds and tests all four modules across Java 11, 17, and 21
2. Per-module compile verification catches leaked API imports (e.g., Gradle API in core)
3. Tagged release (e.g., `v1.0.0`): publishes `jsf-autoreload-gradle-plugin` to Gradle Plugin Portal, and `jsf-autoreload-core`, `jsf-autoreload-maven-plugin`, `jsf-autoreload-runtime` to Maven Central
4. Publish failure: error reported clearly, no partial release in inconsistent state

## Tasks / Subtasks

- [ ] Task 1: Create CI workflow (AC: #1, #2)
  - [ ] 1.1 Create `.github/workflows/ci.yml`
  - [ ] 1.2 Trigger: push to main, pull requests
  - [ ] 1.3 Matrix: Java 11, 17, 21 (using `actions/setup-java` with Temurin)
  - [ ] 1.4 Steps: checkout, setup JDK, setup Gradle, run `./gradlew build`
  - [ ] 1.5 Add per-module compile verification step to catch API leakage
  - [ ] 1.6 Cache Gradle dependencies for faster builds
- [ ] Task 2: Create per-module API leakage check (AC: #2)
  - [ ] 2.1 Add a Gradle task or CI step that compiles core module in isolation
  - [ ] 2.2 Verify core module has no Gradle API, Maven API, or Servlet API on compile classpath
  - [ ] 2.3 Fail CI if leakage detected
- [ ] Task 3: Create publish workflow (AC: #3, #4)
  - [ ] 3.1 Create `.github/workflows/publish.yml`
  - [ ] 3.2 Trigger: tag push matching `v*`
  - [ ] 3.3 Build all modules with release version
  - [ ] 3.4 Publish gradle-plugin to Gradle Plugin Portal (requires `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` secrets)
  - [ ] 3.5 Publish core, maven-plugin, runtime to Maven Central (requires signing keys and OSSRH credentials)
  - [ ] 3.6 Use separate publish steps so failure in one doesn't corrupt others
  - [ ] 3.7 Add error handling and status reporting

## Dev Notes

### CI Matrix Configuration

```yaml
strategy:
  matrix:
    java: [11, 17, 21]
  fail-fast: false
```

Use `fail-fast: false` so all Java versions are tested even if one fails.

### API Leakage Detection

Critical CI check: core module must not transitively import Gradle/Maven/Servlet APIs. Options:
- Compile core module with `--no-daemon` and inspect classpath
- Add a Gradle task that verifies core's compile classpath excludes Gradle API
- Use `./gradlew :jsf-autoreload-core:compileJava` in isolation

### Publishing Secrets Required

- **Gradle Plugin Portal:** `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`
- **Maven Central (OSSRH):** `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`

These must be configured as GitHub repository secrets.

### Publish Workflow Strategy

Publish in order with independent steps:
1. Build and verify all modules
2. Publish to Gradle Plugin Portal
3. Publish to Maven Central
4. Each step is independent — failure in one is reported but doesn't block the other

### GitHub Actions Best Practices

- Use `gradle/actions/setup-gradle@v4` for Gradle setup with caching
- Use `actions/setup-java@v4` with Temurin distribution
- Pin action versions for reproducibility

### Project Structure Notes

- New: `.github/workflows/ci.yml`
- New: `.github/workflows/publish.yml`
- No code changes — pure CI/CD configuration

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#CI/CD]
- [Source: _bmad-output/planning-artifacts/architecture.md#Module Dependency Direction Rule]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 5.1]
- [Source: _bmad-output/planning-artifacts/prd.md#NFR7 — Java 11, 17, 21 CI matrix]
- [Source: _bmad-output/planning-artifacts/prd.md#FR35 — Gradle Plugin Portal publishing]
- [Source: _bmad-output/planning-artifacts/prd.md#FR36 — Maven Central publishing]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
