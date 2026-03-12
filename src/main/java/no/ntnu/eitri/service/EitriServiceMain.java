package no.ntnu.eitri.service;

import java.nio.file.Path;

/**
 * Service-mode entrypoint for container execution.
 */
public final class EitriServiceMain {

    private EitriServiceMain() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        System.exit(exitCode);
    }

    static int run(String[] args) {
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            printHelp();
            return 0;
        }
        if (args.length > 0) {
            System.err.println("eitri-service does not accept positional arguments. Use /run/config/manifest.yaml.");
            return 1;
        }

        Path runDir = Path.of(System.getenv().getOrDefault("EITRI_SERVICE_RUN_DIR", "/run"));
        Path inputDir = Path.of(System.getenv().getOrDefault("EITRI_SERVICE_INPUT_DIR", "/input/repo"));
        Path manifestPath = Path.of(System.getenv().getOrDefault(
                "EITRI_MANIFEST",
                runDir.resolve("config").resolve("manifest.yaml").toString()));

        return new EitriService(inputDir, runDir, manifestPath).run();
    }

    private static void printHelp() {
        System.out.println("""
                Usage: eitri-service

                Container contract:
                  Read-only:
                    /input/repo
                    /run/config/manifest.yaml
                  Writable:
                    /run

                Environment:
                  EITRI_MANIFEST           Optional manifest override (default: /run/config/manifest.yaml)
                  EITRI_SERVICE_INPUT_DIR  Optional input root override for tests
                  EITRI_SERVICE_RUN_DIR    Optional run root override for tests
                """);
    }
}
