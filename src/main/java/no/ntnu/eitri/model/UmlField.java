package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A field in a UML type.
 */
public final class UmlField {
    private final String name;
    private final String type;
    private final String typeSimpleName;
    private final Visibility visibility;
    private final Set<Modifier> modifiers;
    private final boolean readOnly;
    private final List<String> annotations;

    private UmlField(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Field name cannot be null");
        this.type = Objects.requireNonNull(builder.type, "Field type cannot be null");
        this.typeSimpleName = builder.typeSimpleName != null ? builder.typeSimpleName : extractSimpleName(builder.type);
        this.visibility = builder.visibility != null ? builder.visibility : Visibility.PACKAGE;
        this.modifiers = builder.modifiers != null ? Set.copyOf(builder.modifiers) : Set.of();
        this.readOnly = builder.readOnly;
        this.annotations = builder.annotations != null ? List.copyOf(builder.annotations) : List.of();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTypeSimpleName() {
        return typeSimpleName;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public boolean isStatic() {
        return modifiers.contains(Modifier.STATIC);
    }

    public boolean isFinal() {
        return modifiers.contains(Modifier.FINAL);
    }

    /**
     * Renders this field for PlantUML class body.
     * Format: [visibility][{modifiers}] name : Type [{readOnly}]
     * Example: +{static} count : int
     * Example: -name : String {readOnly}
     */
    public String toPlantUml() {
        StringBuilder sb = new StringBuilder();

        // Visibility
        sb.append(visibility.toPlantUml());

        // Modifiers
        String modStr = Modifier.toPlantUml(modifiers);
        if (!modStr.isEmpty()) {
            sb.append(modStr).append(" ");
        }

        // Name and type
        sb.append(name).append(" : ").append(typeSimpleName);

        // ReadOnly constraint
        if (readOnly) {
            sb.append(" {readOnly}");
        }

        return sb.toString();
    }

    private static String extractSimpleName(String fullType) {
        if (fullType == null || fullType.isBlank()) {
            return fullType;
        }
        int genericStart = fullType.indexOf('<');
        String basePart = genericStart > 0 ? fullType.substring(0, genericStart) : fullType;
        int lastDot = basePart.lastIndexOf('.');
        if (lastDot < 0) {
            return fullType;
        }
        return fullType.substring(lastDot + 1);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String type;
        private String typeSimpleName;
        private Visibility visibility;
        private Set<Modifier> modifiers;
        private boolean readOnly;
        private List<String> annotations;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder typeSimpleName(String typeSimpleName) {
            this.typeSimpleName = typeSimpleName;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder modifiers(Set<Modifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder annotations(List<String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder addAnnotation(String annotation) {
            if (this.annotations == null) {
                this.annotations = new ArrayList<>();
            }
            this.annotations.add(annotation);
            return this;
        }

        public UmlField build() {
            return new UmlField(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlField that)) return false;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return toPlantUml();
    }
}
