package no.ntnu.eitri.parser.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Collects non-fatal parse diagnostics.
 */
public final class ParseDiagnostics {

    private final Logger logger;
    private final boolean verbose;
    private final List<String> warnings = new ArrayList<>();

    public ParseDiagnostics(Logger logger, boolean verbose) {
        this.logger = logger;
        this.verbose = verbose;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
        if (verbose) {
            logger.warning(warning);
        }
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
}
