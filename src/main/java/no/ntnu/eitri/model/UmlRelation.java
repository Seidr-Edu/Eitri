package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A relationship between two UML types.
 */
public final class UmlRelation {
    private final String fromTypeId;
    private final String toTypeId;
    private final RelationKind kind;
    private final String label;
    private final String fromMultiplicity;
    private final String toMultiplicity;
    private final String fromMember;  // For member-to-member relations: A::field
    private final String toMember;    // For member-to-member relations: B::field

    private UmlRelation(Builder builder) {
        this.fromTypeId = Objects.requireNonNull(builder.fromTypeId, "fromTypeId cannot be null");
        this.toTypeId = Objects.requireNonNull(builder.toTypeId, "toTypeId cannot be null");
        this.kind = Objects.requireNonNull(builder.kind, "RelationKind cannot be null");
        this.label = builder.label;
        this.fromMultiplicity = builder.fromMultiplicity;
        this.toMultiplicity = builder.toMultiplicity;
        this.fromMember = builder.fromMember;
        this.toMember = builder.toMember;
    }

    public String getFromTypeId() {
        return fromTypeId;
    }

    public String getToTypeId() {
        return toTypeId;
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
     * Renders this relation for PlantUML.
     * <p>
     * For hierarchy relations (EXTENDS, IMPLEMENTS), renders as:
     *   parentType arrowSymbol childType : label
     * <p>
     * For other relations, renders as:
     *   fromType "fromMult" arrowSymbol "toMult" toType : label
     * <p>
     * For member-to-member relations:
     *   FromType::member --> ToType::member : label
     *
     * @param fromTypeName the simple name/alias of the from type
     * @param toTypeName the simple name/alias of the to type
     * @return the PlantUML relation line
     */
    public String toPlantUml(String fromTypeName, String toTypeName) {
        StringBuilder sb = new StringBuilder();

        // Build left side
        String leftSide;
        String rightSide;

        if (kind.isHierarchy()) {
            // For extends/implements: parent <|-- child
            // toType is parent, fromType is child
            leftSide = toTypeName;
            rightSide = fromTypeName;
        } else {
            leftSide = fromTypeName;
            rightSide = toTypeName;
        }

        // Handle member-to-member relations
        if (isMemberRelation()) {
            sb.append(fromTypeName).append("::").append(fromMember);
            sb.append(" ").append(kind.toArrowSymbol()).append(" ");
            sb.append(toTypeName).append("::").append(toMember);
        } else {
            // Left side with multiplicity
            sb.append(leftSide);
            if (!kind.isHierarchy() && fromMultiplicity != null) {
                sb.append(" \"").append(fromMultiplicity).append("\"");
            }

            // Arrow
            sb.append(" ").append(kind.toArrowSymbol()).append(" ");

            // Right side with multiplicity
            if (!kind.isHierarchy() && toMultiplicity != null) {
                sb.append("\"").append(toMultiplicity).append("\" ");
            }
            sb.append(rightSide);
        }

        // Label
        if (label != null && !label.isBlank()) {
            sb.append(" : ").append(label);
        }

        return sb.toString();
    }

    /**
     * Creates a unique key for deduplication.
     * For hierarchy relations, order matters (from extends to).
     * For other relations, we normalize to prevent A--B and B--A duplicates.
     */
    public String getDeduplicationKey() {
        if (kind.isHierarchy()) {
            return fromTypeId + "|" + kind + "|" + toTypeId;
        }
        // Normalize order for non-hierarchy relations
        String first = fromTypeId.compareTo(toTypeId) <= 0 ? fromTypeId : toTypeId;
        String second = fromTypeId.compareTo(toTypeId) <= 0 ? toTypeId : fromTypeId;
        return first + "|" + kind + "|" + second;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience factory for extends relation.
     */
    public static UmlRelation extendsRelation(String childId, String parentId) {
        return builder().fromTypeId(childId).toTypeId(parentId).kind(RelationKind.EXTENDS).build();
    }

    /**
     * Convenience factory for implements relation.
     */
    public static UmlRelation implementsRelation(String implementorId, String interfaceId) {
        return builder().fromTypeId(implementorId).toTypeId(interfaceId).kind(RelationKind.IMPLEMENTS).build();
    }

    /**
     * Convenience factory for dependency relation.
     */
    public static UmlRelation dependency(String fromId, String toId, String label) {
        return builder().fromTypeId(fromId).toTypeId(toId).kind(RelationKind.DEPENDENCY).label(label).build();
    }

    /**
     * Convenience factory for association relation.
     */
    public static UmlRelation association(String fromId, String toId, String label) {
        return builder().fromTypeId(fromId).toTypeId(toId).kind(RelationKind.ASSOCIATION).label(label).build();
    }

    public static final class Builder {
        private String fromTypeId;
        private String toTypeId;
        private RelationKind kind;
        private String label;
        private String fromMultiplicity;
        private String toMultiplicity;
        private String fromMember;
        private String toMember;

        private Builder() {}

        public Builder fromTypeId(String fromTypeId) {
            this.fromTypeId = fromTypeId;
            return this;
        }

        public Builder toTypeId(String toTypeId) {
            this.toTypeId = toTypeId;
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
        return fromTypeId.equals(that.fromTypeId) &&
                toTypeId.equals(that.toTypeId) &&
                kind == that.kind &&
                Objects.equals(fromMember, that.fromMember) &&
                Objects.equals(toMember, that.toMember);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromTypeId, toTypeId, kind, fromMember, toMember);
    }

    @Override
    public String toString() {
        return "UmlRelation{" + fromTypeId + " " + kind + " " + toTypeId + "}";
    }
}
