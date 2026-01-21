package no.ntnu.eitri.model;

import java.util.Objects;

/**
 * Represents a UML relationship between two types.
 * 
 * Supports three relationship types:
 * - EXTENDS: class inheritance (solid line with hollow arrowhead)
 * - IMPLEMENTS: interface implementation (dashed line with hollow arrowhead)
 * - ASSOCIATION: field reference / "has-a" relationship (solid line with arrow)
 */
public class UmlRelation {

    /**
     * UML relationship types with their PlantUML arrow notations:
     * - EXTENDS: --|> (inheritance)
     * - IMPLEMENTS: ..|> (realization)
     * - ASSOCIATION: --> (dependency/association)
     */
    public enum Type {
        EXTENDS, IMPLEMENTS, ASSOCIATION
    }

    private final String from;
    private final String to;
    private final Type type;

    public UmlRelation(String from, String to, Type type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Type getType() {
        return type;
    }

    public String toPlantUmlRelation() {
        String f = cleanTypeName(getSimpleName(from));
        String t = cleanTypeName(getSimpleName(to));
        return switch (type) {
            case EXTENDS -> f + " --|> " + t;
            case IMPLEMENTS -> f + " ..|> " + t;
            case ASSOCIATION -> f + " --> " + t;
        };
    }

    private String getSimpleName(String fullName) {
        int idx = fullName.lastIndexOf('.');
        return (idx >= 0) ? fullName.substring(idx + 1) : fullName;
    }

    /**
     * Removes generic parameters and array brackets from a type name.
     * 
     * PlantUML doesn't handle generics well in relationship arrows,
     * so we strip them: List<String> -> List, Map<K,V> -> Map, byte[] -> byte
     * 
     * @param typeName possibly generic or array type name
     * @return cleaned simple type name
     */
    private String cleanTypeName(String typeName) {
        // Remove everything after '<' to strip generics
        int idx = typeName.indexOf('<');
        String cleaned = (idx >= 0) ? typeName.substring(0, idx) : typeName;
        // Remove array brackets
        cleaned = cleaned.replace("[]", "");
        // Remove any stray closing brackets from nested generics
        cleaned = cleaned.replace(">", "");
        return cleaned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UmlRelation that = (UmlRelation) o;
        return Objects.equals(from, that.from) &&
                Objects.equals(to, that.to) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, type);
    }
}
