package no.ntnu.eitri.parser;

/**
 * Aggregated statistics for type-reference resolution during parsing.
 */
public record TypeResolutionStats(
        int totalRequests,
        int resolvedReferences,
        int placeholdersCreated,
        int reusedKnownTypes,
        int skippedNullOrEmpty,
        int skippedWildcard,
        int skippedPrimitive,
        int skippedNonFqn
) {
    public int skippedTotal() {
        return skippedNullOrEmpty + skippedWildcard + skippedPrimitive + skippedNonFqn;
    }
}
