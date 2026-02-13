package no.ntnu.eitri.parser;

import no.ntnu.eitri.model.RelationKind;
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
                "com.example.A", "com.example.Base", RelationKind.EXTENDS
        ));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        assertEquals(1, relations.size());
        assertEquals(RelationKind.EXTENDS, relations.getFirst().getKind());
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
                "com.example.A", "org.external.Base", RelationKind.EXTENDS
        ));

        List<UmlRelation> relations = store.buildFinalRelations(types, resolver);

        assertEquals(1, relations.size());
        assertEquals(RelationKind.EXTENDS, relations.getFirst().getKind());
        assertEquals("org.external.Base", relations.getFirst().getToTypeFqn());
    }
}
