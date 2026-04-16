# Contract: Container Adapter SPI

**Type**: Java Service Provider Interface
**Module**: Defined in `jsf-autoreload-core`, implemented by adapter modules
**Audience**: Container adapter developers (internal and third-party)

## Interface Definition

```java
package com.jsfautoreload.spi;

/**
 * Service Provider Interface for container-specific operations.
 *
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * Each adapter module must include a
 * {@code META-INF/services/com.jsfautoreload.spi.ContainerAdapter} file
 * listing its implementation class.
 *
 * The first adapter where {@link #supports()} returns {@code true}
 * is selected. If no adapter matches, context reload is not available
 * and the plugin logs a WARNING (browser refresh still works for
 * view/static changes).
 */
public interface ContainerAdapter {

    /**
     * Returns true if this adapter can handle the current servlet container.
     * Called once during plugin initialization.
     *
     * Implementations should detect the container by checking for
     * container-specific classes on the classpath (e.g.,
     * {@code org.apache.catalina.Context} for Tomcat).
     *
     * @return true if this adapter is compatible with the running container
     */
    boolean supports();

    /**
     * Triggers a webapp context reload so that updated class files
     * take effect.
     *
     * This method is called after the debounce window for CLASS-category
     * file changes. It MUST be synchronous — the caller waits for it
     * to complete before broadcasting the SSE reload notification.
     *
     * After this method returns, the servlet context will have been
     * reloaded. Existing SSE connections will be dropped; clients
     * auto-reconnect via EventSource.
     *
     * @param context the current servlet context (bridge-abstracted)
     * @throws ReloadException if the reload fails
     */
    void reload(Object context) throws ReloadException;

    /**
     * Returns a human-readable name for the container, used in
     * log messages.
     *
     * @return container name (e.g., "Apache Tomcat 9.0")
     */
    String containerName();

    /**
     * Returns the priority of this adapter. Lower values = higher priority.
     * Used to resolve conflicts when multiple adapters report support.
     * Default: 100.
     *
     * @return priority value
     */
    default int priority() {
        return 100;
    }
}
```

## Exception

```java
package com.jsfautoreload.spi;

public class ReloadException extends Exception {
    public ReloadException(String message) { super(message); }
    public ReloadException(String message, Throwable cause) { super(message, cause); }
}
```

## ServiceLoader Registration

Each adapter module includes:

```
META-INF/services/com.jsfautoreload.spi.ContainerAdapter
```

Contents (one fully-qualified class name per line):
```
com.jsfautoreload.tomcat.TomcatAdapter
```

## Adapter Selection Algorithm

1. Load all `ContainerAdapter` implementations via `ServiceLoader`
2. Filter to those where `supports()` returns `true`
3. Sort by `priority()` (ascending)
4. Select the first
5. If none match: log `WARNING` — class reload unavailable, view/static reload still works

## Tomcat Adapter (Reference Implementation)

**Module**: `jsf-autoreload-tomcat`
**Class**: `com.jsfautoreload.tomcat.TomcatAdapter`

```java
public class TomcatAdapter implements ContainerAdapter {

    @Override
    public boolean supports() {
        try {
            Class.forName("org.apache.catalina.Context");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void reload(Object servletContext) throws ReloadException {
        // Traverse: ServletContext → ApplicationContextFacade
        //         → ApplicationContext → StandardContext
        // Call: standardContext.reload()
    }

    @Override
    public String containerName() {
        return "Apache Tomcat";
    }
}
```

## Adding a New Adapter

To add support for a new container (e.g., WildFly):

1. Create a new Maven module: `jsf-autoreload-wildfly`
2. Add dependency on `jsf-autoreload-core` and WildFly APIs (provided)
3. Implement `ContainerAdapter`
4. Register in `META-INF/services/com.jsfautoreload.spi.ContainerAdapter`
5. No changes to the core module required
