package no.ntnu.eitri.app;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated repository statistics for a parse run.
 */
public record RepositoryStats(
        int sourcePathCount,
        int sourceFileCount,
        int typeCount,
        int topLevelTypeCount,
        int nestedTypeCount,
        int packageCount,
        List<String> packages,
        Map<String, Integer> packageTypeCounts,
        Map<String, Integer> typeKindCounts
) {
    public RepositoryStats {
        packages = packages != null ? List.copyOf(packages) : List.of();
        packageTypeCounts = packageTypeCounts != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(packageTypeCounts))
                : Map.of();
        typeKindCounts = typeKindCounts != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(typeKindCounts))
                : Map.of();
    }
}
