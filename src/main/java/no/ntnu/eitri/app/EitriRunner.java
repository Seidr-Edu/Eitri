package no.ntnu.eitri.app;

import no.ntnu.eitri.app.registry.ParserRegistry;
import no.ntnu.eitri.app.registry.WriterRegistry;
import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.ConfigResolution;
import no.ntnu.eitri.config.ConfigService;
import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.config.OutputPathInitializer;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import no.ntnu.eitri.parser.SourceParser;
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
                LOGGER.log(Level.SEVERE, "Stack trace:", e);
            }
            return new RunResult(1, e.getMessage(), 0, 0, null, cliOptions.dryRun());
        }
    }

    private ConfigResolution resolveConfig(CliOptions cliOptions) throws ConfigException {
        ConfigService configService = new ConfigService();
        return configService.resolve(cliOptions);
    }

    private void logResolvedConfig(ConfigResolution resolution, EitriConfig config) {
        if (!config.isVerbose()) return;

        if (resolution.configFileUsed() != null) {
            LOGGER.log(Level.INFO, "Using configuration file: {0}", resolution.configFileUsed());
        } else {
            LOGGER.info("Using default configuration (no config file found)");
        }
        LOGGER.log(Level.INFO, "Configuration: {0}", config);
    }

    private UmlModel parseSources(EitriConfig config) throws ParseException {
        SourceParser parser = resolveParser(config);
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
            DiagramWriter writer = resolveWriter(config);
            String rendered = writer.render(model, config);
            LOGGER.info("\n--- Generated ---");
            LOGGER.info(rendered);
            LOGGER.info("--- End ---\n");
        }
    }

    private void writeOutput(UmlModel model, EitriConfig config) throws ConfigException, WriteException {
        OutputPathInitializer.initialize(config.getOutputPath());
        DiagramWriter writer = resolveWriter(config);
        writer.write(model, config, config.getOutputPath());

        LOGGER.log(Level.INFO, "Generated {0} with {1} types and {2} relations.",
                new Object[]{config.getOutputPath(), model.getTypes().size(), model.getRelations().size()});
    }

    private SourceParser resolveParser(EitriConfig config) {
        String extension = config.getParserExtension();
        if (extension == null) extension = detectSourceExtension(config);
        if (extension == null) extension = parserRegistry.getDefaultExtension();
        String resolvedExtension = extension;
        return parserRegistry.getByExtension(resolvedExtension)
                .orElseThrow(() -> new ParseException("No parser registered for extension: " + resolvedExtension));
    }

    private DiagramWriter resolveWriter(EitriConfig config) {
        String extension = config.getWriterExtension();
        if (extension == null) extension = extensionFromPath(config.getOutputPath());
        if (extension == null) extension = writerRegistry.getDefaultExtension();
        String resolvedExtension = extension;
        return writerRegistry.getByExtension(resolvedExtension)
                .orElseThrow(() -> new WriteException("No writer registered for extension: " + resolvedExtension, config.getOutputPath()));
    }

    private String detectSourceExtension(EitriConfig config) {
        for (Path sourcePath : config.getSourcePaths()) {
            if (sourcePath == null) continue;

            if (Files.isRegularFile(sourcePath)) {
                String extension = extensionFromPath(sourcePath);
                if (extension != null) {
                    return extension;
                }
            }
        }
        return null;
    }

    private String extensionFromPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return null;
        }
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return null;
        }
        return name.substring(idx).toLowerCase();
    }
}
