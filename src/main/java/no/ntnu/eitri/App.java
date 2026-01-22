package no.ntnu.eitri;

import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.ConfigLoader;
import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.config.LayoutDirection;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import no.ntnu.eitri.parser.SourceParser;
import no.ntnu.eitri.parser.java.JavaSourceParser;
import no.ntnu.eitri.writer.DiagramWriter;
import no.ntnu.eitri.writer.WriteException;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Eitri - Java to PlantUML class diagram generator.
 * <p>
 * A CLI tool that parses a Java source directory and generates a PlantUML
 * class diagram (.puml) without compiling or running the project.
 */
@Command(
        name = "eitri",
        mixinStandardHelpOptions = true,
        version = "eitri 1.0-SNAPSHOT",
        description = "Generate PlantUML class diagrams from Java source code.",
        sortOptions = false
)
public class App implements Callable<Integer> {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    // === Required Arguments ===

    @Option(
            names = {"--src", "-s"},
            description = "Path to Java source folder(s). Can be specified multiple times.",
            required = true,
            paramLabel = "<path>"
    )
    private List<Path> sourcePaths = new ArrayList<>();

    @Option(
            names = {"--out", "-o"},
            description = "Output .puml file path.",
            required = true,
            paramLabel = "<file>"
    )
    private Path outputPath;

    // === Config File ===

    @Option(
            names = {"--config", "-c"},
            description = "Load configuration from YAML file.",
            paramLabel = "<file>"
    )
    private Path configPath;

    // === Layout Options ===

    @Option(
            names = {"--direction", "-d"},
            description = "Layout direction: 'tb' (top-to-bottom) or 'lr' (left-to-right). Default: tb",
            paramLabel = "<dir>"
    )
    private String direction;

    @Option(
            names = "--group-inheritance",
            description = "Group N+ inheritance arrows together. Default: 0 (disabled)",
            paramLabel = "<n>"
    )
    private Integer groupInheritance;

    @Option(
            names = "--name",
            description = "Diagram name in @startuml. Default: diagram",
            paramLabel = "<name>"
    )
    private String diagramName;

    // === Visibility Filtering ===

    @Option(names = "--hide-private", description = "Hide private members.")
    private Boolean hidePrivate;

    @Option(names = "--hide-protected", description = "Hide protected members.")
    private Boolean hideProtected;

    @Option(names = "--hide-package", description = "Hide package-private members.")
    private Boolean hidePackage;

    // === Member Filtering ===

    @Option(names = "--hide-fields", description = "Hide all fields.")
    private Boolean hideFields;

    @Option(names = "--hide-methods", description = "Hide all methods.")
    private Boolean hideMethods;

    @Option(names = "--hide-empty-members", description = "Hide empty member compartments. Default: true")
    private Boolean hideEmptyMembers;

    @Option(names = "--show-empty-members", description = "Show empty member compartments.")
    private Boolean showEmptyMembers;

    // === Display Options ===

    @Option(names = "--hide-circle", description = "Hide type icons (C/I/E circles).")
    private Boolean hideCircle;

    @Option(names = "--hide-unlinked", description = "Hide types with no relations.")
    private Boolean hideUnlinked;

    @Option(names = "--no-stereotypes", description = "Hide stereotypes.")
    private Boolean noStereotypes;

    @Option(names = "--no-generics", description = "Hide generic type parameters.")
    private Boolean noGenerics;

    @Option(names = "--show-notes", description = "Include Javadoc as notes.")
    private Boolean showNotes;

    @Option(names = "--no-multiplicities", description = "Hide relation multiplicities.")
    private Boolean noMultiplicities;

    @Option(names = "--no-labels", description = "Hide relation labels.")
    private Boolean noLabels;

    // === Relation Filtering ===

    @Option(names = "--hide-inheritance", description = "Hide extends relations.")
    private Boolean hideInheritance;

    @Option(names = "--hide-implements", description = "Hide implements relations.")
    private Boolean hideImplements;

    @Option(names = "--hide-composition", description = "Hide composition relations.")
    private Boolean hideComposition;

    @Option(names = "--hide-aggregation", description = "Hide aggregation relations.")
    private Boolean hideAggregation;

    @Option(names = "--hide-association", description = "Hide association relations.")
    private Boolean hideAssociation;

    @Option(names = "--hide-dependency", description = "Hide dependency relations.")
    private Boolean hideDependency;

    // === Runtime Options ===

    @Option(names = {"-v", "--verbose"}, description = "Verbose output.")
    private boolean verbose;

    @Option(names = "--dry-run", description = "Parse only, don't write output file.")
    private boolean dryRun;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            // Load and merge configuration
            EitriConfig config = buildConfig();

            // Validate inputs
            List<String> errors = validateConfig(config);
            if (!errors.isEmpty()) {
                for (String error : errors) {
                    System.err.println("Error: " + error);
                }
                return 1;
            }

            if (verbose) {
                System.out.println("Configuration: " + config);
            }

            // Parse source files
            SourceParser parser = new JavaSourceParser();
            if (verbose) {
                System.out.println("Parsing with " + parser.getName() + "...");
            }

            UmlModel model = parser.parse(config.getSourcePaths(), config);

            if (verbose) {
                System.out.println("Parsed " + model.getTypes().size() + " types, " 
                        + model.getRelations().size() + " relations");
            }

            if (dryRun) {
                System.out.println("Dry run: Parsed " + model.getTypes().size() + " types from " 
                        + config.getSourcePaths().size() + " source path(s)");
                System.out.println("         Would write to: " + config.getOutputPath());
                
                // In verbose mode, show rendered output
                if (verbose) {
                    DiagramWriter writer = new PlantUmlWriter();
                    String rendered = writer.render(model, config);
                    System.out.println("\n--- Generated PlantUML ---");
                    System.out.println(rendered);
                    System.out.println("--- End PlantUML ---\n");
                }
                return 0;
            }

            // Write output
            DiagramWriter writer = new PlantUmlWriter();
            writer.write(model, config, config.getOutputPath());

            System.out.println("Generated " + config.getOutputPath() + " with " 
                    + model.getTypes().size() + " types and " 
                    + model.getRelations().size() + " relations.");

            return 0;

        } catch (ConfigException e) {
            System.err.println("Configuration error: " + e.getMessage());
            return 1;
        } catch (ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            if (verbose && e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return 1;
        } catch (WriteException e) {
            System.err.println("Write error: " + e.getMessage());
            if (verbose && e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return 1;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    /**
     * Builds the final configuration by merging config file and CLI options.
     */
    private EitriConfig buildConfig() throws ConfigException {
        // Check for config sources and log
        Path workingDirConfig = Path.of(System.getProperty("user.dir"), ConfigLoader.DEFAULT_CONFIG_FILENAME);
        boolean hasWorkingDirConfig = Files.exists(workingDirConfig);
        boolean hasExplicitConfig = configPath != null;

        if (verbose) {
            if (hasExplicitConfig) {
                LOGGER.info("Loading configuration from: " + configPath);
            } else if (hasWorkingDirConfig) {
                LOGGER.info("Loading configuration from: " + workingDirConfig);
            } else {
                LOGGER.info("Using default configuration (no config file found)");
            }
        }

        // Load from files (defaults -> .eitri.config.yaml -> --config)
        EitriConfig config = ConfigLoader.load(configPath);

        // Apply CLI overrides (highest priority)
        applyCliOverrides(config);

        return config;
    }

    /**
     * Applies command-line options to the configuration.
     * CLI options have highest priority and override file-based config.
     */
    private void applyCliOverrides(EitriConfig config) {
        // Required options
        for (Path src : sourcePaths) {
            config.addSourcePath(src);
        }
        config.setOutputPath(outputPath);

        // Layout
        if (direction != null) {
            config.setDirection(LayoutDirection.fromString(direction));
        }
        if (groupInheritance != null) {
            config.setGroupInheritance(groupInheritance);
        }
        if (diagramName != null) {
            config.setDiagramName(diagramName);
        }

        // Visibility
        if (Boolean.TRUE.equals(hidePrivate)) {
            config.setHidePrivate(true);
        }
        if (Boolean.TRUE.equals(hideProtected)) {
            config.setHideProtected(true);
        }
        if (Boolean.TRUE.equals(hidePackage)) {
            config.setHidePackage(true);
        }

        // Members
        if (Boolean.TRUE.equals(hideFields)) {
            config.setHideFields(true);
        }
        if (Boolean.TRUE.equals(hideMethods)) {
            config.setHideMethods(true);
        }
        if (hideEmptyMembers != null) {
            config.setHideEmptyMembers(hideEmptyMembers);
        }
        if (Boolean.TRUE.equals(showEmptyMembers)) {
            config.setHideEmptyMembers(false);
        }

        // Display
        if (Boolean.TRUE.equals(hideCircle)) {
            config.setHideCircle(true);
        }
        if (Boolean.TRUE.equals(hideUnlinked)) {
            config.setHideUnlinked(true);
        }
        if (Boolean.TRUE.equals(noStereotypes)) {
            config.setShowStereotypes(false);
        }
        if (Boolean.TRUE.equals(noGenerics)) {
            config.setShowGenerics(false);
        }
        if (Boolean.TRUE.equals(showNotes)) {
            config.setShowNotes(true);
        }
        if (Boolean.TRUE.equals(noMultiplicities)) {
            config.setShowMultiplicities(false);
        }
        if (Boolean.TRUE.equals(noLabels)) {
            config.setShowLabels(false);
        }

        // Relations
        if (Boolean.TRUE.equals(hideInheritance)) {
            config.setShowInheritance(false);
        }
        if (Boolean.TRUE.equals(hideImplements)) {
            config.setShowImplements(false);
        }
        if (Boolean.TRUE.equals(hideComposition)) {
            config.setShowComposition(false);
        }
        if (Boolean.TRUE.equals(hideAggregation)) {
            config.setShowAggregation(false);
        }
        if (Boolean.TRUE.equals(hideAssociation)) {
            config.setShowAssociation(false);
        }
        if (Boolean.TRUE.equals(hideDependency)) {
            config.setShowDependency(false);
        }

        // Runtime
        config.setVerbose(verbose);
        config.setDryRun(dryRun);
    }

    /**
     * Validates the configuration and returns a list of errors.
     *
     * @param config the configuration to validate
     * @return list of error messages (empty if valid)
     */
    private List<String> validateConfig(EitriConfig config) {
        List<String> errors = new ArrayList<>();

        // Validate source paths
        if (config.getSourcePaths().isEmpty()) {
            errors.add("At least one source path (--src) is required.");
        } else {
            for (Path src : config.getSourcePaths()) {
                if (!Files.exists(src)) {
                    errors.add("Source path does not exist: " + src);
                } else if (!Files.isDirectory(src)) {
                    errors.add("Source path is not a directory: " + src);
                }
            }
        }

        // Validate output path
        if (config.getOutputPath() == null) {
            errors.add("Output path (--out) is required.");
        } else {
            Path parent = config.getOutputPath().getParent();
            if (parent != null && !Files.exists(parent)) {
                // Try to create parent directories
                try {
                    Files.createDirectories(parent);
                } catch (Exception e) {
                    errors.add("Cannot create output directory: " + parent);
                }
            }
            if (parent != null && Files.exists(parent) && !Files.isWritable(parent)) {
                errors.add("Output directory is not writable: " + parent);
            }
        }

        return errors;
    }
}
