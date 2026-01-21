package no.ntnu.eitri.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory model of UML types and their relations.
 * 
 * Acts as the central repository for all discovered classes, interfaces,
 * enums, and the relationships between them. This model is populated by
 * the ClassCollectorVisitor and consumed by PlantUmlWriter.
 */
public class UmlModel {

    // LinkedHashMap preserves insertion order for consistent output
    private final Map<String, UmlType> types = new LinkedHashMap<>();
    
    // LinkedHashSet ensures uniqueness while preserving discovery order
    private final Set<UmlRelation> relations = new LinkedHashSet<>();

    /**
     * Retrieves an existing type or creates a new one if not found.
     * 
     * This pattern ensures each fully-qualified type name maps to exactly
     * one UmlType instance, allowing multiple references to accumulate
     * fields, methods, and relationships on the same object.
     * 
     * @param fullName fully-qualified type name (e.g., "com.example.MyClass")
     * @param kind whether this is a CLASS, INTERFACE, or ENUM
     * @return the existing or newly created UmlType
     */
    public UmlType getOrCreateType(String fullName, UmlType.Kind kind) {
        UmlType existing = types.get(fullName);
        if (existing != null) {
            return existing;
        }
        UmlType t = new UmlType(fullName, kind);
        types.put(fullName, t);
        return t;
    }

    public UmlType getType(String fullName) {
        return types.get(fullName);
    }

    public void addRelation(UmlRelation relation) {
        relations.add(relation);
    }

    public Collection<UmlType> getTypes() {
        return types.values();
    }

    public Set<UmlRelation> getRelations() {
        return relations;
    }
}
