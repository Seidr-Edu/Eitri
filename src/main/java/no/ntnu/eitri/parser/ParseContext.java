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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Mutable context for accumulating parsed UML data.
 * 
 * <p>This class collects types, relations, and warnings during parsing,
 * then produces a finalized {@link UmlModel}. It handles:
 * <ul>
 *   <li>Type registration and lookup by fully-qualified name</li>
 *   <li>On-demand type creation for unresolved references</li>
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
        String fqn = type.getFqn();
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
     * Resolves a type reference.
     *
     * <p>If the type is not already registered, a placeholder type is created
     * so all referenced types are modeled and can be filtered during writing.
     *
     * @param fqn the fully-qualified name to resolve
     * @param referencedFrom context for warning messages
     * @return the resolved (normalized) FQN, or null if the type is empty
     */
    public String resolveTypeReference(String fqn, String referencedFrom) {
        if (fqn == null || fqn.isEmpty()) {
            return null;
        }

        String normalized = normalizeTypeName(fqn);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }

        // Already known?
        if (typesByFqn.containsKey(normalized)) {
            return normalized;
        }

        ensureTypeExists(normalized);

        return normalized;
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
     * Extracts the simple name from a fully-qualified name.
     * 
     * @param fqn the fully-qualified name
     * @return the simple name
     */
    private String extractSimpleName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;
    }

    private void ensureTypeExists(String fqn) {
        if (fqn == null || fqn.isBlank()) {
            return;
        }
        if (typesByFqn.containsKey(fqn)) {
            return;
        }

        UmlType placeholder = UmlType.builder()
                .fqn(fqn)
                .simpleName(extractSimpleName(fqn))
                .kind(TypeKind.CLASS)
                .visibility(no.ntnu.eitri.model.Visibility.PACKAGE)
                .build();

        typesByFqn.put(fqn, placeholder);
    }

    private String normalizeTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }

        String base = typeName.trim();
        if (base.isEmpty()) {
            return null;
        }

        // Strip array notation
        while (base.endsWith("[]")) {
            base = base.substring(0, base.length() - 2).trim();
        }

        // Strip generic arguments
        int genericStart = base.indexOf('<');
        if (genericStart >= 0) {
            base = base.substring(0, genericStart).trim();
        }

        // Strip wildcard bounds
        if (base.startsWith("? extends ")) {
            base = base.substring("? extends ".length()).trim();
        } else if (base.startsWith("? super ")) {
            base = base.substring("? super ".length()).trim();
        } else if ("?".equals(base)) {
            return null;
        }

        return base;
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
            case NESTED -> 7;  // Nesting is structural, highest priority
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
