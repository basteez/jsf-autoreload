# JSF Autoreload

A Gradle plugin that adds live-reload to JSF applications running on IBM Liberty. Edit your `.xhtml` templates or CSS and see changes instantly in the browser — no manual rebuild or refresh needed.

## How it works

1. **`jsfPrepare`** — configures Liberty for dev mode: disables Facelets template caching, injects a tiny servlet filter into the exploded WAR that adds a reload script to every `.xhtml` response, and sets the WebSocket port in `jvm.options`.
2. **`jsfDev`** — starts a WebSocket server and watches your source directories. When a file changes, it's copied to the exploded WAR directory and a reload signal is broadcast to all connected browsers.

The injected script connects to the WebSocket server and calls `window.location.reload()` when it receives a reload message. It includes reconnect logic with exponential backoff.

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
- Start the file watcher and WebSocket server

Edit any file in `src/main/webapp` and the browser will reload automatically.

Press `Ctrl+C` to stop — this shuts down both the dev server and Liberty.

## Configuration

```groovy
jsfAutoreload {
    port = 35729                        // WebSocket server port
    serverName = 'defaultServer'        // Liberty server name
    outputDir = ''                      // auto-resolved from serverName; set explicitly to override
    watchDirs = ['src/main/webapp']     // directories to watch for changes
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
