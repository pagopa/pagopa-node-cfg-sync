package it.gov.pagopa.node.cfg_sync.model;

public enum TargetRefreshEnum {

    config("api-config-cache"),
    standin("stand-in-manager");

    public final String label;

    private TargetRefreshEnum(String label) {
        this.label = label;
    }
}
