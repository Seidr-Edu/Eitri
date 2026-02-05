package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A UML type element (class, interface, enum, annotation, record).
 */
public final class UmlType {
    private final String id;              // Unique identifier (fully qualified name)
    private final String name;            // Simple name
    private final String displayName;     // Optional display name (for aliases)
    private final String packageName;     // Package name
    private final TypeKind kind;
    private final Visibility visibility;
    private final List<UmlStereotype> stereotypes;
    private final List<String> tags;      // PlantUML $tags for hide/show
    private final String style;           // Optional inline style (e.g., "#back:palegreen")
    private final List<UmlGeneric> generics;
    private final List<UmlField> fields;
    private final List<UmlMethod> methods;
    private final String outerTypeId;  // FQN of enclosing type for nested types

    private UmlType(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Type name cannot be null");
        this.packageName = builder.packageName != null ? builder.packageName : "";
        this.id = builder.id != null ? builder.id : computeId(this.packageName, this.name);
        this.displayName = builder.displayName;
        this.kind = builder.kind != null ? builder.kind : TypeKind.CLASS;
        this.visibility = builder.visibility != null ? builder.visibility : Visibility.PACKAGE;
        this.stereotypes = builder.stereotypes != null ? List.copyOf(builder.stereotypes) : List.of();
        this.tags = builder.tags != null ? List.copyOf(builder.tags) : List.of();
        this.style = builder.style;
        this.generics = builder.generics != null ? List.copyOf(builder.generics) : List.of();
        this.fields = builder.fields != null ? List.copyOf(builder.fields) : List.of();
        this.methods = builder.methods != null ? List.copyOf(builder.methods) : List.of();
        this.outerTypeId = builder.outerTypeId;
    }

    private static String computeId(String packageName, String name) {
        if (packageName == null || packageName.isBlank()) {
            return name;
        }
        return packageName + "." + name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
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
     * @return the outer type ID, or null
     */
    public String getOuterTypeId() {
        return outerTypeId;
    }

    /**
     * Checks if this is a nested type (inner class, static nested class, etc.).
     * @return true if this type is nested within another type
     */
    public boolean isNested() {
        return outerTypeId != null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String displayName;
        private String packageName;
        private TypeKind kind;
        private Visibility visibility;
        private List<UmlStereotype> stereotypes;
        private List<String> tags;
        private String style;
        private List<UmlGeneric> generics;
        private List<UmlField> fields;
        private List<UmlMethod> methods;
        private String outerTypeId;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
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

        public Builder stereotypes(List<UmlStereotype> stereotypes) {
            this.stereotypes = stereotypes;
            return this;
        }

        public Builder addStereotype(UmlStereotype stereotype) {
            if (this.stereotypes == null) {
                this.stereotypes = new ArrayList<>();
            }
            this.stereotypes.add(stereotype);
            return this;
        }

        public Builder addStereotype(String name) {
            return addStereotype(new UmlStereotype(name));
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
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

        public Builder generics(List<UmlGeneric> generics) {
            this.generics = generics;
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

        public Builder fields(List<UmlField> fields) {
            this.fields = fields;
            return this;
        }

        public Builder addField(UmlField field) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            this.fields.add(field);
            return this;
        }

        public Builder methods(List<UmlMethod> methods) {
            this.methods = methods;
            return this;
        }

        public Builder addMethod(UmlMethod method) {
            if (this.methods == null) {
                this.methods = new ArrayList<>();
            }
            this.methods.add(method);
            return this;
        }

        public Builder outerTypeId(String outerTypeId) {
            this.outerTypeId = outerTypeId;
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
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UmlType{" + id + "}";
    }
}
