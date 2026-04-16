package it.bstz.jsfautoreload.bridge.javax;

import it.bstz.jsfautoreload.bridge.JsfBridge;
import it.bstz.jsfautoreload.jsf.ScriptInjector;
import it.bstz.jsfautoreload.logging.ReloadLogger;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.ProjectStage;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class JavaxJsfBridge implements JsfBridge {

    @Override
    public boolean isDevelopmentMode(Object facesContext) {
        if (facesContext instanceof FacesContext) {
            return ((FacesContext) facesContext).getApplication()
                    .getProjectStage() == ProjectStage.Development;
        }
        return false;
    }

    @Override
    public void registerScriptInjector(Object application, String scriptContent) {
        if (application instanceof Application) {
            Application app = (Application) application;
            app.subscribeToEvent(PostAddToViewEvent.class, new SystemEventListener() {
                @Override
                public void processEvent(SystemEvent event) {
                    FacesContext ctx = FacesContext.getCurrentInstance();
                    if (ctx == null || ctx.getApplication().getProjectStage() != ProjectStage.Development) {
                        return;
                    }
                    // Guard against re-entry: addComponentResource triggers PostAddToViewEvent
                    if (ctx.getAttributes().containsKey("jsfautoreload.scriptInjected")) {
                        return;
                    }
                    ctx.getAttributes().put("jsfautoreload.scriptInjected", Boolean.TRUE);

                    UIOutput script = new UIOutput() {
                        @Override
                        public void encodeBegin(FacesContext context) throws IOException {
                            context.getResponseWriter().startElement("script", this);
                            context.getResponseWriter().writeAttribute("type", "text/javascript", null);
                        }

                        @Override
                        public void encodeEnd(FacesContext context) throws IOException {
                            context.getResponseWriter().write(scriptContent);
                            context.getResponseWriter().endElement("script");
                        }

                        @Override
                        public boolean getRendersChildren() {
                            return true;
                        }

                        @Override
                        public void encodeChildren(FacesContext context) {
                            // no children
                        }
                    };
                    script.setTransient(true);
                    ctx.getViewRoot().addComponentResource(ctx, script, "head");
                }

                @Override
                public boolean isListenerForSource(Object source) {
                    return true;
                }
            });
        }
    }

    @Override
    public void registerDeferredScriptInjector(Object servletContext, String sseEndpointPath) {
        if (servletContext instanceof ServletContext) {
            ServletContext ctx = (ServletContext) servletContext;
            JsfBridge bridge = this;
            ctx.addListener(new ServletContextListener() {
                @Override
                public void contextInitialized(ServletContextEvent sce) {
                    try {
                        ApplicationFactory factory = (ApplicationFactory)
                                FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
                        Application app = factory.getApplication();
                        String contextPath = sce.getServletContext().getContextPath();
                        String fullPath = (contextPath == null || contextPath.isEmpty())
                                ? sseEndpointPath : contextPath + sseEndpointPath;
                        ScriptInjector injector = new ScriptInjector(bridge, fullPath);
                        injector.register(app);
                        ReloadLogger.info("SCRIPT_INJECTOR",
                                "Deferred registration complete — script injector wired to JSF Application");
                    } catch (Exception e) {
                        ReloadLogger.warning("SCRIPT_INJECTOR",
                                "Deferred registration failed — JSF may not be initialized", e);
                    }
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                    // no-op
                }
            });
            ReloadLogger.info("SCRIPT_INJECTOR",
                    "Deferred registration: ServletContextListener registered for SSE endpoint " + sseEndpointPath);
        }
    }

    @Override
    public String projectStageParamName() {
        return "javax.faces.PROJECT_STAGE";
    }
}
