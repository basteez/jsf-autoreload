# Quickstart: GitHub Pages Project Site

**Feature**: 005-github-pages  
**Date**: 2026-04-17

## Prerequisites

- Ruby 3.x installed locally (for local preview; optional)
- GitHub repository with Pages enabled (Settings → Pages → Source: GitHub Actions)
- Write access to push to `main` branch

## Local Development

1. Navigate to the docs directory:
   ```sh
   cd docs
   ```

2. Install dependencies:
   ```sh
   bundle install
   ```

3. Serve locally:
   ```sh
   bundle exec jekyll serve
   ```

4. Open `http://localhost:4000/jsf-autoreload/` in a browser.

## Deployment

The site deploys automatically when changes under `docs/` are pushed to `main`. The GitHub Actions workflow:

1. Checks out the repository
2. Installs Ruby and runs `bundle install` (cached)
3. Builds the Jekyll site with `bundle exec jekyll build`
4. Uploads the built site as a Pages artifact
5. Deploys to `https://basteez.github.io/jsf-autoreload/`

## File Overview

| File | Purpose |
|------|---------|
| `docs/_config.yml` | Jekyll configuration — theme, title, baseurl, navigation links |
| `docs/Gemfile` | Ruby gem dependencies — jekyll and just-the-docs theme |
| `docs/index.md` | All site content — single page with anchor-based navigation |
| `docs/.gitignore` | Excludes `_site/`, `.jekyll-cache/`, `.jekyll-metadata` |
| `.github/workflows/pages.yml` | GitHub Actions workflow for automated deployment |

## Modifying Content

Edit `docs/index.md` to update the site content. The page uses standard Markdown with `just-the-docs` extensions:

- Headings (`##`) automatically become anchor navigation targets
- The `{:toc}` directive generates the table of contents
- Code blocks support syntax highlighting via Rouge
- Tables, admonitions, and other Kramdown features are supported

After editing, commit and push to `main` to deploy, or preview locally with `bundle exec jekyll serve`.
