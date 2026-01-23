package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlMethod and method signature formatting.
 */
class UmlMethodTest {

    @Nested
    @DisplayName("Signature formatting")
    class SignatureFormatting {

        @Test
        @DisplayName("Simple method with no parameters")
        void noParams() {
            UmlMethod method = UmlMethod.builder()
                    .name("getName")
                    .returnType("String")
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("getName()", method.getSignature());
        }

        @Test
        @DisplayName("Method with single parameter")
        void singleParam() {
            UmlMethod method = UmlMethod.builder()
                    .name("setName")
                    .returnType("void")
                    .visibility(Visibility.PUBLIC)
                    .addParameter("name", "String")
                    .build();

            assertEquals("setName(name: String)", method.getSignature());
        }

        @Test
        @DisplayName("Method with multiple parameters")
        void multipleParams() {
            UmlMethod method = UmlMethod.builder()
                    .name("calculate")
                    .returnType("double")
                    .visibility(Visibility.PUBLIC)
                    .addParameter("x", "int")
                    .addParameter("y", "int")
                    .build();

            assertEquals("calculate(x: int, y: int)", method.getSignature());
        }

        @Test
        @DisplayName("Constructor signature (same as method)")
        void constructorSignature() {
            UmlMethod constructor = UmlMethod.builder()
                    .name("Customer")
                    .constructor(true)
                    .visibility(Visibility.PUBLIC)
                    .addParameter("name", "String")
                    .build();

            assertEquals("Customer(name: String)", constructor.getSignature());
        }
    }

    @Nested
    @DisplayName("PlantUML rendering")
    class PlantUmlRendering {

        @Test
        @DisplayName("Public method with return type")
        void publicMethodWithReturn() {
            UmlMethod method = UmlMethod.builder()
                    .name("getId")
                    .returnType("Long")
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+getId() : Long", method.toPlantUml());
        }

        @Test
        @DisplayName("Private void method")
        void privateVoidMethod() {
            UmlMethod method = UmlMethod.builder()
                    .name("init")
                    .returnType("void")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("-init() : void", method.toPlantUml());
        }

        @Test
        @DisplayName("Protected method with parameters")
        void protectedWithParams() {
            UmlMethod method = UmlMethod.builder()
                    .name("process")
                    .returnType("Result")
                    .visibility(Visibility.PROTECTED)
                    .addParameter("data", "byte[]")
                    .build();

            assertEquals("#process(data: byte[]) : Result", method.toPlantUml());
        }

        @Test
        @DisplayName("Package-private method")
        void packagePrivate() {
            UmlMethod method = UmlMethod.builder()
                    .name("helper")
                    .returnType("void")
                    .visibility(Visibility.PACKAGE)
                    .build();

            assertEquals("~helper() : void", method.toPlantUml());
        }

        @Test
        @DisplayName("Static method")
        void staticMethod() {
            UmlMethod method = UmlMethod.builder()
                    .name("getInstance")
                    .returnType("Singleton")
                    .visibility(Visibility.PUBLIC)
                    .modifiers(Modifier.of(Modifier.STATIC))
                    .build();

            assertEquals("+{static} getInstance() : Singleton", method.toPlantUml());
        }

        @Test
        @DisplayName("Abstract method")
        void abstractMethod() {
            UmlMethod method = UmlMethod.builder()
                    .name("execute")
                    .returnType("void")
                    .visibility(Visibility.PUBLIC)
                    .modifiers(Modifier.of(Modifier.ABSTRACT))
                    .build();

            assertEquals("+{abstract} execute() : void", method.toPlantUml());
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

            // Constructor should not show return type
            assertEquals("+Customer(name: String)", constructor.toPlantUml());
        }

        @Test
        @DisplayName("Generic return type simplifies")
        void genericReturnType() {
            UmlMethod method = UmlMethod.builder()
                    .name("getItems")
                    .returnType("java.util.List<String>")
                    .visibility(Visibility.PUBLIC)
                    .build();

            // Should use simple name
            assertEquals("+getItems() : List<String>", method.toPlantUml());
        }
    }
}
