package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlParameter;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.parser.ParseContext;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects relationships between types based on their members.
 * 
 * <p>
 * Relationship detection strategy:
 * <ul>
 * <li>EXTENDS/IMPLEMENTS - detected during TypeVisitor parsing (from AST)</li>
 * <li>COMPOSITION - fields with strong ownership (final, initialized in
 * constructor)</li>
 * <li>AGGREGATION - collection fields, shared references</li>
 * <li>ASSOCIATION - simple field references</li>
 * <li>DEPENDENCY - method parameter/return type references</li>
 * </ul>
 * 
 * <p>
 * Note: EXTENDS and IMPLEMENTS relations are added by the TypeVisitor when
 * parsing the extends/implements clauses. This detector handles field and
 * method-based relationships.
 */
public class RelationDetector {

    private final ParseContext context;

    /** Pattern to extract generic type arguments like List<String> → String */
    private static final Pattern GENERIC_PATTERN = Pattern.compile("<([^<>]+)>");

    /** Collection type names that indicate aggregation */
    private static final Set<String> COLLECTION_TYPES = Set.of(
            "List", "ArrayList", "LinkedList",
            "Set", "HashSet", "TreeSet", "LinkedHashSet",
            "Map", "HashMap", "TreeMap", "LinkedHashMap",
            "Collection", "Iterable",
            "Queue", "Deque", "ArrayDeque", "PriorityQueue",
            "Stack", "Vector");

    public RelationDetector(ParseContext context) {
        this.context = context;
    }

    /**
     * Detects relationships from all registered types.
     */
    public void detectRelations() {
        // Snapshot types since relation detection can add placeholder types.
        for (UmlType type : new java.util.ArrayList<>(context.getTypes())) {
            String fqn = type.getFqn();
            detectFieldRelations(fqn, type);
            detectMethodDependencies(fqn, type);
        }
    }

    /**
     * Detects field-based relationships for a type.
     * Should be called during type processing.
     * 
     * @param ownerFqn the FQN of the type owning the fields
     * @param type     the UmlType to analyze
     */
    public void detectFieldRelations(String ownerFqn, UmlType type) {
        for (UmlField field : type.getFields()) {
            detectFieldRelation(ownerFqn, field);
        }
    }

    /**
     * Detects method-based dependencies for a type.
     * 
     * @param ownerFqn the FQN of the type owning the methods
     * @param type     the UmlType to analyze
     */
    public void detectMethodDependencies(String ownerFqn, UmlType type) {
        for (UmlMethod method : type.getMethods()) {
            detectMethodDependency(ownerFqn, method);
        }
    }

    /**
     * Detects a relationship from a field declaration.
     */
    private void detectFieldRelation(String ownerFqn, UmlField field) {
        String fieldType = field.getType();

        // Check if it's a collection type
        if (isCollectionType(fieldType)) {
            // Extract the generic type arguments (may be comma-separated for Map<K,V>)
            String genericContent = extractGenericArgument(fieldType);
            if (genericContent != null) {
                for (String elementType : genericContent.split(",")) {
                    String resolvedElement = context.normalizeToValidFqn(elementType.trim());
                    if (resolvedElement != null) {
                        UmlRelation relation = UmlRelation.builder()
                                .fromTypeFqn(ownerFqn)
                                .toTypeFqn(resolvedElement)
                                .kind(RelationKind.AGGREGATION)
                                .toMultiplicity("*")
                                .fromMember(field.getName())
                                .build();
                        context.addRelation(relation);
                    }
                }
            }
            return;
        }

        // Check for array types
        if (fieldType.endsWith("[]")) {
            String elementType = fieldType.substring(0, fieldType.length() - 2);
            String resolvedType = context.normalizeToValidFqn(elementType);
            if (resolvedType != null) {
                UmlRelation relation = UmlRelation.builder()
                        .fromTypeFqn(ownerFqn)
                        .toTypeFqn(resolvedType)
                        .kind(RelationKind.AGGREGATION)
                        .toMultiplicity("*")
                        .fromMember(field.getName())
                        .build();
                context.addRelation(relation);
            }
            return;
        }

        // Simple field reference
        String resolvedType = context.normalizeToValidFqn(fieldType);
        if (resolvedType != null) {
            // Determine if composition or association based on field modifiers
            RelationKind kind = determineFieldRelationKind(field);

            UmlRelation.Builder relBuilder = UmlRelation.builder()
                    .fromTypeFqn(ownerFqn)
                    .toTypeFqn(resolvedType)
                    .kind(kind)
                    .fromMember(field.getName());

            // Add multiplicity based on nullability hints
            if (!field.isFinal()) {
                relBuilder.toMultiplicity("0..1");
            } else {
                relBuilder.toMultiplicity("1");
            }

            context.addRelation(relBuilder.build());
        }
    }

    /**
     * Determines the relation kind for a field.
     */
    private RelationKind determineFieldRelationKind(UmlField field) {
        // Final fields suggest strong ownership (composition)
        if (field.isFinal()) {
            return RelationKind.COMPOSITION;
        }
        // Non-final fields suggest weaker association
        return RelationKind.ASSOCIATION;
    }

    /**
     * Detects dependencies from method signatures.
     */
    private void detectMethodDependency(String ownerFqn, UmlMethod method) {
        Set<String> dependencies = new HashSet<>();

        // Check return type
        String returnType = method.getReturnType();
        if (returnType != null && !returnType.isEmpty() && !returnType.equals("void")) {
            addTypeDependency(dependencies, returnType);
        }

        // Check parameters
        for (UmlParameter param : method.getParameters()) {
            addTypeDependency(dependencies, param.type());
        }

        // Check thrown exceptions
        for (String exception : method.getThrownExceptions()) {
            addTypeDependency(dependencies, exception);
        }

        // Add dependency relations
        for (String dep : dependencies) {
            String resolvedType = context.normalizeToValidFqn(dep);
            if (resolvedType != null && !resolvedType.equals(ownerFqn)) {
                UmlRelation relation = UmlRelation.builder()
                        .fromTypeFqn(ownerFqn)
                        .toTypeFqn(resolvedType)
                        .kind(RelationKind.DEPENDENCY)
                        .build();
                context.addRelation(relation);
            }
        }
    }

    /**
     * Adds a type and its generic arguments to the dependency set.
     */
    private void addTypeDependency(Set<String> dependencies, String type) {
        if (type == null || type.isEmpty()) {
            return;
        }

        // Strip array notation
        String baseType = type.replace("[]", "").trim();

        // Extract base type (before generics)
        int genericStart = baseType.indexOf('<');
        if (genericStart > 0) {
            String rawType = baseType.substring(0, genericStart);
            if (!isCollectionType(rawType)) {
                dependencies.add(rawType);
            }

            // Extract generic arguments
            String genericArg = extractGenericArgument(baseType);
            if (genericArg != null) {
                // Handle multiple generic arguments (e.g., Map<K, V>)
                for (String arg : genericArg.split(",")) {
                    dependencies.add(arg.trim());
                }
            }
        } else {
            dependencies.add(baseType);
        }
    }

    /**
     * Checks if a type name is a collection type.
     */
    private boolean isCollectionType(String typeName) {
        if (typeName == null) {
            return false;
        }
        // Get simple name (without generics)
        String simpleName = typeName;
        int genericStart = typeName.indexOf('<');
        if (genericStart > 0) {
            simpleName = typeName.substring(0, genericStart);
        }
        // Get last part of qualified name
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot > 0) {
            simpleName = simpleName.substring(lastDot + 1);
        }
        return COLLECTION_TYPES.contains(simpleName);
    }

    /**
     * Extracts the first generic type argument from a parameterized type.
     * e.g., "List<String>" → "String", "Map<String, Integer>" → "String, Integer"
     */
    private String extractGenericArgument(String type) {
        Matcher matcher = GENERIC_PATTERN.matcher(type);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
