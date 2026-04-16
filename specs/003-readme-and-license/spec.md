# Feature Specification: README & License for Maven Publishing

**Feature Branch**: `003-readme-and-license`  
**Created**: 2026-04-16  
**Status**: Draft  
**Input**: User description: "Need to update the README to help other developers understand how the plugin works. Also we need to choose a license before publishing on Maven."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Developer Discovers and Understands the Plugin (Priority: P1)

A Java/JSF developer finds jsf-autoreload on Maven Central or GitHub. They visit the repository and read the README to understand what the plugin does, whether it fits their project, and how to get started. Within a few minutes of reading, they have a clear mental model of the plugin's purpose and capabilities.

**Why this priority**: The README is the first point of contact for potential users. Without clear documentation, even the best plugin will not be adopted. This is the prerequisite for all other user stories.

**Independent Test**: Can be tested by having a developer unfamiliar with the project read the README and explain back what the plugin does, what it supports, and how to set it up.

**Acceptance Scenarios**:

1. **Given** a developer visits the repository, **When** they read the README, **Then** they can describe the plugin's purpose (hot-reload for JSF projects) and its key capabilities (XHTML, static resources, class files, auto-compile) without looking at source code.
2. **Given** a developer reads the README, **When** they look for compatibility information, **Then** they can identify the supported JSF versions (javax.faces 2.3+, jakarta.faces 3.0+), Servlet API versions (3.0+), and Java version (8+).
3. **Given** a developer reads the README, **When** they look for the project's module structure, **Then** they can understand what each module does (core, tomcat adapter, maven plugin, integration tests).

---

### User Story 2 - Developer Adds the Plugin to Their Project (Priority: P2)

A developer decides to use jsf-autoreload in their existing JSF project. They follow the README's setup instructions to add the Maven dependency and configure the Maven plugin goal. They start their development server and experience hot-reload working.

**Why this priority**: After understanding the plugin, the immediate next step is integration. Clear, copy-paste-ready setup instructions are essential for adoption and reduce friction to zero.

**Independent Test**: Can be tested by following the README instructions in a fresh JSF project and verifying the plugin activates successfully.

**Acceptance Scenarios**:

1. **Given** a developer has a JSF project, **When** they follow the README's setup instructions, **Then** they can add the correct Maven dependency and plugin configuration to their `pom.xml`.
2. **Given** a developer has configured the plugin, **When** they follow the README's usage instructions, **Then** they can start the watch goal and see hot-reload working for XHTML changes.
3. **Given** a developer wants to customize behavior, **When** they consult the README's configuration section, **Then** they find a reference of available configuration options with descriptions and defaults.

---

### User Story 3 - Developer Understands the License Terms (Priority: P3)

A developer or their organization's legal team evaluates jsf-autoreload for use in a commercial project. They check the repository for a license file to determine whether the plugin's terms are compatible with their project's licensing requirements.

**Why this priority**: A clear, recognized open-source license is a hard requirement for Maven Central publishing and for adoption by organizations that need legal clarity before using third-party dependencies.

**Independent Test**: Can be tested by verifying a LICENSE file exists at the repository root and that the license is referenced in both the README and the Maven POM metadata.

**Acceptance Scenarios**:

1. **Given** a developer visits the repository, **When** they look for licensing information, **Then** they find a LICENSE file at the repository root with a recognized open-source license.
2. **Given** a developer reads the README, **When** they look for license information, **Then** the README states the license name and links to the LICENSE file.
3. **Given** an organization evaluates the plugin, **When** they inspect the Maven POM metadata, **Then** the `<licenses>`, `<developers>`, `<scm>`, `<url>`, and `<description>` sections are all present and correct.

---

### User Story 4 - Contributor Wants to Help (Priority: P4)

A developer wants to contribute to jsf-autoreload (bug fix, feature, documentation). They read the README to understand how to build the project locally and how to run the test suite.

**Why this priority**: Encouraging contributions grows the project. Basic build and test instructions lower the barrier for new contributors.

**Independent Test**: Can be tested by following the README's build instructions on a clean machine with only Java and Maven installed, and verifying the project compiles and tests pass.

**Acceptance Scenarios**:

1. **Given** a developer clones the repository, **When** they follow the build instructions in the README, **Then** the project compiles successfully.
2. **Given** a developer has built the project, **When** they follow the test instructions in the README, **Then** unit tests and integration tests execute and the results are reported.
3. **Given** a developer wants to propose a new feature, **When** they read the contributor section, **Then** they understand the project uses Speckit for feature planning and know that the full workflow (spec → plan → tasks → implement) is expected for new features, while small contributions (bug fixes, docs) can skip it.

---

### Edge Cases

- **Unsupported JSF version**: The README's compatibility section clearly lists supported versions (javax.faces 2.3+, jakarta.faces 3.0+). Older versions (e.g., JSF 1.x) are implicitly unsupported; no special handling beyond the version listing is needed.
- **javax vs jakarta namespace**: FR-011 requires the README to address this distinction explicitly, helping developers choose the right dependency based on their JSF version.
- **Non-Maven build systems**: Gradle is explicitly out of scope (see Assumptions). The README focuses on Maven; no Gradle instructions are provided.
- **Other Servlet containers**: Tomcat is the only officially supported container. The README notes that other Servlet 3.0+ containers may work but are untested.

## Clarifications

### Session 2026-04-16

- Q: Who should be listed as the copyright holder in the Apache 2.0 LICENSE file? → A: Tiziano Basile
- Q: Should this feature include all required Maven Central POM metadata beyond `<licenses>`? → A: Yes — include `<developers>`, `<scm>`, `<url>`, and `<description>` as well
- Q: Should the README list only Tomcat or also mention other Servlet 3.0+ containers? → A: Tomcat officially supported; note other Servlet 3.0+ containers may work but are untested

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The repository MUST contain a README file at the project root that introduces the plugin's purpose and capabilities.
- **FR-002**: The README MUST include a quick-start section with copy-paste-ready Maven dependency and plugin configuration snippets.
- **FR-003**: The README MUST document the project's module structure and explain each module's role.
- **FR-004**: The README MUST list supported environments (Java versions, JSF versions, Servlet API versions, supported containers). Tomcat is the officially supported container; the README should note that other Servlet 3.0+ containers (e.g., Jetty, WildFly) may work but are untested.
- **FR-005**: The README MUST include a configuration reference listing all available options, their descriptions, and default values.
- **FR-006**: The README MUST include build-from-source and test-running instructions for contributors.
- **FR-007**: The repository MUST contain a LICENSE file at the project root with the full text of the chosen open-source license.
- **FR-008**: The README MUST state the license name and reference the LICENSE file.
- **FR-009**: The Maven POM MUST include a `<licenses>` section with the correct license name and URL.
- **FR-009a**: The Maven POM MUST include a `<developers>` section with the project author's name and contact information.
- **FR-009b**: The Maven POM MUST include `<scm>` connection details pointing to the GitHub repository.
- **FR-009c**: The Maven POM MUST include a `<url>` element pointing to the project's GitHub page.
- **FR-009d**: The Maven POM MUST include a `<description>` element summarizing the plugin's purpose.
- **FR-010**: The chosen license MUST be compatible with Maven Central publishing requirements (OSI-approved).
- **FR-011**: The README MUST address the javax.faces vs jakarta.faces namespace distinction so developers can choose the right dependency for their project.
- **FR-012**: The README MUST include a "Development Workflow" subsection in the contributor section that explains the project uses Speckit for feature planning (spec → plan → tasks → implement), links to the tool, and clarifies that the full workflow is optional for small contributions (bug fixes, doc improvements) but expected for new features.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer unfamiliar with the project can set up the plugin in their JSF application within 10 minutes by following only the README.
- **SC-002**: 100% of README code snippets are syntactically valid and copy-paste-ready (no placeholder-only blocks that require interpretation).
- **SC-003**: The LICENSE file is present, contains a recognized OSI-approved license, and matches what is declared in the POM and README.
- **SC-004**: A contributor can build the project and run all tests by following the README instructions in 3 steps or fewer.
- **SC-005**: The README covers all 4 modules of the project with a description of each module's purpose.

## Assumptions

- The target audience is Java developers already familiar with Maven and JSF concepts; the README does not need to explain JSF or Maven basics.
- The project will be published to Maven Central, which requires an OSI-approved license, a `<licenses>` POM element, and proper metadata.
- The README will use English as the primary language.
- Gradle support is out of scope for this feature; the README will focus on Maven-based setup only.
- The chosen license is **Apache License 2.0**, the most common license for Java libraries on Maven Central. It includes a patent grant and requires attribution, and is compatible with most corporate policies.
- The copyright holder for the LICENSE file is **Tiziano Basile** (repository owner and sole author). The copyright year is **2026**.
