package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * A method or constructor parameter in the UML model.
 * Immutable value type.
 */
public record UmlParameter(
        String name,
        String type,
        String typeSimpleName
) {
    /**
     * Creates a parameter with full type information.
     * @param name parameter name
     * @param type fully qualified type (e.g., "java.util.List<String>")
     * @param typeSimpleName simple type name (e.g., "List<String>")
     */
    public UmlParameter {
        Objects.requireNonNull(name, "Parameter name cannot be null");
        Objects.requireNonNull(type, "Parameter type cannot be null");
        if (typeSimpleName == null || typeSimpleName.isBlank()) {
            typeSimpleName = extractSimpleName(type);
        }
    }

    /**
     * Creates a parameter with just name and type (simple name derived).
     * @param name parameter name
     * @param type fully qualified type
     */
    public UmlParameter(String name, String type) {
        this(name, type, null);
    }

    /**
     * Renders this parameter for PlantUML: "name: Type" or just "name" if type is void/empty.
     * @return PlantUML parameter representation
     */
    public String toPlantUml() {
        if (typeSimpleName == null || typeSimpleName.isBlank() || "void".equals(typeSimpleName)) {
            return name;
        }
        return name + ": " + typeSimpleName;
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
}
