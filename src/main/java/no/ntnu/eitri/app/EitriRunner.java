package no.ntnu.eitri.app;

import no.ntnu.eitri.app.registry.ParserRegistry;
import no.ntnu.eitri.app.registry.WriterRegistry;
import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.ConfigResolution;
import no.ntnu.eitri.config.ConfigService;
import no.ntnu.eitri.config.OutputPathInitializer;
import no.ntnu.eitri.config.RunConfig;
import no.ntnu.eitri.config.WriterConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import no.ntnu.eitri.parser.SourceParser;
import no.ntnu.eitri.util.PathExtension;
import no.ntnu.eitri.writer.DiagramWriter;
import no.ntnu.eitri.writer.WriteException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates a full Eitri run from config resolution to output.
 */
public class EitriRunner {

    private static final Logger LOGGER = Logger.getLogger(EitriRunner.class.getName());
    private final ParserRegistry parserRegistry;
    private final WriterRegistry writerRegistry;

    public EitriRunner() {
        this(ParserRegistry.defaultRegistry(), WriterRegistry.defaultRegistry());
    }

    public EitriRunner(ParserRegistry parserRegistry, WriterRegistry writerRegistry) {
        this.parserRegistry = Objects.requireNonNull(parserRegistry, "parserRegistry");
        this.writerRegistry = Objects.requireNonNull(writerRegistry, "writerRegistry");
    }

    public RunResult run(CliOptions cliOptions) {
        try {
            ConfigResolution resolution = resolveConfig(cliOptions);
            RunConfig runConfig = resolution.runConfig();

            logResolvedConfig(resolution);

            UmlModel model = parseSources(runConfig);

            if (runConfig.dryRun()) {
                runDryRun(model, runConfig, resolution);
                return new RunResult(
                        0,
                        null,
                        model.getTypes().size(),
                        model.getRelations().size(),
                        runConfig.outputPath(),
                        true);
            }

            writeOutput(model, runConfig, resolution);
            return new RunResult(
                    0,
                    null,
                    model.getTypes().size(),
                    model.getRelations().size(),
                    runConfig.outputPath(),
                    false);

        } catch (ConfigException e) {
            LOGGER.log(Level.SEVERE, "Configuration error: {0}", e.getMessage());
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "Parse error: {0}", e.getMessage());
            if (cliOptions.verbose() && e.getCause() != null) {
                LOGGER.log(Level.SEVERE, "Cause:", e.getCause());
            }
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        } catch (WriteException e) {
            LOGGER.log(Level.SEVERE, "Write error: {0}", e.getMessage());
            if (cliOptions.verbose() && e.getCause() != null) {
                LOGGER.log(Level.SEVERE, "Cause:", e.getCause());
            }
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error: {0}", e.getMessage());
            if (cliOptions.verbose()) {
                LOGGER.log(Level.SEVERE, "Stack trace:", e);
            }
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        }
    }

    private ConfigResolution resolveConfig(CliOptions cliOptions) throws ConfigException {
        ConfigService configService = new ConfigService();
        return configService.resolve(cliOptions);
    }

    private void logResolvedConfig(ConfigResolution resolution) {
        if (!resolution.runConfig().verbose()) {
            return;
        }

        if (resolution.configFileUsed() != null) {
            LOGGER.log(Level.INFO, "Using configuration file: {0}", resolution.configFileUsed());
        } else {
            LOGGER.info("Using default configuration (no config file found)");
        }
        LOGGER.log(Level.INFO, "Run config: {0}", resolution.runConfig());
        LOGGER.log(Level.INFO, "PlantUML config: {0}", resolution.plantUmlConfig());
    }

    private UmlModel parseSources(RunConfig runConfig) throws ParseException {
        SourceParser parser = resolveParser(runConfig);
        if (runConfig.verbose()) {
            LOGGER.log(Level.INFO, "Parsing with {0}...", parser.getName());
        }

        UmlModel model = parser.parse(runConfig.sourcePaths(), runConfig);

        if (runConfig.verbose()) {
            LOGGER.log(Level.INFO, "Parsed {0} types, {1} relations",
                    new Object[] { model.getTypes().size(), model.getRelations().size() });
        }

        return model;
    }

    private void runDryRun(UmlModel model, RunConfig runConfig, ConfigResolution resolution) throws ConfigException {
        LOGGER.log(Level.INFO, "Dry run: Parsed {0} types from {1} source path(s)",
                new Object[] { model.getTypes().size(), runConfig.sourcePaths().size() });
        LOGGER.log(Level.INFO, "         Would write to: {0}", runConfig.outputPath());

        if (runConfig.verbose()) {
            DiagramWriter<?> writer = resolveWriter(runConfig);
            String rendered = renderWithResolvedConfig(writer, model, resolution);
            LOGGER.info("\n--- Generated ---");
            LOGGER.info(rendered);
            LOGGER.info("--- End ---\n");
        }
    }

    private void writeOutput(UmlModel model, RunConfig runConfig, ConfigResolution resolution)
            throws ConfigException, WriteException {
        OutputPathInitializer.initialize(runConfig.outputPath());
        DiagramWriter<?> writer = resolveWriter(runConfig);
        writeWithResolvedConfig(writer, model, runConfig.outputPath(), resolution);

        LOGGER.log(Level.INFO, "Generated {0}",
                new Object[] { runConfig.outputPath() });
    }

    private SourceParser resolveParser(RunConfig runConfig) {
        String extension = runConfig.parserExtension();
        if (extension == null) {
            extension = detectSourceExtension(runConfig);
        }
        if (extension == null) {
            extension = parserRegistry.getDefaultExtension();
        }
        String resolvedExtension = extension;
        return parserRegistry.getByExtension(resolvedExtension)
                .orElseThrow(() -> new ParseException("No parser registered for extension: " + resolvedExtension));
    }

    private DiagramWriter<?> resolveWriter(RunConfig runConfig) {
        String extension = runConfig.writerExtension();
        if (extension == null) {
            extension = PathExtension.fromPath(runConfig.outputPath());
        }
        if (extension == null) {
            extension = writerRegistry.getDefaultExtension();
        }
        String resolvedExtension = extension;
        return writerRegistry.getByExtension(resolvedExtension)
                .orElseThrow(
                        () -> new WriteException(
                                "No writer registered for extension: " + resolvedExtension,
                                runConfig.outputPath()));
    }

    private <C extends WriterConfig> String renderWithResolvedConfig(
            DiagramWriter<C> writer, UmlModel model, ConfigResolution resolution) throws ConfigException {
        C config = resolution.writerConfig(writer.configType());
        return writer.render(model, config);
    }

    private <C extends WriterConfig> void writeWithResolvedConfig(
            DiagramWriter<C> writer, UmlModel model, Path outputPath, ConfigResolution resolution)
            throws ConfigException, WriteException {
        C config = resolution.writerConfig(writer.configType());
        writer.write(model, config, outputPath);
    }

    private String detectSourceExtension(RunConfig runConfig) {
        for (Path sourcePath : runConfig.sourcePaths()) {
            if (sourcePath == null) {
                continue;
            }

            if (Files.isRegularFile(sourcePath)) {
                String extension = PathExtension.fromPath(sourcePath);
                if (extension != null) {
                    return extension;
                }
            }
        }
        return null;
    }
}
