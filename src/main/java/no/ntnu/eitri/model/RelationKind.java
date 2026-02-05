package no.ntnu.eitri.model;

/**
 * Relationship kinds between UML types.
 */
public enum RelationKind {
    /**
     * Inheritance: A extends B.
     */
    EXTENDS,

    /**
     * Implementation: A implements B.
     */
    IMPLEMENTS,

    /**
     * Composition: A is composed of B (strong ownership, lifecycle bound).
     */
    COMPOSITION,

    /**
     * Aggregation: A aggregates B (weak ownership).
     */
    AGGREGATION,

    /**
     * Association: A is associated with B (field reference).
     */
    ASSOCIATION,

    /**
     * Dependency: A depends on B (method param, return type, local usage).
     */
    DEPENDENCY,

    /**
     * Nesting: A contains nested type B (inner class, static nested class, etc.).
     */
    NESTED;

    /**
     * Whether this relation uses a dotted line.
     * @return true if dotted (IMPLEMENTS, DEPENDENCY)
     */
    public boolean isDotted() {
        return this == IMPLEMENTS || this == DEPENDENCY;
    }

    /**
     * Whether this is a hierarchy relation (extends/implements).
     * For hierarchy relations, the "from" type is the child and "to" is the parent.
     * @return true if hierarchy relation
     */
    public boolean isHierarchy() {
        return this == EXTENDS || this == IMPLEMENTS;
    }

    /**
     * Whether this is a nesting relation.
     * @return true if nesting relation
     */
    public boolean isNesting() {
        return this == NESTED;
    }
}
