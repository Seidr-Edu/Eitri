package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    private final List<String> thrownExceptions;

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
        this.thrownExceptions = builder.thrownExceptions != null ? List.copyOf(builder.thrownExceptions) : List.of();
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

    public List<String> getThrownExceptions() {
        return thrownExceptions;
    }

    public boolean isStatic() {
        return modifiers.contains(Modifier.STATIC);
    }

    public boolean isAbstract() {
        return modifiers.contains(Modifier.ABSTRACT);
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
                
                // Simplify generic arguments
                String simpleGeneric = simplifyGenericArguments(genericPart);
                
                return simpleBase + "<" + simpleGeneric + ">";
            }
        }
        
        return simplifyTypeName(fullType);
    }
    
    private static String simplifyTypeName(String typeName) {
        if (typeName == null) return typeName;
        typeName = typeName.trim();
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }
    
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
                result.append(extractSimpleName(genericPart.substring(start, i).trim()));
                result.append(", ");
                start = i + 1;
            }
        }
        
        result.append(extractSimpleName(genericPart.substring(start).trim()));
        
        return result.toString();
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
        private List<String> thrownExceptions;

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
        
        @SuppressWarnings("null")
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

        public Builder isAbstract(boolean isAbstract) {
            if (isAbstract) {
                return addModifier(Modifier.ABSTRACT);
            }
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

        public Builder thrownExceptions(List<String> thrownExceptions) {
            this.thrownExceptions = thrownExceptions;
            return this;
        }

        public Builder addThrownException(String exception) {
            if (this.thrownExceptions == null) {
                this.thrownExceptions = new ArrayList<>();
            }
            this.thrownExceptions.add(exception);
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
        return "UmlMethod{" +
                "name='" + name + '\'' +
                ", returnType='" + returnType + '\'' +
                '}';
    }
}
