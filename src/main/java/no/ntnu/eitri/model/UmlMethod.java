package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A method or constructor in a UML type.
 */
public final class UmlMethod {
    private final String name;
    private final String returnType;
    private final String returnTypeSimpleName;
    private final List<UmlParameter> parameters;
    private final Visibility visibility;
    private final Set<Modifier> modifiers;
    private final boolean constructor;
    private final List<String> annotations;

    private UmlMethod(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Method name cannot be null");
        this.returnType = builder.returnType != null ? builder.returnType : "void";
        this.returnTypeSimpleName = builder.returnTypeSimpleName != null
                ? builder.returnTypeSimpleName
                : extractSimpleName(this.returnType);
        this.parameters = builder.parameters != null ? List.copyOf(builder.parameters) : List.of();
        this.visibility = builder.visibility != null ? builder.visibility : Visibility.PACKAGE;
        this.modifiers = builder.modifiers != null ? Set.copyOf(builder.modifiers) : Set.of();
        this.constructor = builder.constructor;
        this.annotations = builder.annotations != null ? List.copyOf(builder.annotations) : List.of();
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getReturnTypeSimpleName() {
        return returnTypeSimpleName;
    }

    public List<UmlParameter> getParameters() {
        return parameters;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public boolean isStatic() {
        return modifiers.contains(Modifier.STATIC);
    }

    public boolean isAbstract() {
        return modifiers.contains(Modifier.ABSTRACT);
    }

    /**
     * Returns the method signature string: name(param1: Type1, param2: Type2)
     * For constructors, omits return type.
     * @return the signature string
     */
    public String getSignature() {
        String params = parameters.stream()
                .map(UmlParameter::toPlantUml)
                .collect(Collectors.joining(", "));
        return name + "(" + params + ")";
    }

    /**
     * Renders this method for PlantUML class body.
     * Format: [visibility][{modifiers}] name(params) [: ReturnType]
     * Example: +{abstract} calculate(x: int, y: int) : double
     * Example: +Customer(name: String) -- constructor, no return type shown
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

        // Signature
        sb.append(getSignature());

        // Return type (skip for constructors and void)
        if (!constructor && !"void".equals(returnTypeSimpleName)) {
            sb.append(" : ").append(returnTypeSimpleName);
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
        private String returnType;
        private String returnTypeSimpleName;
        private List<UmlParameter> parameters;
        private Visibility visibility;
        private Set<Modifier> modifiers;
        private boolean constructor;
        private List<String> annotations;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder returnTypeSimpleName(String returnTypeSimpleName) {
            this.returnTypeSimpleName = returnTypeSimpleName;
            return this;
        }

        public Builder parameters(List<UmlParameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder addParameter(UmlParameter parameter) {
            if (this.parameters == null) {
                this.parameters = new ArrayList<>();
            }
            this.parameters.add(parameter);
            return this;
        }

        public Builder addParameter(String name, String type) {
            return addParameter(new UmlParameter(name, type));
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder modifiers(Set<Modifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder constructor(boolean constructor) {
            this.constructor = constructor;
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

        public UmlMethod build() {
            return new UmlMethod(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlMethod that)) return false;
        return name.equals(that.name) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }

    @Override
    public String toString() {
        return toPlantUml();
    }
}
