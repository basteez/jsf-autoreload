# Quickstart: JSF Hot Reload Plugin

## Prerequisites

- Java 11+
- Maven 3.6+
- A JSF application using JSF 2.3 (`javax.faces`) or Jakarta Faces 3.0+ (`jakarta.faces`)
- Servlet 3.0+ compatible container (Tomcat 9+, WildFly, Jetty, etc.)

## 1. Add the dependency

Add the core plugin and your container adapter to your WAR's `pom.xml`:

```xml
<!-- Core plugin (required) -->
<dependency>
    <groupId>com.jsfautoreload</groupId>
    <artifactId>jsf-autoreload-core</artifactId>
    <version>${jsf-autoreload.version}</version>
</dependency>

<!-- Tomcat adapter (if running on Tomcat) -->
<dependency>
    <groupId>com.jsfautoreload</groupId>
    <artifactId>jsf-autoreload-tomcat</artifactId>
    <version>${jsf-autoreload.version}</version>
</dependency>
```

## 2. Ensure Development mode

In your `web.xml`, verify the project stage is set to Development:

```xml
<context-param>
    <param-name>javax.faces.PROJECT_STAGE</param-name>
    <!-- or jakarta.faces.PROJECT_STAGE for Jakarta EE -->
    <param-value>Development</param-value>
</context-param>
```

## 3. Run your application

Start your application server as normal. The plugin auto-activates:
- Watches `src/main/webapp` for view and static resource changes
- Watches `target/classes` for compiled class changes
- Injects a reload script into every JSF page
- Opens an SSE endpoint at `/_jsf-autoreload/events`

Open your application in a browser, edit an `.xhtml` file, save — the browser refreshes automatically.

## 4. (Optional) Customize configuration

Add context parameters to `web.xml` for non-standard project layouts:

```xml
<!-- Disable the plugin explicitly -->
<context-param>
    <param-name>com.jsfautoreload.enabled</param-name>
    <param-value>false</param-value>
</context-param>

<!-- Custom watched directories (comma-separated) -->
<context-param>
    <param-name>com.jsfautoreload.watchDirs</param-name>
    <param-value>src/main/webapp,src/main/resources</param-value>
</context-param>

<!-- Custom debounce interval in milliseconds -->
<context-param>
    <param-name>com.jsfautoreload.debounceMs</param-name>
    <param-value>300</param-value>
</context-param>

<!-- Exclusion patterns (comma-separated globs) -->
<context-param>
    <param-name>com.jsfautoreload.excludePatterns</param-name>
    <param-value>**/.git/**,**/node_modules/**</param-value>
</context-param>
```

## 5. (Optional) Enable auto-compile

For a Quarkus-like experience where `.java` changes are automatically compiled:

```xml
<!-- In pom.xml build plugins section -->
<plugin>
    <groupId>com.jsfautoreload</groupId>
    <artifactId>jsf-autoreload-maven-plugin</artifactId>
    <version>${jsf-autoreload.version}</version>
    <configuration>
        <autoCompile>true</autoCompile>
        <sourceDirectory>src/main/java</sourceDirectory>
    </configuration>
</plugin>
```

## Production safety

The plugin is completely inert when:
- `javax.faces.PROJECT_STAGE` / `jakarta.faces.PROJECT_STAGE` is not `Development`
- `com.jsfautoreload.enabled` is set to `false`

No servlets are registered, no file watchers start, no scripts are injected. Zero overhead.

## What happens on file change

| File type | What the plugin does | Expected latency |
|-----------|---------------------|-----------------|
| `.xhtml`, `.jspx`, `.jsp` | Browser refresh | < 3 seconds |
| `.css`, `.js`, images | Browser refresh | < 3 seconds |
| `.class` | Context reload + browser refresh | < 5 seconds |
| `.java` (auto-compile on) | Compile + context reload + browser refresh | Depends on build |
