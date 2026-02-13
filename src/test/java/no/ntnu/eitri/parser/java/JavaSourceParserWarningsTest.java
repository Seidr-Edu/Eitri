package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.config.RunConfig;
import no.ntnu.eitri.model.UmlModel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceParserWarningsTest {

    @TempDir
    Path tempDir;

    @Test
    void logsWarningsForUnreadableFiles() throws Exception {
        Assumptions.assumeTrue(tempDir.getFileSystem().supportedFileAttributeViews().contains("posix"));

        Path okFile = tempDir.resolve("Ok.java");
        Files.writeString(okFile, "public class Ok {}\n");

        Path badFile = tempDir.resolve("Bad.java");
        Files.writeString(badFile, "public class Bad {}\n");
        Files.setPosixFilePermissions(badFile, Set.of());

        Logger logger = Logger.getLogger(JavaSourceParser.class.getName());
        CapturingHandler handler = new CapturingHandler();
        Level previous = logger.getLevel();
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);

        try {
            RunConfig runConfig = new RunConfig(
                    List.of(tempDir),
                    tempDir.resolve("out.puml"),
                    null,
                    null,
                    true,
                    false);

            JavaSourceParser parser = new JavaSourceParser();
            UmlModel model = parser.parse(List.of(tempDir), runConfig);

            assertNotNull(model);
            assertFalse(handler.messages.isEmpty());
            assertTrue(handler.messages.stream().anyMatch(msg -> msg.contains("warnings")));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previous);
            logger.setUseParentHandlers(true);
        }
    }

    private static final class CapturingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord != null && logRecord.getMessage() != null) {
                messages.add(logRecord.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
