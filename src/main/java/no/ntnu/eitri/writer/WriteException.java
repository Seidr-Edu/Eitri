package no.ntnu.eitri.writer;

import java.nio.file.Path;

/**
 * Exception thrown when diagram writing fails.
 * 
 * <p>This exception wraps errors that occur during diagram generation,
 * such as file access issues or rendering errors.
 */
public class WriteException extends RuntimeException {

    private final Path outputPath;

    /**
     * Creates a write exception with a message.
     * 
     * @param message the error message
     */
    public WriteException(String message) {
        super(message);
        this.outputPath = null;
    }

    /**
     * Creates a write exception with a message and cause.
     * 
     * @param message the error message
     * @param cause the underlying cause
     */
    public WriteException(String message, Throwable cause) {
        super(message, cause);
        this.outputPath = null;
    }

    /**
     * Creates a write exception with output path information.
     * 
     * @param message the error message
     * @param outputPath path where writing was attempted
     */
    public WriteException(String message, Path outputPath) {
        super(formatMessage(message, outputPath));
        this.outputPath = outputPath;
    }

    /**
     * Creates a write exception with output path information and cause.
     * 
     * @param message the error message
     * @param outputPath path where writing was attempted
     * @param cause the underlying cause
     */
    public WriteException(String message, Path outputPath, Throwable cause) {
        super(formatMessage(message, outputPath), cause);
        this.outputPath = outputPath;
    }

    private static String formatMessage(String message, Path outputPath) {
        if (outputPath == null) {
            return message;
        }
        return message + " (output: " + outputPath + ")";
    }

    /**
     * Returns the output file path where the error occurred.
     * 
     * @return the output path, or null if not available
     */
    public Path getOutputPath() {
        return outputPath;
    }

    /**
     * Checks if this exception has output path information.
     * 
     * @return true if output path is available
     */
    public boolean hasOutputPath() {
        return outputPath != null;
    }
}
