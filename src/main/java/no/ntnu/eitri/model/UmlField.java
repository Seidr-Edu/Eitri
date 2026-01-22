package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.EnumSet;
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
        return toPlantUml(true);
    }

    /**
     * Renders this field for PlantUML class body with configurable readOnly display.
     * @param showReadOnly whether to append {readOnly} for final fields
     * @return PlantUML field representation
     */
    public String toPlantUml(boolean showReadOnly) {
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
        if (readOnly && showReadOnly) {
            sb.append(" {readOnly}");
        }

        return sb.toString();
    }

    private static String extractSimpleName(String fullType) {
        if (fullType == null || fullType.isBlank()) {
            return fullType;
        }
        
        // Handle generic types recursively
        int genericStart = fullType.indexOf('<');
        if (genericStart > 0) {
            int genericEnd = fullType.lastIndexOf('>');
            if (genericEnd > genericStart) {
                String basePart = fullType.substring(0, genericStart);
                String genericPart = fullType.substring(genericStart + 1, genericEnd);
                
                // Simplify base type
                String simpleBase = simplifyTypeName(basePart);
                
                // Simplify generic arguments (handle nested generics and multiple args)
                String simpleGeneric = simplifyGenericArguments(genericPart);
                
                return simpleBase + "<" + simpleGeneric + ">";
            }
        }
        
        return simplifyTypeName(fullType);
    }
    
    /**
     * Simplifies a single type name by extracting just the simple class name.
     */
    private static String simplifyTypeName(String typeName) {
        if (typeName == null) return typeName;
        typeName = typeName.trim();
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }
    
    /**
     * Simplifies generic arguments, handling nested generics and multiple arguments.
     */
    private static String simplifyGenericArguments(String genericPart) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        int start = 0;
        
        for (int i = 0; i < genericPart.length(); i++) {
            char c = genericPart.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                // Found argument separator at top level
                result.append(extractSimpleName(genericPart.substring(start, i).trim()));
                result.append(", ");
                start = i + 1;
            }
        }
        
        // Add last argument
        result.append(extractSimpleName(genericPart.substring(start).trim()));
        
        return result.toString();
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

        public Builder addModifier(Modifier modifier) {
            if (this.modifiers == null) {
                this.modifiers = EnumSet.noneOf(Modifier.class);
            } else if (!(this.modifiers instanceof EnumSet)) {
                this.modifiers = EnumSet.copyOf(this.modifiers);
            }
            this.modifiers.add(modifier);
            return this;
        }

        public Builder isStatic(boolean isStatic) {
            if (isStatic) {
                return addModifier(Modifier.STATIC);
            }
            return this;
        }

        public Builder isFinal(boolean isFinal) {
            if (isFinal) {
                return addModifier(Modifier.FINAL);
            }
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
