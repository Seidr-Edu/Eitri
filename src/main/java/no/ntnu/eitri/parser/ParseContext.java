package no.ntnu.eitri.parser;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Mutable context for accumulating parsed UML data.
 * 
 * <p>This class collects types, relations, and warnings during parsing,
 * then produces a finalized {@link UmlModel}. It handles:
 * <ul>
 *   <li>Type registration and lookup by fully-qualified name</li>
 *   <li>Ghost type creation for unresolved references</li>
 *   <li>Relation collection with duplicate/strength management</li>
 *   <li>Warning collection for non-fatal issues</li>
 *   <li>Deferred inheritance resolution (extends/implements)</li>
 * </ul>
 */
public class ParseContext {

    private static final Logger LOGGER = Logger.getLogger(ParseContext.class.getName());

    private final EitriConfig config;
    private final String diagramName;

    /** Types indexed by fully-qualified name. */
    private final Map<String, UmlType> typesByFqn = new HashMap<>();

    /** Collected relations (may contain duplicates, resolved at finalization). */
    private final List<UmlRelation> relations = new ArrayList<>();

    /** Pending inheritance relations to resolve after all types are registered. */
    private final List<PendingInheritance> pendingInheritance = new ArrayList<>();

    /** Ghost types created for unresolved references. */
    private final Set<String> ghostTypes = new HashSet<>();

    /** Warnings collected during parsing. */
    private final List<String> warnings = new ArrayList<>();

    /**
     * Represents a pending inheritance relation (extends/implements) to be resolved
     * after all types are registered.
     *
     * @param fromFqn the FQN of the type doing the extending/implementing
     * @param toFqn the FQN of the target type (may be unresolved simple name)
     * @param kind EXTENDS or IMPLEMENTS
     */
    public record PendingInheritance(String fromFqn, String toFqn, RelationKind kind) {}

    /** Standard library prefixes to skip for ghost type creation. */
    private static final List<String> STDLIB_PREFIXES = List.of(
            "java.", "javax.", "sun.", "com.sun.", "jdk.",
            "kotlin.", "kotlinx.",
            "org.w3c.", "org.xml.", "org.ietf.",
            "android.", "dalvik."
    );

    /** Common types that should never become ghost types. */
    private static final Set<String> PRIMITIVE_AND_COMMON = Set.of(
            "void", "boolean", "byte", "char", "short", "int", "long", "float", "double",
            "String", "Object", "Class", "Enum", "Annotation", "Record",
            "Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float", "Double",
            "Number", "Comparable", "Cloneable", "Serializable", "Iterable",
            "List", "Set", "Map", "Collection", "Optional", "Stream"
    );

    /**
     * Creates a new parse context with the given configuration.
     * 
     * @param config the configuration to use
     */
    public ParseContext(EitriConfig config) {
        this.config = config;
        this.diagramName = config.getDiagramName();
    }

    /**
     * Registers a type in the context.
     * 
     * @param type the type to register
     * @throws IllegalArgumentException if a type with the same FQN already exists
     */
    public void addType(UmlType type) {
        String fqn = type.getId();
        if (typesByFqn.containsKey(fqn)) {
            throw new IllegalArgumentException("Type already registered: " + fqn);
        }
        typesByFqn.put(fqn, type);
    }

    /**
     * Looks up a type by fully-qualified name.
     * 
     * @param fqn the fully-qualified name
     * @return the type, or null if not found
     */
    public UmlType getType(String fqn) {
        return typesByFqn.get(fqn);
    }

    /**
     * Checks if a type is registered.
     * 
     * @param fqn the fully-qualified name
     * @return true if the type exists
     */
    public boolean hasType(String fqn) {
        return typesByFqn.containsKey(fqn);
    }

    /**
     * Adds a relation to the context.
     * Duplicates are handled during finalization.
     * 
     * @param relation the relation to add
     */
    public void addRelation(UmlRelation relation) {
        relations.add(relation);
    }

    /**
     * Adds a pending inheritance relation to be resolved after all types are registered.
     * 
     * @param pending the pending inheritance information
     */
    public void addPendingInheritance(PendingInheritance pending) {
        pendingInheritance.add(pending);
    }

    /**
     * Resolves a type reference, creating a ghost type if necessary.
     * 
     * <p>If the type is:
     * <ul>
     *   <li>Already registered: returns the FQN</li>
     *   <li>A standard library type: returns null (skip)</li>
     *   <li>A primitive/common type: returns null (skip)</li>
     *   <li>Unknown and config.showGhostTypes is true: creates a ghost type</li>
     *   <li>Unknown and config.showGhostTypes is false: logs warning and returns null</li>
     * </ul>
     * 
     * @param fqn the fully-qualified name to resolve
     * @param referencedFrom context for warning messages
     * @return the resolved FQN, or null if the type should be skipped
     */
    public String resolveTypeReference(String fqn, String referencedFrom) {
        if (fqn == null || fqn.isEmpty()) {
            return null;
        }

        // Already known?
        if (typesByFqn.containsKey(fqn)) {
            return fqn;
        }

        // Primitive or common type? (skip these, they're not interesting in diagrams)
        String simpleName = extractSimpleName(fqn);
        if (PRIMITIVE_AND_COMMON.contains(simpleName)) {
            return null;
        }

        // NOTE: Standard library filtering removed to capture all relations.
        // Stdlib types will be included as ghost types if showGhostTypes is enabled.

        // Create ghost type or warn
        if (config.isShowGhostTypes()) {
            if (!ghostTypes.contains(fqn)) {
                createGhostType(fqn);
                addWarning("Created ghost type for unresolved reference: " + fqn + 
                           (referencedFrom != null ? " (referenced from " + referencedFrom + ")" : ""));
            }
            return fqn;
        } else {
            addWarning("Unresolved type reference: " + fqn + 
                       (referencedFrom != null ? " (referenced from " + referencedFrom + ")" : ""));
            return null;
        }
    }

    /**
     * Creates a ghost (placeholder) type for an unresolved reference.
     * Ghost types are rendered with a special stereotype.
     * 
     * @param fqn the fully-qualified name
     */
    private void createGhostType(String fqn) {
        String packageName = extractPackageName(fqn);
        String simpleName = extractSimpleName(fqn);

        UmlType ghost = UmlType.builder()
                .packageName(packageName)
                .name(simpleName)
                .kind(TypeKind.CLASS)  // Default to class for unknowns
                .addStereotype("ghost")
                .build();

        typesByFqn.put(fqn, ghost);
        ghostTypes.add(fqn);
    }

    /**
     * Adds a warning message.
     * 
     * @param warning the warning message
     */
    public void addWarning(String warning) {
        warnings.add(warning);
        if (config.isVerbose()) {
            LOGGER.warning(warning);
        }
    }

    /**
     * Checks if a type is from the standard library.
     * 
     * @param fqn the fully-qualified name
     * @return true if it's a stdlib type
     */
    private boolean isStdlibType(String fqn) {
        for (String prefix : STDLIB_PREFIXES) {
            if (fqn.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the package name from a fully-qualified name.
     * 
     * @param fqn the fully-qualified name
     * @return the package name, or empty string if no package
     */
    private String extractPackageName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot > 0 ? fqn.substring(0, lastDot) : "";
    }

    /**
     * Extracts the simple name from a fully-qualified name.
     * 
     * @param fqn the fully-qualified name
     * @return the simple name
     */
    private String extractSimpleName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;
    }

    /**
     * Returns all warnings collected during parsing.
     * 
     * @return list of warning messages
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Returns the set of ghost type FQNs.
     * 
     * @return set of ghost type names
     */
    public Set<String> getGhostTypes() {
        return new HashSet<>(ghostTypes);
    }

    /**
     * Returns all registered types.
     * 
     * @return collection of types
     */
    public Collection<UmlType> getTypes() {
        return typesByFqn.values();
    }

    /**
     * Returns the configuration.
     * 
     * @return the config
     */
    public EitriConfig getConfig() {
        return config;
    }

    /**
     * Builds the final UML model from collected data.
     * 
     * <p>This method:
     * <ul>
     *   <li>Resolves pending inheritance relations</li>
     *   <li>Deduplicates relations (keeping the strongest)</li>
     *   <li>Removes relations to non-existent types</li>
     *   <li>Assembles the final model</li>
     * </ul>
     * 
     * @return the finalized UML model
     */
    public UmlModel build() {
        UmlModel.Builder modelBuilder = UmlModel.builder()
                .name(diagramName);

        // Add all types
        typesByFqn.values().forEach(modelBuilder::addType);

        // Resolve pending inheritance relations (now that all types are registered)
        for (PendingInheritance pi : pendingInheritance) {
            String targetFqn = resolveTypeReference(pi.toFqn(), pi.fromFqn());
            if (targetFqn != null) {
                UmlRelation relation = UmlRelation.builder()
                        .fromTypeId(pi.fromFqn())
                        .toTypeId(targetFqn)
                        .kind(pi.kind())
                        .build();
                relations.add(relation);
            }
        }

        // Deduplicate and validate relations
        Map<String, UmlRelation> deduped = new HashMap<>();
        for (UmlRelation rel : relations) {
            // Skip relations to non-existent types
            if (!typesByFqn.containsKey(rel.getFromTypeId()) || !typesByFqn.containsKey(rel.getToTypeId())) {
                continue;
            }

            String key = rel.getFromTypeId() + "->" + rel.getToTypeId();
            UmlRelation existing = deduped.get(key);
            if (existing == null || isStronger(rel.getKind(), existing.getKind())) {
                deduped.put(key, rel);
            }
        }

        deduped.values().forEach(modelBuilder::addRelation);

        return modelBuilder.build();
    }

    /**
     * Determines if one relation kind is stronger than another.
     * Strength order: EXTENDS > IMPLEMENTS > COMPOSITION > AGGREGATION > ASSOCIATION > DEPENDENCY
     * 
     * @param a the first relation kind
     * @param b the second relation kind
     * @return true if a is stronger than b
     */
    private boolean isStronger(RelationKind a, RelationKind b) {
        return strengthOf(a) > strengthOf(b);
    }

    private int strengthOf(RelationKind kind) {
        return switch (kind) {
            case EXTENDS -> 6;
            case IMPLEMENTS -> 5;
            case COMPOSITION -> 4;
            case AGGREGATION -> 3;
            case ASSOCIATION -> 2;
            case DEPENDENCY -> 1;
        };
    }

    /**
     * Returns the number of registered types.
     * 
     * @return type count
     */
    public int getTypeCount() {
        return typesByFqn.size();
    }

    /**
     * Returns the number of collected relations.
     * 
     * @return relation count (before deduplication)
     */
    public int getRelationCount() {
        return relations.size();
    }
}
