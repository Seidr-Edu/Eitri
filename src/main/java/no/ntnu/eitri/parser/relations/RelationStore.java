package no.ntnu.eitri.parser.relations;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.ParseContext;
import no.ntnu.eitri.parser.resolution.TypeReferenceResolver;
import no.ntnu.eitri.parser.resolution.TypeRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                // Inheritance targets are normalized but not forced to be registered.
                // This preserves extends/implements edges to external supertypes while
                // still filtering out malformed tokens.
                mergedRelations.add(UmlRelation.builder()
                        .fromTypeFqn(pi.fromFqn())
                        .toTypeFqn(targetFqn)
                        .kind(pi.kind())
                        .build());
            }
        }

        Map<String, UmlRelation> deduped = new LinkedHashMap<>();
        for (UmlRelation relation : mergedRelations) {
            // Require the FROM endpoint to be a parsed type.
            // The TO endpoint may be an external FQN — the writer decides
            // whether to render it based on package-hiding configuration.
            if (!types.hasType(relation.getFromTypeFqn())) {
                continue;
            }
            if (isRedundantEnumSelfRelation(relation, types)) {
                continue;
            }

            String key = relationContextKey(relation);
            deduped.putIfAbsent(key, relation);
        }

        return selectStrongestPerEndpoint(deduped.values());
    }

    /**
     * Applies global "strongest wins" per endpoint pair (from->to), keeping a
     * single relation between each pair.
      *
      * <p>
      * This intentionally favors readability over exhaustiveness for large models:
      * multiple weaker edges between the same two types are suppressed once a
      * stronger semantic relation exists.
     */
    private List<UmlRelation> selectStrongestPerEndpoint(Iterable<UmlRelation> relations) {
        Map<String, UmlRelation> strongestByEndpoint = new LinkedHashMap<>();
        for (UmlRelation relation : relations) {
            String key = endpointKey(relation);
            UmlRelation existing = strongestByEndpoint.get(key);
            if (existing == null || isStronger(relation, existing)) {
                strongestByEndpoint.put(key, relation);
            }
        }
        return new ArrayList<>(strongestByEndpoint.values());
    }

    private String endpointKey(UmlRelation relation) {
        return relation.getFromTypeFqn() + "->" + relation.getToTypeFqn();
    }

    private boolean isStronger(UmlRelation candidate, UmlRelation existing) {
        int candidateStrength = strength(candidate.getKind());
        int existingStrength = strength(existing.getKind());
        if (candidateStrength != existingStrength) {
            return candidateStrength > existingStrength;
        }

        // When equally strong, keep the most informative edge so labels/member context
        // are not lost in deduplication.
        int candidateDetail = detailScore(candidate);
        int existingDetail = detailScore(existing);
        if (candidateDetail != existingDetail) {
            return candidateDetail > existingDetail;
        }

        return false; // Keep first when equally strong and equally detailed.
    }

    private int strength(RelationKind kind) {
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

    private int detailScore(UmlRelation relation) {
        int score = 0;
        if (relation.getFromMultiplicity() != null)
            score++;
        if (relation.getToMultiplicity() != null)
            score++;
        if (relation.getFromMember() != null)
            score++;
        if (relation.getToMember() != null)
            score++;
        if (relation.getLabel() != null)
            score++;
        return score;
    }

    /**
     * Enum self-relations are typically parser artifacts (enum constants and implicit
     * enum APIs) and add little diagram value.
     */
    private boolean isRedundantEnumSelfRelation(UmlRelation relation, TypeRegistry types) {
        if (!relation.getFromTypeFqn().equals(relation.getToTypeFqn())) {
            return false;
        }

        UmlType fromType = types.getType(relation.getFromTypeFqn());
        return fromType != null && fromType.getKind() == TypeKind.ENUM;
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
