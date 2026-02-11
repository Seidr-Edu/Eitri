package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceParserBehaviorTest {

    @TempDir
    Path tempDir;

    @Test
    void throwsWhenSourcePathDoesNotExist() {
        JavaSourceParser parser = new JavaSourceParser();
        Path missing = tempDir.resolve("missing");

        ParseException exception = assertThrows(ParseException.class,
                () -> parser.parse(List.of(missing), EitriConfig.builder().build()));
        assertTrue(exception.getMessage().contains("Source path does not exist"));
    }

    @Test
    void continuesParsingWhenOneFileHasSyntaxError() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Good.java"), "package com.example; public class Good {}");
        Files.writeString(src.resolve("Bad.java"), "package com.example; public class Bad {");

        JavaSourceParser parser = new JavaSourceParser();
        UmlModel model = parser.parse(List.of(src), EitriConfig.builder().build());

        assertTrue(model.hasType("com.example.Good"));
    }

    @Test
    void supportsParsingSingleJavaFilePath() throws Exception {
        Path file = tempDir.resolve("Single.java");
        Files.writeString(file, "package com.example; public class Single {}");

        JavaSourceParser parser = new JavaSourceParser();
        UmlModel model = parser.parse(List.of(file), EitriConfig.builder().build());

        assertEquals(1, model.getTypes().stream().filter(t -> t.getFqn().equals("com.example.Single")).count());
    }
}
