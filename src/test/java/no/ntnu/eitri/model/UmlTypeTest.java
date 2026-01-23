package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlType and its PlantUML rendering.
 */
class UmlTypeTest {
    
    @Nested
    @DisplayName("ID computation")
    class IdComputation {

        @Test
        @DisplayName("ID is fully qualified name with package")
        void idWithPackage() {
            UmlType type = UmlType.builder()
                    .name("Customer")
                    .packageName("com.example.domain")
                    .build();

            assertEquals("com.example.domain.Customer", type.getId());
        }

        @Test
        @DisplayName("ID is simple name without package")
        void idWithoutPackage() {
            UmlType type = UmlType.builder()
                    .name("Helper")
                    .build();

            assertEquals("Helper", type.getId());
        }

        @Test
        @DisplayName("Explicit ID overrides computed")
        void explicitId() {
            UmlType type = UmlType.builder()
                    .id("custom.id")
                    .name("Customer")
                    .packageName("com.example")
                    .build();

            assertEquals("custom.id", type.getId());
        }

        @Test
        @DisplayName("Stable ID for same type")
        void stableId() {
            UmlType type1 = UmlType.builder()
                    .name("Order")
                    .packageName("shop.core")
                    .build();

            UmlType type2 = UmlType.builder()
                    .name("Order")
                    .packageName("shop.core")
                    .build();

            assertEquals(type1.getId(), type2.getId());
            assertEquals(type1, type2);
        }
    }

    @Nested
    @DisplayName("Declaration rendering")
    class DeclarationRendering {

        @Test
        @DisplayName("Simple public class")
        void simplePublicClass() {
            UmlType type = UmlType.builder()
                    .name("Customer")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+class Customer", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Package-private class")
        void packagePrivateClass() {
            UmlType type = UmlType.builder()
                    .name("Helper")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PACKAGE)
                    .build();

            assertEquals("~class Helper", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Public interface")
        void publicInterface() {
            UmlType type = UmlType.builder()
                    .name("Repository")
                    .kind(TypeKind.INTERFACE)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+interface Repository", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Abstract class")
        void abstractClass() {
            UmlType type = UmlType.builder()
                    .name("BaseEntity")
                    .kind(TypeKind.ABSTRACT_CLASS)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+abstract class BaseEntity", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Enum")
        void enumType() {
            UmlType type = UmlType.builder()
                    .name("Status")
                    .kind(TypeKind.ENUM)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+enum Status", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Annotation")
        void annotationType() {
            UmlType type = UmlType.builder()
                    .name("Entity")
                    .kind(TypeKind.ANNOTATION)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+annotation Entity", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Record renders as class (PlantUML has no record keyword)")
        void recordType() {
            UmlType type = UmlType.builder()
                    .name("Point")
                    .kind(TypeKind.RECORD)
                    .visibility(Visibility.PUBLIC)
                    .build();

            // Record uses class keyword since PlantUML doesn't have record
            assertEquals("+class Point", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Class with generic parameter")
        void genericClass() {
            UmlType type = UmlType.builder()
                    .name("Repository")
                    .kind(TypeKind.INTERFACE)
                    .visibility(Visibility.PUBLIC)
                    .addGeneric("T")
                    .build();

            assertEquals("+interface Repository<T>", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Class with bounded generic")
        void boundedGeneric() {
            UmlType type = UmlType.builder()
                    .name("Comparable")
                    .kind(TypeKind.INTERFACE)
                    .visibility(Visibility.PUBLIC)
                    .addGeneric(new UmlGeneric("T", "extends Number"))
                    .build();

            assertEquals("+interface Comparable<T extends Number>", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Class with stereotype")
        void classWithStereotype() {
            UmlType type = UmlType.builder()
                    .name("Customer")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .addStereotype("Entity")
                    .build();

            assertEquals("+class Customer <<Entity>>", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Class with spot stereotype")
        void classWithSpotStereotype() {
            UmlType type = UmlType.builder()
                    .name("Order")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .addStereotype(new UmlStereotype("Aggregate", 'A', "#FF0000"))
                    .build();

            assertEquals("+class Order << (A,#FF0000) Aggregate >>", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Class with display name (alias)")
        void classWithDisplayName() {
            UmlType type = UmlType.builder()
                    .name("OrderHandler")
                    .displayName("Order Handler")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+class \"Order Handler\" as OrderHandler", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Class with style")
        void classWithStyle() {
            UmlType type = UmlType.builder()
                    .name("Order")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .style("#palegreen")
                    .build();

            assertEquals("+class Order #palegreen", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Class with tag")
        void classWithTag() {
            UmlType type = UmlType.builder()
                    .name("Internal")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PACKAGE)
                    .addTag("internal")
                    .build();

            assertEquals("~class Internal $internal", type.toDeclarationPlantUml());
        }

        @Test
        @DisplayName("Complex class with all features")
        void complexClass() {
            UmlType type = UmlType.builder()
                    .name("OrderService")
                    .displayName("Order Service")
                    .kind(TypeKind.CLASS)
                    .visibility(Visibility.PUBLIC)
                    .addGeneric("T")
                    .addStereotype("Service")
                    .addTag("core")
                    .style("#lightblue")
                    .build();

            String expected = "+class \"Order Service\" as OrderService<T> <<Service>> $core #lightblue";
            assertEquals(expected, type.toDeclarationPlantUml());
        }
    }

    @Nested
    @DisplayName("Nested type support")
    class NestedTypeSupport {

        @Test
        @DisplayName("Top-level type has no outer type")
        void topLevelTypeHasNoOuter() {
            UmlType type = UmlType.builder()
                    .name("TopLevel")
                    .packageName("com.example")
                    .build();

            assertNull(type.getOuterTypeId());
            assertFalse(type.isNested());
        }

        @Test
        @DisplayName("Nested type has outer type ID")
        void nestedTypeHasOuterId() {
            UmlType nested = UmlType.builder()
                    .id("com.example.Outer$Inner")
                    .name("Inner")
                    .packageName("com.example")
                    .outerTypeId("com.example.Outer")
                    .build();

            assertEquals("com.example.Outer", nested.getOuterTypeId());
            assertTrue(nested.isNested());
        }

        @Test
        @DisplayName("Nested type ID uses dollar sign convention")
        void nestedTypeIdUsesDollar() {
            UmlType nested = UmlType.builder()
                    .id("com.example.Container$Nested")
                    .name("Nested")
                    .packageName("com.example")
                    .outerTypeId("com.example.Container")
                    .build();

            assertEquals("com.example.Container$Nested", nested.getId());
        }

        @Test
        @DisplayName("Deeply nested types use multiple dollar signs")
        void deeplyNestedTypeId() {
            UmlType deepNested = UmlType.builder()
                    .id("com.example.A$B$C")
                    .name("C")
                    .packageName("com.example")
                    .outerTypeId("com.example.A$B")
                    .build();

            assertEquals("com.example.A$B$C", deepNested.getId());
            assertEquals("com.example.A$B", deepNested.getOuterTypeId());
            assertTrue(deepNested.isNested());
        }

        @Test
        @DisplayName("Static nested type with stereotype")
        void staticNestedType() {
            UmlType staticNested = UmlType.builder()
                    .id("com.example.Outer$StaticNested")
                    .name("StaticNested")
                    .packageName("com.example")
                    .outerTypeId("com.example.Outer")
                    .addStereotype("static")
                    .build();

            String decl = staticNested.toDeclarationPlantUml();
            assertTrue(decl.contains("<<static>>"));
        }
    }
}
