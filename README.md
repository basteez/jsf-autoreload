# JSF Autoreload

A Gradle plugin that adds live-reload to JSF applications running on IBM Liberty. Edit `.xhtml` templates, CSS, JS, or Java classes and see changes instantly in the browser — no manual rebuild or refresh needed.

## How it works

1. **`jsfPrepare`** — configures Liberty for dev mode: disables Facelets template caching via `<context-param>` injection into `web.xml`, injects a tiny servlet filter into the exploded WAR that adds a reload script to every `.xhtml` response, and sets the WebSocket port in `jvm.options`.
2. **`jsfDev`** — starts a WebSocket server and watches your source directories. When a webapp file changes (`.xhtml`, CSS, JS), it's copied to the exploded WAR and a reload signal is broadcast. When a Java source file changes, it's automatically recompiled with `javac`, the compiled classes are synced to `WEB-INF/classes`, Liberty detects the update and restarts the app, and the browser reloads.

The injected script connects to the WebSocket server and calls `window.location.reload()` when it receives a reload message. It includes reconnect logic with exponential backoff.

### What gets live-reloaded

| Resource type | How it works |
|---|---|
| `.xhtml` templates | Copied to exploded WAR, Facelets cache disabled — instant |
| CSS/JS (static or JSF resources) | Copied to exploded WAR, browser reloads |
| Java classes | Recompiled with `javac`, synced to `WEB-INF/classes`, Liberty restarts app (~1-2s) |

## Requirements

- Java 11+
- Gradle 7+
- [Liberty Gradle Plugin](https://github.com/OpenLiberty/ci.gradle) (`io.openliberty.tools.gradle.Liberty`)
- Exploded WAR deployment (not a packaged `.war` file)

## Setup

### 1. Apply the plugin

```groovy
plugins {
    id 'war'
    id 'io.openliberty.tools.gradle.Liberty' version '3.9.0'
    id 'it.bstz.jsf-autoreload' version '0.1.0-SNAPSHOT'
}
```

### 2. Configure exploded WAR deployment

The plugin needs an exploded WAR directory to copy changed files into. Create an `explodeWar` task and wire it up:

```groovy
liberty {
    server {
        name = 'defaultServer'
        looseApplication = false
        deploy {
            apps = []  // disable Liberty's own deploy
        }
    }
}

tasks.named('deploy') {
    enabled = false
}

tasks.register('explodeWar', Copy) {
    dependsOn war
    from zipTree(war.archiveFile)
    into "${buildDir}/wlp/usr/servers/defaultServer/apps/expanded/${project.name}.war"
}

tasks.named('jsfPrepare') {
    dependsOn 'explodeWar'
}
```

### 3. Configure Liberty's `server.xml`

Point your `<webApplication>` to the exploded directory:

```xml
<server>
    <featureManager>
        <feature>jsf-2.3</feature>
    </featureManager>

    <httpEndpoint id="defaultHttpEndpoint"
                  httpPort="9080"
                  httpsPort="9443" />

    <webApplication location="expanded/your-app-name.war"
                    type="war"
                    contextRoot="/" />
</server>
```

### 4. Run

```bash
./gradlew jsfDev
```

This will:
- Build the WAR and explode it
- Create the Liberty server and install features
- Inject the runtime filter and configure facelets refresh
- Start Liberty
- Start file watchers (webapp resources + Java sources) and WebSocket server

Edit any file in `src/main/webapp` or `src/main/java` and the browser will reload automatically.

Press `Ctrl+C` to stop — this shuts down both the dev server and Liberty.

## Configuration

```groovy
jsfAutoreload {
    port = 35729                        // WebSocket server port
    serverName = 'defaultServer'        // Liberty server name
    outputDir = ''                      // auto-resolved from serverName; set explicitly to override
    watchDirs = ['src/main/webapp']     // directories to watch for webapp resource changes
    watchClasses = true                 // watch Java sources, recompile, and sync classes
}
```

## Project structure

```
jsf-autoreload/
├── jsf-autoreload-plugin/     # Gradle plugin (jsfPrepare + jsfDev tasks)
│   └── src/main/java/
│       └── it/bstz/jsfautoreload/
│           ├── JsfAutoreloadPlugin.java
│           ├── JsfAutoreloadExtension.java
│           ├── JsfPrepareTask.java
│           ├── JsfDevTask.java
│           ├── compiler/JavaSourceCompiler.java
│           ├── watcher/FileChangeWatcher.java
│           ├── websocket/DevWebSocketServer.java
│           └── server/
│               ├── ServerAdapter.java
│               └── liberty/LibertyServerAdapter.java
├── jsf-autoreload-runtime/    # Servlet filter JAR (injected into WAR)
│   └── src/main/
│       ├── java/.../filter/DevModeFilter.java
│       └── resources/META-INF/web-fragment.xml
├── settings.gradle.kts
└── build.gradle.kts
```

## Building from source

```bash
./gradlew publishToMavenLocal
```

This publishes the plugin to your local Maven repository so it can be consumed by test projects.

## License

MIT
