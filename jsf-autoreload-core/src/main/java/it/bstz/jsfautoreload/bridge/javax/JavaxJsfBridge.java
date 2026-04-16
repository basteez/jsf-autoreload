package it.bstz.jsfautoreload.bridge.javax;

import it.bstz.jsfautoreload.bridge.JsfBridge;

import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

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
                    UIOutput script = new UIOutput();
                    script.setRendererType("javax.faces.resource.Script");
                    script.getAttributes().put("target", "head");
                    script.setValue("<script>" + scriptContent + "</script>");

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
    public String projectStageParamName() {
        return "javax.faces.PROJECT_STAGE";
    }
}
