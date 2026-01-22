package no.ntnu.eitri;

/**
 * Eitri - Java to PlantUML class diagram generator.
 * <p>
 * A CLI tool that parses a Java source directory and generates a PlantUML
 * class diagram (.puml) without compiling or running the project.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * java -jar eitri.jar --src &lt;path&gt; --out &lt;file.puml&gt;
 * </pre>
 *
 * <h2>Arguments</h2>
 * <ul>
 *   <li>{@code --src} - Path to the root Java source folder</li>
 *   <li>{@code --out} - Output .puml file path</li>
 * </ul>
 */
public class App {

    public static void main(String[] args) {
        if (args.length == 0 || hasHelp(args)) {
            printUsage();
            System.exit(0);
        }

        // TODO: Implement CLI argument parsing (PHASE 2)
        // TODO: Implement JavaParser integration (PHASE 3)
        // TODO: Implement PlantUML writer (PHASE 3)

        System.err.println("Error: CLI not yet implemented. See --help for usage.");
        System.exit(1);
    }

    private static boolean hasHelp(String[] args) {
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("""
            Eitri - Java to PlantUML class diagram generator

            Usage:
              java -jar eitri.jar --src <path> --out <file.puml>

            Arguments:
              --src <path>       Path to the root Java source folder
              --out <file.puml>  Output PlantUML file path

            Options:
              --help, -h         Show this help message

            Example:
              java -jar eitri.jar --src ./src/main/java --out diagram.puml
            """);
    }
}
