package no.ntnu.eitri.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void serializesStringsMapsAndIterables() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", "plain \"quote\" slash\\ backspace\b formfeed\f newline\n carriage\r tab\t ctrl\u0001");
        payload.put("items", Arrays.asList(null, 12, true));

        String json = JsonWriter.toJson(payload);

        assertEquals(
                "{\"text\":\"plain \\\"quote\\\" slash\\\\ backspace\\b formfeed\\f newline\\n carriage\\r tab\\t ctrl\\u0001\","
                        + "\"items\":[null,12,true]}",
                json);
    }

    @Test
    void writesJsonWithTrailingNewline() throws IOException {
        Path path = tempDir.resolve("report.json");

        JsonWriter.write(path, Map.of("status", "passed"));

        String written = Files.readString(path);
        assertTrue(written.endsWith(System.lineSeparator()));
        assertEquals("{\"status\":\"passed\"}" + System.lineSeparator(), written);
    }

    @Test
    void rejectsUnsupportedValues() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.toJson(new Object()));

        assertTrue(error.getMessage().contains("Unsupported JSON value"));
    }
}
