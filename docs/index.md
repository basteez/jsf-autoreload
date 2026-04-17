---
layout: default
title: jsf-autoreload
nav_order: 1
---

# jsf-autoreload
{: .fs-9 }

JSF hot-reload plugin — monitors file changes and automatically refreshes the browser via SSE.
{: .fs-6 .fw-300 }

---

- TOC
{:toc}

---

## Key Features

- **XHTML page reload** — detects changes to `.xhtml` Facelets pages and triggers a browser refresh
- **CSS/JS static resource reload** — watches static resources under JSF resource directories
- **Class file reload** — monitors `.class` file changes with configurable debounce to avoid partial reloads
- **Auto-compile on Java source changes** — optionally runs a compile command when `.java` files change
- **SSE-based browser refresh** — pushes reload events to the browser via Server-Sent Events (no polling)

## Compatibility

| Component | Supported |
|-----------|-----------|
| Java | 8+ |
| JSF (javax) | 2.3+ (`javax.faces-api`) |
| Jakarta Faces | 3.0+ (`jakarta.faces-api`) |
| Servlet API | 3.0+ |
| Tomcat | Officially supported |
| Other containers | Servlet 3.0+ containers (e.g., Jetty, WildFly) may work but are untested |

## Getting Started

### 1. Add dependencies

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>it.bstz</groupId>
    <artifactId>jsf-autoreload-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>it.bstz</groupId>
    <artifactId>jsf-autoreload-tomcat</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Add the Maven plugin

Add to your `<build><plugins>` section:

```xml
<plugin>
    <groupId>it.bstz</groupId>
    <artifactId>jsf-autoreload-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>watch</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 3. Run the watch goal

```sh
mvn jsf-autoreload:watch
```

The plugin will monitor your source directory for changes and automatically recompile and refresh the browser.

### javax vs jakarta

The plugin supports both the legacy `javax.faces` and the modern `jakarta.faces` namespaces:

- **javax.faces 2.3+** — use `javax.faces-api` and `javax.servlet-api` dependencies
- **Jakarta Faces 3.0+** — use `jakarta.faces-api` and `jakarta.servlet-api` dependencies

The core library auto-detects the active namespace at runtime via `BridgeDetector` — no manual configuration is required.

## Modules

| Module | ArtifactId | Description |
|--------|------------|-------------|
| Core | `jsf-autoreload-core` | Core library — SSE handler, file watcher, configuration, JSF bridge |
| Tomcat Adapter | `jsf-autoreload-tomcat` | Tomcat container adapter |
| Maven Plugin | `jsf-autoreload-maven-plugin` | Maven plugin — `watch` and `auto-compile` goals |
| Integration Tests | `jsf-autoreload-integration-tests` | End-to-end tests with embedded Tomcat |

## Configuration Reference

### web.xml Context Parameters

All context parameters use the prefix `it.bstz.jsfautoreload.` in `web.xml` and the prefix `jsfautoreload.` as system properties. System properties take precedence over `web.xml` values.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `enabled` | boolean | `true` | Enable/disable the plugin entirely |
| `debounceMs` | long | `500` | Debounce interval (ms) for non-class file changes |
| `classDebounceMs` | long | `1000` | Debounce interval (ms) for class file changes |
| `sseEndpointPath` | String | `/_jsf-autoreload/events` | SSE endpoint path for browser connection |
| `autoCompileEnabled` | boolean | `false` | Enable automatic compilation on Java source changes |
| `autoCompileCommand` | String | *(none)* | Shell command to execute for auto-compile |
| `sourceDirectory` | String | `src/main/java` | Java source directory to watch for auto-compile |
| `watchDirs` | String | *(none)* | Comma-separated additional directories to watch |
| `excludePatterns` | String | *(none)* | Comma-separated glob patterns to exclude from watching |

Example `web.xml` configuration:

```xml
<context-param>
    <param-name>it.bstz.jsfautoreload.debounceMs</param-name>
    <param-value>300</param-value>
</context-param>
```

Or via system property:

```sh
-Djsfautoreload.debounceMs=300
```

### Maven Plugin Parameters

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `sourceDirectory` | `jsf-autoreload.sourceDirectory` | `src/main/java` | Source directory to monitor |
| `compileCommand` | `jsf-autoreload.compileCommand` | `mvn compile` | Compile command to execute on change |
| `autoCompile` | `jsf-autoreload.autoCompile` | `true` | Enable/disable auto-compile goal |

## Building from Source

1. Clone the repository:
   ```sh
   git clone https://github.com/basteez/jsf-autoreload.git
   ```
2. Build all modules:
   ```sh
   mvn clean install
   ```
3. Run unit and integration tests:
   ```sh
   mvn verify
   ```
