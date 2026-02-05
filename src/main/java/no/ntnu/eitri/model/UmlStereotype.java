package no.ntnu.eitri.model;

import java.util.List;
import java.util.Objects;

/**
 * A stereotype annotation on a UML type.
 * Example: &lt;&lt;Singleton&gt;&gt; or &lt;&lt;(S,#FF0000) Service&gt;&gt;
 */
public record UmlStereotype(
        String name,
        Character spotChar,
        String spotColor,
        List<String> values  // Annotation values for display
) {
    /**
     * Creates a stereotype with optional spot (icon letter and color) and values.
     * @param name the stereotype name (e.g., "Singleton")
     * @param spotChar optional spot character (e.g., 'S')
     * @param spotColor optional spot color (e.g., "#FF0000")
     * @param values optional annotation values
     */
    public UmlStereotype {
        Objects.requireNonNull(name, "Stereotype name cannot be null");
        values = values != null ? List.copyOf(values) : List.of();
    }

    /**
     * Creates a simple stereotype without spot.
     * @param name the stereotype name
     */
    public UmlStereotype(String name) {
        this(name, null, null, List.of());
    }

    /**
     * Creates a stereotype with spot but no values.
     * @param name the stereotype name
     * @param spotChar the spot character
     * @param spotColor the spot color
     */
    public UmlStereotype(String name, Character spotChar, String spotColor) {
        this(name, spotChar, spotColor, List.of());
    }

    /**
     * Creates a stereotype with values but no spot.
     * @param name the stereotype name
     * @param values the annotation values
     */
    public UmlStereotype(String name, List<String> values) {
        this(name, null, null, values);
    }

    /**
     * Checks if this stereotype has annotation values.
     * @return true if values are present
     */
    public boolean hasValues() {
        return !values.isEmpty();
    }
}
