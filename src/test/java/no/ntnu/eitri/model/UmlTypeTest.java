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
    @DisplayName("FQN computation")
    class FqnComputation {

        @Test
        @DisplayName("FQN is fully qualified name with package")
        void fqnWithPackage() {
            UmlType type = UmlType.builder()
                    .fqn("com.example.domain.Customer")
                    .simpleName("Customer")
                    .build();

            assertEquals("com.example.domain.Customer", type.getFqn());
        }

        @Test
        @DisplayName("Stable FQN for same type")
        void stableFqn() {
            UmlType type1 = UmlType.builder()
                    .fqn("com.example.Order")
                    .simpleName("Order")
                    .build();

            UmlType type2 = UmlType.builder()
                    .fqn("com.example.Order")
                    .simpleName("Order")
                    .build();

            assertEquals(type1.getFqn(), type2.getFqn());
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
                    .fqn("com.example.Customer")
                    .simpleName("Customer")
                    .build();

            assertEquals(TypeKind.CLASS, type.getKind());
            assertEquals(Visibility.PACKAGE, type.getVisibility());
        }

        @Test
        @DisplayName("Display name is preserved")
        void displayNamePreserved() {
            UmlType type = UmlType.builder()
                    .fqn("com.example.OrderHandler")
                    .simpleName("OrderHandler")
                    .alias("Order Handler")
                    .build();

            assertEquals("Order Handler", type.getAlias());
        }

        @Test
        @DisplayName("Tags and style are preserved")
        void tagsAndStylePreserved() {
            UmlType type = UmlType.builder()
                    .fqn("com.example.Order")
                    .simpleName("Order")
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
                    .fqn("com.example.OrderService")
                    .simpleName("OrderService")
                    .addGeneric(new UmlGeneric("T", "extends Number"))
                    .addStereotype(new UmlStereotype("Service"))
                    .build();

            assertEquals(1, type.getGenerics().size());
            assertEquals("T", type.getGenerics().get(0).identifier());
            assertEquals("extends Number", type.getGenerics().get(0).bounds());

            assertEquals(1, type.getStereotypes().size());
            assertEquals("Service", type.getStereotypes().get(0).name());
        }

        @Test
        @DisplayName("Duplicate stereotypes by name are ignored")
        void duplicateStereotypesIgnored() {
            UmlType type = UmlType.builder()
                    .fqn("com.example.Service")
                    .simpleName("Service")
                    .addStereotype(new UmlStereotype("Service"))
                    .addStereotype(new UmlStereotype("Service", 'S', "#00FF00"))
                    .build();

            assertEquals(1, type.getStereotypes().size());
            assertEquals("Service", type.getStereotypes().get(0).name());
        }

        @Test
        @DisplayName("Collection properties are immutable")
        void collectionPropertiesImmutable() {
            UmlType type = UmlType.builder()
                    .fqn("com.example.Customer")
                    .simpleName("Customer")
                    .addTag("core")
                    .addGeneric("T")
                    .addField(UmlField.builder().name("id").type("long").build())
                    .addMethod(UmlMethod.builder().name("getId").returnType("long").build())
                    .build();

            assertThrows(UnsupportedOperationException.class, () -> type.getTags().add("new"));
            assertThrows(UnsupportedOperationException.class, () -> type.getGenerics().add(new UmlGeneric("E")));
            assertThrows(UnsupportedOperationException.class,
                    () -> type.getFields().add(UmlField.builder().name("x").type("int").build()));
            assertThrows(UnsupportedOperationException.class,
                    () -> type.getMethods().add(UmlMethod.builder().name("m").build()));
        }
    }

    @Nested
    @DisplayName("Nested type support")
    class NestedTypeSupport {

        @Test
        @DisplayName("Top-level type has no outer type")
        void topLevelTypeHasNoOuter() {
            UmlType type = UmlType.builder()
                    .fqn("com.example.TopLevel")
                    .simpleName("TopLevel")
                    .build();

            assertNull(type.getOuterTypeFqn());
            assertFalse(type.isNested());
        }

        @Test
        @DisplayName("Nested type has outer type ID")
        void nestedTypeHasOuterId() {
            UmlType nested = UmlType.builder()
                    .fqn("com.example.Outer$Inner")
                    .simpleName("Inner")
                    .outerTypeFqn("com.example.Outer")
                    .build();

            assertEquals("com.example.Outer", nested.getOuterTypeFqn());
            assertTrue(nested.isNested());
        }

        @Test
        @DisplayName("Nested type FQN uses dollar sign convention")
        void nestedTypeFqnUsesDollar() {
            UmlType nested = UmlType.builder()
                    .fqn("com.example.Container$Nested")
                    .simpleName("Nested")
                    .outerTypeFqn("com.example.Container")
                    .build();

            assertEquals("com.example.Container$Nested", nested.getFqn());
        }

        @Test
        @DisplayName("Deeply nested types use multiple dollar signs")
        void deeplyNestedTypeFqn() {
            UmlType deepNested = UmlType.builder()
                    .fqn("com.example.A$B$C")
                    .simpleName("C")
                    .outerTypeFqn("com.example.A$B")
                    .build();

            assertEquals("com.example.A$B$C", deepNested.getFqn());
            assertEquals("com.example.A$B", deepNested.getOuterTypeFqn());
            assertTrue(deepNested.isNested());
        }

        @Test
        @DisplayName("Static nested type with stereotype")
        void staticNestedType() {
            UmlType staticNested = UmlType.builder()
                    .fqn("com.example.Outer$StaticNested")
                    .simpleName("StaticNested")
                    .outerTypeFqn("com.example.Outer")
                    .addStereotype("static")
                    .build();

            assertEquals(1, staticNested.getStereotypes().size());
            assertEquals("static", staticNested.getStereotypes().get(0).name());
        }
    }

    @Nested
    @DisplayName("Package and equality behavior")
    class PackageAndEqualityBehavior {

        @Test
        @DisplayName("Default package type has empty package name")
        void defaultPackageName() {
            UmlType type = UmlType.builder()
                    .fqn("TopLevel")
                    .simpleName("TopLevel")
                    .build();

            assertEquals("", type.getPackageName());
        }

        @Test
        @DisplayName("Outer type simple name derived from outer FQN")
        void outerTypeSimpleNameDerived() {
            UmlType nested = UmlType.builder()
                    .fqn("com.example.Outer.Inner")
                    .simpleName("Inner")
                    .outerTypeFqn("com.example.Outer")
                    .build();

            assertEquals("Outer", nested.getOuterTypeSimpleName());
        }

        @Test
        @DisplayName("equals/hashCode depend on FQN only")
        void equalsAndHashCodeByFqn() {
            UmlType a = UmlType.builder().fqn("com.example.A").simpleName("A").kind(TypeKind.CLASS).build();
            UmlType b = UmlType.builder().fqn("com.example.A").simpleName("A2").kind(TypeKind.INTERFACE).build();
            UmlType c = UmlType.builder().fqn("com.example.C").simpleName("C").build();

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, c);
        }
    }
}
