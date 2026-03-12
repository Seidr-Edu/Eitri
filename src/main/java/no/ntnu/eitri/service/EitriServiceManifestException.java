package no.ntnu.eitri.service;

/**
 * Raised when the service manifest is missing or invalid.
 */
final class EitriServiceManifestException extends Exception {

    private final String reasonCode;

    EitriServiceManifestException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    EitriServiceManifestException(String reasonCode, String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
    }

    String reasonCode() {
        return reasonCode;
    }
}
