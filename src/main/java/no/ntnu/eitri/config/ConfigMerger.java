package no.ntnu.eitri.config;

import java.util.List;
import java.util.Optional;

/**
 * Merges configuration sources with default values as a baseline.
 */
public final class ConfigMerger {

    /**
     * Merges the provided sources into a single configuration.
     * Missing sources are ignored, and defaults are always applied.
     *
     * @param sources ordered configuration sources
     * @return merged configuration
     * @throws ConfigException if any source fails to load
     */
    public EitriConfig merge(List<ConfigSource> sources) throws ConfigException {
        EitriConfig.Builder builder = EitriConfig.builder();

        if (sources == null) {
            return builder.build();
        }

        for (ConfigSource source : sources) {
            Optional<EitriConfig> loaded = source.load();
            if (loaded.isPresent()) {
                mergeInto(builder, loaded.get());
            }
        }

        return builder.build();
    }

    private void mergeInto(EitriConfig.Builder target, EitriConfig source) {
        // Additive properties
        source.getSourcePaths().forEach(target::addSourcePath);
        source.getSkinparamLines().forEach(target::addSkinparamLine);

        // Override-if-non-null properties
        if (source.getOutputPath() != null) {
            target.outputPath(source.getOutputPath());
        }
        if (source.getParserExtension() != null) {
            target.parserExtension(source.getParserExtension());
        }
        if (source.getWriterExtension() != null) {
            target.writerExtension(source.getWriterExtension());
        }

        // Override-if-non-default properties
        if (!"diagram".equals(source.getDiagramName())) {
            target.diagramName(source.getDiagramName());
        }
        if (source.getDirection() != LayoutDirection.TOP_TO_BOTTOM) {
            target.direction(source.getDirection());
        }

        // Boolean properties (generic handling based on merge strategy)
        for (ConfigLoader.BoolProp prop : ConfigLoader.boolProps()) {
            boolean sourceValue = prop.getter().test(source);
            if (prop.mergeStrategy() == ConfigLoader.MergeStrategy.OVERRIDE_IF_TRUE && sourceValue) {
                prop.setter().accept(target, true);
            } else if (prop.mergeStrategy() == ConfigLoader.MergeStrategy.OVERRIDE_IF_FALSE && !sourceValue) {
                prop.setter().accept(target, false);
            }
        }

        // Integer properties (override if positive)
        for (ConfigLoader.IntProp prop : ConfigLoader.intProps()) {
            int sourceValue = prop.getter().applyAsInt(source);
            if (sourceValue > 0) {
                prop.setter().accept(target, sourceValue);
            }
        }
    }
}
