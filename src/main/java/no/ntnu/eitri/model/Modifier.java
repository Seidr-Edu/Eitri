package no.ntnu.eitri.model;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modifiers that can apply to UML members (fields, methods) or types.
 * Maps to PlantUML {static}, {abstract}, etc.
 */
public enum Modifier {
    STATIC("{static}"),
    ABSTRACT("{abstract}"),
    FINAL(""),  // Final doesn't have a PlantUML keyword; can be shown via readOnly or stereotype
    SYNCHRONIZED(""),
    NATIVE(""),
    DEFAULT(""),  // For default interface methods
    VOLATILE(""),
    TRANSIENT("");

    private final String plantUmlKeyword;

    Modifier(String plantUmlKeyword) {
        this.plantUmlKeyword = plantUmlKeyword;
    }

    /**
     * Returns the PlantUML keyword for this modifier.
     * @return the PlantUML keyword (may be empty for modifiers without representation)
     */
    public String toPlantUml() {
        return plantUmlKeyword;
    }

    /**
     * Converts a set of modifiers to their PlantUML representation.
     * Only includes modifiers that have a non-empty PlantUML keyword.
     * @param modifiers the set of modifiers
     * @return space-separated PlantUML keywords
     */
    public static String toPlantUml(Set<Modifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "";
        }
        return modifiers.stream()
                .map(Modifier::toPlantUml)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    /**
     * Creates an empty mutable set of modifiers.
     * @return empty EnumSet
     */
    @SuppressWarnings("null")
    public static Set<Modifier> none() {
        return EnumSet.noneOf(Modifier.class);
    }

    /**
     * Creates a set containing the given modifiers.
     * @param modifiers the modifiers to include
     * @return Set with the given modifiers
     */
    public static Set<Modifier> of(Modifier... modifiers) {
        if (modifiers.length == 0) {
            return none();
        }
        return EnumSet.of(modifiers[0], modifiers);
    }
}
