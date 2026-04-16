<!--
  Sync Impact Report
  ====================
  Version change: 0.0.0 → 1.0.0 (initial ratification)
  
  Modified principles: N/A (first version)
  
  Added sections:
    - Core Principles (5 principles)
    - Technical Constraints
    - Development Workflow
    - Governance
  
  Removed sections: N/A
  
  Templates requiring updates:
    - .specify/templates/plan-template.md        ✅ compatible (Constitution Check section is generic)
    - .specify/templates/spec-template.md         ✅ compatible (testing & requirements sections align)
    - .specify/templates/tasks-template.md        ✅ compatible (test-first execution order aligns with TDD principle)
    - .specify/templates/checklist-template.md    ✅ compatible (generic structure)
  
  Follow-up TODOs: none
-->

# JSF Autoreload Constitution

## Core Principles

### I. Plugin Modularity

The plugin MUST be a self-contained JSF library distributable as a
single JAR artifact. It MUST depend only on the JSF specification API
and standard Java SE APIs. No tight coupling to specific application
servers (Tomcat, WildFly, Payara) or JSF implementations
(Mojarra, MyFaces) beyond the public specification interfaces.

**Rationale**: A modular, decoupled plugin can be adopted across any
JSF project without vendor lock-in or classpath conflicts.

### II. Test-Driven Development (NON-NEGOTIABLE)

TDD is mandatory for all feature and bug-fix work. The workflow MUST
follow:

1. Write tests that define expected behavior
2. Confirm tests **fail** (Red)
3. Implement the minimum code to pass (Green)
4. Refactor while keeping tests green (Refactor)

No implementation code may be merged without corresponding tests that
preceded it. Test coverage MUST include unit tests for core logic and
integration tests for JSF lifecycle interactions.

**Rationale**: Strict TDD ensures correctness from the start, prevents
regressions, and produces a living specification of the plugin's
behavior.

### III. JSF Specification Compliance

All functionality MUST comply with the JSF specification (JSF 2.x /
Jakarta Faces 3.x+). The plugin MUST use only standard lifecycle hooks,
resource handling APIs, and component model extensions. Direct use of
implementation-specific internals (e.g., Mojarra `com.sun.faces.*` or
MyFaces `org.apache.myfaces.` internal packages) is prohibited.

**Rationale**: Specification compliance guarantees portability and
forward compatibility as the ecosystem migrates from `javax.faces` to
`jakarta.faces`.

### IV. Zero Production Impact

The plugin MUST have zero impact on production deployments. It MUST
operate exclusively as a development-time dependency. When the plugin
JAR is absent from the classpath or explicitly disabled via
configuration, the host application MUST behave identically to a build
that never included the plugin. The plugin MUST NOT:

- Register persistent servlet filters or listeners in production
- Inject resources into rendered pages outside development mode
- Modify application state or session data

**Rationale**: A development tool that leaks into production creates
performance overhead, security surface, and operational risk.

### V. Observability and Diagnostics

All file-watch events, reload triggers, and JSF lifecycle interactions
MUST be logged with structured output using standard Java logging
(`java.util.logging` or SLF4J). Errors MUST surface actionable
diagnostics including:

- The file path that triggered the reload
- Timestamp of the change detection
- The JSF lifecycle phase affected
- Root cause and suggested remediation for failures

**Rationale**: A hot-reload plugin operates at the boundary between the
filesystem and the JSF runtime; opaque failures waste developer time
and erode trust in the tool.

## Technical Constraints

- **Java version**: Java 8+ (compile with `--release 8`). Single
  artifact runs on Java 8, 11, 17, 21+. Multi-target JDK-optimized
  builds may be introduced via CI/CD if performance profiling warrants
- **JSF versions**: JSF 2.3 (javax.faces) and Jakarta Faces 3.0+
  (jakarta.faces) MUST both be supported or clearly documented as
  separate modules
- **Build tool**: Maven (primary), with Gradle compatibility as a
  non-blocking goal
- **Dependencies**: Minimize third-party dependencies; prefer standard
  library APIs. Any added dependency MUST be justified in the PR
  description
- **Artifact**: Single JAR with no shaded/fat-jar packaging unless
  strictly necessary
- **Compatibility**: The plugin MUST not conflict with common JSF
  component libraries (PrimeFaces, OmniFaces, BootsFaces)

## Development Workflow

- **Branching**: Feature branches follow the naming convention enforced
  by the Specify workflow (`###-feature-name`)
- **Code review**: All changes MUST be reviewed via pull request before
  merging. Reviewers MUST verify TDD compliance (tests exist and were
  written before implementation)
- **Quality gates**:
  - All tests pass (unit + integration)
  - No compiler warnings treated as errors
  - Static analysis clean (SpotBugs / PMD if configured)
- **Commit discipline**: Atomic commits with descriptive messages. Each
  commit MUST leave the build in a passing state
- **Documentation**: Public API changes MUST include updated Javadoc.
  User-facing behavior changes MUST update the relevant spec or README

## Governance

This constitution is the authoritative governance document for the
jsf-autoreload project. It supersedes ad-hoc practices and informal
agreements.

**Amendment procedure**:

1. Propose the change via pull request modifying this file
2. Document the rationale for the change in the PR description
3. Obtain approval from at least one project maintainer
4. Update the version according to semantic versioning (see below)
5. Update `LAST_AMENDED_DATE` to the merge date

**Versioning policy**:

- MAJOR: Backward-incompatible governance changes (principle removal
  or fundamental redefinition)
- MINOR: New principle or section added, or material expansion of
  existing guidance
- PATCH: Clarifications, wording fixes, non-semantic refinements

**Compliance**: All pull requests and code reviews MUST verify
adherence to this constitution. Deviations MUST be justified in the
Complexity Tracking section of the implementation plan.

**Version**: 1.0.1 | **Ratified**: 2026-04-16 | **Last Amended**: 2026-04-16
