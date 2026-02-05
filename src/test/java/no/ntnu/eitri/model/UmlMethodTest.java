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
}
