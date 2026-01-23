package no.ntnu.eitri.parser.java;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseContext;
import no.ntnu.eitri.parser.ParseException;
import no.ntnu.eitri.parser.SourceParser;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java source parser implementation using JavaParser library.
 * 
 * <p>This parser uses the JavaParser symbol solver for type resolution,
 * allowing accurate detection of inheritance and association relationships.
 */
public class JavaSourceParser implements SourceParser {

    private static final String JAVA_EXTENSION = ".java";
    private static final Logger LOGGER = Logger.getLogger(JavaSourceParser.class.getName());
    private static final String NAME = "JavaParser";
    private static final List<String> EXTENSIONS = List.of(JAVA_EXTENSION);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public UmlModel parse(List<Path> sourcePaths, EitriConfig config) throws ParseException {
        if (sourcePaths == null || sourcePaths.isEmpty()) {
            throw new ParseException("No source paths provided");
        }

        // Configure symbol solver
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver(false));  // JDK types only, no jrt module

        // Add each source path as a type solver
        for (Path sourcePath : sourcePaths) {
            if (Files.isDirectory(sourcePath)) {
                typeSolver.add(new JavaParserTypeSolver(sourcePath));
            }
        }

        // Configure parser
        ParserConfiguration parserConfig = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver))
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        StaticJavaParser.setConfiguration(parserConfig);

        // Create parse context
        ParseContext context = new ParseContext(config);

        // Collect all Java files
        List<Path> javaFiles = collectJavaFiles(sourcePaths);

        if (config.isVerbose()) {
            LOGGER.info("Found " + javaFiles.size() + " Java files to parse");
        }

        // Parse each file
        TypeVisitor typeVisitor = new TypeVisitor(context);
        int parsed = 0;
        int failed = 0;

        for (Path javaFile : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(javaFile);
                cu.accept(typeVisitor, null);
                parsed++;
            } catch (IOException e) {
                context.addWarning("Failed to read file: " + javaFile + " - " + e.getMessage());
                failed++;
            } catch (com.github.javaparser.ParseProblemException e) {
                context.addWarning("Failed to parse file: " + javaFile + " - " + e.getMessage());
                failed++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unexpected error parsing " + javaFile, e);
                context.addWarning("Unexpected error parsing: " + javaFile + " - " + e.getMessage());
                failed++;
            }
        }

        if (config.isVerbose()) {
            LOGGER.info("Parsed " + parsed + " files successfully" + (failed > 0 ? ", " + failed + " failed" : ""));
            LOGGER.info("Found " + context.getTypeCount() + " types, " + context.getRelationCount() + " relations");
        }

        // Detect relations
        RelationDetector relationDetector = new RelationDetector(context);
        relationDetector.detectRelations();

        if (config.isVerbose()) {
            LOGGER.info("Detected " + context.getRelationCount() + " total relations (including detected)");
            if (!context.getWarnings().isEmpty()) {
                LOGGER.info("Collected " + context.getWarnings().size() + " warnings");
            }
        }

        // Build and return the model
        return context.build();
    }

    /**
     * Recursively collects all Java files from the given source paths.
     */
    private List<Path> collectJavaFiles(List<Path> sourcePaths) throws ParseException {
        List<Path> javaFiles = new ArrayList<>();

        for (Path sourcePath : sourcePaths) {
            if (!Files.exists(sourcePath)) {
                throw new ParseException("Source path does not exist: " + sourcePath);
            }

            if (Files.isRegularFile(sourcePath)) {
                if (sourcePath.toString().endsWith(".java")) {
                    javaFiles.add(sourcePath);
                }
            } else if (Files.isDirectory(sourcePath)) {
                try {
                    Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (file.toString().endsWith(".java")) {
                                javaFiles.add(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            LOGGER.warning("Failed to access file: " + file);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new ParseException("Failed to walk source path: " + sourcePath, e);
                }
            }
        }

        return javaFiles;
    }
}
