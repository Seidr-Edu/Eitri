package no.ntnu.eitri.parser;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.resolution.TypeResolutionStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParseContextTest {

    @Test
    void resolveTypeReferenceResolvesRegisteredTypeAndNormalizes() {
        ParseContext context = new ParseContext(false);
        context.addType(UmlType.builder().fqn("com.example.Order").simpleName("Order").build());

        String resolved = context.resolveTypeReference("  com.example.Order<java.lang.String>[]  ");

        assertEquals("com.example.Order", resolved);
    }

    @Test
    void resolveTypeReferenceSkipsUnregisteredFqn() {
        ParseContext context = new ParseContext(false);

        assertNull(context.resolveTypeReference("com.example.Unknown"));
        assertTrue(context.getTypes().isEmpty());
    }

    @Test
    void resolveTypeReferenceSkipsPrimitiveAndWildcard() {
        ParseContext context = new ParseContext(false);

        assertNull(context.resolveTypeReference("int"));
        assertNull(context.resolveTypeReference("?"));
        assertNull(context.resolveTypeReference("? extends Number"));
        assertNull(context.resolveTypeReference("? super Number"));
    }

    @Test
    void resolveTypeReferenceSkipsUnqualifiedTypesWithoutCreatingPlaceholders() {
        ParseContext context = new ParseContext(false);

        assertNull(context.resolveTypeReference("T"));
        assertNull(context.resolveTypeReference("UnknownType"));
        assertTrue(context.getTypes().isEmpty());
    }

    @Test
    void addTypeRejectsDuplicates() {
        ParseContext context = new ParseContext(false);
        UmlType type = UmlType.builder().fqn("com.example.A").simpleName("A").build();
        context.addType(type);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> context.addType(type));
        assertEquals("Type already registered: com.example.A", exception.getMessage());
    }

    @Test
    void buildResolvesPendingInheritanceAndKeepsOnlyStrongestRelationPerEndpoint() {
        ParseContext context = new ParseContext(false);
        context.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        context.addType(UmlType.builder().fqn("com.example.B").simpleName("B").build());
        context.addType(UmlType.builder().fqn("com.example.Base").simpleName("Base").build());

        context.addRelation(UmlRelation.dependency("com.example.A", "com.example.B", "uses"));
        context.addRelation(UmlRelation.association("com.example.A", "com.example.B", "holds"));
        context.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", "com.example.Base", RelationKind.EXTENDS
        ));

        UmlModel model = context.build();

        assertEquals("diagram", model.getName());
        assertTrue(model.hasType("com.example.Base"));

        List<UmlRelation> relations = model.getRelations();
        assertEquals(2, relations.size());
        assertTrue(relations.stream().noneMatch(r ->
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
    void buildSkipsRelationsWhereFromTypeIsMissing() {
        ParseContext context = new ParseContext(false);
        context.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        // FROM is missing — relation should be dropped
        context.addRelation(UmlRelation.association("com.example.Missing", "com.example.A", null));

        UmlModel model = context.build();

        assertTrue(model.getRelations().isEmpty());
    }

    @Test
    void buildKeepsRelationsWhereToTypeIsExternalFqn() {
        ParseContext context = new ParseContext(false);
        context.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        // TO is external but FROM is registered — relation should survive
        context.addRelation(UmlRelation.association("com.example.A", "com.example.Missing", null));

        UmlModel model = context.build();

        assertEquals(1, model.getRelations().size());
        assertEquals("com.example.Missing", model.getRelations().getFirst().getToTypeFqn());
    }

    @Test
    void warningsAreCopiedAndSourcePackagesAreTracked() {
        ParseContext context = new ParseContext(false);
        context.addWarning("warn");
        context.addSourcePackage("com.example");
        context.addSourcePackage(" ");

        List<String> warnings = context.getWarnings();
        warnings.add("client-mutation");

        assertEquals(List.of("warn"), context.getWarnings());
        assertEquals(java.util.Set.of("com.example"), context.getSourcePackages());
        assertThrows(UnsupportedOperationException.class, () -> context.getSourcePackages().add("x.y"));
    }

    @Test
    void typeResolutionStatsTrackSkippedAndResolvedReferences() {
        ParseContext context = new ParseContext(false);
        context.addType(UmlType.builder().fqn("com.example.Known").simpleName("Known").build());

        assertNull(context.resolveTypeReference("T"));
        assertNull(context.resolveTypeReference("int"));
        assertNull(context.resolveTypeReference("?"));
        assertNull(context.resolveTypeReference("  "));
        assertNull(context.resolveTypeReference("com.example.NewType"));
        assertEquals("com.example.Known", context.resolveTypeReference("com.example.Known"));

        TypeResolutionStats stats = context.getTypeResolutionStats();
        assertEquals(6, stats.totalRequests());
        assertEquals(1, stats.resolvedReferences());
        assertEquals(1, stats.reusedKnownTypes());
        assertEquals(1, stats.skippedNonFqn());
        assertEquals(1, stats.skippedPrimitive());
        assertEquals(1, stats.skippedWildcard());
        assertEquals(1, stats.skippedNullOrEmpty());
        assertEquals(1, stats.skippedUnknownFqn());
        assertEquals(5, stats.skippedTotal());
    }

    @Test
    void parseReportWrapsWarningsAndResolutionStats() {
        ParseContext context = new ParseContext(false);
        context.addWarning("warn-1");
        context.addWarning("warn-2");
        context.resolveTypeReference("T");
        context.resolveTypeReference("com.example.Valid");

        ParseReport report = context.getReport();

        assertEquals(2, report.warningCount());
        assertEquals(List.of("warn-1", "warn-2"), report.warnings());
        assertEquals(2, report.typeResolutionStats().totalRequests());
        assertEquals(1, report.typeResolutionStats().skippedNonFqn());
        assertEquals(0, report.typeResolutionStats().resolvedReferences());
        assertEquals(1, report.typeResolutionStats().skippedUnknownFqn());
    }
}
