package no.ntnu.eitri.parser.resolution;

/**
 * Aggregated statistics for type-reference resolution during parsing.
 */
public record TypeResolutionStats(
        int totalRequests,
        int resolvedReferences,
        int reusedKnownTypes,
        int skippedNullOrEmpty,
        int skippedWildcard,
        int skippedPrimitive,
        int skippedNonFqn,
        int skippedUnknownFqn
) {
    public int skippedTotal() {
        return skippedNullOrEmpty + skippedWildcard + skippedPrimitive + skippedNonFqn + skippedUnknownFqn;
    }
}
