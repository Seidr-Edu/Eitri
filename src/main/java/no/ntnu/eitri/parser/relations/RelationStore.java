package no.ntnu.eitri.parser.relations;

import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.parser.ParseContext;
import no.ntnu.eitri.parser.resolution.TypeReferenceResolver;
import no.ntnu.eitri.parser.resolution.TypeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects and finalizes relations produced during parsing.
 */
public final class RelationStore {

    private final List<UmlRelation> relations = new ArrayList<>();
    private final List<ParseContext.PendingInheritance> pendingInheritance = new ArrayList<>();

    public void addRelation(UmlRelation relation) {
        relations.add(relation);
    }

    public void addPendingInheritance(ParseContext.PendingInheritance pending) {
        pendingInheritance.add(pending);
    }

    public int relationCount() {
        return relations.size();
    }

    public List<UmlRelation> buildFinalRelations(TypeRegistry types, TypeReferenceResolver typeResolver) {
        List<UmlRelation> mergedRelations = new ArrayList<>(relations);

        for (ParseContext.PendingInheritance pi : pendingInheritance) {
            String targetFqn = typeResolver.normalizeToValidFqn(pi.toFqn());
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
            // Require the FROM endpoint to be a parsed type.
            // The TO endpoint may be an external FQN — the writer decides
            // whether to render it based on package-hiding configuration.
            if (!types.hasType(relation.getFromTypeFqn())) {
                continue;
            }

            String key = relationContextKey(relation);
            deduped.putIfAbsent(key, relation);
        }

        return new ArrayList<>(deduped.values());
    }

    /**
     * Dedupes only semantically identical relations.
     */
    private String relationContextKey(UmlRelation relation) {
        return new StringBuilder()
                .append(relation.getFromTypeFqn())
                .append("->")
                .append(relation.getToTypeFqn())
                .append("|kind:").append(relation.getKind())
                .append("|fromMultiplicity:").append(relation.getFromMultiplicity())
                .append("|toMultiplicity:").append(relation.getToMultiplicity())
                .append("|fromMember:").append(relation.getFromMember())
                .append("|toMember:").append(relation.getToMember())
                .append("|label:").append(relation.getLabel())
                .toString();
    }
}
