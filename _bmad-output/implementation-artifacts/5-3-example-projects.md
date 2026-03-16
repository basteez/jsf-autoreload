# Story 5.3: Example Projects

Status: ready-for-dev

## Story

As a developer,
I want working example projects I can clone and run,
So that I can see live-reload in action on my stack before integrating into my own project.

## Acceptance Criteria

1. `examples/liberty-gradle/`: cloneable, runnable Liberty + Gradle example with live-reload working out of the box; README lists exact prerequisites (JDK version, Liberty installation)
2. `examples/tomcat-maven/`: cloneable, runnable Tomcat + Maven example with live-reload working out of the box; README lists exact prerequisites (JDK version, Tomcat installation)
3. Each example: works on fresh clone with no modifications — minimal JSF 2.x WAR with single `.xhtml` page

## Tasks / Subtasks

- [ ] Task 1: Create Liberty + Gradle example (AC: #1, #3)
  - [ ] 1.1 Create `examples/liberty-gradle/` directory
  - [ ] 1.2 Create `build.gradle` (or `.kts`) applying `it.bstz.jsf-autoreload` plugin
  - [ ] 1.3 Create minimal `src/main/webapp/index.xhtml` JSF page
  - [ ] 1.4 Create `src/main/webapp/WEB-INF/web.xml` with JSF servlet mapping
  - [ ] 1.5 Apply `war` and `liberty-gradle-plugin` plugins
  - [ ] 1.6 Create `src/main/liberty/config/server.xml` with minimal Liberty config
  - [ ] 1.7 Create `README.md` with prerequisites and step-by-step instructions
  - [ ] 1.8 Test: clone-and-run on clean environment
- [ ] Task 2: Create Tomcat + Maven example (AC: #2, #3)
  - [ ] 2.1 Create `examples/tomcat-maven/` directory
  - [ ] 2.2 Create `pom.xml` with `jsf-autoreload-maven-plugin` configured
  - [ ] 2.3 Create minimal `src/main/webapp/index.xhtml` JSF page
  - [ ] 2.4 Create `src/main/webapp/WEB-INF/web.xml` with JSF servlet mapping
  - [ ] 2.5 Add Tomcat Maven plugin or document manual Tomcat setup
  - [ ] 2.6 Create `README.md` with prerequisites and step-by-step instructions
  - [ ] 2.7 Test: clone-and-run on clean environment

## Dev Notes

### Example Project Requirements

Each example must be:
- **Minimal**: single `.xhtml` page, minimal config — demonstrate the plugin, not JSF features
- **Self-contained**: no references to the parent project's build
- **Documented**: README with exact prerequisites, step-by-step setup, expected output

### Liberty + Gradle Example Structure

```
examples/liberty-gradle/
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/
│   ├── java/              (empty or minimal backing bean)
│   ├── webapp/
│   │   ├── index.xhtml
│   │   └── WEB-INF/
│   │       └── web.xml
│   └── liberty/config/
│       └── server.xml
```

### Tomcat + Maven Example Structure

```
examples/tomcat-maven/
├── README.md
├── pom.xml
├── src/main/
│   ├── java/              (empty or minimal backing bean)
│   └── webapp/
│       ├── index.xhtml
│       └── WEB-INF/
│           └── web.xml
```

### NFR21: Clone-and-Run

Examples must work on a fresh clone with NO modifications beyond prerequisite JDK and server installation.

### Project Structure Notes

- New: `examples/liberty-gradle/` (complete example project)
- New: `examples/tomcat-maven/` (complete example project)
- Depends on: All previous epics (plugins must be published or locally installable)

### References

- [Source: _bmad-output/planning-artifacts/prd.md#NFR21 — clone-and-run examples]
- [Source: _bmad-output/planning-artifacts/prd.md#Example Projects]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 5.3]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
