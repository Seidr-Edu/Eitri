package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.config.RunConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceParserBehaviorTest {

    @TempDir
    Path tempDir;

    @Test
    void throwsWhenSourcePathDoesNotExist() {
        JavaSourceParser parser = new JavaSourceParser();
        Path missing = tempDir.resolve("missing");
        RunConfig runConfig = new RunConfig(List.of(missing), tempDir.resolve("out.puml"), null, null, false, false);

        ParseException exception = assertThrows(ParseException.class,
                () -> parser.parse(List.of(missing), runConfig));
        assertTrue(exception.getMessage().contains("Source path does not exist"));
    }

    @Test
    void throwsWhenSourcePathsListIsNull() {
        JavaSourceParser parser = new JavaSourceParser();
        RunConfig runConfig = new RunConfig(List.of(tempDir), tempDir.resolve("out.puml"), null, null, false, false);

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(null, runConfig));
        assertTrue(exception.getMessage().contains("No source paths provided"));
    }

    @Test
    void throwsWhenSourcePathsListIsEmpty() {
        JavaSourceParser parser = new JavaSourceParser();
        RunConfig runConfig = new RunConfig(List.of(tempDir), tempDir.resolve("out.puml"), null, null, false, false);

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(List.of(), runConfig));
        assertTrue(exception.getMessage().contains("No source paths provided"));
    }

    @Test
    void continuesParsingWhenOneFileHasSyntaxError() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Good.java"), "package com.example; public class Good {}");
        Files.writeString(src.resolve("Bad.java"), "package com.example; public class Bad {");

        JavaSourceParser parser = new JavaSourceParser();
        RunConfig runConfig = new RunConfig(List.of(src), tempDir.resolve("out.puml"), null, null, false, false);
        UmlModel model = parser.parse(List.of(src), runConfig);

        assertTrue(model.hasType("com.example.Good"));
    }

    @Test
    void supportsParsingSingleJavaFilePath() throws Exception {
        Path file = tempDir.resolve("Single.java");
        Files.writeString(file, "package com.example; public class Single {}");

        JavaSourceParser parser = new JavaSourceParser();
        RunConfig runConfig = new RunConfig(List.of(file), tempDir.resolve("out.puml"), null, null, false, false);
        UmlModel model = parser.parse(List.of(file), runConfig);

        assertEquals(1, model.getTypes().stream().filter(t -> t.getFqn().equals("com.example.Single")).count());
    }

    @Test
    void skipsTestDirectoriesByDefault() throws Exception {
        Path mainSrc = tempDir.resolve("src/main/java");
        Path testSrc = tempDir.resolve("src/test/java");
        Files.createDirectories(mainSrc);
        Files.createDirectories(testSrc);
        Files.writeString(mainSrc.resolve("Main.java"), "package com.example; public class Main {}");
        Files.writeString(testSrc.resolve("MainTest.java"), "package com.example; public class MainTest {}");

        JavaSourceParser parser = new JavaSourceParser();
        RunConfig runConfig = new RunConfig(List.of(tempDir), tempDir.resolve("out.puml"), null, null, false, false);
        UmlModel model = parser.parse(List.of(tempDir), runConfig);

        assertTrue(model.hasType("com.example.Main"));
        assertFalse(model.hasType("com.example.MainTest"));
    }

    @Test
    void isTestDirectoryMatchesSrcTest() {
        assertTrue(JavaSourceParser.isTestDirectory(Path.of("/project/src/test")));
        assertTrue(JavaSourceParser.isTestDirectory(Path.of("src/test")));
        assertTrue(JavaSourceParser.isTestDirectory(Path.of("/project/test")));
        assertTrue(JavaSourceParser.isTestDirectory(Path.of("/project/tests")));
        assertTrue(JavaSourceParser.isTestDirectory(Path.of("test")));
    }

    @Test
    void isTestDirectoryRejectsNonTestPaths() {
        assertFalse(JavaSourceParser.isTestDirectory(Path.of("/project/src/main")));
        assertFalse(JavaSourceParser.isTestDirectory(Path.of("/project/src/test/java")));
        assertFalse(JavaSourceParser.isTestDirectory(Path.of("/project/src/contest")));
    }

    @Test
    void skipsTopLevelTestAndTestsDirectories() throws Exception {
        Path mainSrc = tempDir.resolve("src/main/java");
        Path topLevelTest = tempDir.resolve("test/unit");
        Path topLevelTests = tempDir.resolve("tests/integration");
        Files.createDirectories(mainSrc);
        Files.createDirectories(topLevelTest);
        Files.createDirectories(topLevelTests);

        Files.writeString(mainSrc.resolve("Main.java"), "package com.example; public class Main {}");
        Files.writeString(topLevelTest.resolve("UnitOnly.java"), "package com.example; public class UnitOnly {}");
        Files.writeString(topLevelTests.resolve("IntegrationOnly.java"),
                "package com.example; public class IntegrationOnly {}");

        JavaSourceParser parser = new JavaSourceParser();
        RunConfig runConfig = new RunConfig(List.of(tempDir), tempDir.resolve("out.puml"), null, null, false, false);
        UmlModel model = parser.parse(List.of(tempDir), runConfig);

        assertTrue(model.hasType("com.example.Main"));
        assertFalse(model.hasType("com.example.UnitOnly"));
        assertFalse(model.hasType("com.example.IntegrationOnly"));
    }

    @Test
    void detectTypeSolverRootsIncludesSrcMainJavaForModuleRoot() throws Exception {
        Path moduleRoot = tempDir.resolve("module");
        Files.createDirectories(moduleRoot.resolve("src/main/java"));

        Set<Path> roots = JavaSourceParser.detectTypeSolverRoots(moduleRoot);

        assertTrue(roots.contains(moduleRoot));
        assertTrue(roots.contains(moduleRoot.resolve("src/main/java")));
    }

    @Test
    void moduleRootPathResolvesInModuleTypesFromSrcMainJava() throws Exception {
        Path moduleRoot = tempDir.resolve("jadx-cli");
        Path mainJava = moduleRoot.resolve("src/main/java");
        Path pkg = mainJava.resolve("com/example");
        Files.createDirectories(pkg);

        Files.writeString(pkg.resolve("B.java"), "package com.example; public class B {}\n");
        Files.writeString(pkg.resolve("A.java"), "package com.example; public class A { private B b; }\n");

        JavaSourceParser parser = new JavaSourceParser();
        RunConfig runConfig = new RunConfig(List.of(moduleRoot), tempDir.resolve("out.puml"), null, null, false, false);
        UmlModel model = parser.parse(List.of(moduleRoot), runConfig);

        assertTrue(model.hasType("com.example.A"));
        assertTrue(model.hasType("com.example.B"));
        assertTrue(model.getRelations().stream().anyMatch(r -> r.getFromTypeFqn().equals("com.example.A")
                && r.getToTypeFqn().equals("com.example.B")));
    }

    @Test
    void parseIncludedModuleIdsReadsGradleSettingsIncludes() throws Exception {
        Path settings = tempDir.resolve("settings.gradle.kts");
        Files.writeString(settings, """
                rootProject.name = \"demo\"
                include(\"a\")
                include(\"b:c\", \"d:e:f\")
                """);

        Set<String> modules = JavaSourceParser.parseIncludedModuleIds(settings);

        assertTrue(modules.contains("a"));
        assertTrue(modules.contains("b:c"));
        assertTrue(modules.contains("d:e:f"));
    }

    @Test
    void parseIncludedModuleIdsReturnsEmptyForMissingFile() {
        Path missing = tempDir.resolve("missing.settings.gradle.kts");

        Set<String> modules = JavaSourceParser.parseIncludedModuleIds(missing);

        assertTrue(modules.isEmpty());
    }

    @Test
    void detectTypeSolverRootsIncludesSiblingModulesFromGradleSettings() throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.writeString(repoRoot.resolve("settings.gradle.kts"), """
                rootProject.name = \"demo\"
                include(\"mod-a\")
                include(\"mod-b\")
                """);

        Path modA = repoRoot.resolve("mod-a");
        Path modB = repoRoot.resolve("mod-b");
        Files.createDirectories(modA.resolve("src/main/java"));
        Files.createDirectories(modB.resolve("src/main/java"));

        Set<Path> roots = JavaSourceParser.detectTypeSolverRoots(modA);

        assertTrue(roots.contains(modA));
        assertTrue(roots.contains(modA.resolve("src/main/java")));
        assertTrue(roots.contains(modB.resolve("src/main/java")));
    }

    @Test
    void detectGradleSiblingSourceRootsReturnsEmptyWhenSettingsFileMissing() throws Exception {
        Path repoRoot = tempDir.resolve("repo-no-settings");
        Path module = repoRoot.resolve("mod");
        Files.createDirectories(module.resolve("src/main/java"));

        Set<Path> roots = JavaSourceParser.detectGradleSiblingSourceRoots(module);

        assertTrue(roots.isEmpty());
    }

    @Test
    void detectClasspathEntriesParsesJavaClasspathAndEnvClasspath() throws Exception {
        Path jar1 = tempDir.resolve("a.jar");
        Path jar2 = tempDir.resolve("b.jar");
        Files.writeString(jar1, "");
        Files.writeString(jar2, "");

        String javaClasspath = jar1.toString();
        String envClasspath = jar2.toString();

        Set<Path> entries = JavaSourceParser.detectClasspathEntries(javaClasspath, envClasspath);

        assertTrue(entries.contains(jar1.toAbsolutePath().normalize()));
        assertTrue(entries.contains(jar2.toAbsolutePath().normalize()));
    }

    @Test
    void detectClasspathEntriesExpandsWildcardJarDirectory() throws Exception {
        Path libs = tempDir.resolve("libs");
        Files.createDirectories(libs);
        Path libJar = libs.resolve("dep.jar");
        Files.writeString(libJar, "");

        String javaClasspath = libs.toString() + File.separator + "*";
        Set<Path> entries = JavaSourceParser.detectClasspathEntries(javaClasspath, null);

        assertTrue(entries.contains(libJar.toAbsolutePath().normalize()));
    }

    @Test
    void detectClasspathEntriesSkipsBlankAndMissingEntries() throws Exception {
        Path presentJar = tempDir.resolve("present.jar");
        Files.writeString(presentJar, "");

        String classpath = "   " + File.pathSeparator + tempDir.resolve("missing.jar")
                + File.pathSeparator + presentJar;
        Set<Path> entries = JavaSourceParser.detectClasspathEntries(classpath, null);

        assertEquals(1, entries.size());
        assertTrue(entries.contains(presentJar.toAbsolutePath().normalize()));
    }

    @Test
    void detectLocalJarFilesFindsModuleAndRepoLibs() throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.writeString(repoRoot.resolve("settings.gradle.kts"), "include(\"mod\")");

        Path moduleRoot = repoRoot.resolve("mod");
        Path moduleJar = moduleRoot.resolve("build/libs/mod.jar");
        Path repoJar = repoRoot.resolve("libs/repo.jar");
        Files.createDirectories(moduleJar.getParent());
        Files.createDirectories(repoJar.getParent());
        Files.writeString(moduleJar, "");
        Files.writeString(repoJar, "");

        Set<Path> jarFiles = JavaSourceParser.detectLocalJarFiles(List.of(moduleRoot));

        assertTrue(jarFiles.contains(moduleJar.toAbsolutePath().normalize()));
        assertTrue(jarFiles.contains(repoJar.toAbsolutePath().normalize()));
    }

    @Test
    void parseGradleDependencyCoordinatesFindsJarCoordinates() throws Exception {
        Path buildFile = tempDir.resolve("build.gradle.kts");
        Files.writeString(buildFile, """
                dependencies {
                  implementation("io.get-coursier.util:directories-jni:0.1.4")
                  compileOnly("org.jetbrains:annotations:24.1.0")
                }
                """);

        Set<String> coordinates = JavaSourceParser.parseGradleDependencyCoordinates(buildFile);

        assertTrue(coordinates.contains("io.get-coursier.util:directories-jni:0.1.4"));
        assertTrue(coordinates.contains("org.jetbrains:annotations:24.1.0"));
    }

    @Test
    void resolveGradleDependencyJarsFindsJarsInGradleCache() throws Exception {
        Path gradleCacheRoot = tempDir.resolve(".gradle/caches/modules-2/files-2.1");
        Path jar = gradleCacheRoot
                .resolve("io.get-coursier.util")
                .resolve("directories-jni")
                .resolve("0.1.4")
                .resolve("abc123")
                .resolve("directories-jni-0.1.4.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "");

        Set<Path> jars = JavaSourceParser.resolveGradleDependencyJars(
                Set.of("io.get-coursier.util:directories-jni:0.1.4"),
                gradleCacheRoot);

        assertTrue(jars.contains(jar.toAbsolutePath().normalize()));
    }

    @Test
    void resolveGradleDependencyJarsIncludesGroupVersionCompanionArtifacts() throws Exception {
        Path gradleCacheRoot = tempDir.resolve(".gradle/caches/modules-2/files-2.1");
        Path directJar = gradleCacheRoot
                .resolve("io.get-coursier.util")
                .resolve("directories-jni")
                .resolve("0.1.4")
                .resolve("aaa111")
                .resolve("directories-jni-0.1.4.jar");
        Path companionJar = gradleCacheRoot
                .resolve("io.get-coursier.util")
                .resolve("directories")
                .resolve("0.1.4")
                .resolve("bbb222")
                .resolve("directories-0.1.4.jar");
        Path sourcesJar = gradleCacheRoot
                .resolve("io.get-coursier.util")
                .resolve("directories")
                .resolve("0.1.4")
                .resolve("ccc333")
                .resolve("directories-0.1.4-sources.jar");

        Files.createDirectories(directJar.getParent());
        Files.createDirectories(companionJar.getParent());
        Files.createDirectories(sourcesJar.getParent());
        Files.writeString(directJar, "");
        Files.writeString(companionJar, "");
        Files.writeString(sourcesJar, "");

        Set<Path> jars = JavaSourceParser.resolveGradleDependencyJars(
                Set.of("io.get-coursier.util:directories-jni:0.1.4"),
                gradleCacheRoot);

        assertTrue(jars.contains(directJar.toAbsolutePath().normalize()));
        assertTrue(jars.contains(companionJar.toAbsolutePath().normalize()));
        assertFalse(jars.contains(sourcesJar.toAbsolutePath().normalize()));
    }

    @Test
    void resolveGradleDependencyJarsSkipsMalformedCoordinates() throws Exception {
        Path gradleCacheRoot = tempDir.resolve(".gradle/caches/modules-2/files-2.1");
        Files.createDirectories(gradleCacheRoot);

        Set<Path> jars = JavaSourceParser.resolveGradleDependencyJars(
                Set.of("org.example:missingVersion", "notACoordinate"),
                gradleCacheRoot);

        assertTrue(jars.isEmpty());
    }

    @Test
    void detectGradleDependencyJarFilesIncludesBuildSrcPluginDependencies() throws Exception {
        Path originalUserHome = Path.of(System.getProperty("user.home"));
        Path fakeHome = tempDir.resolve("home");
        Files.createDirectories(fakeHome);
        System.setProperty("user.home", fakeHome.toString());
        try {
            Path repoRoot = tempDir.resolve("repo");
            Path moduleRoot = repoRoot.resolve("mod");
            Files.createDirectories(moduleRoot);
            Files.writeString(moduleRoot.resolve("build.gradle.kts"), """
                    plugins {
                        id("my-lib")
                    }
                    """);
            Path pluginFile = repoRoot.resolve("buildSrc/src/main/kotlin/my-lib.gradle.kts");
            Files.createDirectories(pluginFile.getParent());
            Files.writeString(pluginFile, """
                    plugins {
                        id("my-base")
                    }
                    dependencies {
                        implementation("org.example:via-lib:1.0.0")
                    }
                    """);
            Path basePluginFile = repoRoot.resolve("buildSrc/src/main/kotlin/my-base.gradle.kts");
            Files.createDirectories(basePluginFile.getParent());
            Files.writeString(basePluginFile, """
                    dependencies {
                        implementation("org.example:via-base:2.0.0")
                    }
                    """);
            Files.writeString(repoRoot.resolve("settings.gradle.kts"), "include(\"mod\")");

            Path gradleCacheRoot = fakeHome.resolve(".gradle/caches/modules-2/files-2.1");
            Path viaLibJar = gradleCacheRoot
                    .resolve("org.example")
                    .resolve("via-lib")
                    .resolve("1.0.0")
                    .resolve("x1")
                    .resolve("via-lib-1.0.0.jar");
            Path viaBaseJar = gradleCacheRoot
                    .resolve("org.example")
                    .resolve("via-base")
                    .resolve("2.0.0")
                    .resolve("x2")
                    .resolve("via-base-2.0.0.jar");
            Files.createDirectories(viaLibJar.getParent());
            Files.createDirectories(viaBaseJar.getParent());
            Files.writeString(viaLibJar, "");
            Files.writeString(viaBaseJar, "");

            Set<Path> jars = JavaSourceParser.detectGradleDependencyJarFiles(List.of(moduleRoot));

            assertTrue(jars.contains(viaLibJar.toAbsolutePath().normalize()));
            assertTrue(jars.contains(viaBaseJar.toAbsolutePath().normalize()));
        } finally {
            System.setProperty("user.home", originalUserHome.toString());
        }
    }

    @Test
    void detectGradleDependencyJarFilesIncludesChildModuleBuildFilesFromAggregateSource() throws Exception {
        Path originalUserHome = Path.of(System.getProperty("user.home"));
        Path fakeHome = tempDir.resolve("home2");
        Files.createDirectories(fakeHome);
        System.setProperty("user.home", fakeHome.toString());
        try {
            Path repoRoot = tempDir.resolve("repo");
            Path aggregate = repoRoot.resolve("commons");
            Path appModule = aggregate.resolve("app");
            Path zipModule = aggregate.resolve("zip");
            Files.createDirectories(appModule);
            Files.createDirectories(zipModule);
            Files.writeString(repoRoot.resolve("settings.gradle.kts"), "include(\"commons:app\", \"commons:zip\")");

            Files.writeString(appModule.resolve("build.gradle.kts"), """
                    dependencies {
                      implementation("org.example:dep-app:1.0.0")
                    }
                    """);
            Files.writeString(zipModule.resolve("build.gradle.kts"), """
                    dependencies {
                      implementation("org.example:dep-zip:2.0.0")
                    }
                    """);

            Path gradleCacheRoot = fakeHome.resolve(".gradle/caches/modules-2/files-2.1");
            Path appJar = gradleCacheRoot
                    .resolve("org.example")
                    .resolve("dep-app")
                    .resolve("1.0.0")
                    .resolve("a1")
                    .resolve("dep-app-1.0.0.jar");
            Path zipJar = gradleCacheRoot
                    .resolve("org.example")
                    .resolve("dep-zip")
                    .resolve("2.0.0")
                    .resolve("z2")
                    .resolve("dep-zip-2.0.0.jar");
            Files.createDirectories(appJar.getParent());
            Files.createDirectories(zipJar.getParent());
            Files.writeString(appJar, "");
            Files.writeString(zipJar, "");

            Set<Path> jars = JavaSourceParser.detectGradleDependencyJarFiles(List.of(aggregate));

            assertTrue(jars.contains(appJar.toAbsolutePath().normalize()));
            assertTrue(jars.contains(zipJar.toAbsolutePath().normalize()));
        } finally {
            System.setProperty("user.home", originalUserHome.toString());
        }
    }
}
