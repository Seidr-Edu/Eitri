package no.ntnu.eitri.app.registry;

import no.ntnu.eitri.util.ExtensionNormalizer;
import no.ntnu.eitri.writer.DiagramWriter;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Registry for diagram writers, keyed by file extension.
 */
public final class WriterRegistry {

    private final Map<String, Supplier<DiagramWriter<?>>> byExtension = new LinkedHashMap<>();
    private String defaultExtension;

    private WriterRegistry() {}

    public static WriterRegistry defaultRegistry() {
        WriterRegistry registry = new WriterRegistry();
        registry.registerFromServiceLoader();
        registry.registerBuiltIns();
        return registry;
    }

    public Optional<DiagramWriter<?>> getByExtension(String extension) {
        String normalized = ExtensionNormalizer.normalizeExtension(extension);
        if (normalized == null) return Optional.empty();
        Supplier<DiagramWriter<?>> supplier = byExtension.get(normalized);
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

    private void register(Supplier<DiagramWriter<?>> supplier, String extension) {
        String normalized = ExtensionNormalizer.normalizeExtension(extension);
        if (normalized == null) return;

        if (defaultExtension == null) {
            defaultExtension = normalized;
        }
        byExtension.putIfAbsent(normalized, supplier);
    }

    @SuppressWarnings("null")
    private void registerFromServiceLoader() {
        ServiceLoader<DiagramWriter> loader = ServiceLoader.load(DiagramWriter.class);
        for (DiagramWriter writer : loader) {
            // Creates new DiagramWriter instances via reflection, avoiding shared instances.
            Class<? extends DiagramWriter> writerClass = writer.getClass();
            Supplier<DiagramWriter<?>> supplier = () -> {
                try {
                    return writerClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to instantiate DiagramWriter: " + writerClass.getName(), e);
                }
            };
            register(supplier, writer.getFileExtension());
        }
    }

    private void registerBuiltIns() {
        register(PlantUmlWriter::new, new PlantUmlWriter().getFileExtension());
    }
}
