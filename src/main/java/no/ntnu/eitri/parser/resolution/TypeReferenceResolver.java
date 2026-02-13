package no.ntnu.eitri.parser.resolution;

/**
 * Normalizes and resolves type references against a registry.
 */
public final class TypeReferenceResolver {

    private final TypeRegistry registry;
    private int totalRequests;
    private int resolvedReferences;
    private int placeholdersCreated;
    private int reusedKnownTypes;
    private int skippedNullOrEmpty;
    private int skippedWildcard;
    private int skippedPrimitive;
    private int skippedNonFqn;

    public TypeReferenceResolver(TypeRegistry registry) {
        this.registry = registry;
    }

    public String resolveTypeReference(String fqn) {
        totalRequests++;

        if (fqn == null || fqn.isEmpty()) {
            skippedNullOrEmpty++;
            return null;
        }

        NormalizationResult normalization = normalizeTypeName(fqn);
        if (normalization.skipReason() != null) {
            incrementSkipCounter(normalization.skipReason());
            return null;
        }
        String normalized = normalization.normalized();

        if (!isFullyQualifiedTypeName(normalized)) {
            skippedNonFqn++;
            return null;
        }

        if (registry.hasType(normalized)) {
            reusedKnownTypes++;
            resolvedReferences++;
            return normalized;
        }

        registry.ensureTypeExists(normalized);
        placeholdersCreated++;
        resolvedReferences++;
        return normalized;
    }

    public TypeResolutionStats getStatsSnapshot() {
        return new TypeResolutionStats(
                totalRequests,
                resolvedReferences,
                placeholdersCreated,
                reusedKnownTypes,
                skippedNullOrEmpty,
                skippedWildcard,
                skippedPrimitive,
                skippedNonFqn);
    }

    private NormalizationResult normalizeTypeName(String typeName) {
        String base = typeName.trim();
        if (base.isEmpty()) {
            return new NormalizationResult(null, SkipReason.NULL_OR_EMPTY);
        }

        while (base.endsWith("[]")) {
            base = base.substring(0, base.length() - 2).trim();
        }

        int genericStart = base.indexOf('<');
        if (genericStart >= 0) {
            base = base.substring(0, genericStart).trim();
        }

        if (base.startsWith("? extends ")) {
            base = base.substring("? extends ".length()).trim();
        } else if (base.startsWith("? super ")) {
            base = base.substring("? super ".length()).trim();
        } else if ("?".equals(base)) {
            return new NormalizationResult(null, SkipReason.WILDCARD);
        }

        if (isPrimitive(base)) {
            return new NormalizationResult(null, SkipReason.PRIMITIVE);
        }

        return new NormalizationResult(base, null);
    }

    private boolean isPrimitive(String type) {
        return switch (type) {
            case "void", "boolean", "byte", "short", "int", "long", "float", "double", "char" -> true;
            default -> false;
        };
    }

    private boolean isFullyQualifiedTypeName(String type) {
        return type.indexOf('.') >= 0;
    }

    private void incrementSkipCounter(SkipReason reason) {
        switch (reason) {
            case NULL_OR_EMPTY -> skippedNullOrEmpty++;
            case WILDCARD -> skippedWildcard++;
            case PRIMITIVE -> skippedPrimitive++;
        }
    }

    private enum SkipReason {
        NULL_OR_EMPTY,
        WILDCARD,
        PRIMITIVE
    }

    private record NormalizationResult(String normalized, SkipReason skipReason) {
    }
}
