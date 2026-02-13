package no.ntnu.eitri.writer;

import no.ntnu.eitri.config.WriterConfig;
import no.ntnu.eitri.model.UmlModel;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

/**
 * Interface for writing UML models to diagram formats.
 */
public interface DiagramWriter<C extends WriterConfig> {

    /**
     * Returns the expected configuration type for this writer.
     */
    Class<C> configType();

    /**
     * Writes the UML model to a file.
     * 
     * @param model      the UML model to write
     * @param config     the configuration for rendering
     * @param outputPath the file path to write to
     * @throws WriteException if writing fails
     */
    void write(UmlModel model, C config, Path outputPath) throws WriteException;

    /**
     * Writes the UML model to a Writer.
     * 
     * @param model  the UML model to write
     * @param config the configuration for rendering
     * @param writer the writer to output to
     * @throws IOException if writing fails
     */
    void write(UmlModel model, C config, Writer writer) throws IOException;

    /**
     * Returns the UML model as a string.
     * 
     * @param model  the UML model to render
     * @param config the configuration for rendering
     * @return the rendered diagram as a string
     */
    String render(UmlModel model, C config);

    /**
     * Returns the name of this writer implementation.
     * Used for logging and diagnostics.
     * 
     * @return the writer name (e.g., "PlantUML", "Mermaid")
     */
    String getName();

    /**
     * Returns the file extension for this output format.
     * 
     * @return the file extension (e.g., ".puml", ".md")
     */
    String getFileExtension();
}
