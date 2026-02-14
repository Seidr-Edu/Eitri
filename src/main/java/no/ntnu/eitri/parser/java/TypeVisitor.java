package no.ntnu.eitri.parser.java;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlGeneric;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlParameter;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlStereotype;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import no.ntnu.eitri.parser.ParseContext;

import java.util.EnumSet;
import java.util.List;

/**
 * AST visitor for extracting type declarations from Java source.
 * 
 * <p>
 * This visitor processes:
 * <ul>
 * <li>Classes and interfaces</li>
 * <li>Enums with constants</li>
 * <li>Annotations</li>
 * <li>Records</li>
 * </ul>
 * 
 * <p>
 * Nested types (inner classes, static nested classes, nested
 * interfaces/enums/annotations/records)
 * are included with Outer$Inner naming convention and explicit "nested"
 * relations.
 * The naming is computed using JavaParser's parent chain.
 */
public class TypeVisitor extends VoidVisitorAdapter<Void> {

    private static final String STATIC_STEREOTYPE = "static";
    private static final String ABSTRACT_STEREOTYPE = "abstract";
    private static final String FINAL_STEREOTYPE = "final";
    private static final String RECORD_STEREOTYPE = "record";

    private final ParseContext context;

    public TypeVisitor(ParseContext context) {
        this.context = context;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        processTypeDeclaration(n, n.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS);
        super.visit(n, arg);
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        processEnumDeclaration(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        processAnnotationDeclaration(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(RecordDeclaration n, Void arg) {
        processRecordDeclaration(n);
        super.visit(n, arg);
    }

    private record TypeBuildContext(String typeFqn, String outerTypeFqn, UmlType.Builder builder) {
    }

    /**
     * Computes the FQN of the enclosing type, or null if top-level.
     */
    private String computeOuterTypeFqn(TypeDeclaration<?> n) {
        // First, check if this type is actually nested by examining the parent node
        if (!isNestedType(n)) {
            return null; // Top-level type has no outer type
        }

        // Type is nested; compute the outer type's FQN
        String typeFqn = n.getFullyQualifiedName().orElse("");
        int lastDot = typeFqn.lastIndexOf('.');
        if (lastDot <= 0) {
            // Fallback: shouldn't happen for nested types with proper FQN
            return null;
        }

        String candidate = typeFqn.substring(0, lastDot);
        // Check if the candidate is a package (all lowercase) or a type (has uppercase)
        // Package names are all lowercase, type names start with uppercase
        int candidateLastDot = candidate.lastIndexOf('.');
        String simplePart = candidateLastDot >= 0 ? candidate.substring(candidateLastDot + 1) : candidate;

        // If the simple part starts with uppercase, it's a type (nested parent)
        // If it's all lowercase, it's a package (shouldn't happen for nested types)
        if (!simplePart.isEmpty() && Character.isUpperCase(simplePart.charAt(0))) {
            return candidate;
        }

        return null;
    }

    /**
     * Checks if this type is nested (has a parent type).
     */
    private boolean isNestedType(TypeDeclaration<?> n) {
        return n.getParentNode().orElse(null) instanceof TypeDeclaration<?>;
    }

    /**
     * Checks if a nested type is static (either explicitly or implicitly).
     * Nested interfaces, enums, records, and annotations are implicitly static.
     */
    private boolean isStaticNested(TypeDeclaration<?> n, TypeKind kind) {
        if (!isNestedType(n)) {
            return false;
        }
        // Interfaces, enums, records, and annotations are implicitly static when nested
        if (kind == TypeKind.INTERFACE || kind == TypeKind.ENUM ||
                kind == TypeKind.RECORD || kind == TypeKind.ANNOTATION) {
            return true;
        }
        // For classes, check the static modifier
        if (n instanceof ClassOrInterfaceDeclaration coid) {
            return coid.isStatic();
        }
        return false;
    }

    /**
     * Process a class or interface declaration.
     */
    private void processTypeDeclaration(ClassOrInterfaceDeclaration n, TypeKind kind) {
        TypeBuildContext typeBuild = createTypeBuildContext(n, kind);
        UmlType.Builder builder = typeBuild.builder();
        String typeFqn = typeBuild.typeFqn();

        // Add modifiers as stereotypes for abstract classes
        if (n.isAbstract() && kind == TypeKind.CLASS) {
            builder.addStereotype(ABSTRACT_STEREOTYPE);
        }

        if (n.isFinal() && kind == TypeKind.CLASS) {
            builder.addStereotype(FINAL_STEREOTYPE);
        }

        addGenerics(n.getTypeParameters(), builder);
        addFields(n.getFields(), builder);
        addMethods(n.getMethods(), builder);
        addConstructors(n.getConstructors(), n.getNameAsString(), builder);
        registerType(typeBuild);

        // Detect inheritance relations
        for (ClassOrInterfaceType extended : n.getExtendedTypes()) {
            addInheritanceRelation(typeFqn, extended, RelationKind.EXTENDS);
        }
        for (ClassOrInterfaceType implemented : n.getImplementedTypes()) {
            addInheritanceRelation(typeFqn, implemented, RelationKind.IMPLEMENTS);
        }
        // Nested types are visited automatically by super.visit()
    }

    /**
     * Adds an inheritance (EXTENDS or IMPLEMENTS) relation.
     * The relation is deferred to ParseContext.build() for resolution after all
     * types are registered.
     */
    private void addInheritanceRelation(String fromFqn, ClassOrInterfaceType toType, RelationKind kind) {
        // Try to resolve the fully qualified name using symbol resolution
        String resolvedFqn = null;
        try {
            var resolvedType = toType.resolve();
            if (resolvedType.isReferenceType()) {
                resolvedFqn = resolvedType.asReferenceType().getQualifiedName();
            } else {
                resolvedFqn = toType.getNameAsString();
            }
        } catch (Exception _) {
            // Symbol resolution failed, use simple name as fallback
            resolvedFqn = toType.getNameAsString();
        }

        // Defer inheritance resolution to build() when all types are registered
        context.addPendingInheritance(new ParseContext.PendingInheritance(fromFqn, resolvedFqn, kind));
    }

    /**
     * Process an enum declaration.
     */
    private void processEnumDeclaration(EnumDeclaration n) {
        TypeBuildContext typeBuild = createTypeBuildContext(n, TypeKind.ENUM);
        UmlType.Builder builder = typeBuild.builder();
        String typeFqn = typeBuild.typeFqn();
        String simpleName = n.getNameAsString();

        // Enum constants as fields
        for (EnumConstantDeclaration constant : n.getEntries()) {
            UmlField constantField = UmlField.builder()
                    .name(constant.getNameAsString())
                    .type(typeFqn)
                    .visibility(Visibility.PUBLIC)
                    .isStatic(true)
                    .isFinal(true)
                    .build();
            builder.addField(constantField);
        }

        addFields(n.getFields(), builder);
        addMethods(n.getMethods(), builder);
        addConstructors(n.getConstructors(), simpleName, builder);
        registerType(typeBuild);

        // Detect implemented interfaces
        for (ClassOrInterfaceType implemented : n.getImplementedTypes()) {
            addInheritanceRelation(typeFqn, implemented, RelationKind.IMPLEMENTS);
        }
        // Nested types are visited automatically by super.visit()
    }

    /**
     * Process an annotation type declaration.
     */
    private void processAnnotationDeclaration(AnnotationDeclaration n) {
        TypeBuildContext typeBuild = createTypeBuildContext(n, TypeKind.ANNOTATION);
        UmlType.Builder builder = typeBuild.builder();

        // Annotation members as methods (they are abstract methods returning values)
        n.getMembers().stream()
                .filter(m -> m instanceof com.github.javaparser.ast.body.AnnotationMemberDeclaration)
                .map(m -> (com.github.javaparser.ast.body.AnnotationMemberDeclaration) m)
                .forEach(member -> {
                    String defaultValue = member.getDefaultValue()
                            .map(expr -> " = " + expr.toString())
                            .orElse("");
                    UmlMethod method = UmlMethod.builder()
                            .name(member.getNameAsString() + "()" + defaultValue)
                            .returnType(resolveTypeFqn(member.getType()))
                            .visibility(Visibility.PUBLIC)
                            .isAbstract(true)
                            .build();
                    builder.addMethod(method);
                });

        registerType(typeBuild);
        // Nested types are visited automatically by super.visit()
    }

    /**
     * Process a record declaration.
     */
    private void processRecordDeclaration(RecordDeclaration n) {
        TypeBuildContext typeBuild = createTypeBuildContext(n, TypeKind.RECORD);
        String typeFqn = typeBuild.typeFqn();
        UmlType.Builder builder = typeBuild.builder();
        builder.addStereotype(RECORD_STEREOTYPE);
        addGenerics(n.getTypeParameters(), builder);

        // Record components as fields
        n.getParameters().forEach(param -> {
            UmlField field = UmlField.builder()
                    .name(param.getNameAsString())
                    .type(resolveTypeFqn(param.getType()))
                    .visibility(Visibility.PRIVATE)
                    .isFinal(true)
                    .build();
            builder.addField(field);
        });

        addMethods(n.getMethods(), builder);
        registerType(typeBuild);

        // Detect implemented interfaces
        for (ClassOrInterfaceType implemented : n.getImplementedTypes()) {
            addInheritanceRelation(typeFqn, implemented, RelationKind.IMPLEMENTS);
        }
        // Nested types are visited automatically by super.visit()
    }

    private TypeBuildContext createTypeBuildContext(TypeDeclaration<?> declaration, TypeKind kind) {
        String typeFqn = declaration.getFullyQualifiedName().orElse("");
        String outerTypeFqn = computeOuterTypeFqn(declaration);
        Visibility visibility = extractVisibility(declaration);

        UmlType.Builder builder = UmlType.builder()
                .fqn(typeFqn)
                .simpleName(declaration.getNameAsString())
                .kind(kind)
                .visibility(visibility);

        if (outerTypeFqn != null) {
            builder.outerTypeFqn(outerTypeFqn);
        }

        if (isStaticNested(declaration, kind)) {
            builder.addStereotype(STATIC_STEREOTYPE);
        }

        addTypeAnnotations(declaration.getAnnotations(), builder);
        return new TypeBuildContext(typeFqn, outerTypeFqn, builder);
    }

    private void registerType(TypeBuildContext typeBuild) {
        context.addType(typeBuild.builder().build());
        if (typeBuild.outerTypeFqn() != null) {
            context.addRelation(UmlRelation.nestedRelation(typeBuild.outerTypeFqn(), typeBuild.typeFqn()));
        }
    }

    private void addTypeAnnotations(List<AnnotationExpr> annotations, UmlType.Builder builder) {
        for (AnnotationExpr annotation : annotations) {
            builder.addStereotype(extractAnnotationAsStereotype(annotation));
        }
    }

    private void addGenerics(List<TypeParameter> typeParameters, UmlType.Builder builder) {
        for (TypeParameter typeParameter : typeParameters) {
            builder.addGeneric(extractGeneric(typeParameter));
        }
    }

    private void addFields(List<FieldDeclaration> fields, UmlType.Builder builder) {
        for (FieldDeclaration field : fields) {
            for (VariableDeclarator variableDeclarator : field.getVariables()) {
                builder.addField(extractField(field, variableDeclarator));
            }
        }
    }

    private void addMethods(List<MethodDeclaration> methods, UmlType.Builder builder) {
        for (MethodDeclaration method : methods) {
            builder.addMethod(extractMethod(method));
        }
    }

    private void addConstructors(List<ConstructorDeclaration> constructors, String ownerSimpleName,
            UmlType.Builder builder) {
        for (ConstructorDeclaration constructor : constructors) {
            builder.addMethod(extractConstructor(constructor, ownerSimpleName));
        }
    }

    /**
     * Extract visibility from a node with modifiers.
     */
    private Visibility extractVisibility(NodeWithModifiers<?> n) {
        if (n.hasModifier(Modifier.Keyword.PUBLIC)) {
            return Visibility.PUBLIC;
        } else if (n.hasModifier(Modifier.Keyword.PROTECTED)) {
            return Visibility.PROTECTED;
        } else if (n.hasModifier(Modifier.Keyword.PRIVATE)) {
            return Visibility.PRIVATE;
        }
        return Visibility.PACKAGE;
    }

    /**
     * Extract a UmlGeneric from a TypeParameter.
     */
    private UmlGeneric extractGeneric(TypeParameter tp) {
        String identifier = tp.getNameAsString();
        List<String> boundsList = tp.getTypeBound().stream()
                .map(ClassOrInterfaceType::asString)
                .toList();

        if (boundsList.isEmpty()) {
            return new UmlGeneric(identifier);
        } else {
            // Join bounds with " & " for multiple bounds (intersection types)
            String bounds = "extends " + String.join(" & ", boundsList);
            return new UmlGeneric(identifier, bounds);
        }
    }

    /**
     * Extract an annotation as a UmlStereotype with its values.
     */
    private UmlStereotype extractAnnotationAsStereotype(AnnotationExpr ann) {
        String name = ann.getNameAsString();

        if (ann instanceof NormalAnnotationExpr normal) {
            List<String> values = normal.getPairs().stream()
                    .map(MemberValuePair::toString)
                    .toList();
            return new UmlStereotype(name, values);
        } else if (ann instanceof SingleMemberAnnotationExpr single) {
            return new UmlStereotype(name, List.of(single.getMemberValue().toString()));
        }

        return new UmlStereotype(name);
    }

    /**
     * Resolves a JavaParser Type to its fully-qualified name.
     * Uses the symbol resolver when available, falls back to the simple name.
     * For generic types like List<Foo>, returns the full representation with
     * resolved type arguments.
     *
     * @param type the JavaParser type to resolve
     * @return the fully-qualified type name, or simple name if resolution fails
     */
    private String resolveTypeFqn(Type type) {
        try {
            ResolvedType resolved = type.resolve();
            return resolveTypeToFqnString(resolved, type.asString());
        } catch (Exception e) {
            String importedFallback = resolveTypeFromImports(type);
            if (importedFallback != null) {
                return importedFallback;
            }
            // Symbol resolution failed, fall back to source representation
            String simpleName = type.asString();
            context.addWarning("Failed to resolve type '" + simpleName + "' at " +
                    type.getBegin().map(Object::toString).orElse("unknown position") +
                    ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return simpleName;
        }
    }

    private String resolveTypeFromImports(Type type) {
        if (type.isArrayType()) {
            String component = resolveTypeFromImports(type.asArrayType().getComponentType());
            if (component != null) {
                return component + "[]";
            }
            return null;
        }
        if (!type.isClassOrInterfaceType()) {
            return null;
        }

        ClassOrInterfaceType classType = type.asClassOrInterfaceType();
        if (classType.getScope().isPresent()) {
            return null;
        }

        CompilationUnit compilationUnit = type.findCompilationUnit().orElse(null);
        if (compilationUnit == null) {
            return null;
        }

        String simpleName = classType.getNameAsString();
        for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
            if (importDeclaration.isAsterisk() || importDeclaration.isStatic()) {
                continue;
            }

            String importedFqn = importDeclaration.getNameAsString();
            if (!importedFqn.endsWith("." + simpleName)) {
                continue;
            }

            if (classType.getTypeArguments().isEmpty()) {
                return importedFqn;
            }

            StringBuilder sb = new StringBuilder(importedFqn);
            sb.append("<");
            NodeList<Type> typeArgs = classType.getTypeArguments().orElseGet(NodeList::new);
            for (int i = 0; i < typeArgs.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Type typeArg = typeArgs.get(i);
                String nested = resolveTypeFromImports(typeArg);
                sb.append(nested != null ? nested : typeArg.asString());
            }
            sb.append(">");
            return sb.toString();
        }

        return null;
    }

    /**
     * Converts a ResolvedType to its FQN string representation.
     * Handles reference types, primitives, arrays, and generic type arguments.
     */
    private String resolveTypeToFqnString(ResolvedType resolved, String fallback) {
        if (resolved.isPrimitive()) {
            return resolved.asPrimitive().describe();
        }
        if (resolved.isVoid()) {
            return "void";
        }
        if (resolved.isArray()) {
            return resolveTypeToFqnString(resolved.asArrayType().getComponentType(), fallback) + "[]";
        }
        if (resolved.isReferenceType()) {
            ResolvedReferenceType refType = resolved.asReferenceType();
            String baseFqn = refType.getQualifiedName();

            // Handle generic type arguments
            List<ResolvedType> typeArgs = refType.typeParametersValues();
            if (typeArgs.isEmpty()) {
                return baseFqn;
            }

            StringBuilder sb = new StringBuilder(baseFqn);
            sb.append("<");
            for (int i = 0; i < typeArgs.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(resolveTypeToFqnString(typeArgs.get(i), "?"));
            }
            sb.append(">");
            return sb.toString();
        }
        if (resolved.isTypeVariable()) {
            return resolved.asTypeVariable().describe();
        }
        if (resolved.isWildcard()) {
            return resolved.asWildcard().describe();
        }
        return fallback;
    }

    /**
     * Extract a UmlField from a field declaration.
     */
    private UmlField extractField(FieldDeclaration field, VariableDeclarator varDec) {
        String name = varDec.getNameAsString();
        String type = resolveTypeFqn(varDec.getType()); // FQN for relation detection
        Visibility visibility = extractVisibility(field);

        boolean isStatic = field.isStatic();
        boolean isFinal = field.isFinal();

        UmlField.Builder builder = UmlField.builder()
                .name(name)
                .type(type)
                .visibility(visibility)
                .isStatic(isStatic)
                .isFinal(isFinal)
                .readOnly(isFinal); // Mark final fields as read-only in PlantUML

        // Add annotations
        for (AnnotationExpr ann : field.getAnnotations()) {
            builder.addAnnotation(ann.getNameAsString());
        }

        // Extract initializer if present (for default values)
        varDec.getInitializer().ifPresent(init -> {
            // Store as annotation for display purposes
            String initStr = init.toString();
            if (initStr.length() <= 50) { // Truncate long initializers
                builder.addAnnotation("init:" + initStr);
            }
        });

        return builder.build();
    }

    /**
     * Extract a UmlMethod from a method declaration.
     */
    private UmlMethod extractMethod(MethodDeclaration method) {
        String name = method.getNameAsString();
        String returnType = resolveTypeFqn(method.getType()); // FQN for relation detection
        Visibility visibility = extractVisibility(method);

        boolean isStatic = method.isStatic();
        boolean isAbstract = method.isAbstract();
        boolean isFinal = method.isFinal();

        UmlMethod.Builder builder = UmlMethod.builder()
                .name(name)
                .returnType(returnType)
                .visibility(visibility)
                .isStatic(isStatic)
                .isAbstract(isAbstract);

        // Extract modifiers
        @SuppressWarnings("null")
        EnumSet<no.ntnu.eitri.model.Modifier> modifiers = EnumSet.noneOf(no.ntnu.eitri.model.Modifier.class);
        if (isStatic)
            modifiers.add(no.ntnu.eitri.model.Modifier.STATIC);
        if (isAbstract)
            modifiers.add(no.ntnu.eitri.model.Modifier.ABSTRACT);
        if (isFinal)
            modifiers.add(no.ntnu.eitri.model.Modifier.FINAL);
        if (method.isSynchronized())
            modifiers.add(no.ntnu.eitri.model.Modifier.SYNCHRONIZED);
        if (method.isNative())
            modifiers.add(no.ntnu.eitri.model.Modifier.NATIVE);
        if (method.isDefault())
            modifiers.add(no.ntnu.eitri.model.Modifier.DEFAULT);
        builder.modifiers(modifiers);

        // Extract parameters with FQN types
        method.getParameters().forEach(param -> {
            UmlParameter umlParam = new UmlParameter(
                    param.getNameAsString(),
                    resolveTypeFqn(param.getType()) // FQN for relation detection
            );
            builder.addParameter(umlParam);
        });

        // Extract thrown exceptions with FQN
        method.getThrownExceptions().forEach(exc -> builder.addThrownException(resolveTypeFqn(exc)));

        // Add annotations as stereotypes in method
        for (AnnotationExpr ann : method.getAnnotations()) {
            // Store significant annotations
            String annName = ann.getNameAsString();
            if (!annName.equals("Override")) { // Skip @Override, too common
                builder.addAnnotation(annName);
            }
        }

        return builder.build();
    }

    /**
     * Extract a constructor as a UmlMethod.
     */
    private UmlMethod extractConstructor(ConstructorDeclaration ctor, String className) {
        Visibility visibility = extractVisibility(ctor);

        UmlMethod.Builder builder = UmlMethod.builder()
                .name(className) // Constructor has class name
                .returnType("") // No return type for constructors
                .visibility(visibility)
                .constructor(true)
                .addAnnotation("constructor");

        // Extract parameters with FQN types
        ctor.getParameters().forEach(param -> {
            UmlParameter umlParam = new UmlParameter(
                    param.getNameAsString(),
                    resolveTypeFqn(param.getType()) // FQN for relation detection
            );
            builder.addParameter(umlParam);
        });

        // Extract thrown exceptions with FQN
        ctor.getThrownExceptions().forEach(exc -> builder.addThrownException(resolveTypeFqn(exc)));

        return builder.build();
    }
}
