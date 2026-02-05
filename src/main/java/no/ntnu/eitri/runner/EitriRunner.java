package no.ntnu.eitri.runner;

import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.ConfigResolution;
import no.ntnu.eitri.config.ConfigService;
import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import no.ntnu.eitri.parser.SourceParser;
import no.ntnu.eitri.parser.java.JavaSourceParser;
import no.ntnu.eitri.writer.DiagramWriter;
import no.ntnu.eitri.writer.WriteException;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates a full Eitri run from config resolution to output.
 */
public class EitriRunner {

    private static final Logger LOGGER = Logger.getLogger(EitriRunner.class.getName());

    public RunResult run(CliOptions cliOptions) {
        try {
            ConfigResolution resolution = resolveConfig(cliOptions);
            EitriConfig config = resolution.config();

            logResolvedConfig(resolution, config);

            UmlModel model = parseSources(config);

            if (config.isDryRun()) {
                runDryRun(model, config);
                return new RunResult(0, null, model.getTypes().size(), model.getRelations().size(), config.getOutputPath(), true);
            }

            writeOutput(model, config);
            return new RunResult(0, null, model.getTypes().size(), model.getRelations().size(), config.getOutputPath(), false);

        } catch (ConfigException e) {
            LOGGER.log(Level.SEVERE, "Configuration error: {0}", e.getMessage());
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "Parse error: {0}", e.getMessage());
            if (cliOptions.verbose() && e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        } catch (WriteException e) {
            LOGGER.log(Level.SEVERE, "Write error: {0}", e.getMessage());
            if (cliOptions.verbose() && e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error: {0}", e.getMessage());
            if (cliOptions.verbose()) {
                e.printStackTrace();
            }
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        }
    }

    private ConfigResolution resolveConfig(CliOptions cliOptions) throws ConfigException {
        ConfigService configService = new ConfigService();
        return configService.resolve(cliOptions);
    }

    private void logResolvedConfig(ConfigResolution resolution, EitriConfig config) {
        if (!config.isVerbose()) {
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
        if (config.isVerbose()) {
            LOGGER.log(Level.INFO, "Parsing with {0}...", parser.getName());
        }

        UmlModel model = parser.parse(config.getSourcePaths(), config);

        if (config.isVerbose()) {
            LOGGER.log(Level.INFO, "Parsed {0} types, {1} relations",
                    new Object[]{model.getTypes().size(), model.getRelations().size()});
        }

        return model;
    }

    private void runDryRun(UmlModel model, EitriConfig config) {
        LOGGER.log(Level.INFO, "Dry run: Parsed {0} types from {1} source path(s)",
                new Object[]{model.getTypes().size(), config.getSourcePaths().size()});
        LOGGER.log(Level.INFO, "         Would write to: {0}", config.getOutputPath());

        if (config.isVerbose()) {
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
