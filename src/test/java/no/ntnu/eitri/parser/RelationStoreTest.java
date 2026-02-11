package no.ntnu.eitri.parser;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
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
    void buildFinalRelationsKeepsStrongestRelationPerTypePair() {
        TypeRegistry types = new TypeRegistry();
        types.addType(UmlType.builder().fqn("com.example.A").simpleName("A").build());
        types.addType(UmlType.builder().fqn("com.example.B").simpleName("B").build());

        RelationStore store = new RelationStore();
        store.addRelation(UmlRelation.dependency("com.example.A", "com.example.B", null));
        store.addRelation(UmlRelation.association("com.example.A", "com.example.B", null));

        List<UmlRelation> relations = store.buildFinalRelations(types, new TypeReferenceResolver(types));

        assertEquals(1, relations.size());
        assertTrue(relations.stream().allMatch(r -> r.getKind() == RelationKind.ASSOCIATION));
    }
}
