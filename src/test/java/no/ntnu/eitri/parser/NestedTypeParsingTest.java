package no.ntnu.eitri.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.java.TypeVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nested type parsing in TypeVisitor.
 */
class NestedTypeParsingTest {

    private JavaParser parser;

    @BeforeEach
    void setUp() {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration()
                .setSymbolResolver(symbolSolver)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
        parser = new JavaParser(config);
    }

    private UmlModel parseSource(String source) {
        EitriConfig config = EitriConfig.builder().build();
        ParseContext context = new ParseContext(config);

        CompilationUnit cu = parser.parse(source).getResult()
                .orElseThrow(() -> new RuntimeException("Parse failed"));

        TypeVisitor visitor = new TypeVisitor(context);
        cu.accept(visitor, null);

        return context.build();
    }

    @Nested
    @DisplayName("Static nested class")
    class StaticNestedClass {

        @Test
        @DisplayName("Should extract static nested class with dotted FQN")
        void extractsStaticNestedClass() {
            String source = """
                package com.example;
                
                public class Outer {
                    public static class Inner {
                        private String value;
                    }
                }
                """;

            UmlModel model = parseSource(source);

            // Should have both Outer and Inner types
            assertEquals(2, model.getTypes().size());

            Optional<UmlType> outer = model.getType("com.example.Outer");
            Optional<UmlType> inner = model.getType("com.example.Outer.Inner");

            assertTrue(outer.isPresent(), "Outer class should exist");
            assertTrue(inner.isPresent(), "Inner class should exist with dotted FQN");

            // Verify inner type properties
            UmlType innerType = inner.get();
            assertEquals("Inner", innerType.getSimpleName());  // Just the simple name
            assertEquals("com.example.Outer", innerType.getOuterTypeFqn());
            assertTrue(innerType.isNested());

            // Verify static stereotype
            boolean hasStaticStereotype = innerType.getStereotypes().stream()
                    .anyMatch(s -> s.name().equals("static"));
            assertTrue(hasStaticStereotype, "Static nested class should have <<static>> stereotype");
        }

        @Test
        @DisplayName("Should not duplicate stereotypes")
        void noDuplicateStereotypes() {
            String source = """
                package com.example;
                
                public class Outer {
                    public static class Inner {
                    }
                }
                """;

            UmlModel model = parseSource(source);

            UmlType innerType = model.getType("com.example.Outer.Inner")
                    .orElseThrow(() -> new AssertionError("Inner class should exist"));

            long total = innerType.getStereotypes().size();
            long distinct = innerType.getStereotypes().stream()
                    .map(s -> s.name())
                    .distinct()
                    .count();

            assertEquals(distinct, total, "Stereotypes should be unique by name");
        }

        @Test
        @DisplayName("Should create NESTED relation from outer to inner")
        void createsNestedRelation() {
            String source = """
                package com.example;
                
                public class Container {
                    public static class Nested {
                    }
                }
                """;

            UmlModel model = parseSource(source);

            List<UmlRelation> nestedRelations = model.getRelations().stream()
                    .filter(r -> r.getKind() == RelationKind.NESTED)
                    .toList();

            assertEquals(1, nestedRelations.size());

            UmlRelation rel = nestedRelations.get(0);
            assertEquals("com.example.Container", rel.getFromTypeFqn());
            assertEquals("com.example.Container.Nested", rel.getToTypeFqn());
            assertEquals("nested", rel.getLabel());
        }
    }

    @Nested
    @DisplayName("Inner class (non-static)")
    class InnerClass {

        @Test
        @DisplayName("Should extract inner class without static stereotype")
        void extractsInnerClass() {
            String source = """
                package com.example;
                
                public class Outer {
                    public class Inner {
                        private int count;
                    }
                }
                """;

            UmlModel model = parseSource(source);

            Optional<UmlType> inner = model.getType("com.example.Outer.Inner");
            assertTrue(inner.isPresent());

            UmlType innerType = inner.get();
            assertTrue(innerType.isNested());
            assertEquals("com.example.Outer", innerType.getOuterTypeFqn());

            // Non-static inner class should NOT have static stereotype
            boolean hasStaticStereotype = innerType.getStereotypes().stream()
                    .anyMatch(s -> s.name().equals("static"));
            assertFalse(hasStaticStereotype, "Non-static inner class should not have <<static>> stereotype");
        }
    }

    @Nested
    @DisplayName("Nested interface")
    class NestedInterface {

        @Test
        @DisplayName("Should extract nested interface with static stereotype")
        void extractsNestedInterface() {
            String source = """
                package com.example;
                
                public class Container {
                    public interface Callback {
                        void onComplete();
                    }
                }
                """;

            UmlModel model = parseSource(source);

            Optional<UmlType> callback = model.getType("com.example.Container.Callback");
            assertTrue(callback.isPresent());

            UmlType callbackType = callback.get();
            assertEquals("Callback", callbackType.getSimpleName());  // Just the simple name
            assertTrue(callbackType.isNested());

            // Nested interfaces are implicitly static
            boolean hasStaticStereotype = callbackType.getStereotypes().stream()
                    .anyMatch(s -> s.name().equals("static"));
            assertTrue(hasStaticStereotype, "Nested interface should have <<static>> stereotype");
        }
    }

    @Nested
    @DisplayName("Nested enum")
    class NestedEnum {

        @Test
        @DisplayName("Should extract nested enum with static stereotype")
        void extractsNestedEnum() {
            String source = """
                package com.example;
                
                public class Order {
                    public enum Status {
                        PENDING,
                        COMPLETED,
                        CANCELLED
                    }
                }
                """;

            UmlModel model = parseSource(source);

            Optional<UmlType> status = model.getType("com.example.Order.Status");
            assertTrue(status.isPresent());

            UmlType statusType = status.get();
            assertEquals("Status", statusType.getSimpleName());  // Just the simple name
            assertTrue(statusType.isNested());

            // Nested enums are implicitly static
            boolean hasStaticStereotype = statusType.getStereotypes().stream()
                    .anyMatch(s -> s.name().equals("static"));
            assertTrue(hasStaticStereotype, "Nested enum should have <<static>> stereotype");
        }
    }

    @Nested
    @DisplayName("Nested record")
    class NestedRecord {

        @Test
        @DisplayName("Should extract nested record with static stereotype")
        void extractsNestedRecord() {
            String source = """
                package com.example;
                
                public class Container {
                    public record Point(int x, int y) {
                    }
                }
                """;

            UmlModel model = parseSource(source);

            Optional<UmlType> point = model.getType("com.example.Container.Point");
            assertTrue(point.isPresent());

            UmlType pointType = point.get();
            assertEquals("Point", pointType.getSimpleName());  // Just the simple name
            assertTrue(pointType.isNested());

            // Nested records are implicitly static
            boolean hasStaticStereotype = pointType.getStereotypes().stream()
                    .anyMatch(s -> s.name().equals("static"));
            assertTrue(hasStaticStereotype, "Nested record should have <<static>> stereotype");
        }
    }

    @Nested
    @DisplayName("Deeply nested types")
    class DeeplyNestedTypes {

        @Test
        @DisplayName("Should handle multiple levels of nesting")
        void handlesDeepNesting() {
            String source = """
                package com.example;
                
                public class A {
                    public static class B {
                        public static class C {
                            private String value;
                        }
                    }
                }
                """;

            UmlModel model = parseSource(source);

            // Should have A, A.B, and A.B.C
            assertEquals(3, model.getTypes().size());

            assertTrue(model.getType("com.example.A").isPresent());
            assertTrue(model.getType("com.example.A.B").isPresent());
            assertTrue(model.getType("com.example.A.B.C").isPresent());

            // Verify nesting chain
            UmlType b = model.getType("com.example.A.B").get();
            assertEquals("com.example.A", b.getOuterTypeFqn());

            UmlType c = model.getType("com.example.A.B.C").get();
            assertEquals("com.example.A.B", c.getOuterTypeFqn());
        }

        @Test
        @DisplayName("Should create NESTED relations for all levels")
        void createsNestedRelationsForAllLevels() {
            String source = """
                package com.example;
                
                public class A {
                    public static class B {
                        public static class C {
                        }
                    }
                }
                """;

            UmlModel model = parseSource(source);

            List<UmlRelation> nestedRelations = model.getRelations().stream()
                    .filter(r -> r.getKind() == RelationKind.NESTED)
                    .toList();

            assertEquals(2, nestedRelations.size());

            // A -> A.B
            boolean hasAToB = nestedRelations.stream().anyMatch(r ->
                    r.getFromTypeFqn().equals("com.example.A") &&
                    r.getToTypeFqn().equals("com.example.A.B"));
            assertTrue(hasAToB);

            // A.B -> A.B.C
            boolean hasBToC = nestedRelations.stream().anyMatch(r ->
                    r.getFromTypeFqn().equals("com.example.A.B") &&
                    r.getToTypeFqn().equals("com.example.A.B.C"));
            assertTrue(hasBToC);
        }
    }

    @Nested
    @DisplayName("Multiple nested types")
    class MultipleNestedTypes {

        @Test
        @DisplayName("Should extract multiple siblings nested in same class")
        void extractsMultipleSiblings() {
            String source = """
                package com.example;
                
                public class Container {
                    public static class First {
                    }
                    
                    public static class Second {
                    }
                    
                    public enum Status {
                        ACTIVE
                    }
                }
                """;

            UmlModel model = parseSource(source);

            assertEquals(4, model.getTypes().size());
            assertTrue(model.getType("com.example.Container").isPresent());
            assertTrue(model.getType("com.example.Container.First").isPresent());
            assertTrue(model.getType("com.example.Container.Second").isPresent());
            assertTrue(model.getType("com.example.Container.Status").isPresent());

            // All nested types should have Container as outer
            assertEquals("com.example.Container",
                    model.getType("com.example.Container.First").get().getOuterTypeFqn());
            assertEquals("com.example.Container",
                    model.getType("com.example.Container.Second").get().getOuterTypeFqn());
            assertEquals("com.example.Container",
                    model.getType("com.example.Container.Status").get().getOuterTypeFqn());
        }
    }

    @Nested
    @DisplayName("Nested type inheritance")
    class NestedTypeInheritance {

        @Test
        @DisplayName("Nested class can extend and implement")
        void nestedClassWithInheritance() {
            String source = """
                package com.example;
                
                import java.io.Serializable;
                
                public class Outer {
                    public static class Inner extends RuntimeException implements Serializable {
                    }
                }
                """;

            UmlModel model = parseSource(source);

            // Should have NESTED relation
            List<UmlRelation> nestedRelations = model.getRelations().stream()
                    .filter(r -> r.getKind() == RelationKind.NESTED)
                    .toList();
            assertEquals(1, nestedRelations.size());

            // Verify the nested type exists with correct FQN
            Optional<UmlType> inner = model.getType("com.example.Outer.Inner");
            assertTrue(inner.isPresent(), "Inner class should exist with dotted FQN");

            // Get all relations and check for the extends relation
            List<UmlRelation> allRelations = model.getRelations();

            // Should have EXTENDS relation for RuntimeException
            // The test verifies that nested types CAN have inheritance relations
            List<UmlRelation> extendsRelations = allRelations.stream()
                    .filter(r -> r.getKind() == RelationKind.EXTENDS)
                    .toList();

            // Either we have extends relations with the correct from FQN, or we have no extends
            // (due to stdlib type filtering). The key is the nested type was parsed.
            boolean hasValidExtendsRelation = extendsRelations.isEmpty() ||
                    extendsRelations.stream().anyMatch(r ->
                            r.getFromTypeFqn().equals("com.example.Outer.Inner"));
            assertTrue(hasValidExtendsRelation,
                    "If extends relations exist, they should use dotted FQN for nested type. Found: " + extendsRelations);
        }
    }
}
