package it.bstz.jsfautoreload.bridge.jakarta;

import it.bstz.jsfautoreload.bridge.JsfBridge;

import jakarta.faces.application.Application;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PostAddToViewEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;

public class JakartaJsfBridge implements JsfBridge {

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
                    script.setRendererType("jakarta.faces.resource.Script");
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
        return "jakarta.faces.PROJECT_STAGE";
    }
}
