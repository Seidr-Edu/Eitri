package no.ntnu.eitri.parser;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.relations.RelationStore;
import no.ntnu.eitri.parser.resolution.TypeReferenceResolver;
import no.ntnu.eitri.parser.resolution.TypeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationStoreTest {

    @Test
    void buildFinalRelationsResolvesPendingInheritance() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.Base").simpleName("Base").build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", "com.example.Base", RelationKind.EXTENDS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        assertEquals(1, relations.size());
        assertEquals(RelationKind.EXTENDS, relations.getFirst().getKind());
    }

    @Test
    void buildFinalRelationsResolvesPendingInheritanceSimpleNameInSamePackage() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.core.DefaultExecutorSupplier")
                .simpleName("DefaultExecutorSupplier").build());
        types.addType(UmlType.builder().fqn("com.example.core.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.core.DefaultExecutorSupplier", "ExecutorSupplier",
                RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        assertEquals(1, relations.size());
        assertEquals(RelationKind.IMPLEMENTS, relations.getFirst().getKind());
        assertEquals("com.example.core.ExecutorSupplier", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsDoesNotUseDifferentPackageForSameSimpleNameWhenSamePackageResolutionFails() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.core.DefaultExecutorSupplier")
                .simpleName("DefaultExecutorSupplier").build());
        types.addType(UmlType.builder().fqn("com.other.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.core.DefaultExecutorSupplier", "ExecutorSupplier",
                RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        // Same-package resolution should fail, and fallback resolver should select the
        // matching type
        // in a different package.
        assertEquals(1, relations.size());
        assertEquals(RelationKind.IMPLEMENTS, relations.getFirst().getKind());
        assertEquals("com.other.ExecutorSupplier", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsSkipsRelationWhenSimpleNameDoesNotMatchAnyType() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.core.DefaultExecutorSupplier")
                .simpleName("DefaultExecutorSupplier").build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.core.DefaultExecutorSupplier", "ExecutorSupplier",
                RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        // No matching type exists for "ExecutorSupplier", so no relation should be
        // produced.
        assertEquals(0, relations.size());
    }

    @Test
    void buildFinalRelationsResolvesPendingInheritanceFromDefaultPackageSource() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("DefaultExecutorSupplier")
                .simpleName("DefaultExecutorSupplier").build());
        types.addType(UmlType.builder().fqn("com.example.core.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "DefaultExecutorSupplier", "ExecutorSupplier", RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        // Source type is in the default package; fallback resolution should still
        // resolve the simple name.
        assertEquals(1, relations.size());
        assertEquals(RelationKind.IMPLEMENTS, relations.getFirst().getKind());
        assertEquals("com.example.core.ExecutorSupplier", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsTreatsTargetWithDotsAsFqnNotSimpleName() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.core.DefaultExecutorSupplier")
                .simpleName("DefaultExecutorSupplier").build());
        types.addType(UmlType.builder().fqn("com.example.core.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.core.DefaultExecutorSupplier",
                "com.example.core.ExecutorSupplier",
                RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        // Target contains dots and should be treated as an FQN; resolution must succeed
        // accordingly.
        assertEquals(1, relations.size());
        assertEquals(RelationKind.IMPLEMENTS, relations.getFirst().getKind());
        assertEquals("com.example.core.ExecutorSupplier", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsSkipsPendingInheritanceWhenTargetSimpleNameIsEmpty() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        RelationStore store = new RelationStore();
        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", "", RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(0, relations.size());
    }

    @Test
    void buildFinalRelationsSkipsPendingInheritanceWhenTargetSimpleNameIsNull() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        RelationStore store = new RelationStore();
        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", null, RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(0, relations.size());
    }

    @Test
    void buildFinalRelationsSkipsPendingInheritanceWhenTargetContainsDollarSign() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.Outer.Inner")
                .simpleName("Inner")
                .build());

        RelationStore store = new RelationStore();
        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", "Outer$Inner", RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(0, relations.size());
    }

    @Test
    void buildFinalRelationsSkipsPendingInheritanceWhenTargetIsNotValidJavaIdentifier() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        RelationStore store = new RelationStore();
        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", "1ExecutorSupplier", RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(0, relations.size());
    }

    @Test
    void buildFinalRelationsResolvesFromSourceWithDollarNestedClassName() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.Outer$Inner")
                .simpleName("Inner")
                .outerTypeFqn("com.example.Outer")
                .build());
        types.addType(UmlType.builder().fqn("com.example.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());
        types.addType(UmlType.builder().fqn("com.aaa.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        RelationStore store = new RelationStore();
        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.Outer$Inner", "ExecutorSupplier", RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(1, relations.size());
        assertEquals("com.example.ExecutorSupplier", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsResolvesSamePackageFromNestedSourceType() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.Outer.Inner")
                .simpleName("Inner")
                .outerTypeFqn("com.example.Outer")
                .build());
        types.addType(UmlType.builder().fqn("com.example.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());
        types.addType(UmlType.builder().fqn("com.aaa.ExecutorSupplier")
                .simpleName("ExecutorSupplier")
                .kind(TypeKind.INTERFACE)
                .build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.Outer.Inner", "ExecutorSupplier", RelationKind.IMPLEMENTS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        assertEquals(1, relations.size());
        assertEquals(RelationKind.IMPLEMENTS, relations.getFirst().getKind());
        assertEquals("com.example.ExecutorSupplier", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsKeepsOnlyStrongestRelationKindForSameTypePair() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.B").simpleName("B").build());

        RelationStore store = new RelationStore();
        store.addRelation(UmlRelation.dependency("com.example.A", "com.example.B", null));
        store.addRelation(UmlRelation.association("com.example.A", "com.example.B", null));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(1, relations.size());
        assertTrue(relations.stream().noneMatch(r -> r.getKind() == RelationKind.DEPENDENCY));
        assertTrue(relations.stream().anyMatch(r -> r.getKind() == RelationKind.ASSOCIATION));
    }

    @Test
    void buildFinalRelationsCollapsesMemberToMemberRelationsByEndpoint() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.B").simpleName("B").build());

        RelationStore store = new RelationStore();
        store.addRelation(UmlRelation.builder()
                .fromTypeFqn("com.example.A")
                .toTypeFqn("com.example.B")
                .kind(RelationKind.ASSOCIATION)
                .fromMember("primary")
                .toMember("x")
                .build());
        store.addRelation(UmlRelation.builder()
                .fromTypeFqn("com.example.A")
                .toTypeFqn("com.example.B")
                .kind(RelationKind.ASSOCIATION)
                .fromMember("secondary")
                .toMember("y")
                .build());

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(1, relations.size());
        assertEquals("primary", relations.getFirst().getFromMember());
    }

    @Test
    void buildFinalRelationsCollapsesFromMemberOnlyRelationsByEndpoint() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.B").simpleName("B").build());

        RelationStore store = new RelationStore();
        store.addRelation(UmlRelation.builder()
                .fromTypeFqn("com.example.A")
                .toTypeFqn("com.example.B")
                .kind(RelationKind.ASSOCIATION)
                .fromMember("f1")
                .build());
        store.addRelation(UmlRelation.builder()
                .fromTypeFqn("com.example.A")
                .toTypeFqn("com.example.B")
                .kind(RelationKind.ASSOCIATION)
                .fromMember("f2")
                .build());

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(1, relations.size());
        assertEquals("f1", relations.getFirst().getFromMember());
    }

    @Test
    void buildFinalRelationsAllowsExternalToEndpoint() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        // com.external.Lib is NOT registered

        RelationStore store = new RelationStore();
        store.addRelation(UmlRelation.dependency("com.example.A", "com.external.Lib", null));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(1, relations.size());
        assertEquals("com.external.Lib", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsDropsRelationWhenFromEndpointNotRegistered() {
        TypeRegistry types = new TypeRegistry();
        // Neither endpoint is registered

        RelationStore store = new RelationStore();
        store.addRelation(UmlRelation.dependency("com.unknown.A", "com.unknown.B", null));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(0, relations.size());
    }

    @Test
    void buildFinalRelationsResolvesPendingInheritanceToExternalType() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());

        TypeReferenceResolver resolver = new TypeReferenceResolver(types);
        RelationStore store = new RelationStore();

        store.addPendingInheritance(new ParseContext.PendingInheritance(
                "com.example.A", "org.external.Base", RelationKind.EXTENDS));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        assertEquals(1, relations.size());
        assertEquals(RelationKind.EXTENDS, relations.getFirst().getKind());
        assertEquals("org.external.Base", relations.getFirst().getToTypeFqn());
    }

    @Test
    void buildFinalRelationsPrunesEnumSelfRelations() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder()
                .fqn("com.example.Status")
                .simpleName("Status")
                .kind(TypeKind.ENUM)
                .build());

        RelationStore store = new RelationStore();
        store.addRelation(UmlRelation.association("com.example.Status", "com.example.Status", null));
        store.addRelation(UmlRelation.dependency("com.example.Status", "com.example.Status", null));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(0, relations.size());
    }
}
