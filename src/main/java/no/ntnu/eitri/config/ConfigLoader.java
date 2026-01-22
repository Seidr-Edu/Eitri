package no.ntnu.eitri.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Loads and merges configuration from multiple sources.
 * <p>
 * Resolution order (later overrides earlier):
 * <ol>
 *   <li>Built-in defaults (new EitriConfig())</li>
 *   <li>.eitri.config.yaml in working directory (if exists)</li>
 *   <li>Explicit --config file (if specified)</li>
 *   <li>CLI flags (applied by caller)</li>
 * </ol>
 */
public final class ConfigLoader {

    public static final String DEFAULT_CONFIG_FILENAME = ".eitri.config.yaml";

    // ========================================================================
    // Merge Strategies
    // ========================================================================

    /**
     * Defines how a property is merged when combining configurations.
     */
    enum MergeStrategy {
        /** Override target if source value is true (default is false) */
        OVERRIDE_IF_TRUE,
        /** Override target if source value is false (default is true) */
        OVERRIDE_IF_FALSE
    }

    // ========================================================================
    // Property Descriptors
    // ========================================================================

    /**
     * Descriptor for a boolean configuration property.
     * Captures YAML location, getter/setter, default value, and merge strategy.
     */
    record BoolProp(
            String section,
            String key,
            Predicate<EitriConfig> getter,
            BiConsumer<EitriConfig, Boolean> setter,
            boolean defaultValue,
            MergeStrategy mergeStrategy
    ) {
        /** Convenience constructor that derives merge strategy from default value */
        BoolProp(String section, String key,
                 Predicate<EitriConfig> getter,
                 BiConsumer<EitriConfig, Boolean> setter,
                 boolean defaultValue) {
            this(section, key, getter, setter, defaultValue,
                    defaultValue ? MergeStrategy.OVERRIDE_IF_FALSE : MergeStrategy.OVERRIDE_IF_TRUE);
        }
    }

    /**
     * Descriptor for an integer configuration property.
     * Uses OVERRIDE_IF_POSITIVE merge strategy (override if source > 0).
     */
    record IntProp(
            String section,
            String key,
            ToIntFunction<EitriConfig> getter,
            BiConsumer<EitriConfig, Integer> setter
    ) {}

    // ========================================================================
    // Property Registry - Single Source of Truth
    // ========================================================================

    @SuppressWarnings("null")
    private static final List<BoolProp> BOOL_PROPS = List.of(
            // Visibility
            new BoolProp("visibility", "hidePrivate",
                    EitriConfig::isHidePrivate, EitriConfig::setHidePrivate, false),
            new BoolProp("visibility", "hideProtected",
                    EitriConfig::isHideProtected, EitriConfig::setHideProtected, false),
            new BoolProp("visibility", "hidePackage",
                    EitriConfig::isHidePackage, EitriConfig::setHidePackage, false),

            // Members
            new BoolProp("members", "hideFields",
                    EitriConfig::isHideFields, EitriConfig::setHideFields, false),
            new BoolProp("members", "hideMethods",
                    EitriConfig::isHideMethods, EitriConfig::setHideMethods, false),
            new BoolProp("members", "hideEmptyMembers",
                    EitriConfig::isHideEmptyMembers, EitriConfig::setHideEmptyMembers, true),

            // Display
            new BoolProp("display", "hideCircle",
                    EitriConfig::isHideCircle, EitriConfig::setHideCircle, false),
            new BoolProp("display", "hideUnlinked",
                    EitriConfig::isHideUnlinked, EitriConfig::setHideUnlinked, false),
            new BoolProp("display", "showStereotypes",
                    EitriConfig::isShowStereotypes, EitriConfig::setShowStereotypes, true),
            new BoolProp("display", "showGenerics",
                    EitriConfig::isShowGenerics, EitriConfig::setShowGenerics, true),
            new BoolProp("display", "showNotes",
                    EitriConfig::isShowNotes, EitriConfig::setShowNotes, false),
            new BoolProp("display", "showMultiplicities",
                    EitriConfig::isShowMultiplicities, EitriConfig::setShowMultiplicities, true),
            new BoolProp("display", "showLabels",
                    EitriConfig::isShowLabels, EitriConfig::setShowLabels, true),
            new BoolProp("members", "showReadOnly",
                    EitriConfig::isShowReadOnly, EitriConfig::setShowReadOnly, true),

            // Relations
            new BoolProp("relations", "showInheritance",
                    EitriConfig::isShowInheritance, EitriConfig::setShowInheritance, true),
            new BoolProp("relations", "showImplements",
                    EitriConfig::isShowImplements, EitriConfig::setShowImplements, true),
            new BoolProp("relations", "showComposition",
                    EitriConfig::isShowComposition, EitriConfig::setShowComposition, true),
            new BoolProp("relations", "showAggregation",
                    EitriConfig::isShowAggregation, EitriConfig::setShowAggregation, true),
            new BoolProp("relations", "showAssociation",
                    EitriConfig::isShowAssociation, EitriConfig::setShowAssociation, true),
            new BoolProp("relations", "showDependency",
                    EitriConfig::isShowDependency, EitriConfig::setShowDependency, true),

            // Runtime
            new BoolProp("runtime", "verbose",
                    EitriConfig::isVerbose, EitriConfig::setVerbose, false),
            new BoolProp("runtime", "dryRun",
                    EitriConfig::isDryRun, EitriConfig::setDryRun, false)
    );

    @SuppressWarnings("null")
    private static final List<IntProp> INT_PROPS = List.of(
            // Layout section
            new IntProp("layout", "groupInheritance",
                    EitriConfig::getGroupInheritance, EitriConfig::setGroupInheritance),
            new IntProp("layout", "classAttributeIconSize",
                    EitriConfig::getClassAttributeIconSize, EitriConfig::setClassAttributeIconSize)
    );

    // ========================================================================
    // Constructor
    // ========================================================================

    private ConfigLoader() {
        // Utility class
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Loads configuration with the standard resolution order.
     *
     * @param explicitConfigPath optional explicit config file path (from --config)
     * @return merged configuration
     * @throws ConfigException if config file exists but cannot be parsed
     */
    public static EitriConfig load(Path explicitConfigPath) throws ConfigException {
        EitriConfig config = new EitriConfig();

        // Try to load .eitri.config.yaml from working directory
        Path workingDirConfig = Path.of(System.getProperty("user.dir"), DEFAULT_CONFIG_FILENAME);
        if (Files.exists(workingDirConfig)) {
            merge(config, loadFromYaml(workingDirConfig));
        }

        // Load explicit config if specified
        if (explicitConfigPath != null) {
            if (!Files.exists(explicitConfigPath)) {
                throw new ConfigException("Config file not found: " + explicitConfigPath);
            }
            merge(config, loadFromYaml(explicitConfigPath));
        }

        return config;
    }

    /**
     * Loads configuration from a YAML file.
     *
     * @param path path to the YAML file
     * @return parsed configuration
     * @throws ConfigException if file cannot be read or parsed
     */
    public static EitriConfig loadFromYaml(Path path) throws ConfigException {
        try (InputStream in = Files.newInputStream(path)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(in);
            return data == null ? new EitriConfig() : mapToConfig(data);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file: " + path, e);
        } catch (Exception e) {
            throw new ConfigException("Failed to parse config file: " + path + " - " + e.getMessage(), e);
        }
    }

    // ========================================================================
    // YAML to Config Mapping
    // ========================================================================

    /**
     * Converts a YAML map to EitriConfig using property descriptors.
     */
    @SuppressWarnings("unchecked")
    private static EitriConfig mapToConfig(Map<String, Object> data) {
        EitriConfig config = new EitriConfig();

        // Special cases: complex/additive properties
        mapInputSection(data, config);
        mapOutputSection(data, config);
        mapLayoutDirection(data, config);
        mapSkinparamCustom(data, config);

        // Boolean properties (generic handling)
        for (BoolProp prop : BOOL_PROPS) {
            Map<String, Object> section = (Map<String, Object>) data.get(prop.section());
            if (section != null && section.containsKey(prop.key())) {
                prop.setter().accept(config, toBool(section.get(prop.key())));
            }
        }

        // Integer properties (generic handling)
        for (IntProp prop : INT_PROPS) {
            Map<String, Object> section = (Map<String, Object>) data.get(prop.section());
            if (section != null && section.containsKey(prop.key())) {
                prop.setter().accept(config, toInt(section.get(prop.key())));
            }
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    private static void mapInputSection(Map<String, Object> data, EitriConfig config) {
        Map<String, Object> input = (Map<String, Object>) data.get("input");
        if (input == null) return;

        List<String> sources = (List<String>) input.get("sources");
        if (sources != null) {
            sources.forEach(src -> config.addSourcePath(Path.of(src)));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapOutputSection(Map<String, Object> data, EitriConfig config) {
        Map<String, Object> output = (Map<String, Object>) data.get("output");
        if (output == null) return;

        if (output.containsKey("file")) {
            config.setOutputPath(Path.of((String) output.get("file")));
        }
        if (output.containsKey("name")) {
            config.setDiagramName((String) output.get("name"));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapLayoutDirection(Map<String, Object> data, EitriConfig config) {
        Map<String, Object> layout = (Map<String, Object>) data.get("layout");
        if (layout != null && layout.containsKey("direction")) {
            config.setDirection(LayoutDirection.fromString((String) layout.get("direction")));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapSkinparamCustom(Map<String, Object> data, EitriConfig config) {
        Map<String, Object> skinparam = (Map<String, Object>) data.get("skinparam");
        if (skinparam == null) return;

        List<String> custom = (List<String>) skinparam.get("custom");
        if (custom != null) {
            custom.forEach(config::addSkinparamLine);
        }
    }

    // ========================================================================
    // Config Merging
    // ========================================================================

    /**
     * Merges source config into target using property descriptors.
     * Merge strategy is determined by each property's default value.
     */
    private static void merge(EitriConfig target, EitriConfig source) {
        // Additive properties
        source.getSourcePaths().forEach(target::addSourcePath);
        source.getSkinparamLines().forEach(target::addSkinparamLine);

        // Override-if-non-null properties
        if (source.getOutputPath() != null) {
            target.setOutputPath(source.getOutputPath());
        }

        // Override-if-non-default properties
        if (!"diagram".equals(source.getDiagramName())) {
            target.setDiagramName(source.getDiagramName());
        }
        if (source.getDirection() != LayoutDirection.TOP_TO_BOTTOM) {
            target.setDirection(source.getDirection());
        }

        // Boolean properties (generic handling based on merge strategy)
        for (BoolProp prop : BOOL_PROPS) {
            boolean sourceValue = prop.getter().test(source);
            switch (prop.mergeStrategy()) {
                case OVERRIDE_IF_TRUE -> {
                    if (sourceValue) prop.setter().accept(target, true);
                }
                case OVERRIDE_IF_FALSE -> {
                    if (!sourceValue) prop.setter().accept(target, false);
                }
            }
        }

        // Integer properties (override if positive)
        for (IntProp prop : INT_PROPS) {
            int sourceValue = prop.getter().applyAsInt(source);
            if (sourceValue > 0) {
                prop.setter().accept(target, sourceValue);
            }
        }
    }

    // ========================================================================
    // Type Conversion Helpers
    // ========================================================================

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static boolean toBool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return false;
    }
}
