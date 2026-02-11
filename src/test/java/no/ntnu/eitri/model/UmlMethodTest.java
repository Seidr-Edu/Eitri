package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlMethod model behavior.
 */
class UmlMethodTest {

    @Nested
    @DisplayName("Method properties")
    class MethodProperties {

        @Test
        @DisplayName("Default return type is void")
        void defaultReturnType() {
            UmlMethod method = UmlMethod.builder()
                    .name("getName")
                    .build();

            assertEquals("void", method.getReturnType());
            assertEquals("void", method.getReturnTypeSimpleName());
        }

        @Test
        @DisplayName("Return type simple name is derived")
        void returnTypeSimpleName() {
            UmlMethod method = UmlMethod.builder()
                    .name("getItems")
                    .returnType("java.util.List<String>")
                    .build();

            assertEquals("List<String>", method.getReturnTypeSimpleName());
        }

        @Test
        @DisplayName("Parameters are preserved")
        void parametersPreserved() {
            UmlMethod method = UmlMethod.builder()
                    .name("calculate")
                    .returnType("double")
                    .visibility(Visibility.PUBLIC)
                    .addParameter("x", "int")
                    .addParameter("y", "int")
                    .build();

            assertEquals(2, method.getParameters().size());
            assertEquals("x", method.getParameters().get(0).name());
            assertEquals("int", method.getParameters().get(0).type());
            assertEquals("y", method.getParameters().get(1).name());
        }

        @Test
        @DisplayName("Constructor flag is preserved")
        void constructorFlag() {
            UmlMethod constructor = UmlMethod.builder()
                    .name("Customer")
                    .constructor(true)
                    .visibility(Visibility.PUBLIC)
                    .addParameter("name", "String")
                    .build();

            assertTrue(constructor.isConstructor());
        }

        @Test
        @DisplayName("Static modifier sets isStatic")
        void staticModifier() {
            UmlMethod method = UmlMethod.builder()
                    .name("getInstance")
                    .returnType("Singleton")
                    .addModifier(Modifier.STATIC)
                    .build();

            assertTrue(method.isStatic());
        }

        @Test
        @DisplayName("Abstract modifier sets isAbstract")
        void abstractModifier() {
            UmlMethod method = UmlMethod.builder()
                    .name("execute")
                    .returnType("void")
                    .addModifier(Modifier.ABSTRACT)
                    .build();

            assertTrue(method.isAbstract());
        }
    }

    @Nested
    @DisplayName("Additional behavior")
    class AdditionalBehavior {

        @Test
        @DisplayName("Nested generic return type is simplified recursively")
        void nestedGenericReturnType() {
            UmlMethod method = UmlMethod.builder()
                    .name("index")
                    .returnType("java.util.Map<java.lang.String, java.util.List<com.acme.Item>>")
                    .build();

            assertEquals("Map<String, List<Item>>", method.getReturnTypeSimpleName());
        }

        @Test
        @DisplayName("Collections are immutable after build")
        void collectionsAreImmutable() {
            UmlMethod method = UmlMethod.builder()
                    .name("work")
                    .addParameter("id", "long")
                    .addAnnotation("Transactional")
                    .addThrownException("java.io.IOException")
                    .addModifier(Modifier.STATIC)
                    .build();

            assertThrows(UnsupportedOperationException.class, () -> method.getParameters().add(new UmlParameter("x", "int")));
            assertThrows(UnsupportedOperationException.class, () -> method.getAnnotations().add("Other"));
            assertThrows(UnsupportedOperationException.class, () -> method.getThrownExceptions().add("java.lang.Exception"));
            assertThrows(UnsupportedOperationException.class, () -> method.getModifiers().add(Modifier.FINAL));
        }

        @Test
        @DisplayName("equals/hashCode depend on name and parameters only")
        void equalsAndHashCodeByNameAndParams() {
            UmlMethod a = UmlMethod.builder().name("m").returnType("int").addParameter("x", "int").build();
            UmlMethod b = UmlMethod.builder().name("m").returnType("java.lang.Integer").addParameter("x", "int").build();
            UmlMethod c = UmlMethod.builder().name("m").returnType("int").addParameter("y", "int").build();

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, c);
        }
    }
}
