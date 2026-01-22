package no.ntnu.eitri.parser;

/**
 * Exception thrown when source parsing fails.
 * 
 * <p>This exception wraps errors that occur during parsing,
 * such as file access issues, syntax errors, or symbol resolution failures.
 */
public class ParseException extends RuntimeException {

    private final String sourcePath;
    private final int line;
    private final int column;

    /**
     * Creates a parse exception with a message.
     * 
     * @param message the error message
     */
    public ParseException(String message) {
        super(message);
        this.sourcePath = null;
        this.line = -1;
        this.column = -1;
    }

    /**
     * Creates a parse exception with a message and cause.
     * 
     * @param message the error message
     * @param cause the underlying cause
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
        this.sourcePath = null;
        this.line = -1;
        this.column = -1;
    }

    /**
     * Creates a parse exception with location information.
     * 
     * @param message the error message
     * @param sourcePath path to the source file
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     */
    public ParseException(String message, String sourcePath, int line, int column) {
        super(formatMessage(message, sourcePath, line, column));
        this.sourcePath = sourcePath;
        this.line = line;
        this.column = column;
    }

    /**
     * Creates a parse exception with location information and cause.
     * 
     * @param message the error message
     * @param sourcePath path to the source file
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     * @param cause the underlying cause
     */
    public ParseException(String message, String sourcePath, int line, int column, Throwable cause) {
        super(formatMessage(message, sourcePath, line, column), cause);
        this.sourcePath = sourcePath;
        this.line = line;
        this.column = column;
    }

    private static String formatMessage(String message, String sourcePath, int line, int column) {
        if (sourcePath == null) {
            return message;
        }
        if (line > 0 && column > 0) {
            return String.format("%s:%d:%d: %s", sourcePath, line, column, message);
        } else if (line > 0) {
            return String.format("%s:%d: %s", sourcePath, line, message);
        } else {
            return String.format("%s: %s", sourcePath, message);
        }
    }

    /**
     * Returns the source file path where the error occurred.
     * 
     * @return the source path, or null if not available
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Returns the line number where the error occurred.
     * 
     * @return the line number (1-based), or -1 if not available
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number where the error occurred.
     * 
     * @return the column number (1-based), or -1 if not available
     */
    public int getColumn() {
        return column;
    }

    /**
     * Checks if this exception has location information.
     * 
     * @return true if source path, line, or column is available
     */
    public boolean hasLocation() {
        return sourcePath != null || line > 0 || column > 0;
    }
}
