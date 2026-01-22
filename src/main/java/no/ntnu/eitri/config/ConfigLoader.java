package no.ntnu.eitri.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads and merges configuration from multiple sources.
 * <p>
 * Resolution order (later overrides earlier):
 * <ol>
 *   <li>Built-in defaults (new EitriConfig())</li>
 *   <li>.eitri.yaml in working directory (if exists)</li>
 *   <li>Explicit --config file (if specified)</li>
 *   <li>CLI flags (applied by caller)</li>
 * </ol>
 */
public final class ConfigLoader {

    public static final String DEFAULT_CONFIG_FILENAME = ".eitri.yaml";

    private ConfigLoader() {
        // Utility class
    }

    /**
     * Loads configuration with the standard resolution order.
     *
     * @param explicitConfigPath optional explicit config file path (from --config)
     * @return merged configuration
     * @throws ConfigException if config file exists but cannot be parsed
     */
    public static EitriConfig load(Path explicitConfigPath) throws ConfigException {
        // Start with defaults
        EitriConfig config = new EitriConfig();

        // Try to load .eitri.yaml from working directory
        Path workingDirConfig = Path.of(System.getProperty("user.dir"), DEFAULT_CONFIG_FILENAME);
        if (Files.exists(workingDirConfig)) {
            EitriConfig workingConfig = loadFromYaml(workingDirConfig);
            merge(config, workingConfig);
        }

        // Load explicit config if specified
        if (explicitConfigPath != null) {
            if (!Files.exists(explicitConfigPath)) {
                throw new ConfigException("Config file not found: " + explicitConfigPath);
            }
            EitriConfig explicitConfig = loadFromYaml(explicitConfigPath);
            merge(config, explicitConfig);
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
            if (data == null) {
                return new EitriConfig();
            }
            return mapToConfig(data);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file: " + path, e);
        } catch (Exception e) {
            throw new ConfigException("Failed to parse config file: " + path + " - " + e.getMessage(), e);
        }
    }

    /**
     * Converts a YAML map to EitriConfig.
     */
    @SuppressWarnings("unchecked")
    private static EitriConfig mapToConfig(Map<String, Object> data) {
        EitriConfig config = new EitriConfig();

        // Input section
        if (data.containsKey("input")) {
            Map<String, Object> input = (Map<String, Object>) data.get("input");
            if (input.containsKey("sources")) {
                List<String> sources = (List<String>) input.get("sources");
                for (String src : sources) {
                    config.addSourcePath(Path.of(src));
                }
            }
        }

        // Output section
        if (data.containsKey("output")) {
            Map<String, Object> output = (Map<String, Object>) data.get("output");
            if (output.containsKey("file")) {
                config.setOutputPath(Path.of((String) output.get("file")));
            }
            if (output.containsKey("name")) {
                config.setDiagramName((String) output.get("name"));
            }
        }

        // Layout section
        if (data.containsKey("layout")) {
            Map<String, Object> layout = (Map<String, Object>) data.get("layout");
            if (layout.containsKey("direction")) {
                config.setDirection(LayoutDirection.fromString((String) layout.get("direction")));
            }
            if (layout.containsKey("groupInheritance")) {
                config.setGroupInheritance(toInt(layout.get("groupInheritance")));
            }
            if (layout.containsKey("classAttributeIconSize")) {
                config.setClassAttributeIconSize(toInt(layout.get("classAttributeIconSize")));
            }
        }

        // Visibility section
        if (data.containsKey("visibility")) {
            Map<String, Object> vis = (Map<String, Object>) data.get("visibility");
            if (vis.containsKey("hidePrivate")) {
                config.setHidePrivate(toBool(vis.get("hidePrivate")));
            }
            if (vis.containsKey("hideProtected")) {
                config.setHideProtected(toBool(vis.get("hideProtected")));
            }
            if (vis.containsKey("hidePackage")) {
                config.setHidePackage(toBool(vis.get("hidePackage")));
            }
        }

        // Members section
        if (data.containsKey("members")) {
            Map<String, Object> members = (Map<String, Object>) data.get("members");
            if (members.containsKey("hideFields")) {
                config.setHideFields(toBool(members.get("hideFields")));
            }
            if (members.containsKey("hideMethods")) {
                config.setHideMethods(toBool(members.get("hideMethods")));
            }
            if (members.containsKey("hideEmptyMembers")) {
                config.setHideEmptyMembers(toBool(members.get("hideEmptyMembers")));
            }
        }

        // Display section
        if (data.containsKey("display")) {
            Map<String, Object> display = (Map<String, Object>) data.get("display");
            if (display.containsKey("hideCircle")) {
                config.setHideCircle(toBool(display.get("hideCircle")));
            }
            if (display.containsKey("hideUnlinked")) {
                config.setHideUnlinked(toBool(display.get("hideUnlinked")));
            }
            if (display.containsKey("showStereotypes")) {
                config.setShowStereotypes(toBool(display.get("showStereotypes")));
            }
            if (display.containsKey("showGenerics")) {
                config.setShowGenerics(toBool(display.get("showGenerics")));
            }
            if (display.containsKey("showNotes")) {
                config.setShowNotes(toBool(display.get("showNotes")));
            }
            if (display.containsKey("showMultiplicities")) {
                config.setShowMultiplicities(toBool(display.get("showMultiplicities")));
            }
            if (display.containsKey("showLabels")) {
                config.setShowLabels(toBool(display.get("showLabels")));
            }
        }

        // Relations section
        if (data.containsKey("relations")) {
            Map<String, Object> rel = (Map<String, Object>) data.get("relations");
            if (rel.containsKey("showInheritance")) {
                config.setShowInheritance(toBool(rel.get("showInheritance")));
            }
            if (rel.containsKey("showImplements")) {
                config.setShowImplements(toBool(rel.get("showImplements")));
            }
            if (rel.containsKey("showComposition")) {
                config.setShowComposition(toBool(rel.get("showComposition")));
            }
            if (rel.containsKey("showAggregation")) {
                config.setShowAggregation(toBool(rel.get("showAggregation")));
            }
            if (rel.containsKey("showAssociation")) {
                config.setShowAssociation(toBool(rel.get("showAssociation")));
            }
            if (rel.containsKey("showDependency")) {
                config.setShowDependency(toBool(rel.get("showDependency")));
            }
        }

        // Skinparam section
        if (data.containsKey("skinparam")) {
            Map<String, Object> skin = (Map<String, Object>) data.get("skinparam");
            if (skin.containsKey("custom")) {
                List<String> custom = (List<String>) skin.get("custom");
                for (String line : custom) {
                    config.addSkinparamLine(line);
                }
            }
            // Also support direct skinparam entries
            if (skin.containsKey("classAttributeIconSize")) {
                config.setClassAttributeIconSize(toInt(skin.get("classAttributeIconSize")));
            }
            if (skin.containsKey("groupInheritance")) {
                config.setGroupInheritance(toInt(skin.get("groupInheritance")));
            }
        }

        return config;
    }

    /**
     * Merges source config into target, only overriding non-default values.
     * This is a simple merge that copies all explicitly set values.
     */
    private static void merge(EitriConfig target, EitriConfig source) {
        // Source paths are additive
        if (!source.getSourcePaths().isEmpty()) {
            for (Path p : source.getSourcePaths()) {
                target.addSourcePath(p);
            }
        }

        // Output path overrides
        if (source.getOutputPath() != null) {
            target.setOutputPath(source.getOutputPath());
        }

        // Diagram name
        if (!"diagram".equals(source.getDiagramName())) {
            target.setDiagramName(source.getDiagramName());
        }

        // Layout
        if (source.getDirection() != LayoutDirection.TOP_TO_BOTTOM) {
            target.setDirection(source.getDirection());
        }
        if (source.getGroupInheritance() > 0) {
            target.setGroupInheritance(source.getGroupInheritance());
        }
        if (source.getClassAttributeIconSize() > 0) {
            target.setClassAttributeIconSize(source.getClassAttributeIconSize());
        }

        // Visibility - copy all (these are explicit toggles)
        // Note: This is a simplified merge; in practice you might want 
        // to track which values were explicitly set
        if (source.isHidePrivate()) target.setHidePrivate(true);
        if (source.isHideProtected()) target.setHideProtected(true);
        if (source.isHidePackage()) target.setHidePackage(true);

        // Members
        if (source.isHideFields()) target.setHideFields(true);
        if (source.isHideMethods()) target.setHideMethods(true);
        // hideEmptyMembers defaults to true, so only override if false
        if (!source.isHideEmptyMembers()) target.setHideEmptyMembers(false);

        // Display
        if (source.isHideCircle()) target.setHideCircle(true);
        if (source.isHideUnlinked()) target.setHideUnlinked(true);
        if (!source.isShowStereotypes()) target.setShowStereotypes(false);
        if (!source.isShowGenerics()) target.setShowGenerics(false);
        if (source.isShowNotes()) target.setShowNotes(true);
        if (!source.isShowMultiplicities()) target.setShowMultiplicities(false);
        if (!source.isShowLabels()) target.setShowLabels(false);

        // Relations
        if (!source.isShowInheritance()) target.setShowInheritance(false);
        if (!source.isShowImplements()) target.setShowImplements(false);
        if (!source.isShowComposition()) target.setShowComposition(false);
        if (!source.isShowAggregation()) target.setShowAggregation(false);
        if (!source.isShowAssociation()) target.setShowAssociation(false);
        if (!source.isShowDependency()) target.setShowDependency(false);

        // Skinparam lines are additive
        for (String line : source.getSkinparamLines()) {
            target.addSkinparamLine(line);
        }

        // Runtime
        if (source.isVerbose()) target.setVerbose(true);
        if (source.isDryRun()) target.setDryRun(true);
    }

    private static int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static boolean toBool(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
}
