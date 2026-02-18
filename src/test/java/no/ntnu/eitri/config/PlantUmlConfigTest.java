package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUmlConfigTest {

    @Test
    void plantUmlDefaultsAreStable() {
        PlantUmlConfig config = PlantUmlConfig.defaults();

        assertEquals("diagram", config.diagramName());
        assertEquals(LayoutDirection.TOP_TO_BOTTOM, config.direction());
        assertTrue(config.showLabels());
        assertFalse(config.showNested());
        assertTrue(config.showUnlinked());
        assertTrue(config.hideEmptyMembers());
        assertFalse(config.showThrows());
    }

    @Test
    void plantUmlConstructorNormalizesBounds() {
        PlantUmlConfig config = new PlantUmlConfig(
                null,
                null,
                -5,
                -10,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false);

        assertEquals("diagram", config.diagramName());
        assertEquals(LayoutDirection.TOP_TO_BOTTOM, config.direction());
        assertEquals(1, config.groupInheritance());
        assertEquals(0, config.classAttributeIconSize());
    }

    @Test
    void runConfigNormalizesExtensionsAndList() {
        RunConfig config = new RunConfig(
                null,
                Path.of("out.puml"),
                "java",
                "puml",
                true,
                false);

        assertNotNull(config.sourcePaths());
        assertTrue(config.sourcePaths().isEmpty());
        assertEquals(".java", config.parserExtension());
        assertEquals(".puml", config.writerExtension());
        assertTrue(config.verbose());
        assertFalse(config.dryRun());
    }

    @Test
    void runConfigCopiesSourcePaths() {
        List<Path> src = List.of(Path.of("src/main/java"));
        RunConfig config = new RunConfig(src, Path.of("out.puml"), null, null, false, true);

        assertEquals(1, config.sourcePaths().size());
        assertEquals(Path.of("src/main/java"), config.sourcePaths().getFirst());
        assertTrue(config.dryRun());
    }
}
