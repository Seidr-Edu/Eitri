package no.ntnu.eitri;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.visitor.ClassCollectorVisitor;
import no.ntnu.eitri.writer.PlantUmlWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CLI entry point for generating PlantUML class diagrams from Java source code.
 * 
 * This tool parses Java source files using JavaParser, extracts class structures,
 * relationships (inheritance, implementation, associations), and outputs a PlantUML
 * diagram that can be rendered using PlantUML tools.
 * 
 * Usage:
 *   java -jar eitri.jar --src /path/to/src --out /path/to/output.puml
 * 
 * @see <a href="https://plantuml.com/class-diagram">PlantUML Class Diagrams</a>
 */
public class App {

    public static void main(String[] args) throws IOException {
        Map<String, String> argMap = parseArgs(args);

        String srcDir = argMap.get("--src");
        String outFile = argMap.get("--out");

        if (srcDir == null || outFile == null) {
            System.err.println("Usage: java -jar java-uml-generator.jar --src <srcDir> --out <output.puml>");
            System.exit(1);
        }

        Path srcPath = new File(srcDir).toPath();
        if (!Files.isDirectory(srcPath)) {
            System.err.println("Source directory does not exist: " + srcDir);
            System.exit(1);
        }

        UmlModel model = parseSourceDirectory(srcPath);
        
        PlantUmlWriter writer = new PlantUmlWriter();
        writer.write(model, outFile);
        
        System.out.println("Wrote PlantUML to " + outFile);
    }

    /**
     * Parses command-line arguments into a key-value map.
     * 
     * Expects arguments in the format: --key value --key2 value2
     * Uses a while loop with explicit index control to properly skip
     * over the value after consuming a --key pair.
     * 
     * @param args command-line arguments from main()
     * @return map of argument keys (including --) to their values
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        int i = 0;
        while (i < args.length - 1) {
            if (args[i].startsWith("--")) {
                map.put(args[i], args[i + 1]);
                // Skip both the key and value by advancing by 2
                i += 2;
            } else {
                // Unknown argument format, skip it
                i++;
            }
        }
        return map;
    }

    /**
     * Recursively parses all Java files in a directory and builds a UML model.
     * 
     * Uses JavaParser to parse source files and a visitor pattern to extract
     * class/interface/enum declarations along with their members and relationships.
     * 
     * @param srcPath root directory containing Java source files
     * @return populated UmlModel containing all discovered types and relations
     * @throws IOException if directory traversal fails
     */
    private static UmlModel parseSourceDirectory(Path srcPath) throws IOException {
        UmlModel model = new UmlModel();

        // Configure JavaParser for modern Java syntax (switch expressions, records, etc.)
        // Must match the Java version used in pom.xml to avoid parsing errors
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);

        // Use try-with-resources to ensure the directory stream is closed
        // Files.walk recursively traverses all subdirectories
        try (Stream<Path> paths = Files.walk(srcPath)) {
            // Collect to list first to avoid stream reuse issues
            List<Path> javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path p : javaFiles) {
                try {
                    // Parse the source file into an AST (Abstract Syntax Tree)
                    CompilationUnit cu = StaticJavaParser.parse(p);
                    // Visit all nodes in the AST to collect UML-relevant information
                    cu.accept(new ClassCollectorVisitor(model), null);
                } catch (Exception e) {
                    // Continue processing other files even if one fails
                    // Common causes: syntax errors, unsupported language features
                    System.err.println("Failed to parse " + p + ": " + e.getMessage());
                }
            }
        }

        return model;
    }
}
