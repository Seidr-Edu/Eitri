package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A method or constructor parameter in the UML model.
 * Immutable value type.
 */
public record UmlParameter(
        String name,
        String type,
        String typeSimpleName
) {
    /**
     * Creates a parameter with full type information.
     * @param name parameter name
     * @param type fully qualified type (e.g., "java.util.List<String>")
     * @param typeSimpleName simple type name (e.g., "List<String>")
     */
    public UmlParameter {
        Objects.requireNonNull(name, "Parameter name cannot be null");
        Objects.requireNonNull(type, "Parameter type cannot be null");
        if (typeSimpleName == null || typeSimpleName.isBlank()) {
            typeSimpleName = extractSimpleName(type);
        }
    }

    /**
     * Creates a parameter with just name and type (simple name derived).
     * @param name parameter name
     * @param type fully qualified type
     */
    public UmlParameter(String name, String type) {
        this(name, type, null);
    }

    /**
     * Renders this parameter for PlantUML: "name: Type" or just "name" if type is void/empty.
     * @return PlantUML parameter representation
     */
    public String toPlantUml() {
        if (typeSimpleName == null || typeSimpleName.isBlank() || "void".equals(typeSimpleName)) {
            return name;
        }
        return name + ": " + typeSimpleName;
    }

    private static String extractSimpleName(String fullType) {
        if (fullType == null || fullType.isBlank()) {
            return fullType;
        }
        // Handle generics: keep everything after last dot before '<'
        int genericStart = fullType.indexOf('<');
        String basePart = genericStart > 0 ? fullType.substring(0, genericStart) : fullType;
        int lastDot = basePart.lastIndexOf('.');
        if (lastDot < 0) {
            return fullType;
        }
        return fullType.substring(lastDot + 1);
    }
}
