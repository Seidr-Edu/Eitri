package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
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
        context.addType(UmlType.builder().fqn("com.example.Dependency").simpleName("Dependency").build());
        context.addType(UmlType.builder().fqn("com.example.Failure").simpleName("Failure").build());

        new RelationDetector(context).detectMethodDependencies(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(2, model.getRelations().size());
        assertTrue(model.getRelations().stream().allMatch(r -> r.getKind() == RelationKind.DEPENDENCY));
        assertTrue(model.getRelations().stream().noneMatch(r -> r.getToTypeFqn().equals(owner.getFqn())));
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
        context.addType(UmlType.builder().fqn("no.ntnu.eitri.writer.DiagramWriter").simpleName("DiagramWriter").build());
        context.addType(UmlType.builder().fqn("java.util.function.Supplier").simpleName("Supplier").build());

        new RelationDetector(context).detectMethodDependencies(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(2, model.getRelations().size());
        assertTrue(model.getRelations().stream().allMatch(r -> r.getKind() == RelationKind.DEPENDENCY));
        assertTrue(model.getRelations().stream().anyMatch(r -> "no.ntnu.eitri.writer.DiagramWriter".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().anyMatch(r -> "java.util.function.Supplier".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().noneMatch(r -> "C".equals(r.getToTypeFqn()) || "T".equals(r.getToTypeFqn())));
    }

    @Test
    void methodDependenciesSplitNestedGenericArgumentsAtTopLevelOnly() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addMethod(UmlMethod.builder()
                        .name("convert")
                        .returnType("java.util.Map<com.example.Pair<com.example.A, com.example.B>, com.example.Value>")
                        .build())
                .build();
        context.addType(owner);
        context.addType(UmlType.builder().fqn("com.example.Pair").simpleName("Pair").build());
        context.addType(UmlType.builder().fqn("com.example.Value").simpleName("Value").build());

        new RelationDetector(context).detectMethodDependencies(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertTrue(model.getRelations().stream().anyMatch(r -> "com.example.Pair".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().anyMatch(r -> "com.example.Value".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().noneMatch(r -> r.getToTypeFqn().endsWith(">")));
    }

    @Test
    void fieldRelationToExternalTypeIsCreated() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("logger").type("org.slf4j.Logger").isFinal(true).build())
                .build();
        context.addType(owner);
        // org.slf4j.Logger is NOT registered in context

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.COMPOSITION, model.getRelations().getFirst().getKind());
        assertEquals("org.slf4j.Logger", model.getRelations().getFirst().getToTypeFqn());
    }

    @Test
    void collectionFieldRelationToExternalTypeIsCreated() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("items").type("java.util.List<org.external.Item>").build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.AGGREGATION, model.getRelations().getFirst().getKind());
        assertEquals("org.external.Item", model.getRelations().getFirst().getToTypeFqn());
    }

    @Test
    void mapFieldCreatesSeparateRelationsPerTypeArgument() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("lookup")
                        .type("java.util.Map<com.example.Key, com.example.Value>").build())
                .build();
        context.addType(owner);
        context.addType(UmlType.builder().fqn("com.example.Key").simpleName("Key").build());
        context.addType(UmlType.builder().fqn("com.example.Value").simpleName("Value").build());

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(2, model.getRelations().size());
        assertTrue(model.getRelations().stream().allMatch(r -> r.getKind() == RelationKind.AGGREGATION));
        assertTrue(model.getRelations().stream().anyMatch(r -> "com.example.Key".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().anyMatch(r -> "com.example.Value".equals(r.getToTypeFqn())));
    }

    @Test
    void mapFieldWithNestedGenericTypeArgumentsSplitsOnlyAtTopLevelCommas() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("lookup")
                        .type("java.util.Map<com.example.Pair<com.example.A, com.example.B>, com.example.Value>")
                        .build())
                .build();
        context.addType(owner);
        context.addType(UmlType.builder().fqn("com.example.Pair").simpleName("Pair").build());
        context.addType(UmlType.builder().fqn("com.example.Value").simpleName("Value").build());

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(2, model.getRelations().size());
        assertTrue(model.getRelations().stream().anyMatch(r -> "com.example.Pair".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().anyMatch(r -> "com.example.Value".equals(r.getToTypeFqn())));
        assertTrue(model.getRelations().stream().noneMatch(r -> r.getToTypeFqn().endsWith(">")));
    }

    @Test
    void mapFieldWithSameTypeArgumentsCreatesOneRelation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("options")
                        .type("java.util.Map<java.lang.String, java.lang.String>").build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        // Both arguments resolve to the same type, dedup keeps one
        assertEquals(1, model.getRelations().size());
        assertEquals("java.lang.String", model.getRelations().getFirst().getToTypeFqn());
    }

    @Test
    void methodDependencyToExternalTypeIsCreated() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Service")
                .simpleName("Service")
                .addMethod(UmlMethod.builder()
                        .name("process")
                        .returnType("void")
                        .addParameter("input", "org.external.DataInput")
                        .build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectMethodDependencies(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.DEPENDENCY, model.getRelations().getFirst().getKind());
        assertEquals("org.external.DataInput", model.getRelations().getFirst().getToTypeFqn());
    }

    @Test
    void fieldWithPrimitiveOrSimpleNameProducesNoRelation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("count").type("int").build())
                .addField(UmlField.builder().name("label").type("String").build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(0, model.getRelations().size());
    }

    @Test
    void classFieldOfEnumTypeCreatesAssociation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Order")
                .simpleName("Order")
                .kind(TypeKind.CLASS)
                .addField(UmlField.builder().name("state").type("com.example.OrderState").isFinal(true).build())
                .build();
        UmlType enumType = UmlType.builder()
                .fqn("com.example.OrderState")
                .simpleName("OrderState")
                .kind(TypeKind.ENUM)
                .build();
        context.addType(owner);
        context.addType(enumType);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.ASSOCIATION, model.getRelations().getFirst().getKind());
        assertEquals(owner.getFqn(), model.getRelations().getFirst().getFromTypeFqn());
        assertEquals(enumType.getFqn(), model.getRelations().getFirst().getToTypeFqn());
        assertEquals("1", model.getRelations().getFirst().getToMultiplicity());
    }

    @Test
    void classFieldOfExternalEnumTypeCreatesAssociationWhenEnumIsLoadable() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Schedule")
                .simpleName("Schedule")
                .kind(TypeKind.CLASS)
                .addField(UmlField.builder().name("day").type("java.time.DayOfWeek").isFinal(true).build())
                .build();
        context.addType(owner);
        // java.time.DayOfWeek is not registered in the parse context.

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.ASSOCIATION, model.getRelations().getFirst().getKind());
        assertEquals("java.time.DayOfWeek", model.getRelations().getFirst().getToTypeFqn());
        assertEquals("1", model.getRelations().getFirst().getToMultiplicity());
    }

    @Test
    void staticSelfTypeFieldDoesNotCreateRelation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Singleton")
                .simpleName("Singleton")
                .addField(UmlField.builder()
                        .name("INSTANCE")
                        .type("com.example.Singleton")
                        .isStatic(true)
                        .isFinal(true)
                        .build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(0, model.getRelations().size());
    }

    @Test
    void collectionFieldWithoutGenericArgumentProducesNoRelation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("items").type("java.util.List").build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(0, model.getRelations().size());
    }

    @Test
    void collectionFieldWithTypeVariableArgumentProducesNoRelation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("items").type("java.util.List<T>").build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(0, model.getRelations().size());
    }

    @Test
    void malformedCollectionGenericProducesNoRelation() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.Owner")
                .simpleName("Owner")
                .addField(UmlField.builder().name("items").type("java.util.List<com.example.Item").build())
                .build();
        context.addType(owner);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(0, model.getRelations().size());
    }

    @Test
    void interfaceOwnerWithFinalEnumFieldStillCreatesComposition() {
        ParseContext context = new ParseContext(false);
        UmlType owner = UmlType.builder()
                .fqn("com.example.View")
                .simpleName("View")
                .kind(TypeKind.INTERFACE)
                .addField(UmlField.builder().name("mode").type("com.example.Mode").isFinal(true).build())
                .build();
        UmlType enumType = UmlType.builder()
                .fqn("com.example.Mode")
                .simpleName("Mode")
                .kind(TypeKind.ENUM)
                .build();
        context.addType(owner);
        context.addType(enumType);

        new RelationDetector(context).detectFieldRelations(owner.getFqn(), owner);
        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals(RelationKind.COMPOSITION, model.getRelations().getFirst().getKind());
    }
}
