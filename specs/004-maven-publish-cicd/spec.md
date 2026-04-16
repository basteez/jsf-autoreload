# Feature Specification: Maven Central Publishing & CI/CD Pipeline

**Feature Branch**: `004-maven-publish-cicd`  
**Created**: 2026-04-16  
**Status**: Draft  
**Input**: User description: "Get ready for Maven Central publish (per Sonatype requirements) and set up CI/CD for project release, including version management"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Publish a Release to Maven Central (Priority: P1)

As a project maintainer, I want to publish jsf-autoreload artifacts to Maven Central so that consumers can depend on the library via standard Maven/Gradle coordinates without adding custom repositories.

**Why this priority**: This is the primary goal of the feature. Without a working publish pipeline, the library cannot reach its audience. Maven Central is the de-facto standard for Java library distribution.

**Independent Test**: Can be fully tested by running the release build locally and verifying that all required artifacts (JAR, sources JAR, javadoc JAR, PGP signatures, checksums) are generated and the POM meets Central's validation rules. A dry-run deploy to a local staging repository confirms correctness before actual publication.

**Acceptance Scenarios**:

1. **Given** the project is at a release version (non-SNAPSHOT), **When** the maintainer triggers a release build, **Then** the following artifacts are produced for each publishable module: main JAR, sources JAR, javadoc JAR, PGP `.asc` signatures for each file, and MD5/SHA1 checksums.
2. **Given** all artifacts are produced and signed, **When** they are submitted to the Sonatype Central Portal, **Then** they pass all validation rules (POM metadata, signatures, javadoc/sources presence) and are published to Maven Central.
3. **Given** a consumer adds the library coordinates to their build file, **When** they resolve dependencies, **Then** the artifacts are downloadable from Maven Central with working transitive dependency resolution.

---

### User Story 2 - Automated CI Pipeline for PRs and Pushes (Priority: P2)

As a project maintainer, I want every pull request targeting the main branch to be automatically built and tested so that regressions are caught before merging.

**Why this priority**: A CI pipeline is foundational for release confidence. Without automated validation, publishing to Maven Central carries the risk of shipping broken artifacts.

**Independent Test**: Can be tested by opening a PR with a failing test — CI should report failure. A PR with all tests passing should report success.

**Acceptance Scenarios**:

1. **Given** a developer opens a pull request targeting main, **When** the CI pipeline runs, **Then** the project compiles successfully and all unit and integration tests pass on both JDK 11 and JDK 21.
2. **Given** a pull request is merged to main, **When** the CI pipeline runs, **Then** the full build and test suite executes and results are visible on the merge commit.
3. **Given** a test failure occurs, **When** the CI pipeline completes, **Then** the failure is clearly reported with actionable output.

---

### User Story 3 - Automated Release via Tag (Priority: P3)

As a project maintainer, I want to trigger a release to Maven Central by pushing a version tag so that the release process is reproducible, auditable, and not dependent on a single developer's local environment.

**Why this priority**: Manual releases are error-prone and require the maintainer to have GPG keys and Sonatype credentials configured locally. Automating this via CI reduces human error and makes the project bus-factor resilient.

**Independent Test**: Can be tested by pushing a version tag (e.g., `v0.1.0`) and verifying that the CI pipeline builds, signs, and deploys artifacts to Maven Central (or a staging repository for dry-run).

**Acceptance Scenarios**:

1. **Given** the maintainer pushes a Git tag matching the version pattern (e.g., `v0.1.0`), **When** the CI release pipeline triggers, **Then** all publishable modules are built, signed, and deployed to Maven Central.
2. **Given** a release pipeline run completes successfully, **When** the maintainer checks Maven Central, **Then** the artifacts are available under the project's group coordinates.
3. **Given** the release pipeline fails (e.g., signing error, validation failure), **When** the maintainer checks CI, **Then** the error is clearly reported and no partial artifacts are published.

---

### User Story 4 - Version Management (Priority: P2)

As a project maintainer, I want a clear and consistent versioning strategy so that version numbers across all modules stay in sync and consumers can rely on semantic versioning guarantees.

**Why this priority**: Maven Central requires non-SNAPSHOT versions for releases and will never overwrite a published version. Incorrect version management can lead to publishing failures or consumer confusion.

**Independent Test**: Can be tested by verifying that all module POMs reference a consistent version, that the release process strips the SNAPSHOT qualifier, and that after release the version is bumped back to the next SNAPSHOT.

**Acceptance Scenarios**:

1. **Given** all modules are at version `X.Y.Z-SNAPSHOT`, **When** a release is prepared, **Then** all modules are set to version `X.Y.Z` (SNAPSHOT qualifier removed) consistently.
2. **Given** a release has been published at version `X.Y.Z`, **When** the CI release pipeline completes successfully, **Then** CI automatically commits and pushes a version bump setting all modules to `X.Y.(Z+1)-SNAPSHOT`.
3. **Given** any module POM, **When** inspected, **Then** the version matches the parent POM version (no version drift between modules).

---

### Edge Cases

- Partial publish is prevented by design: all modules are bundled into a single atomic deployment unit. If any module fails validation, the entire bundle is rejected and no artifacts are published.
- How does the system handle attempting to publish a version that already exists on Maven Central?
- What happens if the GPG signing key expires or is revoked during a CI release run?
- What happens if the Sonatype Central Portal is temporarily unavailable during deployment?
- How are non-publishable modules (e.g., integration-tests) excluded from deployment?

## Clarifications

### Session 2026-04-16

- Q: How should multi-module deployment atomicity be handled? → A: Atomic bundle — all modules bundled into a single deployment unit; all publish or none publish.
- Q: Should the post-release version bump to next SNAPSHOT be automated in CI? → A: Automated — CI creates a commit bumping all modules to next SNAPSHOT after successful release and pushes it to main.
- Q: Should main branch builds publish SNAPSHOT artifacts to a snapshot repository? → A: No. Main is the release branch (Gitflow model); builds only compile and test. Only tagged releases are deployed. Feature development happens on development branches.
- Q: Which JDK strategy should CI use for build and test? → A: Matrix with JDK 11 (minimum LTS) and JDK 21 (current LTS).
- Q: What is the target CI feedback time for pull requests? → A: Under 10 minutes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The build MUST produce a sources JAR (`-sources.jar`) for each publishable module (core, tomcat, maven-plugin).
- **FR-002**: The build MUST produce a javadoc JAR (`-javadoc.jar`) for each publishable module.
- **FR-003**: All release artifacts MUST be signed with GPG/PGP, producing `.asc` signature files.
- **FR-004**: All release artifacts MUST have MD5 and SHA1 checksum files generated.
- **FR-005**: The POM for each module MUST include all required Maven Central metadata: name, description, url, licenses, developers, and SCM information.
- **FR-006**: The integration-tests module MUST be excluded from deployment to Maven Central.
- **FR-007**: The CI pipeline MUST compile and run all tests (unit and integration) on every pull request targeting the main branch and on every push to main (merge commits), using a JDK matrix of JDK 11 and JDK 21.
- **FR-008**: The CI release pipeline MUST trigger on Git tags matching a version pattern (e.g., `v*.*.*`).
- **FR-009**: The CI release pipeline MUST deploy signed artifacts to Maven Central via the Sonatype Central Portal.
- **FR-010**: The project MUST use semantic versioning (MAJOR.MINOR.PATCH) for all releases.
- **FR-011**: All module versions MUST be kept in sync via the parent POM (no independent module versioning).
- **FR-012**: The release process MUST reject attempts to publish SNAPSHOT versions.
- **FR-013**: GPG signing keys and Sonatype credentials MUST be stored as CI secrets, never in the repository.
- **FR-014**: All publishable modules MUST be deployed as a single atomic bundle to the Sonatype Central Portal, ensuring either all artifacts are published or none are (no partial publishes).
- **FR-015**: After a successful release, the CI pipeline MUST automatically create a commit that bumps all module versions to the next SNAPSHOT (patch increment) and pushes it to the main branch.

### Key Entities

- **Publishable Module**: A Maven module whose artifacts are deployed to Maven Central (core, tomcat, maven-plugin). Distinguished from non-publishable modules (integration-tests) by deployment configuration.
- **Release Artifact Set**: For each publishable module, the complete set of files required by Maven Central: main JAR, sources JAR, javadoc JAR, POM, PGP signatures, and checksums.
- **Version Tag**: A Git tag (e.g., `v0.1.0`) that triggers the automated release pipeline and determines the release version.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All publishable modules pass Sonatype's Central Portal validation on every release attempt (zero validation failures after initial setup).
- **SC-002**: A complete release (from tag push to artifacts available on Maven Central) completes without manual intervention.
- **SC-003**: Every pull request receives automated build and test feedback within 10 minutes.
- **SC-004**: Consumers can resolve the library and all its transitive dependencies using only Maven Central (no custom repository configuration needed).
- **SC-005**: Version numbers across all modules are always consistent (zero version drift).

## Assumptions

- The project maintainer owns or can verify the domain `bstz.it` for the `it.bstz` groupId on the Sonatype Central Portal. If domain verification is not possible, an alternative groupId such as `io.github.basteez` may be used instead.
- The CI/CD platform is GitHub Actions, since the project is hosted on GitHub.
- The project follows a Gitflow branching model: `main` is the release branch, and feature development happens on development branches. No SNAPSHOT artifacts are published; only tagged releases are deployed to Maven Central.
- The first release version will be `1.0.0`. The current `0.1.0-SNAPSHOT` version will be updated to `1.0.0-SNAPSHOT` as part of this feature.
- The GPG signing key will be generated by the maintainer and its public key published to a public keyserver (e.g., keys.openpgp.org).
- The maintainer will register a Sonatype Central Portal account and claim the namespace before the first publish.
- Tag-based releases (push a `v*.*.*` tag to trigger release) are the preferred automation model.
- The `jsf-autoreload-integration-tests` module is for internal testing only and will not be published to Maven Central.
- Module child POMs already inherit required metadata (name, description, url, licenses, developers, scm) from the parent POM or will have it added as part of this feature.
