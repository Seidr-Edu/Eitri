package no.ntnu.eitri.model;

/**
 * Relationship kinds between UML types.
 * Each kind maps to a specific PlantUML arrow symbol.
 */
public enum RelationKind {
    /**
     * Inheritance: A extends B.
     * PlantUML: B &lt;|-- A (B is parent, A is child)
     */
    EXTENDS("<|--"),

    /**
     * Implementation: A implements B.
     * PlantUML: B &lt;|.. A (B is interface, A is implementor)
     */
    IMPLEMENTS("<|.."),

    /**
     * Composition: A is composed of B (strong ownership, lifecycle bound).
     * PlantUML: A *-- B
     */
    COMPOSITION("*--"),

    /**
     * Aggregation: A aggregates B (weak ownership).
     * PlantUML: A o-- B
     */
    AGGREGATION("o--"),

    /**
     * Association: A is associated with B (field reference).
     * PlantUML: A -- B
     */
    ASSOCIATION("--"),

    /**
     * Dependency: A depends on B (method param, return type, local usage).
     * PlantUML: A ..> B
     */
    DEPENDENCY("..>"),

    /**
     * Nesting: A contains nested type B (inner class, static nested class, etc.).
     * PlantUML: A +-- B : nested
     */
    NESTED("+--");

    private final String arrowSymbol;

    RelationKind(String arrowSymbol) {
        this.arrowSymbol = arrowSymbol;
    }

    /**
     * Returns the PlantUML arrow symbol for this relation.
     * Note: For EXTENDS and IMPLEMENTS, the arrow points from child to parent,
     * so render as: parentType arrowSymbol childType
     * @return the PlantUML arrow symbol
     */
    public String toArrowSymbol() {
        return arrowSymbol;
    }

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
