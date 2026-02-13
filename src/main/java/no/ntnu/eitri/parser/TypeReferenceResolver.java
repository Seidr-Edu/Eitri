package no.ntnu.eitri.parser;

/**
 * Normalizes and resolves type references against a registry.
 */
final class TypeReferenceResolver {

    private final TypeRegistry registry;

    TypeReferenceResolver(TypeRegistry registry) {
        this.registry = registry;
    }

    String resolveTypeReference(String fqn) {
        if (fqn == null || fqn.isEmpty()) {
            return null;
        }

        String normalized = normalizeTypeName(fqn);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }

        if (!isFullyQualifiedTypeName(normalized)) {
            return null;
        }

        if (registry.hasType(normalized)) {
            return normalized;
        }

        registry.ensureTypeExists(normalized);
        return normalized;
    }

    private String normalizeTypeName(String typeName) {
        String base = typeName.trim();
        if (base.isEmpty()) {
            return null;
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
            return null;
        }

        return isPrimitive(base) ? null : base;
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
}
