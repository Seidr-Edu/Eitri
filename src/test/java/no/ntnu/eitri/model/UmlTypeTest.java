package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlType model behavior.
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
    @DisplayName("Type properties")
    class TypeProperties {

        @Test
        @DisplayName("Defaults are applied")
        void defaultsApplied() {
            UmlType type = UmlType.builder()
                    .name("Customer")
                    .build();

            assertEquals(TypeKind.CLASS, type.getKind());
            assertEquals(Visibility.PACKAGE, type.getVisibility());
        }

        @Test
        @DisplayName("Display name is preserved")
        void displayNamePreserved() {
            UmlType type = UmlType.builder()
                    .name("OrderHandler")
                    .displayName("Order Handler")
                    .build();

            assertEquals("Order Handler", type.getDisplayName());
        }

        @Test
        @DisplayName("Tags and style are preserved")
        void tagsAndStylePreserved() {
            UmlType type = UmlType.builder()
                    .name("Order")
                    .addTag("core")
                    .style("#lightblue")
                    .build();

            assertEquals(1, type.getTags().size());
            assertEquals("core", type.getTags().get(0));
            assertEquals("#lightblue", type.getStyle());
        }

        @Test
        @DisplayName("Generics and stereotypes are preserved")
        void genericsAndStereotypesPreserved() {
            UmlType type = UmlType.builder()
                    .name("OrderService")
                    .addGeneric(new UmlGeneric("T", "extends Number"))
                    .addStereotype(new UmlStereotype("Service"))
                    .build();

            assertEquals(1, type.getGenerics().size());
            assertEquals("T", type.getGenerics().get(0).identifier());
            assertEquals("extends Number", type.getGenerics().get(0).bounds());

            assertEquals(1, type.getStereotypes().size());
            assertEquals("Service", type.getStereotypes().get(0).name());
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

            assertEquals(1, staticNested.getStereotypes().size());
            assertEquals("static", staticNested.getStereotypes().get(0).name());
        }
    }
}
