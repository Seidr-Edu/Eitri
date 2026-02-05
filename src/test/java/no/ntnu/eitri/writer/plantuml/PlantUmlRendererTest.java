package no.ntnu.eitri.writer.plantuml;

import no.ntnu.eitri.model.Modifier;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlGeneric;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlStereotype;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlantUmlRenderer syntax output.
 */
class PlantUmlRendererTest {

    private final PlantUmlRenderer renderer = new PlantUmlRenderer();

    @Nested
    @DisplayName("Type declarations")
    class TypeDeclarations {

        @Test
        @DisplayName("Simple public class")
        void simplePublicClass() {
            UmlType type = UmlType.builder()
                    .name("Customer")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+class Customer", renderer.renderTypeDeclaration(type));
        }

        @Test
        @DisplayName("Interface and abstract class")
        void interfaceAndAbstractClass() {
            UmlType iface = UmlType.builder()
                    .name("Repository")
                    .kind(TypeKind.INTERFACE)
                    .visibility(Visibility.PUBLIC)
                    .build();

            UmlType abs = UmlType.builder()
                    .name("BaseEntity")
                    .kind(TypeKind.ABSTRACT_CLASS)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+interface Repository", renderer.renderTypeDeclaration(iface));
            assertEquals("+abstract class BaseEntity", renderer.renderTypeDeclaration(abs));
        }

        @Test
        @DisplayName("Enum, annotation, and record")
        void enumAnnotationRecord() {
            UmlType enumType = UmlType.builder()
                    .name("Status")
                    .kind(TypeKind.ENUM)
                    .visibility(Visibility.PUBLIC)
                    .build();

            UmlType annotation = UmlType.builder()
                    .name("Entity")
                    .kind(TypeKind.ANNOTATION)
                    .visibility(Visibility.PUBLIC)
                    .build();

            UmlType recordType = UmlType.builder()
                    .name("Point")
                    .kind(TypeKind.RECORD)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+enum Status", renderer.renderTypeDeclaration(enumType));
            assertEquals("+annotation Entity", renderer.renderTypeDeclaration(annotation));
            assertEquals("+class Point", renderer.renderTypeDeclaration(recordType));
        }

        @Test
        @DisplayName("Generics and stereotypes")
        void genericsAndStereotypes() {
            UmlType generic = UmlType.builder()
                    .name("Repository")
                    .kind(TypeKind.INTERFACE)
                    .visibility(Visibility.PUBLIC)
                    .addGeneric("T")
                    .build();

            UmlType bounded = UmlType.builder()
                    .name("Comparable")
                    .kind(TypeKind.INTERFACE)
                    .visibility(Visibility.PUBLIC)
                    .addGeneric(new UmlGeneric("T", "extends Number"))
                    .build();

            UmlType stereotype = UmlType.builder()
                    .name("Customer")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .addStereotype("Entity")
                    .build();

            UmlType spot = UmlType.builder()
                    .name("Order")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .addStereotype(new UmlStereotype("Aggregate", 'A', "#FF0000"))
                    .build();

            assertEquals("+interface Repository<T>", renderer.renderTypeDeclaration(generic));
            assertEquals("+interface Comparable<T extends Number>", renderer.renderTypeDeclaration(bounded));
            assertEquals("+class Customer <<Entity>>", renderer.renderTypeDeclaration(stereotype));
            assertEquals("+class Order << (A,#FF0000) Aggregate >>", renderer.renderTypeDeclaration(spot));
        }

        @Test
        @DisplayName("Display name, tags, and style")
        void displayNameTagsStyle() {
            UmlType type = UmlType.builder()
                    .name("OrderService")
                    .displayName("Order Service")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .addTag("core")
                    .style("#lightblue")
                    .build();

            assertEquals("+class \"Order Service\" as OrderService $core #lightblue",
                    renderer.renderTypeDeclaration(type));
        }
    }

    @Nested
    @DisplayName("Field rendering")
    class FieldRendering {

        @Test
        @DisplayName("Visibility and type")
        void visibilityAndType() {
            UmlField field = UmlField.builder()
                    .name("count")
                    .type("int")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("-count : int", renderer.renderField(field, true));
        }

        @Test
        @DisplayName("Static and readOnly")
        void staticAndReadOnly() {
            UmlField field = UmlField.builder()
                    .name("MAX")
                    .type("int")
                    .visibility(Visibility.PUBLIC)
                    .addModifier(Modifier.STATIC)
                    .readOnly(true)
                    .build();

            assertEquals("+{static} MAX : int {readOnly}", renderer.renderField(field, true));
        }

        @Test
        @DisplayName("Generic type uses simple name")
        void genericTypeSimplifies() {
            UmlField field = UmlField.builder()
                    .name("items")
                    .type("java.util.List<String>")
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+items : List<String>", renderer.renderField(field, true));
        }
    }

    @Nested
    @DisplayName("Method rendering")
    class MethodRendering {

        @Test
        @DisplayName("Return type and parameters")
        void returnTypeAndParameters() {
            UmlMethod method = UmlMethod.builder()
                    .name("calculate")
                    .returnType("double")
                    .visibility(Visibility.PUBLIC)
                    .addParameter("x", "int")
                    .addParameter("y", "int")
                    .build();

            assertEquals("+calculate(x: int, y: int) : double", renderer.renderMethod(method, true));
        }

        @Test
        @DisplayName("Void return type shown when enabled")
        void voidReturnTypeShown() {
            UmlMethod method = UmlMethod.builder()
                    .name("init")
                    .returnType("void")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("-init() : void", renderer.renderMethod(method, true));
        }

        @Test
        @DisplayName("Static and abstract modifiers")
        void staticAndAbstract() {
            UmlMethod staticMethod = UmlMethod.builder()
                    .name("getInstance")
                    .returnType("Singleton")
                    .visibility(Visibility.PUBLIC)
                    .addModifier(Modifier.STATIC)
                    .build();

            UmlMethod abstractMethod = UmlMethod.builder()
                    .name("execute")
                    .returnType("void")
                    .visibility(Visibility.PUBLIC)
                    .addModifier(Modifier.ABSTRACT)
                    .build();

            assertEquals("+{static} getInstance() : Singleton", renderer.renderMethod(staticMethod, true));
            assertEquals("+{abstract} execute() : void", renderer.renderMethod(abstractMethod, true));
        }

        @Test
        @DisplayName("Constructor omits return type")
        void constructorNoReturnType() {
            UmlMethod constructor = UmlMethod.builder()
                    .name("Customer")
                    .constructor(true)
                    .visibility(Visibility.PUBLIC)
                    .addParameter("name", "String")
                    .build();

            assertEquals("+Customer(name: String)", renderer.renderMethod(constructor, true));
        }

        @Test
        @DisplayName("Generic return type simplifies")
        void genericReturnTypeSimplifies() {
            UmlMethod method = UmlMethod.builder()
                    .name("getItems")
                    .returnType("java.util.List<String>")
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+getItems() : List<String>", renderer.renderMethod(method, true));
        }
    }

    @Nested
    @DisplayName("Relation rendering")
    class RelationRendering {

        @Test
        @DisplayName("Hierarchy relations")
        void hierarchyRelations() {
            UmlRelation extendsRel = UmlRelation.extendsRelation("Child", "Parent");
            UmlRelation implementsRel = UmlRelation.implementsRelation("Impl", "Api");

            assertEquals("Parent <|-- Child", renderer.renderRelation(extendsRel, "Child", "Parent", true, true));
            assertEquals("Api <|.. Impl", renderer.renderRelation(implementsRel, "Impl", "Api", true, true));
        }

        @Test
        @DisplayName("Association with multiplicities and label")
        void associationWithMultiplicities() {
            UmlRelation relation = UmlRelation.builder()
                    .fromTypeId("Customer")
                    .toTypeId("Order")
                    .kind(RelationKind.ASSOCIATION)
                    .fromMultiplicity("1")
                    .toMultiplicity("0..*")
                    .label("places")
                    .build();

            assertEquals("Customer \"1\" -- \"0..*\" Order : places",
                    renderer.renderRelation(relation, "Customer", "Order", true, true));
        }

        @Test
        @DisplayName("Dependency and member relations")
        void dependencyAndMemberRelations() {
            UmlRelation dependency = UmlRelation.dependency("Service", "Repo", "uses");
            UmlRelation member = UmlRelation.builder()
                    .fromTypeId("Order")
                    .toTypeId("Status")
                    .kind(RelationKind.DEPENDENCY)
                    .fromMember("status")
                    .toMember("PAID")
                    .label("transitions")
                    .build();

            assertEquals("Service ..> Repo : uses",
                    renderer.renderRelation(dependency, "Service", "Repo", true, true));
            assertEquals("Order::status ..> Status::PAID : transitions",
                    renderer.renderRelation(member, "Order", "Status", true, true));
        }

        @Test
        @DisplayName("Nested relation")
        void nestedRelation() {
            UmlRelation relation = UmlRelation.nestedRelation("Outer", "Outer$Inner");

            assertEquals("Outer +-- Inner : nested",
                    renderer.renderRelation(relation, "Outer", "Inner", true, true));
        }
    }
}
