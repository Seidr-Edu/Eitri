package no.ntnu.eitri.app.registry;

import no.ntnu.eitri.parser.SourceParser;
import no.ntnu.eitri.parser.java.JavaSourceParser;
import no.ntnu.eitri.util.ExtensionNormalizer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Registry for source parsers, keyed by file extension.
 */
public final class ParserRegistry {

    private final Map<String, Supplier<SourceParser>> byExtension = new LinkedHashMap<>();
    private String defaultExtension;

    private ParserRegistry() {}

    public static ParserRegistry defaultRegistry() {
        ParserRegistry registry = new ParserRegistry();
        registry.registerFromServiceLoader();
        registry.registerBuiltIns();
        return registry;
    }

    public Optional<SourceParser> getByExtension(String extension) {
        String normalized = ExtensionNormalizer.normalizeExtension(extension);
        if (normalized == null) return Optional.empty();

        Supplier<SourceParser> supplier = byExtension.get(normalized);
        return supplier == null ? Optional.empty() : Optional.of(supplier.get());
    }

    public boolean supports(String extension) {
        String normalized = ExtensionNormalizer.normalizeExtension(extension);
        return normalized != null && byExtension.containsKey(normalized);
    }

    public Set<String> getSupportedExtensions() {
        return byExtension.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }

    public String getDefaultExtension() {
        return defaultExtension;
    }

    private void register(Supplier<SourceParser> supplier, Iterable<String> extensions) {
        if (extensions == null) {
            return;
        }
        for (String extension : extensions) {
            String normalized = ExtensionNormalizer.normalizeExtension(extension);
            if (normalized == null) {
                continue;
            }
            if (defaultExtension == null) {
                defaultExtension = normalized;
            }
            byExtension.putIfAbsent(normalized, supplier);
        }
    }

    private void registerFromServiceLoader() {
        ServiceLoader<SourceParser> loader = ServiceLoader.load(SourceParser.class);
        for (SourceParser parser : loader) {
            register(() -> parser, parser.getSupportedExtensions());
        }
    }

    private void registerBuiltIns() {
        register(JavaSourceParser::new, new JavaSourceParser().getSupportedExtensions());
    }
}
