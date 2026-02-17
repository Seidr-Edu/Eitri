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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        // Map-like declarations contain multiple type arguments. Split only at
        // top-level commas so nested generic commas are preserved.
        for (String elementType : splitTopLevelTypeArguments(genericContent)) {
            addFieldRelationIfValid(ownerFqn, field, elementType, RelationKind.AGGREGATION, "*");
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
                && isEnumTypeReference(resolvedType, targetType)) {
            return RelationKind.ASSOCIATION;
        }

        // Final fields suggest strong ownership (composition)
        if (field.isFinal()) {
            return RelationKind.COMPOSITION;
        }
        // Non-final fields suggest weaker association
        return RelationKind.ASSOCIATION;
    }

    private boolean isEnumTypeReference(String resolvedType, UmlType targetType) {
        if (targetType != null) {
            return targetType.getKind() == TypeKind.ENUM;
        }

        try {
            Class<?> type = Class.forName(resolvedType, false, Thread.currentThread().getContextClassLoader());
            return type.isEnum();
        } catch (ClassNotFoundException | LinkageError _) {
            return false;
        }
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
                for (String arg : splitTopLevelTypeArguments(genericArg)) {
                    dependencies.add(arg);
                }
            }
        } else {
            dependencies.add(baseType);
        }
    }

    private List<String> splitTopLevelTypeArguments(String genericContent) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < genericContent.length(); i++) {
            char c = genericContent.charAt(i);
            if (c == '<') {
                depth++;
                current.append(c);
                continue;
            }
            if (c == '>') {
                if (depth > 0) {
                    depth--;
                }
                current.append(c);
                continue;
            }
            if (c == ',' && depth == 0) {
                String token = current.toString().trim();
                if (!token.isEmpty()) {
                    args.add(token);
                }
                current.setLength(0);
                continue;
            }
            current.append(c);
        }

        String token = current.toString().trim();
        if (!token.isEmpty()) {
            args.add(token);
        }
        return args;
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
     * Extracts top-level generic content from a parameterized type.
     * e.g., "List<String>" -> "String",
     * "Map<String, Integer>" -> "String, Integer",
     * "Map<Pair<A,B>, Value>" -> "Pair<A,B>, Value"
     */
    private String extractGenericArgument(String type) {
        int start = type.indexOf('<');
        if (start < 0) {
            return null;
        }

        int depth = 0;
        for (int i = start; i < type.length(); i++) {
            char c = type.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return type.substring(start + 1, i).trim();
                }
            }
        }

        return null;
    }
}
