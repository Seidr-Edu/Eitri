package no.ntnu.eitri.parser.resolution;

/**
 * Normalizes and resolves type references against a registry.
 */
public final class TypeReferenceResolver {

    private final TypeRegistry registry;
    private int totalRequests;
    private int resolvedReferences;
    private int reusedKnownTypes;
    private int skippedNullOrEmpty;
    private int skippedWildcard;
    private int skippedPrimitive;
    private int skippedNonFqn;
    private int skippedUnknownFqn;

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
            // Keep the registry-backed resolver strict: simple names and partially
            // qualified names are intentionally rejected to avoid accidental links to
            // wrong local types with matching simple names.
            skippedNonFqn++;
            return null;
        }

        if (registry.hasType(normalized)) {
            reusedKnownTypes++;
            resolvedReferences++;
            return normalized;
        }

        skippedUnknownFqn++;
        return null;
    }

    /**
     * Normalizes a type reference to a valid fully-qualified name without
     * requiring it to be registered in the type registry.
     *
     * <p>
     * This allows relation detection to produce relations targeting types
     * outside the parsed source (external libraries, JDK types, sibling
     * packages). The writer then decides which of these to render based on
     * the {@code hideCommonPackages}, {@code hideExternalPackages}, and
     * {@code hideSiblingPackages} configuration flags.
     *
     * @param fqn the type reference to normalize
     * @return the normalized FQN if valid, or {@code null} for primitives,
     *         wildcards, non-FQN simple names, etc.
     */
    public String normalizeToValidFqn(String fqn) {
        if (fqn == null || fqn.isEmpty()) {
            return null;
        }

        NormalizationResult normalization = normalizeTypeName(fqn);
        if (normalization.skipReason() != null) {
            return null;
        }
        String normalized = normalization.normalized();

        if (!isFullyQualifiedTypeName(normalized)) {
            // Normalization-only path uses the same lexical guard as the registry path.
            // This keeps relation endpoints deterministic and prevents malformed targets
            // from entering downstream package filtering/rendering logic.
            return null;
        }

        return normalized;
    }

    public TypeResolutionStats getStatsSnapshot() {
        return new TypeResolutionStats(
                totalRequests,
                resolvedReferences,
                reusedKnownTypes,
                skippedNullOrEmpty,
                skippedWildcard,
                skippedPrimitive,
                skippedNonFqn,
                skippedUnknownFqn);
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

        // Reject obvious "multiple types in one token" forms after normalization.
        if (base.indexOf(',') >= 0 || base.indexOf(' ') >= 0) {
            return new NormalizationResult(null, SkipReason.NON_FQN);
        }

        return new NormalizationResult(base, null);
    }

    private boolean isPrimitive(String type) {
        return switch (type) {
            case "void", "boolean", "byte", "short", "int", "long", "float", "double", "char" -> true;
            default -> false;
        };
    }

    /**
     * Validates that a type name is a proper fully-qualified Java type name.
     * Allows uppercase package segments, but still requires at least one
     * package-like (lowercase-leading) segment before the terminal type segment.
     * This keeps names like {@code Acme.tools.Service} valid while rejecting
     * likely no-package inner-class-style names like {@code JCommander.Builder}.
     */
    private boolean isFullyQualifiedTypeName(String type) {
        String[] segments = type.split("\\.");
        if (segments.length < 2) {
            return false;
        }

        boolean hasLowercaseLeadingPackageSegment = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isEmpty() || !isJavaIdentifier(segment)) {
                return false;
            }
            if (i < segments.length - 1 && Character.isLowerCase(segment.charAt(0))) {
                hasLowercaseLeadingPackageSegment = true;
            }
        }

        return hasLowercaseLeadingPackageSegment;
    }

    private boolean isJavaIdentifier(String segment) {
        if (!Character.isJavaIdentifierStart(segment.charAt(0))) {
            return false;
        }
        for (int i = 1; i < segment.length(); i++) {
            if (!Character.isJavaIdentifierPart(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void incrementSkipCounter(SkipReason reason) {
        switch (reason) {
            case NULL_OR_EMPTY -> skippedNullOrEmpty++;
            case WILDCARD -> skippedWildcard++;
            case PRIMITIVE -> skippedPrimitive++;
            case NON_FQN -> skippedNonFqn++;
        }
    }

    private enum SkipReason {
        NULL_OR_EMPTY,
        WILDCARD,
        PRIMITIVE,
        NON_FQN
    }

    private record NormalizationResult(String normalized, SkipReason skipReason) {
    }
}
