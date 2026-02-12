package no.ntnu.eitri.parser;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParseContextTest {

    @Test
    void resolveTypeReferenceCreatesPlaceholderAndNormalizes() {
        ParseContext context = new ParseContext(EitriConfig.builder().build());

        String resolved = context.resolveTypeReference("  com.example.Order<java.lang.String>[]  ", "Owner.field");

        assertEquals("com.example.Order", resolved);
        assertTrue(context.hasType("com.example.Order"));
        assertEquals("Order", context.getType("com.example.Order").getSimpleName());
    }

    @Test
    void resolveTypeReferenceSkipsPrimitiveAndWildcard() {
        ParseContext context = new ParseContext(EitriConfig.builder().build());

        assertNull(context.resolveTypeReference("int", "Owner.field"));
        assertNull(context.resolveTypeReference("?", "Owner.field"));
        assertEquals("Number", context.resolveTypeReference("? extends Number", "Owner.field"));
        assertEquals("Number", context.resolveTypeReference("? super Number", "Owner.field"));
    }

    @Test
    void addTypeRejectsDuplicates() {
        ParseContext context = new ParseContext(EitriConfig.builder().build());
        UmlType type = UmlType.builder().fqn("com.example.A").simpleName("A").build();
        context.addType(type);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> context.addType(type));
        assertEquals("Type already registered: com.example.A", exception.getMessage());
    }

    @Test
    void buildResolvesPendingInheritanceAndPreservesDistinctRelationKinds() {
        ParseContext context = new ParseContext(EitriConfig.builder().diagramName("demo").build());
        context.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        context.addType(UmlType.builder().fqn("com.example.B").simpleName("B").build());
        context.addType(UmlType.builder().fqn("com.example.Base").simpleName("Base").build());

        context.addRelation(UmlRelation.dependency("com.example.A", "com.example.B", "uses"));
        context.addRelation(UmlRelation.association("com.example.A", "com.example.B", "holds"));
        context.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", "com.example.Base", RelationKind.EXTENDS
        ));

        UmlModel model = context.build();

        assertEquals("demo", model.getName());
        assertTrue(model.hasType("com.example.Base"));

        List<UmlRelation> relations = model.getRelations();
        assertEquals(3, relations.size());
        assertTrue(relations.stream().anyMatch(r ->
                r.getFromTypeFqn().equals("com.example.A")
                        && r.getToTypeFqn().equals("com.example.B")
                        && r.getKind() == RelationKind.DEPENDENCY));
        assertTrue(relations.stream().anyMatch(r ->
                r.getFromTypeFqn().equals("com.example.A")
                        && r.getToTypeFqn().equals("com.example.B")
                        && r.getKind() == RelationKind.ASSOCIATION));
        assertTrue(relations.stream().anyMatch(r ->
                r.getFromTypeFqn().equals("com.example.A")
                        && r.getToTypeFqn().equals("com.example.Base")
                        && r.getKind() == RelationKind.EXTENDS));
    }

    @Test
    void buildSkipsRelationsToMissingTypes() {
        ParseContext context = new ParseContext(EitriConfig.builder().build());
        context.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        context.addRelation(UmlRelation.association("com.example.A", "com.example.Missing", null));

        UmlModel model = context.build();

        assertTrue(model.getRelations().isEmpty());
    }

    @Test
    void warningsAreCopiedAndSourcePackagesAreTracked() {
        ParseContext context = new ParseContext(EitriConfig.builder().build());
        context.addWarning("warn");
        context.addSourcePackage("com.example");
        context.addSourcePackage(" ");

        List<String> warnings = context.getWarnings();
        warnings.add("client-mutation");

        assertEquals(List.of("warn"), context.getWarnings());
        assertEquals(java.util.Set.of("com.example"), context.getSourcePackages());
        assertThrows(UnsupportedOperationException.class, () -> context.getSourcePackages().add("x.y"));
    }
}
