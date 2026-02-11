package no.ntnu.eitri.parser.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeVisitorFeatureTest {

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

    @Test
    void annotationMemberDefaultValueIsRenderedInMethodName() {
        String source = """
                package com.example;
                public @interface Flag {
                    String value() default "x";
                }
                """;

        ParseContext context = new ParseContext(EitriConfig.builder().build());
        CompilationUnit cu = parser.parse(source).getResult().orElseThrow();
        cu.accept(new TypeVisitor(context), null);

        UmlType annotation = context.build().getType("com.example.Flag").orElseThrow();
        assertEquals(TypeKind.ANNOTATION, annotation.getKind());
        assertEquals(1, annotation.getMethods().size());

        UmlMethod method = annotation.getMethods().getFirst();
        assertEquals("value() = \"x\"", method.getName());
        assertTrue(method.isAbstract());
    }

    @Test
    void unresolvedTypesFallbackToSourceRepresentationAndAddWarning() {
        String source = """
                package com.example;
                public class Holder {
                    private UnknownType value;
                }
                """;

        ParseContext context = new ParseContext(EitriConfig.builder().build());
        CompilationUnit cu = parser.parse(source).getResult().orElseThrow();
        cu.accept(new TypeVisitor(context), null);

        UmlType holder = context.build().getType("com.example.Holder").orElseThrow();
        assertEquals("UnknownType", holder.getFields().getFirst().getType());
        assertFalse(context.getWarnings().isEmpty());
        assertTrue(context.getWarnings().stream().anyMatch(w -> w.contains("Failed to resolve type 'UnknownType'")));
    }
}
