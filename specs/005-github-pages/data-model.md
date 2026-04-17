# Data Model: GitHub Pages Project Site

**Feature**: 005-github-pages  
**Date**: 2026-04-17

## Overview

This feature has no runtime data model — it produces a static documentation site. This document defines the **content model** (site structure and file layout) and the **configuration model** (Jekyll and workflow configuration).

## Content Model

### Site Structure (Single Page Sections)

| Section | Heading Level | Source | Required By |
|---------|--------------|--------|-------------|
| Hero / Overview | h1 | README features list, project description | FR-002 |
| Compatibility | h2 | README compatibility table | FR-002 (SC-002) |
| Modules | h2 | README module structure table | FR-005 |
| Getting Started | h2 | README quick start section | FR-003 |
| Configuration Reference | h2 | README configuration reference | FR-004 |
| Maven Plugin Parameters | h3 (under Configuration) | README Maven plugin parameters | FR-004 |
| Building from Source | h2 | README building from source | SC-002 |

### Navigation Model

```
[In-page TOC at top]
  ├── Compatibility (anchor: #compatibility)
  ├── Modules (anchor: #modules)
  ├── Getting Started (anchor: #getting-started)
  ├── Configuration Reference (anchor: #configuration-reference)
  └── Building from Source (anchor: #building-from-source)

[Top-right aux link]
  └── GitHub → https://github.com/basteez/jsf-autoreload
```

## File Layout Model

```text
docs/
├── _config.yml          # Jekyll configuration (theme, baseurl, aux links)
├── Gemfile              # Ruby dependencies (jekyll, just-the-docs)
├── .gitignore           # Ignore _site/, .jekyll-cache/, .jekyll-metadata
└── index.md             # Single-page site content
```

## Configuration Model

### _config.yml Properties

| Property | Value | Purpose |
|----------|-------|---------|
| `title` | `jsf-autoreload` | Site title in header/sidebar |
| `description` | JSF hot-reload library description | SEO meta description |
| `baseurl` | `/jsf-autoreload` | URL subpath for project site |
| `url` | `https://basteez.github.io` | Base hostname |
| `theme` | `just-the-docs` | Jekyll theme gem |
| `heading_anchors` | `true` | Anchor links on headings |
| `back_to_top` | `true` | Back-to-top link |
| `aux_links` | GitHub repo URL | Top-right navigation link (FR-008) |
| `aux_links_new_tab` | `true` | Open GitHub link in new tab |
| `color_scheme` | `light` | Default color scheme |

### Gemfile Dependencies

| Gem | Version | Purpose |
|-----|---------|---------|
| `jekyll` | `~> 4.4` | Static site generator |
| `just-the-docs` | `0.12.0` | Documentation theme |

### GitHub Actions Workflow

| Setting | Value |
|---------|-------|
| Trigger | `push` to `main`, `workflow_dispatch` |
| Runner | `ubuntu-latest` |
| Ruby version | `3.3` |
| Working directory | `docs` |
| Build output | `docs/_site` |
| Permissions | `contents: read`, `pages: write`, `id-token: write` |
| Environment | `github-pages` |
