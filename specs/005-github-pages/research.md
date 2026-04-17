# Research: GitHub Pages Project Site

**Feature**: 005-github-pages  
**Date**: 2026-04-17

## R-001: Static Site Generator & Theme

**Decision**: Jekyll with `just-the-docs` theme (gem-based, version 0.12.0)

**Rationale**: The spec mandates Jekyll with `just-the-docs` (FR-010). The gem-based approach (vs `remote_theme`) provides version pinning, faster builds, and local development support. The official `just-the-docs` template repository uses this approach.

**Alternatives considered**:
- `remote_theme` directive — rejected because it always pulls latest (or a pinned ref), has slower builds (downloads theme each time), and is harder to debug locally
- Other themes (Minimal Mistakes, Cayman) — out of scope per spec

## R-002: Single-Page Layout with Anchor Navigation

**Decision**: Use a single `index.md` file with Kramdown's `{:toc}` directive for in-page anchor navigation

**Rationale**: `just-the-docs` generates `id` attributes on all headings automatically. The `{:toc}` directive produces an ordered/unordered list of anchor links to all headings on the page. With `heading_anchors: true` in `_config.yml`, each heading also gets a hover-able anchor icon. Since there is only one page, the left sidebar shows just the site title — the in-page TOC provides all navigation.

**Alternatives considered**:
- Multi-page site with sidebar navigation — rejected by spec (FR-007: single-page with anchor-based navigation)
- `nav_enabled: false` to hide sidebar entirely — considered but keeping it provides the GitHub link and site title

## R-003: GitHub Actions Deployment Workflow

**Decision**: Gem-based Jekyll build using Ruby setup + `bundle exec jekyll build`, deployed via `actions/deploy-pages`

**Rationale**: The recommended approach from the `just-the-docs` template repo. Uses `ruby/setup-ruby@v1` with bundler caching for fast builds. The `actions/jekyll-build-pages` alternative does not support custom gem themes — only `remote_theme` works with it.

**Workflow actions**:
- `actions/checkout@v4`
- `ruby/setup-ruby@v1` (Ruby 3.3, bundler-cache enabled)
- `actions/configure-pages@v5`
- `actions/upload-pages-artifact@v4`
- `actions/deploy-pages@v5`

**Trigger**: Push to `main` branch + `workflow_dispatch` for manual runs

**Required permissions**: `contents: read`, `pages: write`, `id-token: write`

**Alternatives considered**:
- `actions/jekyll-build-pages` (simpler, no Gemfile) — rejected because it does not support gem-based themes like `just-the-docs`
- GitHub Pages native build from branch — rejected because GitHub now recommends Actions-based deployment, and it provides more control over the build process

## R-004: Site Source Location

**Decision**: Place Jekyll source files in a `docs/` directory at the repository root

**Rationale**: Keeps documentation separate from source code. The GitHub Actions workflow uses `working-directory: docs` and `upload-pages-artifact` with `path: docs/_site`. This is a common pattern for project sites that coexist with source code.

**Alternatives considered**:
- Repository root — rejected because it would mix Jekyll config files (`_config.yml`, `Gemfile`) with Java project files (`pom.xml`)
- Separate `gh-pages` branch — rejected because maintaining a separate branch adds complexity and the spec calls for deployment from `main`

## R-005: Content Strategy

**Decision**: Derive initial site content from the existing README, structured as sections in a single page

**Rationale**: The README already contains comprehensive documentation (features, compatibility, quick start, configuration reference, module structure). The site reformats this content for better presentation with the `just-the-docs` theme. Per the spec, the site and README are maintained independently after initial creation.

**Site sections** (mapped from README):
1. Hero / Overview — project name, tagline, key features
2. Compatibility — supported versions table
3. Modules — module descriptions table
4. Getting Started — dependency and plugin configuration
5. Configuration Reference — web.xml parameters and Maven plugin parameters
6. Building from Source — clone, build, test instructions

**Alternatives considered**:
- Auto-generate site from README — rejected by spec (maintained independently)
- Minimal landing page linking to README — rejected by spec (FR-002 through FR-005 require full content on the site)
