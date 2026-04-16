# Quickstart: README & License for Maven Publishing

**Feature**: 003-readme-and-license | **Date**: 2026-04-16

## What This Feature Does

Adds three deliverables to the jsf-autoreload project:
1. **README.md** — comprehensive project documentation for users and contributors
2. **LICENSE** — Apache License 2.0 full text
3. **pom.xml update** — Maven Central required metadata (`<licenses>`, `<developers>`, `<scm>`, `<url>`)

## Prerequisites

- Git (for working on the feature branch)
- A text editor
- Familiarity with the jsf-autoreload project structure (4 modules: core, tomcat, maven-plugin, integration-tests)

No build tools required — this feature produces no compiled code.

## Key Files to Reference

When writing or reviewing the README content, these source files are the authoritative references:

| What | File |
|------|------|
| Configuration options (web.xml) | `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/ConfigurationReader.java` |
| Configuration defaults | `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/config/PluginConfiguration.java` |
| Maven plugin `watch` goal | `jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/maven/WatchMojo.java` |
| Maven plugin `auto-compile` goal | `jsf-autoreload-maven-plugin/src/main/java/it/bstz/jsfautoreload/maven/AutoCompileMojo.java` |
| Parent POM (metadata target) | `pom.xml` |
| Integration test examples | `jsf-autoreload-integration-tests/src/test/java/it/bstz/jsfautoreload/it/` |

## Verification Checklist

After implementation, verify:

- [ ] `README.md` exists at repository root
- [ ] `LICENSE` exists at repository root with Apache 2.0 text and correct copyright
- [ ] `pom.xml` contains `<licenses>`, `<developers>`, `<scm>`, `<url>` elements
- [ ] README configuration table matches `ConfigurationReader.java` exactly
- [ ] README module table covers all 4 modules
- [ ] README code snippets are syntactically valid XML
- [ ] README build instructions work in 3 steps or fewer
- [ ] License name in POM matches LICENSE file
