# Contract: JSF/Servlet Bridge SPI (Internal)

**Type**: Internal Java interfaces for namespace abstraction
**Module**: `jsf-autoreload-core`
**Audience**: Internal to the plugin (not a public extension point)

## Purpose

These interfaces abstract over the `javax.faces` / `jakarta.faces` and `javax.servlet` / `jakarta.servlet` namespace differences, allowing all core logic to be namespace-agnostic.

## JsfBridge Interface

```java
package it.bstz.jsfautoreload.bridge;

/**
 * Abstraction over JSF namespace differences.
 * Two implementations exist: one for javax.faces, one for jakarta.faces.
 * Selected at startup based on which namespace is on the classpath.
 */
public interface JsfBridge {

    /**
     * Returns true if the application is in JSF Development stage.
     */
    boolean isDevelopmentMode(Object facesContext);

    /**
     * Creates and registers a SystemEventListener that injects the
     * reload script into every JSF view's head section.
     *
     * @param application the JSF Application object
     * @param scriptContent the JavaScript to inject
     */
    void registerScriptInjector(Object application, String scriptContent);

    /**
     * Returns the JSF project stage context-param name for this namespace.
     * e.g., "javax.faces.PROJECT_STAGE" or "jakarta.faces.PROJECT_STAGE"
     */
    String projectStageParamName();
}
```

## ServletBridge Interface

```java
package it.bstz.jsfautoreload.bridge;

/**
 * Abstraction over Servlet API namespace differences.
 */
public interface ServletBridge {

    /**
     * Registers the SSE servlet dynamically on the given servlet context.
     *
     * @param servletContext the raw servlet context object
     * @param path the URL pattern for the SSE endpoint
     * @param handler the SSE request handler
     */
    void registerServlet(Object servletContext, String path, SseHandler handler);

    /**
     * Starts an async context for the given request/response pair.
     *
     * @return an abstracted async context wrapper
     */
    AsyncContextWrapper startAsync(Object request, Object response);

    /**
     * Registers a context destroyed listener for cleanup.
     */
    void registerShutdownListener(Object servletContext, Runnable onShutdown);
}
```

## Bridge Detection

```java
package it.bstz.jsfautoreload.bridge;

public final class BridgeDetector {

    /**
     * Detects which JSF/Servlet namespace is available and returns
     * the appropriate bridge implementations.
     *
     * Detection order:
     * 1. Try jakarta.faces.context.FacesContext → Jakarta bridge
     * 2. Try javax.faces.context.FacesContext → Javax bridge
     * 3. Neither found → throw IllegalStateException
     */
    public static BridgePair detect() { ... }
}

public record BridgePair(JsfBridge jsf, ServletBridge servlet) {}
```

## Implementation Packages

### javax bridge
- `it.bstz.jsfautoreload.bridge.javax.JavaxJsfBridge` implements `JsfBridge`
- `it.bstz.jsfautoreload.bridge.javax.JavaxServletBridge` implements `ServletBridge`
- Imports: `javax.faces.*`, `javax.servlet.*`

### jakarta bridge
- `it.bstz.jsfautoreload.bridge.jakarta.JakartaJsfBridge` implements `JsfBridge`
- `it.bstz.jsfautoreload.bridge.jakarta.JakartaServletBridge` implements `ServletBridge`
- Imports: `jakarta.faces.*`, `jakarta.servlet.*`

## Important Constraints

- Core module code MUST NOT import `javax.faces.*`, `javax.servlet.*`, `jakarta.faces.*`, or `jakarta.servlet.*` directly — only through bridge interfaces
- Bridge implementations are the ONLY classes that import namespace-specific types
- The bridge layer is internal and NOT part of the public API — adapter modules interact with the container adapter SPI, not the bridges
