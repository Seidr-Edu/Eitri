package no.ntnu.eitri.parser.java;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import no.ntnu.eitri.config.RunConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseContext;
import no.ntnu.eitri.parser.ParseException;
import no.ntnu.eitri.parser.ParseReport;
import no.ntnu.eitri.parser.SourceParser;
import no.ntnu.eitri.parser.resolution.TypeResolutionStats;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java source parser implementation using JavaParser library.
 */
public class JavaSourceParser implements SourceParser {

    private static final String JAVA_EXTENSION = ".java";
    private static final String SETTINGS_GRADLE = "settings.gradle";
    private static final String SETTINGS_GRADLE_KTS = "settings.gradle.kts";
    private static final Pattern INCLUDE_QUOTED_MODULE = Pattern.compile("\"([^\"]+)\"");
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
    public UmlModel parse(List<Path> sourcePaths, RunConfig runConfig) throws ParseException {
        if (sourcePaths == null || sourcePaths.isEmpty()) {
            throw new ParseException("No source paths provided");
        }

        configureParser(sourcePaths);

        ParseContext context = new ParseContext(runConfig.verbose());
        List<Path> javaFiles = collectJavaFiles(sourcePaths);

        if (runConfig.verbose()) {
            LOGGER.log(Level.INFO, "Found {0} Java files to parse", javaFiles.size());
        }

        TypeVisitor typeVisitor = new TypeVisitor(context);

        ParseStats stats = parseFiles(javaFiles, typeVisitor, context);

        if (runConfig.verbose()) {
            LOGGER.log(Level.INFO, "Parsed {0} files successfully{1}",
                    new Object[] { stats.parsed(), (stats.failed() > 0 ? ", " + stats.failed() + " failed" : "") });
            LOGGER.log(Level.INFO, "Found {0} types, {1} relations",
                    new Object[] { context.getTypeCount(), context.getRelationCount() });
        }

        RelationDetector relationDetector = new RelationDetector(context);
        relationDetector.detectRelations();

        if (runConfig.verbose()) {
            ParseReport report = context.getReport();
            LOGGER.log(Level.INFO, "Detected {0} total relations (including detected)", context.getRelationCount());
            logTypeResolutionStats(report.typeResolutionStats());
            if (report.warningCount() > 0) {
                LOGGER.log(Level.INFO, "Collected {0} warnings", report.warningCount());
            }
        }

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
        typeSolver.add(new ReflectionTypeSolver(false)); // JDK types only, no jrt module

        for (Path sourcePath : sourcePaths) {
            if (Files.isDirectory(sourcePath)) {
                for (Path root : detectTypeSolverRoots(sourcePath)) {
                    typeSolver.add(new JavaParserTypeSolver(root));
                }
            }
        }

        ParserConfiguration parserConfig = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver))
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);

        StaticJavaParser.setConfiguration(parserConfig);
    }

    /**
     * Detects parser roots for symbol resolution.
     *
     * <p>
     * If the provided path is a module root (e.g. contains {@code src/main/java}),
     * include that source root so package-relative resolution works.
     */
    static Set<Path> detectTypeSolverRoots(Path sourcePath) {
        Set<Path> roots = new LinkedHashSet<>();
        roots.add(sourcePath);

        Path mainJava = sourcePath.resolve("src/main/java");
        if (Files.isDirectory(mainJava)) {
            roots.add(mainJava);
        }

        roots.addAll(detectGradleSiblingSourceRoots(sourcePath));

        return roots;
    }

    static Set<Path> detectGradleSiblingSourceRoots(Path sourcePath) {
        Set<Path> roots = new LinkedHashSet<>();

        Path moduleRoot = detectModuleRoot(sourcePath);
        Path repoRoot = findGradleRepoRoot(moduleRoot);
        if (repoRoot == null) {
            return roots;
        }

        Path settingsFile = resolveSettingsFile(repoRoot);
        if (settingsFile == null) {
            return roots;
        }

        for (String moduleId : parseIncludedModuleIds(settingsFile)) {
            Path moduleDir = repoRoot.resolve(moduleId.replace(':', '/'));
            Path moduleMainJava = moduleDir.resolve("src/main/java");
            if (Files.isDirectory(moduleMainJava)) {
                roots.add(moduleMainJava);
            }
        }

        return roots;
    }

    private static Path detectModuleRoot(Path sourcePath) {
        if (sourcePath == null) {
            return null;
        }
        Path normalized = sourcePath.normalize();
        int count = normalized.getNameCount();
        if (count >= 3
                && "src".equals(normalized.getName(count - 3).toString())
                && "main".equals(normalized.getName(count - 2).toString())
                && "java".equals(normalized.getName(count - 1).toString())) {
            return normalized.getParent().getParent().getParent();
        }
        return normalized;
    }

    private static Path findGradleRepoRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (resolveSettingsFile(current) != null) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Path resolveSettingsFile(Path dir) {
        if (dir == null) {
            return null;
        }
        Path kts = dir.resolve(SETTINGS_GRADLE_KTS);
        if (Files.isRegularFile(kts)) {
            return kts;
        }
        Path groovy = dir.resolve(SETTINGS_GRADLE);
        if (Files.isRegularFile(groovy)) {
            return groovy;
        }
        return null;
    }

    static Set<String> parseIncludedModuleIds(Path settingsFile) {
        Set<String> moduleIds = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(settingsFile)) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("include(")) {
                    continue;
                }
                Matcher matcher = INCLUDE_QUOTED_MODULE.matcher(trimmed);
                while (matcher.find()) {
                    moduleIds.add(matcher.group(1));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to read settings file: {0}", settingsFile);
        }
        return moduleIds;
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
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isTestDirectory(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

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

    /**
     * Returns {@code true} if the directory is a test source root.
     * Matches directories named {@code test} directly under a {@code src} parent,
     * following the standard Maven/Gradle {@code src/test/java} convention.
     */
    static boolean isTestDirectory(Path dir) {
        Path name = dir.getFileName();
        Path parent = dir.getParent();
        return name != null
                && parent != null
                && name.toString().equals("test")
                && parent.getFileName() != null
                && parent.getFileName().toString().equals("src");
    }

    private void logTypeResolutionStats(TypeResolutionStats stats) {
        LOGGER.info(() -> "Type reference resolution: "
                + stats.totalRequests()
                + " requests, "
                + stats.resolvedReferences()
                + " resolved ("
                + stats.reusedKnownTypes()
                + " known type(s)), "
                + stats.skippedTotal()
                + " skipped");

        if (stats.skippedTotal() > 0) {
            LOGGER.info(() -> "Type reference skips by reason: non-FQN=" + stats.skippedNonFqn()
                    + ", unknown-FQN=" + stats.skippedUnknownFqn()
                    + ", primitive=" + stats.skippedPrimitive()
                    + ", wildcard=" + stats.skippedWildcard()
                    + ", empty=" + stats.skippedNullOrEmpty());
        }
    }

    private record ParseStats(int parsed, int failed) {
    }
}
