package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A relationship between two UML types.
 */
public final class UmlRelation {
    private final String fromTypeFqn;
    private final String toTypeFqn;
    private final RelationKind kind;
    private final String label;
    private final String fromMultiplicity;
    private final String toMultiplicity;
    private final String fromMember;  // For member-to-member relations: A::field
    private final String toMember;    // For member-to-member relations: B::field

    private UmlRelation(Builder builder) {
        this.fromTypeFqn = Objects.requireNonNull(builder.fromTypeFqn, "fromTypeFqn cannot be null");
        this.toTypeFqn = Objects.requireNonNull(builder.toTypeFqn, "toTypeFqn cannot be null");
        this.kind = Objects.requireNonNull(builder.kind, "RelationKind cannot be null");
        this.label = builder.label;
        this.fromMultiplicity = builder.fromMultiplicity;
        this.toMultiplicity = builder.toMultiplicity;
        this.fromMember = builder.fromMember;
        this.toMember = builder.toMember;
    }

    public String getFromTypeFqn() {
        return fromTypeFqn;
    }

    public String getToTypeFqn() {
        return toTypeFqn;
    }

    public RelationKind getKind() {
        return kind;
    }

    public String getLabel() {
        return label;
    }

    public String getFromMultiplicity() {
        return fromMultiplicity;
    }

    public String getToMultiplicity() {
        return toMultiplicity;
    }

    public String getFromMember() {
        return fromMember;
    }

    public String getToMember() {
        return toMember;
    }

    /**
     * Checks if this is a member-to-member relation.
     * @return true if both fromMember and toMember are set
     */
    public boolean isMemberRelation() {
        return fromMember != null && toMember != null;
    }

    /**
     * Creates a unique key for deduplication.
     * For hierarchy relations, order matters (from extends to).
     * For other relations, we normalize to prevent A--B and B--A duplicates.
     */
    public String getDeduplicationKey() {
        if (kind.isHierarchy()) {
            return fromTypeFqn + "|" + kind + "|" + toTypeFqn;
        }
        // Normalize order for non-hierarchy relations
        String first = fromTypeFqn.compareTo(toTypeFqn) <= 0 ? fromTypeFqn : toTypeFqn;
        String second = fromTypeFqn.compareTo(toTypeFqn) <= 0 ? toTypeFqn : fromTypeFqn;
        return first + "|" + kind + "|" + second;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience factory for extends relation.
     */
    public static UmlRelation extendsRelation(String childFqn, String parentFqn) {
        return builder().fromTypeFqn(childFqn).toTypeFqn(parentFqn).kind(RelationKind.EXTENDS).build();
    }

    /**
     * Convenience factory for implements relation.
     */
    public static UmlRelation implementsRelation(String implementorFqn, String interfaceFqn) {
        return builder().fromTypeFqn(implementorFqn).toTypeFqn(interfaceFqn).kind(RelationKind.IMPLEMENTS).build();
    }

    /**
     * Convenience factory for dependency relation.
     */
    public static UmlRelation dependency(String fromFqn, String toFqn, String label) {
        return builder().fromTypeFqn(fromFqn).toTypeFqn(toFqn).kind(RelationKind.DEPENDENCY).label(label).build();
    }

    /**
     * Convenience factory for association relation.
     */
    public static UmlRelation association(String fromFqn, String toFqn, String label) {
        return builder().fromTypeFqn(fromFqn).toTypeFqn(toFqn).kind(RelationKind.ASSOCIATION).label(label).build();
    }

    /**
     * Convenience factory for nested type relation.
     * Creates a relation from outer type to nested type with "nested" label.
     */
    public static UmlRelation nestedRelation(String outerFqn, String nestedFqn) {
        return builder().fromTypeFqn(outerFqn).toTypeFqn(nestedFqn).kind(RelationKind.NESTED).label("nested").build();
    }

    public static final class Builder {
        private String fromTypeFqn;
        private String toTypeFqn;
        private RelationKind kind;
        private String label;
        private String fromMultiplicity;
        private String toMultiplicity;
        private String fromMember;
        private String toMember;

        private Builder() {}

        public Builder fromTypeFqn(String fromTypeFqn) {
            this.fromTypeFqn = fromTypeFqn;
            return this;
        }

        public Builder toTypeFqn(String toTypeFqn) {
            this.toTypeFqn = toTypeFqn;
            return this;
        }

        public Builder kind(RelationKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder fromMultiplicity(String fromMultiplicity) {
            this.fromMultiplicity = fromMultiplicity;
            return this;
        }

        public Builder toMultiplicity(String toMultiplicity) {
            this.toMultiplicity = toMultiplicity;
            return this;
        }

        public Builder fromMember(String fromMember) {
            this.fromMember = fromMember;
            return this;
        }

        public Builder toMember(String toMember) {
            this.toMember = toMember;
            return this;
        }

        public UmlRelation build() {
            return new UmlRelation(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlRelation that)) return false;
        return fromTypeFqn.equals(that.fromTypeFqn) &&
                toTypeFqn.equals(that.toTypeFqn) &&
                kind == that.kind &&
                Objects.equals(fromMember, that.fromMember) &&
                Objects.equals(toMember, that.toMember);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromTypeFqn, toTypeFqn, kind, fromMember, toMember);
    }

    @Override
    public String toString() {
        return "UmlRelation{" + fromTypeFqn + " " + kind + " " + toTypeFqn + "}";
    }
}
