package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A stereotype annotation on a UML type.
 * Example: &lt;&lt;Singleton&gt;&gt; or &lt;&lt;(S,#FF0000) Service&gt;&gt;
 */
public record UmlStereotype(
        String name,
        Character spotChar,
        String spotColor
) {
    /**
     * Creates a stereotype with optional spot (icon letter and color).
     * @param name the stereotype name (e.g., "Singleton")
     * @param spotChar optional spot character (e.g., 'S')
     * @param spotColor optional spot color (e.g., "#FF0000")
     */
    public UmlStereotype {
        Objects.requireNonNull(name, "Stereotype name cannot be null");
    }

    /**
     * Creates a simple stereotype without spot.
     * @param name the stereotype name
     */
    public UmlStereotype(String name) {
        this(name, null, null);
    }

    /**
     * Renders this stereotype for PlantUML.
     * Example: &lt;&lt;Singleton&gt;&gt; or &lt;&lt;(S,#FF0000) Service&gt;&gt;
     */
    public String toPlantUml() {
        if (spotChar != null && spotColor != null) {
            return "<< (" + spotChar + "," + spotColor + ") " + name + " >>";
        } else if (spotChar != null) {
            return "<< (" + spotChar + ") " + name + " >>";
        } else {
            return "<<" + name + ">>";
        }
    }
}
