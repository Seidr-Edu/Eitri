package no.ntnu.eitri.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EitriConfig.
 */
class EitriConfigTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("Direction defaults to TOP_TO_BOTTOM")
        void defaultDirection() {
            EitriConfig config = new EitriConfig();
            assertEquals(LayoutDirection.TOP_TO_BOTTOM, config.getDirection());
        }

        @Test
        @DisplayName("Diagram name defaults to 'diagram'")
        void defaultDiagramName() {
            EitriConfig config = new EitriConfig();
            assertEquals("diagram", config.getDiagramName());
        }

        @Test
        @DisplayName("hideEmptyMembers defaults to true")
        void defaultHideEmptyMembers() {
            EitriConfig config = new EitriConfig();
            assertTrue(config.isHideEmptyMembers());
        }

        @Test
        @DisplayName("Visibility filters default to false")
        void defaultVisibilityFilters() {
            EitriConfig config = new EitriConfig();
            assertFalse(config.isHidePrivate());
            assertFalse(config.isHideProtected());
            assertFalse(config.isHidePackage());
        }

        @Test
        @DisplayName("All relation types shown by default")
        void defaultRelationsShown() {
            EitriConfig config = new EitriConfig();
            assertTrue(config.isShowInheritance());
            assertTrue(config.isShowImplements());
            assertTrue(config.isShowComposition());
            assertTrue(config.isShowAggregation());
            assertTrue(config.isShowAssociation());
            assertTrue(config.isShowDependency());
        }

        @Test
        @DisplayName("Stereotypes and generics shown by default")
        void defaultDisplayOptions() {
            EitriConfig config = new EitriConfig();
            assertTrue(config.isShowStereotypes());
            assertTrue(config.isShowGenerics());
            assertTrue(config.isShowMultiplicities());
            assertTrue(config.isShowLabels());
        }

        @Test
        @DisplayName("Notes hidden by default")
        void defaultNotesHidden() {
            EitriConfig config = new EitriConfig();
            assertFalse(config.isShowNotes());
        }
    }

    @Nested
    @DisplayName("Builder pattern")
    class BuilderPattern {

        @Test
        @DisplayName("Builder sets source paths")
        void builderSourcePaths() {
            EitriConfig config = EitriConfig.builder()
                    .addSourcePath(Path.of("src/main/java"))
                    .addSourcePath(Path.of("src/test/java"))
                    .build();

            assertEquals(2, config.getSourcePaths().size());
        }

        @Test
        @DisplayName("Builder sets output path")
        void builderOutputPath() {
            EitriConfig config = EitriConfig.builder()
                    .outputPath(Path.of("diagram.puml"))
                    .build();

            assertEquals(Path.of("diagram.puml"), config.getOutputPath());
        }

        @Test
        @DisplayName("Builder sets layout options")
        void builderLayoutOptions() {
            EitriConfig config = EitriConfig.builder()
                    .direction(LayoutDirection.LEFT_TO_RIGHT)
                    .groupInheritance(3)
                    .classAttributeIconSize(10)
                    .build();

            assertEquals(LayoutDirection.LEFT_TO_RIGHT, config.getDirection());
            assertEquals(3, config.getGroupInheritance());
            assertEquals(10, config.getClassAttributeIconSize());
        }

        @Test
        @DisplayName("Builder sets visibility options")
        void builderVisibilityOptions() {
            EitriConfig config = EitriConfig.builder()
                    .hidePrivate(true)
                    .hideProtected(true)
                    .build();

            assertTrue(config.isHidePrivate());
            assertTrue(config.isHideProtected());
            assertFalse(config.isHidePackage());
        }

        @Test
        @DisplayName("Builder sets relation options")
        void builderRelationOptions() {
            EitriConfig config = EitriConfig.builder()
                    .showDependency(false)
                    .showAssociation(false)
                    .build();

            assertFalse(config.isShowDependency());
            assertFalse(config.isShowAssociation());
            assertTrue(config.isShowInheritance());
        }

        @Test
        @DisplayName("Builder adds skinparam lines")
        void builderSkinparamLines() {
            EitriConfig config = EitriConfig.builder()
                    .addSkinparamLine("class { BackgroundColor #FEFECE }")
                    .addSkinparamLine("ArrowColor #333333")
                    .build();

            assertEquals(2, config.getSkinparamLines().size());
            assertTrue(config.getSkinparamLines().contains("class { BackgroundColor #FEFECE }"));
        }
    }

    @Nested
    @DisplayName("Setters validation")
    class SettersValidation {

        @Test
        @DisplayName("setGroupInheritance clamps negative values to 0")
        void groupInheritanceNegative() {
            EitriConfig config = new EitriConfig();
            config.setGroupInheritance(-5);
            assertEquals(0, config.getGroupInheritance());
        }

        @Test
        @DisplayName("setDiagramName defaults to 'diagram' for null")
        void diagramNameNull() {
            EitriConfig config = new EitriConfig();
            config.setDiagramName(null);
            assertEquals("diagram", config.getDiagramName());
        }

        @Test
        @DisplayName("setSourcePaths handles null")
        void sourcePathsNull() {
            EitriConfig config = new EitriConfig();
            config.setSourcePaths(null);
            assertNotNull(config.getSourcePaths());
            assertTrue(config.getSourcePaths().isEmpty());
        }
    }
}
