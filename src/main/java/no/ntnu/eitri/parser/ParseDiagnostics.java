package no.ntnu.eitri.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Collects non-fatal parse diagnostics.
 */
final class ParseDiagnostics {

    private final Logger logger;
    private final boolean verbose;
    private final List<String> warnings = new ArrayList<>();

    ParseDiagnostics(Logger logger, boolean verbose) {
        this.logger = logger;
        this.verbose = verbose;
    }

    void addWarning(String warning) {
        warnings.add(warning);
        if (verbose) {
            logger.warning(warning);
        }
    }

    List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
}
