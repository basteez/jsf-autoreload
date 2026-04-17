# Implementation Plan: GitHub Pages Project Site

**Branch**: `005-github-pages` | **Date**: 2026-04-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-github-pages/spec.md`

## Summary

Create a GitHub Pages documentation site for jsf-autoreload using Jekyll with the `just-the-docs` theme. The site is a single page with anchor-based navigation, hosted at `https://basteez.github.io/jsf-autoreload/`, and deployed automatically via GitHub Actions when changes are pushed to `main`. Site content is derived from the existing README (features, compatibility, modules, getting started, configuration reference) but maintained independently.

## Technical Context

**Language/Version**: Ruby 3.3 (Jekyll build only — no runtime code)  
**Primary Dependencies**: Jekyll ~> 4.4, just-the-docs 0.12.0  
**Storage**: N/A (static site, no database)  
**Testing**: Manual browser verification (responsive layout, anchor navigation, content accuracy). No automated tests — this is a documentation-only feature.  
**Target Platform**: GitHub Pages (static hosting)  
**Project Type**: Documentation site (static)  
**Performance Goals**: N/A (static site served by GitHub CDN)  
**Constraints**: Must work with GitHub Pages deployment infrastructure; single-page layout  
**Scale/Scope**: Single page, ~6 content sections derived from existing README

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Plugin Modularity | N/A | Documentation-only feature — no runtime code |
| II. Test-Driven Development | N/A | No implementation code to test. This feature produces only static documentation files (Markdown, YAML, workflow config). The constitution mandates TDD for "feature and bug-fix work" — pure documentation does not fall under this scope. |
| III. JSF Specification Compliance | N/A | Documentation-only feature — no JSF code |
| IV. Zero Production Impact | PASS | No runtime artifact produced. The docs/ directory and Pages workflow have zero impact on the Maven build or published JAR. |
| V. Observability and Diagnostics | N/A | Documentation-only feature — no runtime behavior |

**Gate result**: PASS — no violations.

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Plugin Modularity | N/A | No change — docs/ directory is isolated from Maven modules |
| II. Test-Driven Development | N/A | No change — no implementation code |
| III. JSF Specification Compliance | N/A | No change |
| IV. Zero Production Impact | PASS | Confirmed: docs/ directory excluded from Maven build; Pages workflow triggers only on `main` push; no new Maven dependencies |
| V. Observability and Diagnostics | N/A | No change |

**Post-design gate result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/005-github-pages/
├── plan.md              # This file
├── research.md          # Phase 0 output — technology decisions
├── data-model.md        # Phase 1 output — content and configuration model
├── quickstart.md        # Phase 1 output — local dev and deployment guide
└── tasks.md             # Phase 2 output (created by /speckit-tasks)
```

### Source Code (repository root)

```text
docs/
├── _config.yml          # Jekyll configuration (theme, baseurl, aux links)
├── Gemfile              # Ruby dependencies (jekyll ~> 4.4, just-the-docs 0.12.0)
├── .gitignore           # Ignore _site/, .jekyll-cache/, .jekyll-metadata
└── index.md             # Single-page site content (all sections)

.github/workflows/
└── pages.yml            # GitHub Actions workflow for Jekyll build + Pages deploy
```

**Structure Decision**: Jekyll source files live in `docs/` at the repository root, keeping them separate from the Java project. The GitHub Actions workflow is added alongside existing workflows in `.github/workflows/`. No changes to the Maven module structure — `docs/` is completely isolated from the build.

## Complexity Tracking

> No Constitution Check violations — this section is intentionally empty.
