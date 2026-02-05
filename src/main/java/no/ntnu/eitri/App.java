package no.ntnu.eitri;

import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import no.ntnu.eitri.parser.SourceParser;
import no.ntnu.eitri.parser.java.JavaSourceParser;
import no.ntnu.eitri.writer.DiagramWriter;
import no.ntnu.eitri.writer.WriteException;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;
import no.ntnu.eitri.config.ConfigResolution;
import no.ntnu.eitri.config.ConfigService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
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
            ConfigResolution resolution = resolveConfig();
            EitriConfig config = resolution.config();

            logResolvedConfig(resolution, config);

            UmlModel model = parseSources(config);

            if (dryRun) {
                runDryRun(model, config);
                return 0;
            }

            writeOutput(model, config);
            return 0;

        } catch (ConfigException e) {
            LOGGER.log(Level.SEVERE, "Configuration error: {0}", e.getMessage());
            return 1;
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "Parse error: {0}", e.getMessage());
            if (verbose && e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return 1;
        } catch (WriteException e) {
            LOGGER.log(Level.SEVERE, "Write error: {0}", e.getMessage());
            if (verbose && e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return 1;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error: {0}", e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private ConfigResolution resolveConfig() throws ConfigException {
        CliOptions cliOptions = new CliOptions(sourcePaths, outputPath, configPath, verbose, dryRun);
        ConfigService configService = new ConfigService();
        return configService.resolve(cliOptions);
    }

    private void logResolvedConfig(ConfigResolution resolution, EitriConfig config) {
        if (!verbose) {
            return;
        }

        if (resolution.configFileUsed() != null) {
            LOGGER.log(Level.INFO, "Using configuration file: {0}", resolution.configFileUsed());
        } else {
            LOGGER.info("Using default configuration (no config file found)");
        }
        LOGGER.log(Level.INFO, "Configuration: {0}", config);
    }

    private UmlModel parseSources(EitriConfig config) throws ParseException {
        SourceParser parser = new JavaSourceParser();
        if (verbose) {
            LOGGER.log(Level.INFO, "Parsing with {0}...", parser.getName());
        }

        UmlModel model = parser.parse(config.getSourcePaths(), config);

        if (verbose) {
            LOGGER.log(Level.INFO, "Parsed {0} types, {1} relations",
                    new Object[]{model.getTypes().size(), model.getRelations().size()});
        }

        return model;
    }

    private void runDryRun(UmlModel model, EitriConfig config) {
        LOGGER.log(Level.INFO, "Dry run: Parsed {0} types from {1} source path(s)",
                new Object[]{model.getTypes().size(), config.getSourcePaths().size()});
        LOGGER.log(Level.INFO, "         Would write to: {0}", config.getOutputPath());

        if (verbose) {
            DiagramWriter writer = new PlantUmlWriter();
            String rendered = writer.render(model, config);
            LOGGER.info("\n--- Generated PlantUML ---");
            LOGGER.info(rendered);
            LOGGER.info("--- End PlantUML ---\n");
        }
    }

    private void writeOutput(UmlModel model, EitriConfig config) throws WriteException {
        DiagramWriter writer = new PlantUmlWriter();
        writer.write(model, config, config.getOutputPath());

        LOGGER.log(Level.INFO, "Generated {0} with {1} types and {2} relations.",
                new Object[]{config.getOutputPath(), model.getTypes().size(), model.getRelations().size()});
    }
}
