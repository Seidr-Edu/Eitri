package no.ntnu.eitri.cli;

import no.ntnu.eitri.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainIntegrationTest {

        @TempDir
        Path tempDir;

        @Test
        void returnsZeroOnSuccessfulDryRun() throws Exception {
                Path src = tempDir.resolve("src");
                Files.createDirectories(src);
                Files.writeString(src.resolve("Sample.java"), "public class Sample {}\n");

                Path out = tempDir.resolve("diagram.puml");

                int exitCode = new CommandLine(new Main()).execute(
                                "--src", src.toString(),
                                "--out", out.toString(),
                                "--dry-run");

                assertEquals(0, exitCode);
        }

        @Test
        void returnsNonZeroOnInvalidSourcePath() {
                Path missing = tempDir.resolve("missing");
                Path out = tempDir.resolve("diagram.puml");

                int exitCode = new CommandLine(new Main()).execute(
                                "--src", missing.toString(),
                                "--out", out.toString());

                assertNotEquals(0, exitCode);
        }

        @Test
        void writesOutputFileOnSuccessfulRun() throws Exception {
                Path src = tempDir.resolve("src");
                Files.createDirectories(src);
                Files.writeString(src.resolve("Outer.java"), """
                                package com.example;

                                public class Outer {
                                  public static class Inner {}
                                }
                                """);
                Files.writeString(src.resolve("Leaf.java"), """
                                package com.example;

                                import org.junit.jupiter.api.extension.Extension;

                                public class Leaf implements Extension {}
                                """);
                Files.writeString(src.resolve("Hub.java"), """
                                package com.example;

                                public abstract class Hub {}
                                """);
                Path config = tempDir.resolve("eitri.yaml");
                Files.writeString(config, """
                                writers:
                                  plantuml:
                                    generateDegradedDiagrams: true
                                    showNested: true
                                """);
                Path out = tempDir.resolve("diagram.puml");
                Path diagramV2 = tempDir.resolve("diagram_v2.puml");
                Path diagramV3 = tempDir.resolve("diagram_v3.puml");
                Path report = tempDir.resolve("run_report.json");
                Path summary = tempDir.resolve("summary.md");

                int exitCode = new CommandLine(new Main()).execute(
                                "--src", src.toString(),
                                "--out", out.toString(),
                                "--config", config.toString());

                assertEquals(0, exitCode);
                assertTrue(Files.exists(out));
                assertTrue(Files.exists(diagramV2));
                assertTrue(Files.exists(diagramV3));
                assertTrue(Files.exists(report));
                assertTrue(Files.exists(summary));
                assertTrue(Files.readString(out).contains("@startuml"));
                assertNotEquals(Files.readString(out), Files.readString(diagramV2));
                assertNotEquals(Files.readString(out), Files.readString(diagramV3));

                String reportContent = Files.readString(report);
                assertTrue(reportContent.contains("\"diagram_v2_path\""));
                assertTrue(reportContent.contains("\"diagram_v3_path\""));
                assertTrue(reportContent.contains("\"degradation\""));
                assertTrue(reportContent.contains("\"eligible_kind_counts\""));
                assertTrue(reportContent.contains("\"applied_kind_counts\""));
                assertTrue(reportContent.contains("\"effective_percentage\""));
                String summaryContent = Files.readString(summary);
                assertTrue(summaryContent.contains("diagram_v2_path"));
                assertTrue(summaryContent.contains("diagram_v3_path"));
                assertTrue(summaryContent.contains("diagram_v2_effective_percentage"));
                assertTrue(summaryContent.contains("diagram_v3_effective_percentage"));
                assertTrue(summaryContent.contains("diagram_v2_eligible_kind_counts"));
                assertTrue(summaryContent.contains("diagram_v3_eligible_kind_counts"));
                assertTrue(summaryContent.contains("diagram_v2_applied_kind_counts"));
                assertTrue(summaryContent.contains("diagram_v3_applied_kind_counts"));
        }

        @Test
        void skipsDegradedArtifactsWhenDisabledInConfig() throws Exception {
                Path src = tempDir.resolve("src-no-degradation");
                Files.createDirectories(src);
                Files.writeString(src.resolve("Sample.java"), """
                                package com.example;

                                public class Sample {
                                  public String value;
                                }
                                """);
                Path config = tempDir.resolve("eitri-no-degradation.yaml");
                Files.writeString(config, """
                                writers:
                                  plantuml:
                                    generateDegradedDiagrams: false
                                """);
                Path out = tempDir.resolve("diagram.puml");
                Path diagramV2 = tempDir.resolve("diagram_v2.puml");
                Path diagramV3 = tempDir.resolve("diagram_v3.puml");
                Path report = tempDir.resolve("run_report.json");

                int exitCode = new CommandLine(new Main()).execute(
                                "--src", src.toString(),
                                "--out", out.toString(),
                                "--config", config.toString());

                assertEquals(0, exitCode);
                assertTrue(Files.exists(out));
                assertFalse(Files.exists(diagramV2));
                assertFalse(Files.exists(diagramV3));

                String reportContent = Files.readString(report);
                assertTrue(reportContent.contains("\"diagram_v2_path\":null"));
                assertTrue(reportContent.contains("\"diagram_v3_path\":null"));
                assertTrue(reportContent.contains("\"degradation\":null"));
        }

        @Test
        void returnsNonZeroOnUnsupportedWriterExtension() throws Exception {
                Path src = tempDir.resolve("src");
                Files.createDirectories(src);
                Files.writeString(src.resolve("Sample.java"), "public class Sample {}\n");
                Path out = tempDir.resolve("diagram.puml");

                int exitCode = new CommandLine(new Main()).execute(
                                "--src", src.toString(),
                                "--out", out.toString(),
                                "--writer", ".invalid");

                assertNotEquals(0, exitCode);
        }
}
