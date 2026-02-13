package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.config.RunConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
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
    }

    @Test
    void isTestDirectoryRejectsNonTestPaths() {
        assertFalse(JavaSourceParser.isTestDirectory(Path.of("/project/src/main")));
        assertFalse(JavaSourceParser.isTestDirectory(Path.of("/project/test")));
        assertFalse(JavaSourceParser.isTestDirectory(Path.of("/project/src/test/java")));
        assertFalse(JavaSourceParser.isTestDirectory(Path.of("test")));
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
}
