package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Root container for the entire UML model.
 * Contains all types and relations for a diagram.
 */
public final class UmlModel {
    private final String name;
    private final Map<String, UmlType> types;  // Keyed by type ID (fully qualified name)
    private final List<UmlRelation> relations;
    private final List<UmlNote> notes;

    private UmlModel(Builder builder) {
        this.name = builder.name != null ? builder.name : "diagram";
        this.types = builder.types != null
                ? new LinkedHashMap<>(builder.types)
                : new LinkedHashMap<>();
        this.relations = builder.relations != null
                ? new ArrayList<>(builder.relations)
                : new ArrayList<>();
        this.notes = builder.notes != null
                ? new ArrayList<>(builder.notes)
                : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    /**
     * Returns all types in the model.
     * @return unmodifiable collection of types
     */
    public Collection<UmlType> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    /**
     * Returns types sorted by package then name for deterministic output.
     * @return sorted list of types
     */
    public List<UmlType> getTypesSorted() {
        return types.values().stream()
                .sorted(Comparator.comparing(UmlType::getPackageName)
                        .thenComparing(UmlType::getSimpleName))
                .toList();
    }

    /**
     * Looks up a type by its ID.
     * @param typeId the fully qualified type ID
     * @return the type, or empty if not found
     */
    public Optional<UmlType> getType(String typeId) {
        return Optional.ofNullable(types.get(typeId));
    }

    /**
     * Checks if a type exists in the model.
     * @param typeId the fully qualified type ID
     * @return true if the type exists
     */
    public boolean hasType(String typeId) {
        return types.containsKey(typeId);
    }

    /**
     * Returns all relations in the model.
     * @return unmodifiable list of relations
     */
    public List<UmlRelation> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    /**
     * Returns relations sorted for deterministic output.
     * Hierarchy relations first, then by kind, then by type IDs.
     * @return sorted list of relations
     */
    public List<UmlRelation> getRelationsSorted() {
        return relations.stream()
                .sorted(Comparator
                        .comparing((UmlRelation r) -> !r.getKind().isHierarchy())  // hierarchy first
                        .thenComparing(r -> r.getKind().ordinal())
                        .thenComparing(UmlRelation::getFromTypeId)
                        .thenComparing(UmlRelation::getToTypeId))
                .toList();
    }

    /**
     * Returns all notes in the model.
     * @return unmodifiable list of notes
     */
    public List<UmlNote> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    /**
     * Returns all unique package names in the model, sorted.
     * @return sorted list of package names
     */
    public List<String> getPackages() {
        return types.values().stream()
                .map(UmlType::getPackageName)
                .filter(pkg -> pkg != null && !pkg.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Returns types in a specific package.
     * @param packageName the package name
     * @return list of types in that package
     */
    public List<UmlType> getTypesInPackage(String packageName) {
        return types.values().stream()
                .filter(t -> packageName.equals(t.getPackageName()))
                .sorted(Comparator.comparing(UmlType::getSimpleName))
                .toList();
    }

    /**
     * Returns types without a package (default package).
     * @return list of types in default package
     */
    public List<UmlType> getTypesInDefaultPackage() {
        return types.values().stream()
                .filter(t -> t.getPackageName() == null || t.getPackageName().isBlank())
                .sorted(Comparator.comparing(UmlType::getSimpleName))
                .toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private Map<String, UmlType> types;
        private List<UmlRelation> relations;
        private List<UmlNote> notes;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addType(UmlType type) {
            if (this.types == null) {
                this.types = new LinkedHashMap<>();
            }
            this.types.put(type.getFqn(), type);
            return this;
        }

        public Builder types(Collection<UmlType> types) {
            if (this.types == null) {
                this.types = new LinkedHashMap<>();
            }
            for (UmlType type : types) {
                this.types.put(type.getFqn(), type);
            }
            return this;
        }

        public Builder addRelation(UmlRelation relation) {
            if (this.relations == null) {
                this.relations = new ArrayList<>();
            }
            this.relations.add(relation);
            return this;
        }

        public Builder relations(List<UmlRelation> relations) {
            this.relations = relations;
            return this;
        }

        public Builder addNote(UmlNote note) {
            if (this.notes == null) {
                this.notes = new ArrayList<>();
            }
            this.notes.add(note);
            return this;
        }

        public Builder notes(List<UmlNote> notes) {
            this.notes = notes;
            return this;
        }

        public UmlModel build() {
            return new UmlModel(this);
        }
    }

    @Override
    public String toString() {
        return "UmlModel{name='" + name + "', types=" + types.size() + ", relations=" + relations.size() + "}";
    }
}
