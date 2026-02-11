package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A UML type element (class, interface, enum, annotation, record).
 */
public final class UmlType {
    private final String fqn;             // Unique identifier (fully qualified name)
    private final String simpleName;      // Simple name
    private final String alias;           // Optional alias if name clashes occur
    private final String packageName;     // Computed package name
    private final TypeKind kind;
    private final Visibility visibility;
    private final List<UmlStereotype> stereotypes;
    private final List<String> tags;      // PlantUML $tags for hide/show
    private final String style;           // Optional inline style (e.g., "#back:palegreen")
    private final List<UmlGeneric> generics;
    private final List<UmlField> fields;
    private final List<UmlMethod> methods;
    private final String outerTypeFqn;  // FQN of enclosing type for nested types
    private final String outerTypeSimpleName; // Simple name of enclosing type

    private UmlType(Builder builder) {
        this.fqn = Objects.requireNonNull(builder.fqn, "Type fqn cannot be null");
        this.simpleName = Objects.requireNonNull(builder.simpleName, "Type simpleName cannot be null");
        this.alias = builder.alias;
        this.packageName = computePackageName(this.fqn);
        this.kind = builder.kind != null ? builder.kind : TypeKind.CLASS;
        this.visibility = builder.visibility != null ? builder.visibility : Visibility.PACKAGE;
        this.stereotypes = builder.stereotypes != null ? List.copyOf(builder.stereotypes) : List.of();
        this.tags = builder.tags != null ? List.copyOf(builder.tags) : List.of();
        this.style = builder.style;
        this.generics = builder.generics != null ? List.copyOf(builder.generics) : List.of();
        this.fields = builder.fields != null ? List.copyOf(builder.fields) : List.of();
        this.methods = builder.methods != null ? List.copyOf(builder.methods) : List.of();
        this.outerTypeFqn = builder.outerTypeFqn;
        this.outerTypeSimpleName = computeOuterTypeSimpleName();
    }

    private String computeOuterTypeSimpleName() {
        if (outerTypeFqn == null) {
            return null;
        }
        int lastDot = outerTypeFqn.lastIndexOf('.');
        return lastDot == -1 ? outerTypeFqn : outerTypeFqn.substring(lastDot + 1);
    }

    public String computePackageName(String fqn) {
        // Parse FQN to find where package ends and type hierarchy begins
        // Package components are lowercase, type names start with uppercase
        String[] parts = fqn.split("\\.");
        StringBuilder packageName = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            // Once we hit an uppercase part, that's the start of the type hierarchy
            if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
                // Return everything before this point (or empty string if no package)
                return packageName.length() > 0 ? packageName.toString() : "";
            }
            if (i > 0) {
                packageName.append(".");
            }
            packageName.append(part);
        }

        // All parts were lowercase (shouldn't happen for valid Java)
        return packageName.length() > 0 ? packageName.toString() : "";
    }

    public String getFqn() {
        return fqn;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getAlias() {
        return alias;
    }

    public String getPackageName() {
        return packageName;
    }


    public TypeKind getKind() {
        return kind;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public List<UmlStereotype> getStereotypes() {
        return stereotypes;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getStyle() {
        return style;
    }

    public List<UmlGeneric> getGenerics() {
        return generics;
    }

    public List<UmlField> getFields() {
        return fields;
    }

    public List<UmlMethod> getMethods() {
        return methods;
    }

    /**
     * Returns the FQN of the enclosing type, or null if this is a top-level type.
     * @return the outer type FQN, or null
     */
    public String getOuterTypeFqn() {
        return outerTypeFqn;
    }

    /**
     * Returns the simple name of the enclosing type, or null if this is a top-level type.
     * @return the outer type simple name, or null
     */
    public String getOuterTypeSimpleName() {
        return outerTypeSimpleName;
    }

    /**
     * Checks if this is a nested type (inner class, static nested class, etc.).
     * @return true if this type is nested within another type
     */
    public boolean isNested() {
        return outerTypeFqn != null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String fqn;
        private String simpleName;
        private String alias;
        private TypeKind kind;
        private Visibility visibility;
        private List<UmlStereotype> stereotypes;
        private List<String> tags;
        private String style;
        private List<UmlGeneric> generics;
        private List<UmlField> fields;
        private List<UmlMethod> methods;
        private String outerTypeFqn;
        private Builder() {}

        public Builder fqn(String fqn) {
            this.fqn = fqn;
            return this;
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder kind(TypeKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder addStereotype(UmlStereotype stereotype) {
            if (this.stereotypes == null) {
                this.stereotypes = new ArrayList<>();
            }
            for (UmlStereotype existing : this.stereotypes) {
                if (existing.name().equals(stereotype.name())) {
                    return this;
                }
            }
            this.stereotypes.add(stereotype);
            return this;
        }

        public Builder addStereotype(String name) {
            return addStereotype(new UmlStereotype(name));
        }

        public Builder addTag(String tag) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            this.tags.add(tag);
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public Builder addGeneric(UmlGeneric generic) {
            if (this.generics == null) {
                this.generics = new ArrayList<>();
            }
            this.generics.add(generic);
            return this;
        }

        public Builder addGeneric(String identifier) {
            return addGeneric(new UmlGeneric(identifier));
        }

        public Builder addField(UmlField field) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            this.fields.add(field);
            return this;
        }

        public Builder addMethod(UmlMethod method) {
            if (this.methods == null) {
                this.methods = new ArrayList<>();
            }
            this.methods.add(method);
            return this;
        }

        public Builder outerTypeFqn(String outerTypeFqn) {
            this.outerTypeFqn = outerTypeFqn;
            return this;
        }

        public UmlType build() {
            return new UmlType(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlType that)) return false;
        return fqn.equals(that.fqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fqn);
    }

    @Override
    public String toString() {
        return "UmlType{" + fqn + "}";
    }
}
