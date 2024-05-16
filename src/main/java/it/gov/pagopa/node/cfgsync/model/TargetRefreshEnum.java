package it.gov.pagopa.node.cfgsync.model;

public enum TargetRefreshEnum {

    cache("api-config-cache"),
    standin("stand-in-manager"),
    riversamento("riversamento");

    public final String label;

    private TargetRefreshEnum(String label) {
        this.label = label;
    }
}
