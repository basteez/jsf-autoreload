# Story 5.2: README with Quick Start & Compatibility Matrix

Status: ready-for-dev

## Story

As a developer evaluating jsf-autoreload,
I want a README with a GIF demo, 3-line quick start, and compatibility matrix,
So that I can determine if my stack is supported and get started in under 5 minutes.

## Acceptance Criteria

1. Compatibility matrix table: developer determines stack support (server, build tool, JSF implementation) within 30 seconds
2. Quick Start section: zero to working live-reload in under 5 minutes with 3-line Gradle setup or Maven snippet
3. GIF demo showing live-reload experience (file save -> browser refresh)
4. Configuration reference section: all DSL/configuration options (port, serverName, outputDir, watchDirs) with defaults

## Tasks / Subtasks

- [ ] Task 1: Write compatibility matrix (AC: #1)
  - [ ] 1.1 Create table: Servers (Liberty, Tomcat) x Build Tools (Gradle, Maven) x JSF Implementations (Mojarra, MyFaces)
  - [ ] 1.2 Mark supported combinations
  - [ ] 1.3 Note extensibility via ServerAdapter API
- [ ] Task 2: Write Quick Start section (AC: #2)
  - [ ] 2.1 Gradle quick start: 3 lines (apply plugin, run `gradle jsfDev`)
  - [ ] 2.2 Maven quick start: equivalent pom.xml snippet + `mvn jsf-autoreload:dev`
  - [ ] 2.3 Prerequisites: JDK 11+, application server running
- [ ] Task 3: Create or source GIF demo (AC: #3)
  - [ ] 3.1 Record or generate a GIF showing: edit .xhtml -> save -> browser auto-refreshes
  - [ ] 3.2 Place in repo (e.g., `docs/demo.gif`) and reference in README
- [ ] Task 4: Write configuration reference (AC: #4)
  - [ ] 4.1 Document all options: port (default 35729), serverName (default "defaultServer"), outputDir, watchDirs (default ["src/main/webapp"])
  - [ ] 4.2 Show Gradle DSL and Maven XML examples
- [ ] Task 5: Structure the README (AC: #1-#4)
  - [ ] 5.1 Sections: tagline, GIF demo, compatibility matrix, quick start (Gradle + Maven), configuration reference, how it works, contributing link
  - [ ] 5.2 Replace existing README.md content

## Dev Notes

### Existing README

The current README.md (154 lines) exists but is for v0.1-beta. It needs to be rewritten for v1.0 with the expanded feature set (4 modules, 2 servers, 2 build tools).

### README Structure (Recommended)

1. **Tagline**: "Edit, save, see it live. Live-reload for JSF."
2. **GIF Demo**: Visual proof of the value proposition
3. **Compatibility Matrix**: Table showing supported combinations
4. **Quick Start - Gradle**: 3-line setup
5. **Quick Start - Maven**: Equivalent setup
6. **Configuration**: All options with defaults
7. **How It Works**: Brief architecture overview
8. **Contributing**: Link to CONTRIBUTING.md
9. **License**

### NFR Targets

- NFR19: Stack assessment within 30 seconds -> compatibility matrix must be near the top
- NFR20: Zero to live-reload in under 5 minutes -> quick start must be minimal and clear

### Project Structure Notes

- Modified: `README.md` (complete rewrite)
- New: `docs/demo.gif` (or similar location for demo asset)
- Depends on: All previous epics (features must exist to document)

### References

- [Source: _bmad-output/planning-artifacts/prd.md#NFR19 — 30-second stack assessment]
- [Source: _bmad-output/planning-artifacts/prd.md#NFR20 — 5-minute setup]
- [Source: _bmad-output/planning-artifacts/prd.md#Documentation Strategy]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 5.2]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
