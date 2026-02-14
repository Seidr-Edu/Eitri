package no.ntnu.eitri.app.registry;

import no.ntnu.eitri.parser.SourceParser;
import no.ntnu.eitri.writer.DiagramWriter;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class ServiceLoaderDiscoveryTest {

    @SuppressWarnings("null")
    @Test
    void sourceParserProvidersAreDiscovered() {
        ServiceLoader<SourceParser> loader = ServiceLoader.load(SourceParser.class);
        boolean hasJava = loader.stream()
                .map(ServiceLoader.Provider::get)
                .anyMatch(parser -> parser.getSupportedExtensions().contains(".java"));

        assertTrue(hasJava);
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void diagramWriterProvidersAreDiscovered() {
        ServiceLoader<DiagramWriter> loader = ServiceLoader.load(DiagramWriter.class);
        boolean hasPuml = loader.stream()
                .map(ServiceLoader.Provider::get)
                .anyMatch(writer -> ".puml".equalsIgnoreCase(writer.getFileExtension()));

        assertTrue(hasPuml);
    }
}
