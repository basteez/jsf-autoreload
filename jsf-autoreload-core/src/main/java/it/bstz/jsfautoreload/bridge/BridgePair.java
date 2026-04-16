package it.bstz.jsfautoreload.bridge;

public final class BridgePair {

    private final JsfBridge jsf;
    private final ServletBridge servlet;

    public BridgePair(JsfBridge jsf, ServletBridge servlet) {
        this.jsf = jsf;
        this.servlet = servlet;
    }

    public JsfBridge jsf() {
        return jsf;
    }

    public ServletBridge servlet() {
        return servlet;
    }
}
