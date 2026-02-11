package no.ntnu.eitri.parser;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlRelation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects and finalizes relations produced during parsing.
 */
final class RelationStore {

    private final List<UmlRelation> relations = new ArrayList<>();
    private final List<ParseContext.PendingInheritance> pendingInheritance = new ArrayList<>();

    void addRelation(UmlRelation relation) {
        relations.add(relation);
    }

    void addPendingInheritance(ParseContext.PendingInheritance pending) {
        pendingInheritance.add(pending);
    }

    int relationCount() {
        return relations.size();
    }

    List<UmlRelation> buildFinalRelations(TypeRegistry types, TypeReferenceResolver typeResolver) {
        List<UmlRelation> mergedRelations = new ArrayList<>(relations);

        for (ParseContext.PendingInheritance pi : pendingInheritance) {
            String targetFqn = typeResolver.resolveTypeReference(pi.toFqn());
            if (targetFqn != null) {
                mergedRelations.add(UmlRelation.builder()
                        .fromTypeFqn(pi.fromFqn())
                        .toTypeFqn(targetFqn)
                        .kind(pi.kind())
                        .build());
            }
        }

        Map<String, UmlRelation> deduped = new HashMap<>();
        for (UmlRelation relation : mergedRelations) {
            if (!types.hasType(relation.getFromTypeFqn()) || !types.hasType(relation.getToTypeFqn())) {
                continue;
            }

            String key = relation.getFromTypeFqn() + "->" + relation.getToTypeFqn();
            UmlRelation existing = deduped.get(key);
            if (existing == null || isStronger(relation.getKind(), existing.getKind())) {
                deduped.put(key, relation);
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private boolean isStronger(RelationKind a, RelationKind b) {
        return strengthOf(a) > strengthOf(b);
    }

    private int strengthOf(RelationKind kind) {
        return switch (kind) {
            case NESTED -> 7;
            case EXTENDS -> 6;
            case IMPLEMENTS -> 5;
            case COMPOSITION -> 4;
            case AGGREGATION -> 3;
            case ASSOCIATION -> 2;
            case DEPENDENCY -> 1;
        };
    }
}
