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

        configureParser(sourcePaths);

        // Create parse context
        ParseContext context = new ParseContext(config);

        // Collect all Java files
        List<Path> javaFiles = collectJavaFiles(sourcePaths);

        if (config.isVerbose()) {
            LOGGER.log(Level.INFO, "Found {0} Java files to parse", javaFiles.size());
        }

        // Parse each file
        TypeVisitor typeVisitor = new TypeVisitor(context);
        int parsed = 0;
        int failed = 0;

        ParseStats stats = parseFiles(javaFiles, typeVisitor, context);
        parsed = stats.parsed();
        failed = stats.failed();

        if (config.isVerbose()) {
            LOGGER.log(Level.INFO, "Parsed {0} files successfully{1}", new Object[]{parsed, (failed > 0 ? ", " + failed + " failed" : "")});
            LOGGER.log(Level.INFO, "Found {0} types, {1} relations", new Object[]{context.getTypeCount(), context.getRelationCount()});
        }

        // Detect relations
        RelationDetector relationDetector = new RelationDetector(context);
        relationDetector.detectRelations();

        if (config.isVerbose()) {
            LOGGER.log(Level.INFO, "Detected {0} total relations (including detected)", context.getRelationCount());
            if (!context.getWarnings().isEmpty()) {
                LOGGER.log(Level.INFO, "Collected {0} warnings", context.getWarnings().size());
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
            collectFromSourcePath(sourcePath, javaFiles);
        }

        return javaFiles;
    }

    private void configureParser(List<Path> sourcePaths) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver(false));  // JDK types only, no jrt module

        for (Path sourcePath : sourcePaths) {
            if (Files.isDirectory(sourcePath)) {
                typeSolver.add(new JavaParserTypeSolver(sourcePath));
            }
        }

        ParserConfiguration parserConfig = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver))
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);

        StaticJavaParser.setConfiguration(parserConfig);
    }

    private ParseStats parseFiles(List<Path> javaFiles, TypeVisitor typeVisitor, ParseContext context) {
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
                LOGGER.info(() -> "Unexpected error parsing " + javaFile + " - " + e.getMessage());
                context.addWarning("Unexpected error parsing: " + javaFile + " - " + e.getMessage());
                failed++;
            }
        }

        return new ParseStats(parsed, failed);
    }

    private void collectFromSourcePath(Path sourcePath, List<Path> javaFiles) throws ParseException {
        if (!Files.exists(sourcePath)) {
            throw new ParseException("Source path does not exist: " + sourcePath);
        }

        if (Files.isRegularFile(sourcePath)) {
            addJavaFileIfMatches(sourcePath, javaFiles);
            return;
        }

        if (Files.isDirectory(sourcePath)) {
            walkDirectory(sourcePath, javaFiles);
        }
    }

    private void addJavaFileIfMatches(Path file, List<Path> javaFiles) {
        if (file.toString().endsWith(JAVA_EXTENSION)) {
            javaFiles.add(file);
        }
    }

    private void walkDirectory(Path sourcePath, List<Path> javaFiles) throws ParseException {
        try {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    addJavaFileIfMatches(file, javaFiles);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    LOGGER.log(Level.WARNING, "Failed to access file: {0}", file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ParseException("Failed to walk source path: " + sourcePath, e);
        }
    }

    private record ParseStats(int parsed, int failed) {}
}
