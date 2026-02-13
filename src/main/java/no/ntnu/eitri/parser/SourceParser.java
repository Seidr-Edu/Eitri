package no.ntnu.eitri.parser;

import no.ntnu.eitri.config.RunConfig;
import no.ntnu.eitri.model.UmlModel;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for parsing source files into a UML model.
 * 
 * <p>
 * Implementations of this interface handle specific source languages
 * (e.g., Java, Kotlin) and convert them into Eitri's internal UML model.
 * 
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * SourceParser parser = new JavaSourceParser();
 * UmlModel model = parser.parse(List.of(Path.of("src/main/java")), runConfig);
 * }</pre>
 */
public interface SourceParser {

    /**
     * Parses source files from the given paths and produces a UML model.
     * 
     * @param sourcePaths list of source directories or files to parse
     * @param runConfig   the configuration to use during parsing
     * @return the populated UML model
     * @throws ParseException if parsing fails
     */
    UmlModel parse(List<Path> sourcePaths, RunConfig runConfig) throws ParseException;

    /**
     * Returns the name of this parser implementation.
     * Used for logging and diagnostics.
     * 
     * @return the parser name (e.g., "JavaParser", "KotlinParser")
     */
    String getName();

    /**
     * Returns the file extensions this parser can handle.
     * 
     * @return list of file extensions (e.g., ".java", ".kt")
     */
    List<String> getSupportedExtensions();
}
