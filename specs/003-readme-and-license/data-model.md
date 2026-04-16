# Data Model: README & License for Maven Publishing

**Feature**: 003-readme-and-license | **Date**: 2026-04-16

This feature produces no runtime entities or database models. The "data model" here describes the structure and content of each deliverable file.

## Entity 1: README.md

**Location**: Repository root (`/README.md`)

### Sections (ordered)

| # | Section | Content Source | Maps to |
|---|---------|---------------|---------|
| 1 | Title + tagline | Hardcoded: "jsf-autoreload" + one-line description | FR-001 |
| 2 | Features | Bullet list from plugin capabilities (XHTML, CSS/JS, class reload, auto-compile, SSE) | FR-001 |
| 3 | Compatibility | Table built from `pom.xml` properties + spec FR-004 | FR-004 |
| 4 | Quick Start | Maven dependency XML (`jsf-autoreload-core` + `jsf-autoreload-tomcat`) + plugin XML (`jsf-autoreload-maven-plugin:watch`) | FR-002 |
| 5 | javax vs jakarta | Explanation of namespace distinction + which dependency to use for each | FR-011 |
| 6 | Module Structure | Table: 4 modules with name, artifactId, description | FR-003 |
| 7 | Configuration Reference | Two sub-tables: web.xml context-params (from `ConfigurationReader.java`) and Maven plugin params (from `WatchMojo.java`/`AutoCompileMojo.java`) | FR-005 |
| 8 | Building from Source | 3-step instructions: clone, build (`mvn clean install`), test | FR-006 |
| 9 | License | "Apache License 2.0" + link to LICENSE file | FR-008 |

### Content Rules
- All XML snippets must use the current version `0.1.0-SNAPSHOT` (or a placeholder convention)
- All configuration defaults must exactly match `ConfigurationReader.java` and `PluginConfiguration.Builder` defaults
- The compatibility table must list: Java 8+, JSF 2.3+ (javax.faces), Jakarta Faces 3.0+ (jakarta.faces), Servlet API 3.0+, Tomcat (official) + note about other Servlet 3.0+ containers

## Entity 2: LICENSE

**Location**: Repository root (`/LICENSE`)

### Fields

| Field | Value | Source |
|-------|-------|--------|
| License type | Apache License, Version 2.0 | Spec assumption + FR-010 |
| Copyright year | 2026 | Spec assumption |
| Copyright holder | Tiziano Basile | Spec clarification 2026-04-16 |
| Body text | Standard Apache 2.0 full text (verbatim, no modifications) | apache.org |

### Validation Rules
- File must be named exactly `LICENSE` (no extension)
- Copyright line must appear before the license body
- License text must be the complete, unmodified Apache 2.0 text

## Entity 3: POM Metadata Update

**Location**: `/pom.xml` (parent POM, existing file)

### Elements to Add

| Element | Content | Placement |
|---------|---------|-----------|
| `<url>` | `https://github.com/basteez/jsf-autoreload` | After `<description>` |
| `<licenses>` | Apache License 2.0 with URL and `<distribution>repo</distribution>` | After `<url>` |
| `<developers>` | Tiziano Basile (`basteez`, `tiziano.basile@nearform.com`) | After `<licenses>` |
| `<scm>` | GitHub SSH/HTTPS connection URLs | After `<developers>` |

### Elements Already Present (verify only)
| Element | Current Value | Action |
|---------|--------------|--------|
| `<name>` | "JSF Auto Reload" | Keep as-is |
| `<description>` | "JSF hot-reload plugin — monitors file changes and automatically refreshes the browser via SSE" | Keep as-is |
| `<groupId>` | `it.bstz` | Keep as-is |
| `<artifactId>` | `jsf-autoreload` | Keep as-is |
| `<version>` | `0.1.0-SNAPSHOT` | Keep as-is |

### Validation Rules
- `<licenses>` name must match LICENSE file: "Apache License, Version 2.0"
- `<scm><url>` must match `<url>` element
- All required OSSRH elements must be present for Maven Central publishing validation
