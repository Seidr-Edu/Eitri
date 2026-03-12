package no.ntnu.eitri.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EitriServiceMainTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsZeroAndPrintsHelpForHelpFlag() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        try (PrintStream replacement = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);

            int exitCode = EitriServiceMain.run(new String[]{"--help"});

            assertEquals(0, exitCode);
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(captured.toString(StandardCharsets.UTF_8).contains("Usage: eitri-service"));
    }

    @Test
    void returnsOneWhenPositionalArgumentsProvided() {
        int exitCode = EitriServiceMain.run(new String[]{"unexpected"});

        assertEquals(1, exitCode);
    }

    @Test
    void mainUsesEnvironmentOverridesToRunService() throws Exception {
        Path inputDir = tempDir.resolve("input-repo");
        Path runDir = tempDir.resolve("service-run");
        Path manifestPath = runDir.resolve("custom-config").resolve("manifest.yaml");
        Files.createDirectories(manifestPath.getParent());
        Files.createDirectories(inputDir.resolve("src/main/java/demo"));
        Files.writeString(
                inputDir.resolve("src/main/java/demo/Sample.java"),
                """
                        package demo;
                        public class Sample {}
                        """);
        Files.writeString(manifestPath, """
                version: 1
                source_relpaths:
                  - src/main/java
                """);

        ProcessBuilder processBuilder = new ProcessBuilder(
                javaBinary(),
                "-cp",
                testClasspath(),
                "no.ntnu.eitri.service.EitriServiceMain");
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("EITRI_SERVICE_INPUT_DIR", inputDir.toString());
        processBuilder.environment().put("EITRI_SERVICE_RUN_DIR", runDir.toString());
        processBuilder.environment().put("EITRI_MANIFEST", manifestPath.toString());

        Process process = processBuilder.start();
        String output;
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "process did not exit in time");
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            process.destroyForcibly();
        }

        assertEquals(0, process.exitValue(), output);
        assertTrue(Files.exists(runDir.resolve("outputs/run_report.json")));
        assertTrue(Files.exists(runDir.resolve("artifacts/model/diagram.puml")));
    }

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static String testClasspath() {
        return System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    }
}
