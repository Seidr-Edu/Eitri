package no.ntnu.eitri.config;

import no.ntnu.eitri.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that the showNested configuration option
 * correctly filters nested types and their relations from the output.
 */
class NestedTypesConfigIntegrationTest {

    @TempDir
    Path tempDir;

    private static final String JAVA_SOURCE_WITH_NESTED = """
        package com.example;
        
        public class Outer {
            private String outerField;
            
            public void outerMethod() {}
            
            public static class StaticNested {
                private int nestedField;
                
                public void nestedMethod() {}
            }
            
            public class InnerClass {
                private double innerField;
            }
            
            public enum Status {
                ACTIVE, INACTIVE
            }
            
            public record Point(int x, int y) {}
        }
        """;

    @Test
    void includesNestedTypesWhenConfiguredToShow() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Outer.java"), JAVA_SOURCE_WITH_NESTED);

        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            relations:
              showNested: true
            """);

        Path out = tempDir.resolve("diagram.puml");

        int exitCode = new CommandLine(new Main()).execute(
                "--src", src.toString(),
                "--out", out.toString(),
                "--config", configFile.toString()
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(out));

        String output = Files.readString(out);
        
        // Should include the outer class
        assertTrue(output.contains("class com.example.Outer"), 
                "Output should contain Outer class");
        
        // Should include nested types when showNested is true
        assertTrue(output.contains("com.example.Outer$StaticNested") || 
                   output.contains("com.example.Outer.StaticNested"),
                "Output should contain StaticNested when showNested is true");
        
        assertTrue(output.contains("com.example.Outer$InnerClass") || 
                   output.contains("com.example.Outer.InnerClass"),
                "Output should contain InnerClass when showNested is true");
        
        assertTrue(output.contains("com.example.Outer$Status") || 
                   output.contains("com.example.Outer.Status"),
                "Output should contain Status enum when showNested is true");
        
        assertTrue(output.contains("com.example.Outer$Point") || 
                   output.contains("com.example.Outer.Point"),
                "Output should contain Point record when showNested is true");
    }

    @Test
    void excludesNestedTypesWhenConfiguredToHide() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Outer.java"), JAVA_SOURCE_WITH_NESTED);

        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            relations:
              showNested: false
            """);

        Path out = tempDir.resolve("diagram.puml");

        int exitCode = new CommandLine(new Main()).execute(
                "--src", src.toString(),
                "--out", out.toString(),
                "--config", configFile.toString()
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(out));

        String output = Files.readString(out);
        
        // Should include the outer class
        assertTrue(output.contains("class com.example.Outer"), 
                "Output should contain Outer class");
        
        // Should NOT include nested types when showNested is false
        assertFalse(output.contains("com.example.Outer$StaticNested") || 
                    output.contains("com.example.Outer.StaticNested"),
                "Output should not contain StaticNested when showNested is false");
        
        assertFalse(output.contains("com.example.Outer$InnerClass") || 
                    output.contains("com.example.Outer.InnerClass"),
                "Output should not contain InnerClass when showNested is false");
        
        assertFalse(output.contains("com.example.Outer$Status") || 
                    output.contains("com.example.Outer.Status"),
                "Output should not contain Status enum when showNested is false");
        
        assertFalse(output.contains("com.example.Outer$Point") || 
                    output.contains("com.example.Outer.Point"),
                "Output should not contain Point record when showNested is false");
    }

    @Test
    void defaultBehaviorShowsNestedTypes() throws Exception {
        // When no config is specified, nested types should be shown by default
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Outer.java"), JAVA_SOURCE_WITH_NESTED);

        Path out = tempDir.resolve("diagram.puml");

        int exitCode = new CommandLine(new Main()).execute(
                "--src", src.toString(),
                "--out", out.toString()
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(out));

        String output = Files.readString(out);
        
        // By default, nested types should be shown
        assertTrue(output.contains("StaticNested") || 
                   output.contains("InnerClass") || 
                   output.contains("Status") || 
                   output.contains("Point"),
                "Output should contain nested types by default");
    }
}
