# Research: README & License for Maven Publishing

**Feature**: 003-readme-and-license | **Date**: 2026-04-16

## Research Task 1: License Selection for Maven Central

**Question**: What license is appropriate for a Java library targeting Maven Central?

**Decision**: Apache License 2.0

**Rationale**:
- OSI-approved, satisfying Maven Central's requirement (FR-010)
- Most widely used license for Java libraries on Maven Central (Spring, Guava, etc.)
- Includes an explicit patent grant, which corporate adopters prefer
- Requires attribution but permits commercial use, modification, and redistribution
- Compatible with most corporate policies and other open-source licenses
- Already confirmed by the user during spec clarification

**Alternatives considered**:
- MIT License: simpler text, widely permissive, but lacks explicit patent grant — less common for Java libraries
- EPL-2.0: used by Eclipse ecosystem, but secondary linking provisions can complicate adoption in non-Eclipse projects
- LGPL-2.1: copyleft applies to modifications of the library itself, which may deter some corporate users

## Research Task 2: Maven Central POM Metadata Requirements

**Question**: What POM metadata is required for Maven Central publishing?

**Decision**: The parent pom.xml must include `<licenses>`, `<developers>`, `<scm>`, `<url>`, and `<description>` elements.

**Rationale**:
- Maven Central (via Sonatype OSSRH) requires specific POM metadata for artifact validation:
  - `<groupId>`, `<artifactId>`, `<version>` — already present
  - `<name>` — already present ("JSF Auto Reload")
  - `<description>` — already present but should be verified/updated
  - `<url>` — **missing** — must point to project homepage (GitHub repo)
  - `<licenses>` — **missing** — must declare license name, URL, and distribution
  - `<developers>` — **missing** — must list at least one developer with id, name, email
  - `<scm>` — **missing** — must include connection, developerConnection, url

**Specific values to add**:
```xml
<url>https://github.com/basteez/jsf-autoreload</url>

<licenses>
    <license>
        <name>Apache License, Version 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
    </license>
</licenses>

<developers>
    <developer>
        <id>basteez</id>
        <name>Tiziano Basile</name>
        <email>tiziano.basile@nearform.com</email>
    </developer>
</developers>

<scm>
    <connection>scm:git:git://github.com/basteez/jsf-autoreload.git</connection>
    <developerConnection>scm:git:ssh://github.com:basteez/jsf-autoreload.git</developerConnection>
    <url>https://github.com/basteez/jsf-autoreload</url>
</scm>
```

**Alternatives considered**:
- Minimal POM (only `<licenses>`): would fail OSSRH validation — `<developers>` and `<scm>` are also mandatory
- Using a Sonatype parent POM: adds unnecessary dependency; all required elements can be declared directly

## Research Task 3: README Structure Best Practices for Java Libraries

**Question**: What sections should a README include for a Java library targeting Maven Central?

**Decision**: Use a standard structure covering: intro, badges (future), quick-start, module overview, compatibility, configuration reference, build instructions, license.

**Rationale**:
- Aligns with the feature spec's requirements (FR-001 through FR-011)
- Follows conventions of established Java libraries (Spring Boot, Micronaut, Quarkus plugins)
- Prioritizes copy-paste-ready code snippets (SC-002) over prose explanations

**README section outline**:
1. **Title + one-line description** — what the plugin does
2. **Features** — bullet list of capabilities (XHTML, static resources, class reload, auto-compile)
3. **Compatibility** — table: Java versions, JSF versions (javax vs jakarta), Servlet API, containers
4. **Quick Start** — Maven dependency snippet + plugin configuration + run command
5. **Module Structure** — table describing each of the 4 modules
6. **Configuration Reference** — full table of `web.xml` context-params and Maven plugin params
7. **javax vs jakarta** — explicit section addressing namespace distinction (FR-011)
8. **Building from Source** — clone, build, test commands (max 3 steps per SC-004)
9. **License** — name + link to LICENSE file

**Alternatives considered**:
- Single long-form narrative: harder to scan, doesn't serve the "10-minute setup" success criterion (SC-001)
- Separate documentation site (GitHub Pages/Docusaurus): overkill for a focused plugin; README is the right scope

## Research Task 4: Apache License 2.0 — LICENSE File Content

**Question**: What is the correct content for the LICENSE file?

**Decision**: Use the standard Apache License 2.0 full text with the copyright notice customized for "2026 Tiziano Basile".

**Rationale**:
- The Apache Software Foundation provides a standard boilerplate
- The copyright line at the top is the only customizable part: `Copyright 2026 Tiziano Basile`
- The rest of the file is the verbatim Apache 2.0 text (no modifications allowed)

**Alternatives considered**: N/A — the license text is standardized.
