# Tasks: Maven Central Publishing & CI/CD Pipeline

**Input**: Design documents from `/specs/004-maven-publish-cicd/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not applicable — this feature is build infrastructure and CI/CD configuration with no runtime code. Validation is performed via CI pipeline execution and local dry-run artifact verification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create directory structure for GitHub Actions workflows

- [X] T001 Create `.github/workflows/` directory structure at repository root ([#110](https://github.com/basteez/jsf-autoreload/issues/110))

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: POM metadata and versioning prerequisites that MUST be complete before any user story can be implemented

**Why blocking**: Maven Central validation requires complete developer metadata (R-006), and the agreed first release version is `1.0.0` (spec assumption). All subsequent phases depend on these being in place.

- [X] T002 Add missing `organization` and `organizationUrl` fields to `<developer>` entry in `pom.xml` (see research.md R-006 for required fields) ([#111](https://github.com/basteez/jsf-autoreload/issues/111))
- [X] T003 Update project version from `0.1.0-SNAPSHOT` to `1.0.0-SNAPSHOT` in `pom.xml` using `mvn versions:set -DnewVersion=1.0.0-SNAPSHOT -DgenerateBackupPoms=false` (applies to all modules via parent inheritance) ([#112](https://github.com/basteez/jsf-autoreload/issues/112))

**Checkpoint**: POM metadata is Central-compliant and version is set for first release

---

## Phase 3: User Story 1 — Publish a Release to Maven Central (Priority: P1) :dart: MVP

**Goal**: Configure the Maven build to produce all artifacts required by Maven Central (sources JAR, javadoc JAR, PGP signatures, checksums) via a `release` profile, so that a maintainer can publish from the command line.

**Independent Test**: Run `mvn -B -P release verify -DskipTests` locally and confirm each publishable module's `target/` contains: main JAR, `-sources.jar`, `-javadoc.jar`, and `.asc` signature files. Verify the integration-tests module does NOT produce javadoc/sources/signature artifacts.

### Implementation for User Story 1

- [X] T004 [US1] Add `release` profile to parent `pom.xml` containing `maven-source-plugin:3.3.1`, `maven-javadoc-plugin:3.11.2`, `maven-gpg-plugin:3.2.7`, and `central-publishing-maven-plugin:0.10.0` per `specs/004-maven-publish-cicd/contracts/release-profile.xml` ([#113](https://github.com/basteez/jsf-autoreload/issues/113))
- [X] T005 [US1] Configure `jsf-autoreload-integration-tests/pom.xml` to skip javadoc generation and GPG signing when the `release` profile is active (add `release` profile with `maven-javadoc-plugin` `<skip>true</skip>` and `maven-gpg-plugin` `<skip>true</skip>`; deploy skip is already configured) ([#114](https://github.com/basteez/jsf-autoreload/issues/114))
- [X] T006 [US1] Verify release artifact generation by running `mvn -B -P release verify -DskipTests` and confirming sources JAR, javadoc JAR, and `.asc` files exist in `target/` for each publishable module (`jsf-autoreload-core`, `jsf-autoreload-tomcat`, `jsf-autoreload-maven-plugin`) ([#115](https://github.com/basteez/jsf-autoreload/issues/115))

**Checkpoint**: At this point, the project can produce all Maven Central-required artifacts locally. User Story 1 is fully functional and testable independently.

---

## Phase 4: User Story 2 — Automated CI Pipeline for PRs and Pushes (Priority: P2)

**Goal**: Every pull request targeting main and every push to main is automatically built and tested on JDK 11 and JDK 21, providing CI feedback under 10 minutes.

**Independent Test**: Open a PR targeting main — CI should trigger and report build+test results on both JDK 11 and JDK 21.

### Implementation for User Story 2

- [X] T007 [US2] Create CI workflow at `.github/workflows/ci.yml` per `specs/004-maven-publish-cicd/contracts/ci-workflow.yml` — triggers on PRs to main and pushes to main, runs `mvn -B verify` on a JDK `[11, 21]` matrix using `actions/setup-java@v4` with Temurin distribution and Maven caching ([#116](https://github.com/basteez/jsf-autoreload/issues/116))

**Checkpoint**: At this point, PRs and pushes to main get automated CI feedback.

---

## Phase 5: User Story 4 — Version Management (Priority: P2)

**Goal**: All module versions are consistent (inherited from parent POM), semantic versioning is enforced, and no module has independent version drift.

**Independent Test**: Inspect all child module POMs and confirm none define their own `<version>` outside the `<parent>` block; run `mvn help:evaluate -Dexpression=project.version -q -DforceStdout` in each module and verify all return the same version.

### Implementation for User Story 4

- [X] T008 [US4] Verify all child module POMs (`jsf-autoreload-core/pom.xml`, `jsf-autoreload-tomcat/pom.xml`, `jsf-autoreload-maven-plugin/pom.xml`, `jsf-autoreload-integration-tests/pom.xml`) inherit version from parent POM with no independent `<version>` tags, and document the version inheritance model in a comment in `pom.xml` if needed ([#117](https://github.com/basteez/jsf-autoreload/issues/117))

**Checkpoint**: Version consistency is verified. Combined with T003, the versioning strategy is in place.

---

## Phase 6: User Story 3 — Automated Release via Tag (Priority: P3)

**Goal**: Pushing a version tag (e.g., `v1.0.0`) triggers a fully automated CI pipeline that builds, signs, deploys all publishable modules atomically to Maven Central, and bumps versions to the next SNAPSHOT.

**Independent Test**: Push a version tag and verify the release workflow triggers, executes all steps (GPG import, version set, build+sign+deploy, SNAPSHOT bump), and completes successfully.

**Dependencies**: Requires US1 (release profile must exist for `mvn -P release deploy`) and US4 (version management strategy for SNAPSHOT bump).

### Implementation for User Story 3

- [X] T009 [US3] Create release workflow at `.github/workflows/release.yml` per `specs/004-maven-publish-cicd/contracts/release-workflow.yml` — triggers on tags matching `v*.*.*`, checks out with `RELEASE_TOKEN` and full history, sets up JDK 21 with `server-id: central`, imports GPG key from `GPG_PRIVATE_KEY` secret, sets release version from tag via `mvn versions:set`, runs `mvn -B -P release deploy` with `MAVEN_GPG_PASSPHRASE`/`CENTRAL_USERNAME`/`CENTRAL_PASSWORD` env vars, then bumps to next SNAPSHOT and pushes to main with `[skip ci]` ([#118](https://github.com/basteez/jsf-autoreload/issues/118))

**Checkpoint**: The full release pipeline is in place — tag-to-Central is fully automated.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validation and documentation consistency checks

- [X] T010 [P] Validate `specs/004-maven-publish-cicd/quickstart.md` instructions match actual workflow files and POM configuration — update quickstart if any details diverged during implementation ([#119](https://github.com/basteez/jsf-autoreload/issues/119))
- [X] T011 Run full build `mvn -B verify` to confirm existing tests still pass with all POM changes ([#120](https://github.com/basteez/jsf-autoreload/issues/120))

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (directory exists) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 (POM metadata and version ready)
- **US2 (Phase 4)**: Depends on Phase 1 (workflows directory exists) — independent of US1
- **US4 (Phase 5)**: Depends on Phase 2 (version update done) — independent of US1 and US2
- **US3 (Phase 6)**: Depends on US1 (release profile) and US4 (version management)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2 — no dependencies on other stories
- **User Story 2 (P2)**: Can start after Phase 1 — no dependencies on other stories
- **User Story 4 (P2)**: Can start after Phase 2 — no dependencies on other stories
- **User Story 3 (P3)**: Depends on US1 and US4 completion (release profile + version strategy)

### Within Each User Story

- POM configuration before verification
- Workflows reference contract files for structure
- Each story should be independently verifiable at its checkpoint

### Parallel Opportunities

- **After Phase 2**: US1, US2, and US4 can all proceed in parallel (different files, no dependencies)
  - US1 modifies `pom.xml` and `jsf-autoreload-integration-tests/pom.xml`
  - US2 creates `.github/workflows/ci.yml`
  - US4 verifies child module POMs (read-only)
- **Within Phase 7**: T010 and T011 can run in parallel

---

## Parallel Example: After Foundational Phase

```bash
# These three stories can be launched in parallel after Phase 2:

# Story 1 — Release profile (pom.xml modifications):
Task: T004 "Add release profile to parent pom.xml"
Task: T005 "Configure integration-tests to skip release plugins"
Task: T006 "Verify release artifacts with dry-run"

# Story 2 — CI workflow (new file):
Task: T007 "Create CI workflow at .github/workflows/ci.yml"

# Story 4 — Version verification (read-only):
Task: T008 "Verify child module version inheritance"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (directory structure)
2. Complete Phase 2: Foundational (POM metadata + version update)
3. Complete Phase 3: User Story 1 (release profile)
4. **STOP and VALIDATE**: Run `mvn -B -P release verify -DskipTests` and confirm all artifacts
5. The project can now be published manually from any authorized machine

### Incremental Delivery

1. Setup + Foundational → POM is Central-compliant
2. Add US1 → Local release capability (MVP!)
3. Add US2 → Automated CI on PRs/pushes
4. Add US4 → Version consistency verified
5. Add US3 → Fully automated tag-to-Central release pipeline
6. Polish → Documentation alignment and final validation

### Single Maintainer Strategy (Recommended)

This project has a single maintainer, so sequential execution in priority order is most practical:

1. Complete Phases 1+2 together (Setup + Foundational)
2. Complete US1 (Phase 3) → Validate locally
3. Complete US2 (Phase 4) → Push and verify CI triggers
4. Complete US4 (Phase 5) → Quick verification pass
5. Complete US3 (Phase 6) → Full release automation
6. Complete Phase 7 → Polish

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All contract files are in `specs/004-maven-publish-cicd/contracts/` — use as reference, not copy-paste
- Research decisions (R-001 through R-007) in `research.md` explain the "why" behind each configuration choice
- The `release` profile is intentionally build-time only — local development is unaffected
- CI secrets (`GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `RELEASE_TOKEN`) must be configured manually in GitHub Settings → Secrets → Actions before the release workflow can succeed
