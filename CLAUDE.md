# jsf-autoreload Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-16

## Active Technologies
- Java 8+ (compile with `--release 8`) + JSF API (javax.faces 2.3 / jakarta.faces 3.0+), Servlet API 3.0+ (async support) — all `provided` scope (002-fix-reload-bugs)
- N/A (documentation-only feature — no runtime code changes) (003-readme-and-license)
- Java 8+ (compile with `--release 8`); CI matrix: JDK 11 + JDK 21 + JSF API (javax.faces 2.3 / jakarta.faces 3.0+), Servlet API 3.0+, Maven Plugin API — all `provided` scope. New build-only deps: `central-publishing-maven-plugin:0.10.0`, `maven-source-plugin:3.3.1`, `maven-javadoc-plugin:3.11.2`, `maven-gpg-plugin:3.2.7` (004-maven-publish-cicd)

- Java 8+ (compile with --release 8) + JSF API (javax.faces 2.3 / jakarta.faces 3.0+), Servlet API 3.0+ — all `provided` scope (001-jsf-hot-reload)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 8+ (compile with --release 8)

## Code Style

Java 8+ (compile with --release 8): Follow standard conventions

## Recent Changes
- 004-maven-publish-cicd: Added Java 8+ (compile with `--release 8`); CI matrix: JDK 11 + JDK 21 + JSF API (javax.faces 2.3 / jakarta.faces 3.0+), Servlet API 3.0+, Maven Plugin API — all `provided` scope. New build-only deps: `central-publishing-maven-plugin:0.10.0`, `maven-source-plugin:3.3.1`, `maven-javadoc-plugin:3.11.2`, `maven-gpg-plugin:3.2.7`
- 004-maven-publish-cicd: Added [if applicable, e.g., PostgreSQL, CoreData, files or N/A]
- 003-readme-and-license: Added N/A (documentation-only feature — no runtime code changes)


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
