package no.ntnu.eitri;

import no.ntnu.eitri.app.EitriRunner;
import no.ntnu.eitri.app.RunResult;
import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.cli.ManifestVersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Eitri - Class diagram generator.
 * <p>
 * A CLI tool that parses a source directory and generates a UML
 * class diagram without compiling or running the project.
 */
@Command(
        name = "eitri",
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        description = "Generate UML class diagrams from source code.",
        sortOptions = false
)
public class Main implements Callable<Integer> {

    // === Required Arguments ===

    @Option(
            names = {"--src", "-s"},
            description = "Path to source folder(s). Can be specified multiple times.",
            required = true,
            paramLabel = "<path>"
    )
    private List<Path> sourcePaths = new ArrayList<>();

    @Option(
            names = {"--out", "-o"},
            description = "Output UML file path.",
            required = true,
            paramLabel = "<file>"
    )
    private Path outputPath;

    @Option(
            names = "--parser",
            description = "Parser extension id (e.g., .java).",
            paramLabel = "<ext>"
    )
    private String parserExtension;

    @Option(
            names = "--writer",
            description = "Writer extension id (e.g., .puml).",
            paramLabel = "<ext>"
    )
    private String writerExtension;

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
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
                CliOptions cliOptions = new CliOptions(
                        sourcePaths,
                        outputPath,
                        configPath,
                        parserExtension,
                        writerExtension,
                        verbose,
                        dryRun
                );
        EitriRunner runner = new EitriRunner();
        RunResult result = runner.run(cliOptions);
        return result.exitCode();
    }
}
