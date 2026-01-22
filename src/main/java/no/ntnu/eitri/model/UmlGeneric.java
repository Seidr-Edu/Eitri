package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A generic type parameter on a UML type.
 * Example: T, E extends Comparable, ? super Number
 */
public record UmlGeneric(
        String identifier,
        String bounds
) {
    /**
     * Creates a generic type parameter.
     * @param identifier the type variable name (e.g., "T", "E", "?")
     * @param bounds optional bounds (e.g., "extends Comparable", "super Number")
     */
    public UmlGeneric {
        Objects.requireNonNull(identifier, "Generic identifier cannot be null");
    }

    /**
     * Creates a simple generic without bounds.
     * @param identifier the type variable name
     */
    public UmlGeneric(String identifier) {
        this(identifier, null);
    }

    /**
     * Renders this generic for PlantUML.
     * Example: T or E extends Comparable
     */
    public String toPlantUml() {
        if (bounds != null && !bounds.isBlank()) {
            return identifier + " " + bounds;
        }
        return identifier;
    }
}
