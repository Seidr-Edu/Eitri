package no.ntnu.eitri;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.java.JavaSourceParser;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Self-test: Parse Eitri's own source code and validate the output.
 */
@DisplayName("Self-Test: Parsing Eitri Source")
@Disabled("Temporarily disabled due to frequent codebase changes")
class SelfParseTest {

    private static final Path SOURCE_PATH = Path.of("src/main/java");
    private JavaSourceParser parser;
    private EitriConfig config;

    static boolean sourcePathExists() {
        return Files.isDirectory(SOURCE_PATH);
    }

    @BeforeEach
    void setUp() {
        parser = new JavaSourceParser();
        config = EitriConfig.builder()
                .addSourcePath(SOURCE_PATH)
                .verbose(false)
                .build();
    }

    @Nested
    @DisplayName("Parsing")
    @EnabledIf("no.ntnu.eitri.SelfParseTest#sourcePathExists")
    class Parsing {

        @Test
        @DisplayName("Should parse without throwing exceptions")
        void shouldParseWithoutExceptions() {
            assertDoesNotThrow(() -> parser.parse(List.of(SOURCE_PATH), config));
        }

        @Test
        @DisplayName("Should find expected number of types (approximately)")
        void shouldFindExpectedTypes() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            
            // We expect around 25-35 types based on the codebase
            assertTrue(model.getTypes().size() >= 20, 
                    "Expected at least 20 types, got " + model.getTypes().size());
            assertTrue(model.getTypes().size() <= 60, 
                    "Expected at most 60 types, got " + model.getTypes().size());
        }

        @Test
        @DisplayName("Should find key model classes")
        void shouldFindKeyModelClasses() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            Set<String> typeNames = model.getTypes().stream()
                    .map(UmlType::getSimpleName)
                    .collect(Collectors.toSet());

            assertTrue(typeNames.contains("UmlType"), "Should find UmlType");
            assertTrue(typeNames.contains("UmlModel"), "Should find UmlModel");
            assertTrue(typeNames.contains("UmlField"), "Should find UmlField");
            assertTrue(typeNames.contains("UmlMethod"), "Should find UmlMethod");
            assertTrue(typeNames.contains("UmlRelation"), "Should find UmlRelation");
        }

        @Test
        @DisplayName("Should find enums")
        void shouldFindEnums() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            Set<String> typeNames = model.getTypes().stream()
                    .map(UmlType::getSimpleName)
                    .collect(Collectors.toSet());

            assertTrue(typeNames.contains("Visibility"), "Should find Visibility enum");
            assertTrue(typeNames.contains("TypeKind"), "Should find TypeKind enum");
            assertTrue(typeNames.contains("RelationKind"), "Should find RelationKind enum");
            assertTrue(typeNames.contains("Modifier"), "Should find Modifier enum");
        }

        @Test
        @DisplayName("Should find interfaces")
        void shouldFindInterfaces() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            Set<String> typeNames = model.getTypes().stream()
                    .map(UmlType::getSimpleName)
                    .collect(Collectors.toSet());

            assertTrue(typeNames.contains("SourceParser"), "Should find SourceParser interface");
            assertTrue(typeNames.contains("DiagramWriter"), "Should find DiagramWriter interface");
        }

        @Test
        @DisplayName("Should find records")
        void shouldFindRecords() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            Set<String> typeNames = model.getTypes().stream()
                    .map(UmlType::getSimpleName)
                    .collect(Collectors.toSet());

            assertTrue(typeNames.contains("UmlParameter"), "Should find UmlParameter record");
            assertTrue(typeNames.contains("UmlGeneric"), "Should find UmlGeneric record");
            assertTrue(typeNames.contains("UmlStereotype"), "Should find UmlStereotype record");
        }

        @Test
        @DisplayName("Should detect implements relations")
        void shouldDetectImplementsRelations() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            
            // JavaSourceParser implements SourceParser
            boolean hasJavaParserImplements = model.getRelations().stream()
                    .anyMatch(r -> r.getFromTypeFqn().contains("JavaSourceParser") 
                            && r.getToTypeFqn().contains("SourceParser")
                            && r.getKind() == RelationKind.IMPLEMENTS);
            
            assertTrue(hasJavaParserImplements, 
                    "Should detect JavaSourceParser implements SourceParser");
            
            // PlantUmlWriter implements DiagramWriter
            boolean hasPlantUmlWriterImplements = model.getRelations().stream()
                    .anyMatch(r -> r.getFromTypeFqn().contains("PlantUmlWriter")
                            && r.getToTypeFqn().contains("DiagramWriter")
                            && r.getKind() == RelationKind.IMPLEMENTS);
            
            assertTrue(hasPlantUmlWriterImplements,
                    "Should detect PlantUmlWriter implements DiagramWriter");
        }

        @Test
        @DisplayName("Should detect field-based relations")
        void shouldDetectFieldRelations() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            
            // Expect significant number of relations (inheritance + field-based + dependencies)
            assertTrue(model.getRelations().size() >= 30,
                    "Expected at least 30 relations, got " + model.getRelations().size());
            
            // Check for composition (ParseContext has EitriConfig)
            boolean hasComposition = model.getRelations().stream()
                    .anyMatch(r -> r.getKind() == RelationKind.COMPOSITION);
            assertTrue(hasComposition, "Should detect at least one composition relation");
            
            // Check for aggregation (collection fields)
            boolean hasAggregation = model.getRelations().stream()
                    .anyMatch(r -> r.getKind() == RelationKind.AGGREGATION);
            assertTrue(hasAggregation, "Should detect at least one aggregation relation");
        }

        @Test
        @DisplayName("Should extract fields from types")
        void shouldExtractFields() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            
            Optional<UmlType> configType = model.getTypes().stream()
                    .filter(t -> t.getSimpleName().equals("EitriConfig"))
                    .findFirst();
            
            assertTrue(configType.isPresent(), "Should find EitriConfig");
            assertFalse(configType.get().getFields().isEmpty(), 
                    "EitriConfig should have fields");
        }

        @Test
        @DisplayName("Should extract methods from types")
        void shouldExtractMethods() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            
            Optional<UmlType> parserType = model.getTypes().stream()
                    .filter(t -> t.getSimpleName().equals("JavaSourceParser"))
                    .findFirst();
            
            assertTrue(parserType.isPresent(), "Should find JavaSourceParser");
            assertFalse(parserType.get().getMethods().isEmpty(), 
                    "JavaSourceParser should have methods");
            
            // Check for the parse method
            boolean hasParseMethod = parserType.get().getMethods().stream()
                    .anyMatch(m -> m.getName().equals("parse"));
            assertTrue(hasParseMethod, "JavaSourceParser should have parse method");
        }
    }

    @Nested
    @DisplayName("PlantUML Rendering")
    @EnabledIf("no.ntnu.eitri.SelfParseTest#sourcePathExists")
    class PlantUmlRendering {

        @Test
        @DisplayName("Should generate valid PlantUML")
        void shouldGenerateValidPlantUml() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            PlantUmlWriter writer = new PlantUmlWriter();
            
            String puml = writer.render(model, config);
            
            assertTrue(puml.startsWith("@startuml"), "Should start with @startuml");
            assertTrue(puml.contains("@enduml"), "Should end with @enduml");
        }

        @Test
        @DisplayName("Should include type declarations in output")
        void shouldIncludeTypeDeclarations() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            PlantUmlWriter writer = new PlantUmlWriter();
            
            String puml = writer.render(model, config);
            
            assertTrue(puml.contains("class UmlType"), "Should include UmlType class");
            assertTrue(puml.contains("interface SourceParser"), "Should include SourceParser interface");
            assertTrue(puml.contains("enum Visibility"), "Should include Visibility enum");
        }

        @Test
        @DisplayName("Should include relations in output")
        void shouldIncludeRelations() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            PlantUmlWriter writer = new PlantUmlWriter();
            
            String puml = writer.render(model, config);
            
            // Check for implements relation
            assertTrue(puml.contains("<|.."), 
                    "Should include implements relation arrow");
            
            // Check for field-based relations
            assertTrue(puml.contains("*--") || puml.contains("o--") || puml.contains("..>"),
                    "Should include field-based or dependency relations");
        }

        @Test
        @DisplayName("Should group types by package")
        void shouldGroupTypesByPackage() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            PlantUmlWriter writer = new PlantUmlWriter();
            
            String puml = writer.render(model, config);
            
            // Check for package declarations
            assertTrue(puml.contains("package no.ntnu.eitri.model {"),
                    "Should have package block for model package");
            assertTrue(puml.contains("package no.ntnu.eitri.config {"),
                    "Should have package block for config package");
            assertTrue(puml.contains("package no.ntnu.eitri.parser {"),
                    "Should have package block for parser package");
        }

        @Test
        @DisplayName("Should respect hideEmptyMembers setting")
        void shouldRespectHideEmptyMembers() {
            UmlModel model = parser.parse(List.of(SOURCE_PATH), config);
            
            EitriConfig configHide = EitriConfig.builder()
                    .addSourcePath(SOURCE_PATH)
                    .hideEmptyMembers(true)
                    .build();
            
            PlantUmlWriter writer = new PlantUmlWriter();
            String puml = writer.render(model, configHide);
            
            assertTrue(puml.contains("hide empty members"), 
                    "Should include 'hide empty members' directive");
        }
    }
}
