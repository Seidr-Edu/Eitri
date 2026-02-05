package no.ntnu.eitri.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Modifiers that can apply to UML members (fields, methods) or types.
 */
public enum Modifier {
    STATIC,
    ABSTRACT,
    FINAL,
    SYNCHRONIZED,
    NATIVE,
    DEFAULT,
    VOLATILE,
    TRANSIENT;

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
