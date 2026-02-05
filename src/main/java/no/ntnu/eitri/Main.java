package no.ntnu.eitri;

import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.runner.EitriRunner;
import no.ntnu.eitri.runner.RunResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

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
public class Main implements Callable<Integer> {

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
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CliOptions cliOptions = new CliOptions(sourcePaths, outputPath, configPath, verbose, dryRun);
        EitriRunner runner = new EitriRunner();
        RunResult result = runner.run(cliOptions);
        return result.exitCode();
    }
}
