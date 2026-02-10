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
 *   <li>Built-in defaults (EitriConfig.builder().build())</li>
 *   <li>.eitri.config.yaml in working directory (if exists)</li>
 *   <li>Explicit --config file (if specified)</li>
 *   <li>CLI flags (applied by caller)</li>
 * </ol>
 */
public final class ConfigLoader {

    public static final String DEFAULT_CONFIG_FILENAME = ".eitri.config.yaml";

    // ========================================================================
    // Property Descriptors
    // ========================================================================

    /**
     * Descriptor for a boolean configuration property.
     * Captures YAML location and getter/setter.
     */
    record BoolProp(
            String section,
            String key,
            Predicate<EitriConfig> getter,
            BiConsumer<EitriConfig.Builder, Boolean> setter
    ) {}

    /**
     * Descriptor for an integer configuration property.
     */
        record IntProp(
            String section,
            String key,
            ToIntFunction<EitriConfig> getter,
            BiConsumer<EitriConfig.Builder, Integer> setter
        ) {}

    // ========================================================================
    // Property Registry - Single Source of Truth
    // ========================================================================

    private static final String SECTION_VISIBILITY = "visibility";
    private static final String SECTION_MEMBERS = "members";
    private static final String SECTION_DISPLAY = "display";
    private static final String SECTION_RELATIONS = "relations";
    private static final String SECTION_LAYOUT = "layout";
    private static final String SECTION_RUNTIME = "runtime";
    private static final String SECTION_INPUT = "input";
    private static final String SECTION_OUTPUT = "output";

    @SuppressWarnings("null")
    private static final List<BoolProp> BOOL_PROPS = List.of(
            // Visibility
            new BoolProp(SECTION_VISIBILITY, "hidePrivate",
                EitriConfig::isHidePrivate, EitriConfig.Builder::hidePrivate),
            new BoolProp(SECTION_VISIBILITY, "hideProtected",
                EitriConfig::isHideProtected, EitriConfig.Builder::hideProtected),
            new BoolProp(SECTION_VISIBILITY, "hidePackage",
                EitriConfig::isHidePackage, EitriConfig.Builder::hidePackage),

            // Members
            new BoolProp(SECTION_MEMBERS, "hideFields",
                EitriConfig::isHideFields, EitriConfig.Builder::hideFields),
            new BoolProp(SECTION_MEMBERS, "hideEmptyFields",
                EitriConfig::isHideEmptyFields, EitriConfig.Builder::hideEmptyFields),
            new BoolProp(SECTION_MEMBERS, "hideMethods",
                EitriConfig::isHideMethods, EitriConfig.Builder::hideMethods),
            new BoolProp(SECTION_MEMBERS, "hideEmptyMethods",
                EitriConfig::isHideEmptyMethods, EitriConfig.Builder::hideEmptyMethods),
            new BoolProp(SECTION_MEMBERS, "hideEmptyMembers",
                EitriConfig::isHideEmptyMembers, EitriConfig.Builder::hideEmptyMembers),

            // Display
            new BoolProp(SECTION_DISPLAY, "hideCircle",
                EitriConfig::isHideCircle, EitriConfig.Builder::hideCircle),
            new BoolProp(SECTION_DISPLAY, "hideUnlinked",
                EitriConfig::isHideUnlinked, EitriConfig.Builder::hideUnlinked),
            new BoolProp(SECTION_DISPLAY, "showStereotypes",
                EitriConfig::isShowStereotypes, EitriConfig.Builder::showStereotypes),
            new BoolProp(SECTION_DISPLAY, "showGenerics",
                EitriConfig::isShowGenerics, EitriConfig.Builder::showGenerics),
            new BoolProp(SECTION_DISPLAY, "showNotes",
                EitriConfig::isShowNotes, EitriConfig.Builder::showNotes),
            new BoolProp(SECTION_DISPLAY, "showMultiplicities",
                EitriConfig::isShowMultiplicities, EitriConfig.Builder::showMultiplicities),
            new BoolProp(SECTION_DISPLAY, "showLabels",
                EitriConfig::isShowLabels, EitriConfig.Builder::showLabels),
            new BoolProp(SECTION_DISPLAY, "showReadOnly",
                EitriConfig::isShowReadOnly, EitriConfig.Builder::showReadOnly),
            new BoolProp(SECTION_DISPLAY, "showVoidReturnTypes",
                EitriConfig::isShowVoidReturnTypes, EitriConfig.Builder::showVoidReturnTypes),

            // Relations
            new BoolProp(SECTION_RELATIONS, "showInheritance",
                EitriConfig::isShowInheritance, EitriConfig.Builder::showInheritance),
            new BoolProp(SECTION_RELATIONS, "showImplements",
                EitriConfig::isShowImplements, EitriConfig.Builder::showImplements),
            new BoolProp(SECTION_RELATIONS, "showComposition",
                EitriConfig::isShowComposition, EitriConfig.Builder::showComposition),
            new BoolProp(SECTION_RELATIONS, "showAggregation",
                EitriConfig::isShowAggregation, EitriConfig.Builder::showAggregation),
            new BoolProp(SECTION_RELATIONS, "showAssociation",
                EitriConfig::isShowAssociation, EitriConfig.Builder::showAssociation),
            new BoolProp(SECTION_RELATIONS, "showDependency",
                EitriConfig::isShowDependency, EitriConfig.Builder::showDependency),
            new BoolProp(SECTION_RELATIONS, "showNested",
                EitriConfig::isShowNested, EitriConfig.Builder::showNested),

            // Runtime
            new BoolProp(SECTION_RUNTIME, "verbose",
                EitriConfig::isVerbose, EitriConfig.Builder::verbose),
            new BoolProp(SECTION_RUNTIME, "dryRun",
                EitriConfig::isDryRun, EitriConfig.Builder::dryRun)
    );

    @SuppressWarnings("null")
    private static final List<IntProp> INT_PROPS = List.of(
            // Layout section
                    new IntProp(SECTION_LAYOUT, "groupInheritance",
                        EitriConfig::getGroupInheritance, EitriConfig.Builder::groupInheritance),
                    new IntProp(SECTION_LAYOUT, "classAttributeIconSize",
                        EitriConfig::getClassAttributeIconSize, EitriConfig.Builder::classAttributeIconSize)
    );

    // ========================================================================
    // Constructor
    // ========================================================================

    private ConfigLoader() {
        // Utility class
    }

    static List<BoolProp> boolProps() {
        return BOOL_PROPS;
    }

    static List<IntProp> intProps() {
        return INT_PROPS;
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
        ConfigMerger merger = new ConfigMerger();

        List<ConfigSource> sources = new java.util.ArrayList<>();
        Path workingDirConfig = Path.of(System.getProperty("user.dir"), DEFAULT_CONFIG_FILENAME);
        if (Files.exists(workingDirConfig)) {
            sources.add(new YamlConfigSource(workingDirConfig));
        }

        if (explicitConfigPath != null) {
            if (!Files.exists(explicitConfigPath)) {
                throw new ConfigException("Config file not found: " + explicitConfigPath);
            }
            sources.add(new YamlConfigSource(explicitConfigPath));
        }

        return merger.merge(sources);
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
            return data == null ? EitriConfig.builder().build() : mapToConfig(data);
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
        EitriConfig.Builder builder = EitriConfig.builder();

        // Special cases: complex/additive properties
        mapInputSection(data, builder);
        mapOutputSection(data, builder);
        mapLayoutDirection(data, builder);

        // Boolean properties (generic handling)
        for (BoolProp prop : BOOL_PROPS) {
            Map<String, Object> section = (Map<String, Object>) data.get(prop.section());
            if (section != null && section.containsKey(prop.key())) {
                prop.setter().accept(builder, toBool(section.get(prop.key())));
            }
        }

        // Integer properties (generic handling)
        for (IntProp prop : INT_PROPS) {
            Map<String, Object> section = (Map<String, Object>) data.get(prop.section());
            if (section != null && section.containsKey(prop.key())) {
                prop.setter().accept(builder, toInt(section.get(prop.key())));
            }
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static void mapInputSection(Map<String, Object> data, EitriConfig.Builder builder) {
        Map<String, Object> input = (Map<String, Object>) data.get(SECTION_INPUT);
        if (input == null) return;

        List<String> sources = (List<String>) input.get("sources");
        if (sources != null) {
            sources.forEach(src -> builder.addSourcePath(Path.of(src)));
        }

        if (input.containsKey("parserExtension")) {
            builder.parserExtension((String) input.get("parserExtension"));
        } else if (input.containsKey("parser")) {
            builder.parserExtension((String) input.get("parser"));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapOutputSection(Map<String, Object> data, EitriConfig.Builder builder) {
        Map<String, Object> output = (Map<String, Object>) data.get(SECTION_OUTPUT);
        if (output == null) return;

        if (output.containsKey("file")) {
            builder.outputPath(Path.of((String) output.get("file")));
        }
        if (output.containsKey("name")) {
            builder.diagramName((String) output.get("name"));
        }
        if (output.containsKey("writerExtension")) {
            builder.writerExtension((String) output.get("writerExtension"));
        } else if (output.containsKey("writer")) {
            builder.writerExtension((String) output.get("writer"));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapLayoutDirection(Map<String, Object> data, EitriConfig.Builder builder) {
        Map<String, Object> layout = (Map<String, Object>) data.get(SECTION_LAYOUT);
        if (layout != null && layout.containsKey("direction")) {
            builder.direction(LayoutDirection.fromString((String) layout.get("direction")));
        }
    }

    // ========================================================================
    // Config Merging
    // ========================================================================

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
            } catch (NumberFormatException _) {
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
