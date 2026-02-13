package no.ntnu.eitri.parser;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.diagnostics.ParseDiagnostics;
import no.ntnu.eitri.parser.relations.RelationStore;
import no.ntnu.eitri.parser.resolution.TypeReferenceResolver;
import no.ntnu.eitri.parser.resolution.TypeRegistry;
import no.ntnu.eitri.parser.resolution.TypeResolutionStats;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Mutable context for accumulating parsed UML data.
 * 
 * <p>
 * This class collects types, relations, and warnings during parsing,
 * then produces a finalized {@link UmlModel}. It handles:
 * <ul>
 * <li>Type registration and lookup by fully-qualified name</li>
 * <li>On-demand type creation for unresolved references</li>
 * <li>Relation collection with duplicate/strength management</li>
 * <li>Warning collection for non-fatal issues</li>
 * <li>Deferred inheritance resolution (extends/implements)</li>
 * </ul>
 */
public class ParseContext {

    private static final Logger LOGGER = Logger.getLogger(ParseContext.class.getName());

    private final TypeRegistry types;
    private final TypeReferenceResolver typeResolver;
    private final RelationStore relations;
    private final ParseDiagnostics diagnostics;

    /**
     * Represents a pending inheritance relation (extends/implements) to be resolved
     * after all types are registered.
     */
    public record PendingInheritance(String fromFqn, String toFqn, RelationKind kind) {
    }

    /**
     * Creates a new parse context.
     *
     * @param verbose whether diagnostics should log warnings during parse
     */
    public ParseContext(boolean verbose) {
        this.types = new TypeRegistry();
        this.typeResolver = new TypeReferenceResolver(types);
        this.relations = new RelationStore();
        this.diagnostics = new ParseDiagnostics(LOGGER, verbose);
    }

    /**
     * Registers a type in the context.
     * 
     * @param type the type to register
     * @throws IllegalArgumentException if a type with the same FQN already exists
     */
    public void addType(UmlType type) {
        types.addType(type);
    }

    /**
     * Looks up a type by fully-qualified name.
     * 
     * @param fqn the fully-qualified name
     * @return the type, or null if not found
     */
    public UmlType getType(String fqn) {
        return types.getType(fqn);
    }

    /**
     * Checks if a type is registered.
     * 
     * @param fqn the fully-qualified name
     * @return true if the type exists
     */
    public boolean hasType(String fqn) {
        return types.hasType(fqn);
    }

    /**
     * Adds a relation to the context.
     * Duplicates are handled during finalization.
     * 
     * @param relation the relation to add
     */
    public void addRelation(UmlRelation relation) {
        relations.addRelation(relation);
    }

    /**
     * Adds a pending inheritance relation to be resolved after all types are
     * registered.
     * 
     * @param pending the pending inheritance information
     */
    public void addPendingInheritance(PendingInheritance pending) {
        relations.addPendingInheritance(pending);
    }

    /**
     * Resolves a type reference.
     *
     * <p>
     * Only fully-qualified type names that are already registered are resolved.
     * External types and unresolved simple names are skipped.
     *
     * @param fqn the fully-qualified name to resolve
     * @return the resolved (normalized) FQN, or null if the type is not registered
     */
    public String resolveTypeReference(String fqn) {
        return typeResolver.resolveTypeReference(fqn);
    }

    /**
     * Returns aggregate type-reference resolution statistics.
     *
     * @return current resolution statistics snapshot
     */
    public TypeResolutionStats getTypeResolutionStats() {
        return typeResolver.getStatsSnapshot();
    }

    /**
     * Returns parser report containing warnings and type-resolution metrics.
     *
     * @return parser report snapshot
     */
    public ParseReport getReport() {
        return new ParseReport(diagnostics.getWarnings(), typeResolver.getStatsSnapshot());
    }

    /**
     * Adds a warning message.
     * 
     * @param warning the warning message
     */
    public void addWarning(String warning) {
        diagnostics.addWarning(warning);
    }

    /**
     * Records a package as directly parsed from source files.
     * 
     * @param packageName the package name to record
     */
    public void addSourcePackage(String packageName) {
        types.addSourcePackage(packageName);
    }

    /**
     * Returns the packages that were directly parsed from source files.
     * 
     * @return unmodifiable set of source package names
     */
    public Set<String> getSourcePackages() {
        return types.getSourcePackages();
    }

    /**
     * Returns all warnings collected during parsing.
     * 
     * @return list of warning messages
     */
    public List<String> getWarnings() {
        return diagnostics.getWarnings();
    }

    /**
     * Returns all registered types.
     * 
     * @return collection of types
     */
    public Collection<UmlType> getTypes() {
        return types.getTypes();
    }

    /**
     * Builds the final UML model from collected data.
     */
    public UmlModel build() {
        UmlModel.Builder modelBuilder = UmlModel.builder()
                .sourcePackages(types.getSourcePackages());

        types.getTypes().forEach(modelBuilder::addType);
        relations.buildFinalRelations(types, typeResolver).forEach(modelBuilder::addRelation);

        return modelBuilder.build();
    }

    /**
     * Returns the number of registered types.
     * 
     * @return type count
     */
    public int getTypeCount() {
        return types.getTypes().size();
    }

    /**
     * Returns the number of collected relations.
     * 
     * @return relation count (before deduplication)
     */
    public int getRelationCount() {
        return relations.relationCount();
    }
}
