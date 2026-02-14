package no.ntnu.eitri.parser.java;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
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
import java.io.File;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
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
    private static final String JAR_EXTENSION = ".jar";
    private static final String SETTINGS_GRADLE = "settings.gradle";
    private static final String SETTINGS_GRADLE_KTS = "settings.gradle.kts";
    private static final String BUILD_GRADLE = "build.gradle";
    private static final String BUILD_GRADLE_KTS = "build.gradle.kts";
    private static final String GLOB_ALL = "*";
    private static final Pattern INCLUDE_QUOTED_MODULE = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern GRADLE_DEPENDENCY_COORDINATE = Pattern
            .compile(
                    "(?:implementation|api|compileOnly|runtimeOnly|testImplementation)\\s*\\(\\s*\"([^\"]+:[^\"]+:[^\"]+)\"\\s*\\)");
    private static final Pattern GRADLE_PLUGIN_ID = Pattern.compile("id\\(\\s*\"([^\"]+)\"\\s*\\)");
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

        configureParser(sourcePaths, runConfig.verbose());

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

    private void configureParser(List<Path> sourcePaths, boolean verbose) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver(false)); // JDK types only, no jrt module
        // Keep current process classpath visible to the solver. This is important when
        // Eitri itself is run from a fat jar, because that classpath often already
        // contains libraries needed to resolve source references.
        typeSolver.add(new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()));

        for (Path sourcePath : sourcePaths) {
            if (Files.isDirectory(sourcePath)) {
                for (Path root : detectTypeSolverRoots(sourcePath)) {
                    typeSolver.add(new JavaParserTypeSolver(root));
                }
            }
        }

        Set<Path> jarPaths = new LinkedHashSet<>();
        for (Path classpathEntry : detectClasspathEntries()) {
            if (Files.isRegularFile(classpathEntry) && classpathEntry.toString().endsWith(JAR_EXTENSION)) {
                jarPaths.add(classpathEntry);
            }
        }
        jarPaths.addAll(detectLocalJarFiles(sourcePaths));
        jarPaths.addAll(detectGradleDependencyJarFiles(sourcePaths));

        int addedJarSolvers = 0;
        for (Path jarPath : jarPaths) {
            try {
                typeSolver.add(new JarTypeSolver(jarPath.toString()));
                addedJarSolvers++;
            } catch (Exception | LinkageError e) {
                // Some jars in local caches are valid artifacts but still fail to load in
                // JavaParser/Javassist (module-info edge-cases, bytecode quirks, etc.).
                // We intentionally skip those jars to keep parsing best effort instead of
                // aborting the whole run.
                LOGGER.log(Level.FINE, "Failed to add jar type solver for: {0}", jarPath);
            }
        }

        if (verbose && addedJarSolvers > 0) {
            LOGGER.log(Level.INFO, "Added {0} jar(s) to symbol solver classpath", addedJarSolvers);
        }

        ParserConfiguration parserConfig = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver))
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);

        StaticJavaParser.setConfiguration(parserConfig);
    }

    static Set<Path> detectClasspathEntries() {
        return detectClasspathEntries(System.getProperty("java.class.path"), System.getenv("CLASSPATH"));
    }

    static Set<Path> detectClasspathEntries(String javaClassPath, String envClassPath) {
        Set<Path> entries = new LinkedHashSet<>();
        collectClasspathEntriesFromString(javaClassPath, entries);
        collectClasspathEntriesFromString(envClassPath, entries);
        return entries;
    }

    private static void collectClasspathEntriesFromString(String classpath, Set<Path> target) {
        if (classpath == null || classpath.isBlank()) {
            return;
        }

        String[] rawEntries = classpath.split(Pattern.quote(File.pathSeparator));
        for (String raw : rawEntries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String entry = raw.trim();
            if (entry.endsWith(File.separator + GLOB_ALL)) {
                Path dir = Path.of(entry.substring(0, entry.length() - 2)).toAbsolutePath().normalize();
                if (Files.isDirectory(dir)) {
                    // Classpath wildcards in Java are directory-local (dir/*), not recursive.
                    // Keep depth=1 here so behavior matches user expectations from JVM classpath
                    // semantics.
                    collectJarFilesFromDirectory(dir, target, 1);
                }
                continue;
            }

            Path path = Path.of(entry).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                target.add(path);
            }
        }
    }

    static Set<Path> detectLocalJarFiles(List<Path> sourcePaths) {
        Set<Path> jarFiles = new LinkedHashSet<>();
        Set<Path> candidateDirs = new LinkedHashSet<>();

        for (Path sourcePath : sourcePaths) {
            Path moduleRoot = detectModuleRoot(sourcePath);
            if (moduleRoot != null) {
                candidateDirs.add(moduleRoot.resolve("libs"));
                candidateDirs.add(moduleRoot.resolve("build/libs"));
                candidateDirs.add(moduleRoot.resolve("target/dependency"));
            }

            Path repoRoot = findGradleRepoRoot(moduleRoot != null ? moduleRoot : sourcePath);
            if (repoRoot != null) {
                candidateDirs.add(repoRoot.resolve("libs"));
                candidateDirs.add(repoRoot.resolve("build/libs"));
            }
        }

        for (Path dir : candidateDirs) {
            collectJarFilesFromDirectory(dir, jarFiles, 2);
        }

        return jarFiles;
    }

    static Set<Path> detectGradleDependencyJarFiles(List<Path> sourcePaths) {
        Set<Path> dependencyJars = new LinkedHashSet<>();
        Path gradleCacheRoot = Path.of(System.getProperty("user.home"), ".gradle", "caches", "modules-2",
                "files-2.1");

        Set<String> dependencyCoordinates = new LinkedHashSet<>();
        for (Path sourcePath : sourcePaths) {
            for (Path buildFile : detectGradleBuildFiles(sourcePath)) {
                Path repoRoot = findGradleRepoRoot(buildFile.getParent());
                dependencyCoordinates.addAll(collectDependencyCoordinatesFromBuildFile(buildFile, repoRoot));
            }
        }

        dependencyJars.addAll(resolveGradleDependencyJars(dependencyCoordinates, gradleCacheRoot));
        return dependencyJars;
    }

    private static Set<Path> detectGradleBuildFiles(Path sourcePath) {
        Set<Path> buildFiles = new LinkedHashSet<>();
        Path moduleRoot = detectModuleRoot(sourcePath);
        if (moduleRoot == null) {
            return buildFiles;
        }

        Path rootBuildFile = resolveBuildFile(moduleRoot);
        if (rootBuildFile != null) {
            buildFiles.add(rootBuildFile);
        }

        if (!Files.isDirectory(moduleRoot)) {
            return buildFiles;
        }

        try {
            Files.walkFileTree(moduleRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isExcludedBuildScanDirectory(moduleRoot, dir)) {
                        // Build outputs and Gradle internals can contain copied/generated
                        // build scripts that should not be interpreted as source module
                        // definitions.
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName() == null ? "" : file.getFileName().toString();
                    if (BUILD_GRADLE.equals(fileName) || BUILD_GRADLE_KTS.equals(fileName)) {
                        buildFiles.add(file.toAbsolutePath().normalize());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to scan Gradle build files under: {0}", moduleRoot);
        }

        return buildFiles;
    }

    private static boolean isExcludedBuildScanDirectory(Path moduleRoot, Path dir) {
        Path relative = moduleRoot.relativize(dir);
        if (relative.getNameCount() == 0) {
            return false;
        }
        String name = relative.getFileName() == null ? "" : relative.getFileName().toString();
        return ".git".equals(name)
                || ".gradle".equals(name)
                || "build".equals(name)
                || "target".equals(name);
    }

    private static Path resolveBuildFile(Path moduleRoot) {
        if (moduleRoot == null) {
            return null;
        }
        Path kts = moduleRoot.resolve(BUILD_GRADLE_KTS);
        if (Files.isRegularFile(kts)) {
            return kts;
        }
        Path groovy = moduleRoot.resolve(BUILD_GRADLE);
        if (Files.isRegularFile(groovy)) {
            return groovy;
        }
        return null;
    }

    static Set<String> parseGradleDependencyCoordinates(Path buildFile) {
        Set<String> coordinates = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(buildFile)) {
                Matcher matcher = GRADLE_DEPENDENCY_COORDINATE.matcher(line.trim());
                while (matcher.find()) {
                    coordinates.add(matcher.group(1));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to read build file: {0}", buildFile);
        }
        return coordinates;
    }

    private static Set<String> collectDependencyCoordinatesFromBuildFile(Path buildFile, Path repoRoot) {
        Set<String> coordinates = new LinkedHashSet<>();
        if (buildFile == null) {
            return coordinates;
        }

        Set<Path> visitedBuildFiles = new LinkedHashSet<>();
        Deque<Path> queue = new ArrayDeque<>();
        queue.add(buildFile);

        while (!queue.isEmpty()) {
            Path currentBuildFile = queue.removeFirst().toAbsolutePath().normalize();
            if (!visitedBuildFiles.add(currentBuildFile)) {
                continue;
            }

            coordinates.addAll(parseGradleDependencyCoordinates(currentBuildFile));

            // Follow precompiled convention plugins from buildSrc because projects like
            // jadx centralize key dependencies (e.g. slf4j) there instead of declaring
            // them directly in every module build file.
            for (String pluginId : parseAppliedGradlePluginIds(currentBuildFile)) {
                Path pluginBuildFile = resolveBuildSrcPluginFile(repoRoot, pluginId);
                if (pluginBuildFile != null) {
                    queue.addLast(pluginBuildFile);
                }
            }
        }

        return coordinates;
    }

    private static Set<String> parseAppliedGradlePluginIds(Path buildFile) {
        Set<String> pluginIds = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(buildFile)) {
                Matcher matcher = GRADLE_PLUGIN_ID.matcher(line.trim());
                while (matcher.find()) {
                    pluginIds.add(matcher.group(1));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to parse plugin IDs from build file: {0}", buildFile);
        }
        return pluginIds;
    }

    private static Path resolveBuildSrcPluginFile(Path repoRoot, String pluginId) {
        if (repoRoot == null || pluginId == null || pluginId.isBlank()) {
            return null;
        }
        Path kotlinPluginFile = repoRoot.resolve("buildSrc/src/main/kotlin")
                .resolve(pluginId + ".gradle.kts");
        if (Files.isRegularFile(kotlinPluginFile)) {
            return kotlinPluginFile;
        }

        Path groovyPluginFile = repoRoot.resolve("buildSrc/src/main/groovy")
                .resolve(pluginId + ".gradle");
        if (Files.isRegularFile(groovyPluginFile)) {
            return groovyPluginFile;
        }

        return null;
    }

    static Set<Path> resolveGradleDependencyJars(Set<String> dependencyCoordinates, Path gradleCacheRoot) {
        Set<Path> jars = new LinkedHashSet<>();
        if (dependencyCoordinates == null || dependencyCoordinates.isEmpty() || gradleCacheRoot == null
                || !Files.isDirectory(gradleCacheRoot)) {
            return jars;
        }

        for (String coordinate : dependencyCoordinates) {
            String[] parts = coordinate.split(":");
            if (parts.length < 3) {
                continue;
            }
            String group = parts[0];
            String artifact = parts[1];
            String version = parts[2];

            Path artifactDir = gradleCacheRoot.resolve(group).resolve(artifact).resolve(version);
            collectJarFilesFromDirectory(artifactDir, jars, 3);
            // Include sibling artifacts in the same group/version because some ecosystems
            // split APIs and implementations (for example directories-jni + directories).
            // Resolving only the direct artifact can leave otherwise importable types missing.
            collectGroupVersionJarFiles(gradleCacheRoot, group, version, jars);
        }
        return jars;
    }

    private static void collectGroupVersionJarFiles(Path gradleCacheRoot, String group, String version,
            Set<Path> jars) {
        Path groupDir = gradleCacheRoot.resolve(group);
        if (!Files.isDirectory(groupDir)) {
            return;
        }

        try (var artifacts = Files.list(groupDir)) {
            artifacts.filter(Files::isDirectory)
                    .map(artifactDir -> artifactDir.resolve(version))
                    .filter(Files::isDirectory)
                    .forEach(versionDir -> collectJarFilesFromDirectory(versionDir, jars, 3));
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to scan Gradle group directory: {0}", groupDir);
        }
    }

    private static void collectJarFilesFromDirectory(Path dir, Set<Path> target, int maxDepth) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (var stream = Files.walk(dir, maxDepth)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(JAR_EXTENSION))
                    .filter(JavaSourceParser::isBinaryJar)
                    .forEach(path -> target.add(path.toAbsolutePath().normalize()));
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to scan jar directory: {0}", dir);
        }
    }

    private static boolean isBinaryJar(Path jarPath) {
        String fileName = jarPath.getFileName() == null ? "" : jarPath.getFileName().toString();
        // Source/javadoc jars are intentionally excluded: they do not provide bytecode for
        // symbol solving and can trigger avoidable loader failures.
        return !fileName.endsWith("-sources.jar")
                && !fileName.endsWith("-javadoc.jar");
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
