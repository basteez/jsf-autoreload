# Tasks: GitHub Pages Project Site

**Input**: Design documents from `/specs/005-github-pages/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Not applicable — this is a documentation-only feature with manual browser verification only (per plan.md).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the Jekyll project skeleton with dependencies and build configuration

- [x] T001 Create docs/Gemfile with Jekyll ~> 4.4 and just-the-docs 0.12.0 gem dependencies in docs/Gemfile ([#122](https://github.com/basteez/jsf-autoreload/issues/122))
- [x] T002 [P] Create docs/.gitignore to exclude _site/, .jekyll-cache/, .jekyll-metadata, .bundle/, and vendor/ in docs/.gitignore ([#123](https://github.com/basteez/jsf-autoreload/issues/123))

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Jekyll configuration that MUST be complete before any site content can be authored

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Create docs/_config.yml with full Jekyll configuration: title (jsf-autoreload), description, baseurl (/jsf-autoreload), url (https://basteez.github.io), theme (just-the-docs), heading_anchors (true), back_to_top (true), color_scheme (light), aux_links (GitHub repo URL), aux_links_new_tab (true) per data-model.md in docs/_config.yml ([#124](https://github.com/basteez/jsf-autoreload/issues/124))

**Checkpoint**: Jekyll skeleton ready — site content authoring can now begin

---

## Phase 3: User Story 1 - Visit the Project Site (Priority: P1) 🎯 MVP

**Goal**: A visitor sees a professional landing page with project description, key features, compatibility, and getting-started information

**Independent Test**: Navigate to the GitHub Pages URL (or local Jekyll serve) and verify the landing page loads with project description, feature highlights, compatibility table, and getting-started instructions

### Implementation for User Story 1

- [x] T004 [US1] Create docs/index.md with YAML front matter (layout: default, title, nav_order: 1), {:toc} table-of-contents directive, and Hero/Overview section containing project name, tagline, and key features list derived from README.md in docs/index.md ([#125](https://github.com/basteez/jsf-autoreload/issues/125))
- [x] T005 [US1] Add Compatibility section to docs/index.md with h2 heading and supported-versions table (Java 8+, JSF javax 2.3+, Jakarta Faces 3.0+, Servlet API 3.0+, Tomcat) derived from README.md in docs/index.md ([#126](https://github.com/basteez/jsf-autoreload/issues/126))
- [x] T006 [US1] Add Getting Started section to docs/index.md with h2 heading, Maven dependency snippet, Maven plugin snippet, javax-vs-jakarta note, and run instructions derived from README.md in docs/index.md ([#127](https://github.com/basteez/jsf-autoreload/issues/127))

**Checkpoint**: At this point, User Story 1 should be fully functional — the site shows a complete landing page with project overview, compatibility info, and getting-started instructions

---

## Phase 4: User Story 2 - Browse Configuration and Usage Documentation (Priority: P2)

**Goal**: A developer finds detailed configuration reference, module descriptions, and usage instructions on the site

**Independent Test**: Navigate to the documentation sections and verify that configuration parameters, module descriptions, and build instructions are present and accurate

### Implementation for User Story 2

- [x] T007 [US2] Add Modules section to docs/index.md with h2 heading and module descriptions table (core, tomcat adapter, maven-plugin, integration-tests) derived from README.md in docs/index.md ([#128](https://github.com/basteez/jsf-autoreload/issues/128))
- [x] T008 [US2] Add Configuration Reference section to docs/index.md with h2 heading, web.xml context parameters table, system property override examples, and Maven Plugin Parameters h3 subsection with parameters table derived from README.md in docs/index.md ([#129](https://github.com/basteez/jsf-autoreload/issues/129))
- [x] T009 [US2] Add Building from Source section to docs/index.md with h2 heading, clone/build/test instructions derived from README.md in docs/index.md ([#130](https://github.com/basteez/jsf-autoreload/issues/130))

**Checkpoint**: At this point, User Stories 1 AND 2 should both work — the site has full landing page content plus detailed documentation sections

---

## Phase 5: User Story 3 - Access the Site from the GitHub Repository (Priority: P3)

**Goal**: Visitors can navigate between the GitHub repository and the project site seamlessly; the site is automatically deployed on push to main

**Independent Test**: Verify the GitHub repo has a link to the Pages site, the site has a link back to the repo (via aux_links), and the deployment workflow runs successfully

### Implementation for User Story 3

- [x] T010 [US3] Create .github/workflows/pages.yml with GitHub Actions deployment workflow: trigger on push to main (paths: docs/**) + workflow_dispatch, Ruby 3.3 setup via ruby/setup-ruby with bundler-cache, Jekyll build via bundle exec jekyll build in docs/ working directory, upload-pages-artifact from docs/_site, deploy-pages to github-pages environment, with permissions contents:read/pages:write/id-token:write per research.md in .github/workflows/pages.yml ([#131](https://github.com/basteez/jsf-autoreload/issues/131))
- [x] T011 [US3] Update README.md to add a link to the GitHub Pages site (https://basteez.github.io/jsf-autoreload/) as the canonical documentation source in README.md ([#132](https://github.com/basteez/jsf-autoreload/issues/132))

**Checkpoint**: All user stories should now be independently functional — the site is deployed, discoverable, and cross-linked with the repository

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation and verification across all user stories

- [x] T012 Verify local Jekyll build succeeds by running bundle install and bundle exec jekyll build in docs/ ([#133](https://github.com/basteez/jsf-autoreload/issues/133))
- [x] T013 Run quickstart.md validation — verify local serve at http://localhost:4000/jsf-autoreload/ renders correctly with all sections and anchor navigation ([#134](https://github.com/basteez/jsf-autoreload/issues/134))

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2)
- **User Story 2 (Phase 4)**: Depends on User Story 1 (Phase 3) — adds content sections to the same docs/index.md file
- **User Story 3 (Phase 5)**: Depends on Foundational (Phase 2) — T010 (workflow) and T011 (README) are independent of site content
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — no dependencies on other stories
- **User Story 2 (P2)**: Depends on US1 completion because T007–T009 append to the same docs/index.md created in T004
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) — T010 creates a new file (.github/workflows/pages.yml), T011 modifies a different file (README.md), so US3 can run in parallel with US1/US2

### Within Each User Story

- Tasks within a story phase are sequential (all modify docs/index.md)
- Commit after each task or logical group

### Parallel Opportunities

- T001 and T002 can run in parallel (different files in docs/)
- US3 (Phase 5) can run in parallel with US1/US2 (Phase 3–4) since it touches different files
- T010 and T011 can run in parallel (different files)

---

## Parallel Example: Setup Phase

```bash
# Launch setup tasks together:
Task: "Create docs/Gemfile with Jekyll and just-the-docs dependencies"
Task: "Create docs/.gitignore for Jekyll build artifacts"
```

## Parallel Example: User Story 3

```bash
# Launch US3 tasks together (while US1/US2 content work proceeds):
Task: "Create .github/workflows/pages.yml deployment workflow"
Task: "Update README.md with GitHub Pages link"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (Gemfile, .gitignore)
2. Complete Phase 2: Foundational (_config.yml)
3. Complete Phase 3: User Story 1 (index.md with overview, compatibility, getting started)
4. **STOP and VALIDATE**: Run `bundle exec jekyll serve` and verify landing page
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Jekyll skeleton ready
2. Add User Story 1 → Verify locally → MVP landing page
3. Add User Story 2 → Verify locally → Full documentation site
4. Add User Story 3 → Deploy workflow + README link → Site is live and discoverable
5. Polish → Validate build and quickstart flow

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 + User Story 2 (sequential — same file)
   - Developer B: User Story 3 (independent files)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- No automated tests — this is a documentation-only feature (manual browser verification per plan.md)
- Content is derived from README.md but maintained independently thereafter
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
