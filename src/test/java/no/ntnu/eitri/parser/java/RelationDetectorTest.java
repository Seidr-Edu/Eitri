package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationDetectorTest {

    @Test
    void finalFieldCreatesCompositionWithMultiplicityOne() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("part").type("com.example.Part").isFinal(true).build())
                .build();
        context.addType(owner);
        context.addType(UmlType.builder().fqn("com.example.Part").simpleName("Part").build());

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.COMPOSITION, model.getRelations().getFirst().getKind());
        assertEquals("1", model.getRelations().getFirst().getToMultiplicity());
    }

    @Test
    void nonFinalFieldCreatesAssociationWithOptionalMultiplicity() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("ref").type("com.example.Ref").build())
                .build();
        context.addType(owner);
        context.addType(UmlType.builder().fqn("com.example.Ref").simpleName("Ref").build());

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.ASSOCIATION, model.getRelations().getFirst().getKind());
        assertEquals("0..1", model.getRelations().getFirst().getToMultiplicity());
    }

    @Test
    void collectionAndArrayFieldsCreateAggregation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("items").type("java.util.List<com.example.Item>").build())
                .addField(UmlField.builder().name("refs").type("com.example.Ref[]").build())
                .build();
        context.addType(owner);
        context.addType(UmlType.builder().fqn("com.example.Item").simpleName("Item").build());
        context.addType(UmlType.builder().fqn("com.example.Ref").simpleName("Ref").build());

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        List<RelationKind> kinds = model.getRelations().stream().map(r -> r.getKind()).toList();
        assertEquals(List.of(RelationKind.AGGREGATION, RelationKind.AGGREGATION), kinds);
        assertTrue(model.getRelations().stream().allMatch(r -> "*".equals(r.getToMultiplicity())));
    }

    @Test
    void methodDependenciesIncludeParamsAndThrowsAndExcludeSelf() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addMethod(UmlMethod.builder()
                        .name("work")
                        .returnType("com.example.Owner")
                        .addParameter("dep", "com.example.Dependency")
                        .addThrownException("com.example.Failure")
                        .build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectMethodDependencies(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(2, model.getRelations().size());
        assertTrue(model.getRelations().stream().allMatch(r -> r.getKind() == RelationKind.DEPENDENCY));
        assertTrue(model.getRelations().stream().noneMatch(r -> r.getToTypeFqn().equals(owner.getFqn())));
        assertTrue(model.hasType("com.example.Dependency"));
        assertTrue(model.hasType("com.example.Failure"));
    }

    @Test
    void methodDependenciesSkipGenericTypeParametersButKeepFqnTypes() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addMethod(UmlMethod.builder()
                        .name("render")
                        .returnType("no.ntnu.eitri.writer.DiagramWriter<C>")
                        .addParameter("supplier", "java.util.function.Supplier<T>")
                        .build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectMethodDependencies(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(2, model.getRelations().size());
        assertTrue(model.getRelations().stream().allMatch(r -> r.getKind() == RelationKind.DEPENDENCY));
        assertTrue(model.getRelations().stream().anyMatch(r -> "no.ntnu.eitri.writer.DiagramWriter".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().anyMatch(r -> "java.util.function.Supplier".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().noneMatch(r -> "C".equals(r.getToTypeFqn()) || "T".equals(r.getToTypeFqn())));
        assertTrue(model.hasType("no.ntnu.eitri.writer.DiagramWriter"));
        assertTrue(model.hasType("java.util.function.Supplier"));
        assertTrue(!model.hasType("C") && !model.hasType("T"));
    }
}
