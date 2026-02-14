package no.ntnu.eitri.parser.java;

import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
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

        if (isCollectionType(fieldType)) {
            addCollectionFieldRelations(ownerFqn, field, fieldType);
            return;
        }

        if (fieldType.endsWith("[]")) {
            String elementType = fieldType.substring(0, fieldType.length() - 2).trim();
            addFieldRelationIfValid(ownerFqn, field, elementType, RelationKind.AGGREGATION, "*");
            return;
        }

        addSimpleFieldRelation(ownerFqn, field, fieldType);
    }

    private void addCollectionFieldRelations(String ownerFqn, UmlField field, String fieldType) {
        String genericContent = extractGenericArgument(fieldType);
        if (genericContent == null) {
            return;
        }
        // Map-like declarations contain multiple type arguments. Splitting here
        // avoids emitting invalid combined targets such as "java.lang.String,
        // java.lang.String" as a single relation endpoint.
        for (String elementType : genericContent.split(",")) {
            addFieldRelationIfValid(ownerFqn, field, elementType.trim(), RelationKind.AGGREGATION, "*");
        }
    }

    private void addSimpleFieldRelation(String ownerFqn, UmlField field, String fieldType) {
        String resolvedType = context.normalizeToValidFqn(fieldType);
        if (resolvedType == null || shouldSkipStaticSelfFieldRelation(ownerFqn, field, resolvedType)) {
            return;
        }

        RelationKind kind = determineFieldRelationKind(ownerFqn, field, resolvedType);
        String toMultiplicity = field.isFinal() ? "1" : "0..1";
        addFieldRelation(ownerFqn, field, resolvedType, kind, toMultiplicity);
    }

    private void addFieldRelationIfValid(String ownerFqn, UmlField field, String candidateType,
            RelationKind kind, String toMultiplicity) {
        String resolvedType = context.normalizeToValidFqn(candidateType);
        if (resolvedType == null || shouldSkipStaticSelfFieldRelation(ownerFqn, field, resolvedType)) {
            return;
        }
        addFieldRelation(ownerFqn, field, resolvedType, kind, toMultiplicity);
    }

    private void addFieldRelation(String ownerFqn, UmlField field, String targetFqn,
            RelationKind kind, String toMultiplicity) {
        UmlRelation.Builder relationBuilder = UmlRelation.builder()
                .fromTypeFqn(ownerFqn)
                .toTypeFqn(targetFqn)
                .kind(kind)
                .fromMember(field.getName());

        if (toMultiplicity != null) {
            relationBuilder.toMultiplicity(toMultiplicity);
        }

        context.addRelation(relationBuilder.build());
    }

    private boolean shouldSkipStaticSelfFieldRelation(String ownerFqn, UmlField field, String targetFqn) {
        return field.isStatic() && ownerFqn.equals(targetFqn);
    }

    /**
     * Determines the relation kind for a field.
     */
    private RelationKind determineFieldRelationKind(String ownerFqn, UmlField field, String resolvedType) {
        UmlType ownerType = context.getType(ownerFqn);
        UmlType targetType = context.getType(resolvedType);

        if (ownerType != null
                && ownerType.getKind() == TypeKind.CLASS
                && targetType != null
                && targetType.getKind() == TypeKind.ENUM) {
            return RelationKind.ASSOCIATION;
        }

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
