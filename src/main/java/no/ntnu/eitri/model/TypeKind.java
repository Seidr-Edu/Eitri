package no.ntnu.eitri.model;

/**
 * The kind/category of a UML type element.
 */
public enum TypeKind {
    CLASS,
    ABSTRACT_CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    RECORD;

    /**
     * Whether this kind represents an abstract type (abstract class or interface).
     * @return true if abstract
     */
    public boolean isAbstract() {
        return this == ABSTRACT_CLASS || this == INTERFACE;
    }
}
