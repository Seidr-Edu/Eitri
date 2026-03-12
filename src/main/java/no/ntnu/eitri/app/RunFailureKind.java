package no.ntnu.eitri.app;

/**
 * Categorises the primary failure mode of an Eitri run.
 */
public enum RunFailureKind {
    CONFIG_ERROR("config-error"),
    PARSE_ERROR("parse-error"),
    WRITE_ERROR("write-error"),
    UNEXPECTED_ERROR("unexpected-error");

    private final String reasonCode;

    RunFailureKind(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
