package no.ntnu.eitri.parser.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
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
import no.ntnu.eitri.model.UmlStereotype;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import no.ntnu.eitri.parser.ParseContext;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AST visitor for extracting type declarations from Java source.
 * 
 * <p>This visitor processes:
 * <ul>
 *   <li>Classes and interfaces</li>
 *   <li>Enums with constants</li>
 *   <li>Annotations</li>
 *   <li>Records</li>
 * </ul>
 * 
 * <p>Inner classes are skipped (deferred to Phase 4).
 */
public class TypeVisitor extends VoidVisitorAdapter<Void> {

    private final ParseContext context;

    public TypeVisitor(ParseContext context) {
        this.context = context;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        // Skip inner/nested classes (deferred to Phase 4)
        if (n.isNestedType()) {
            return;
        }

        processTypeDeclaration(n, n.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS);
        super.visit(n, arg);
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        // Skip inner/nested enums
        if (n.isNestedType()) {
            return;
        }

        processEnumDeclaration(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        // Skip inner/nested annotations
        if (n.isNestedType()) {
            return;
        }

        processAnnotationDeclaration(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(RecordDeclaration n, Void arg) {
        // Skip inner/nested records
        if (n.isNestedType()) {
            return;
        }

        processRecordDeclaration(n);
        super.visit(n, arg);
    }

    /**
     * Process a class or interface declaration.
     */
    private void processTypeDeclaration(ClassOrInterfaceDeclaration n, TypeKind kind) {
        String packageName = getPackageName(n);
        String simpleName = n.getNameAsString();
        Visibility visibility = extractVisibility(n);

        UmlType.Builder builder = UmlType.builder()
                .name(simpleName)
                .packageName(packageName)
                .kind(kind)
                .visibility(visibility);

        // Add modifiers as stereotypes for abstract classes
        if (n.isAbstract() && kind == TypeKind.CLASS) {
            builder.addStereotype("abstract");
        }

        // Extract generics
        for (TypeParameter tp : n.getTypeParameters()) {
            builder.addGeneric(extractGeneric(tp));
        }

        // Extract annotations
        for (AnnotationExpr ann : n.getAnnotations()) {
            builder.addStereotype(extractAnnotationAsStereotype(ann));
        }

        // Extract fields
        for (FieldDeclaration field : n.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                builder.addField(extractField(field, var));
            }
        }

        // Extract methods
        for (MethodDeclaration method : n.getMethods()) {
            builder.addMethod(extractMethod(method));
        }

        // Extract constructors (as methods with <<constructor>> stereotype)
        for (ConstructorDeclaration ctor : n.getConstructors()) {
            builder.addMethod(extractConstructor(ctor, simpleName));
        }

        UmlType type = builder.build();
        context.addType(type);

        // Detect inheritance relations
        String typeFqn = type.getId();

        // Extended types (superclass for classes, extended interfaces for interfaces)
        for (ClassOrInterfaceType extended : n.getExtendedTypes()) {
            addInheritanceRelation(typeFqn, extended, RelationKind.EXTENDS);
        }

        // Implemented interfaces (only for classes)
        for (ClassOrInterfaceType implemented : n.getImplementedTypes()) {
            addInheritanceRelation(typeFqn, implemented, RelationKind.IMPLEMENTS);
        }
    }

    /**
     * Adds an inheritance (EXTENDS or IMPLEMENTS) relation.
     * The relation is deferred to ParseContext.build() for resolution after all types are registered.
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
        } catch (Exception e) {
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
        String packageName = getPackageName(n);
        String simpleName = n.getNameAsString();
        Visibility visibility = extractVisibility(n);

        UmlType.Builder builder = UmlType.builder()
                .name(simpleName)
                .packageName(packageName)
                .kind(TypeKind.ENUM)
                .visibility(visibility);

        // Extract annotations
        for (AnnotationExpr ann : n.getAnnotations()) {
            builder.addStereotype(extractAnnotationAsStereotype(ann));
        }

        // Enum constants as fields
        for (EnumConstantDeclaration constant : n.getEntries()) {
            UmlField constantField = UmlField.builder()
                    .name(constant.getNameAsString())
                    .type(simpleName)
                    .visibility(Visibility.PUBLIC)
                    .isStatic(true)
                    .isFinal(true)
                    .build();
            builder.addField(constantField);
        }

        // Extract regular fields
        for (FieldDeclaration field : n.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                builder.addField(extractField(field, var));
            }
        }

        // Extract methods
        for (MethodDeclaration method : n.getMethods()) {
            builder.addMethod(extractMethod(method));
        }

        // Extract constructors
        for (ConstructorDeclaration ctor : n.getConstructors()) {
            builder.addMethod(extractConstructor(ctor, simpleName));
        }

        UmlType type = builder.build();
        context.addType(type);

        // Detect implemented interfaces
        String typeFqn = type.getId();
        for (ClassOrInterfaceType implemented : n.getImplementedTypes()) {
            addInheritanceRelation(typeFqn, implemented, RelationKind.IMPLEMENTS);
        }
    }

    /**
     * Process an annotation type declaration.
     */
    private void processAnnotationDeclaration(AnnotationDeclaration n) {
        String packageName = getPackageName(n);
        String simpleName = n.getNameAsString();
        Visibility visibility = extractVisibility(n);

        UmlType.Builder builder = UmlType.builder()
                .name(simpleName)
                .packageName(packageName)
                .kind(TypeKind.ANNOTATION)
                .visibility(visibility);

        // Extract meta-annotations
        for (AnnotationExpr ann : n.getAnnotations()) {
            builder.addStereotype(extractAnnotationAsStereotype(ann));
        }

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
                            .returnType(member.getType().asString())
                            .visibility(Visibility.PUBLIC)
                            .isAbstract(true)
                            .build();
                    builder.addMethod(method);
                });

        UmlType type = builder.build();
        context.addType(type);
    }

    /**
     * Process a record declaration.
     */
    private void processRecordDeclaration(RecordDeclaration n) {
        String packageName = getPackageName(n);
        String simpleName = n.getNameAsString();
        Visibility visibility = extractVisibility(n);

        UmlType.Builder builder = UmlType.builder()
                .name(simpleName)
                .packageName(packageName)
                .kind(TypeKind.RECORD)
                .visibility(visibility);

        // Extract generics
        for (TypeParameter tp : n.getTypeParameters()) {
            builder.addGeneric(extractGeneric(tp));
        }

        // Extract annotations
        for (AnnotationExpr ann : n.getAnnotations()) {
            builder.addStereotype(extractAnnotationAsStereotype(ann));
        }

        // Record components as fields
        n.getParameters().forEach(param -> {
            UmlField field = UmlField.builder()
                    .name(param.getNameAsString())
                    .type(param.getType().asString())
                    .visibility(Visibility.PRIVATE)
                    .isFinal(true)
                    .build();
            builder.addField(field);
        });

        // Extract additional methods
        for (MethodDeclaration method : n.getMethods()) {
            builder.addMethod(extractMethod(method));
        }

        UmlType type = builder.build();
        context.addType(type);

        // Detect implemented interfaces
        String typeFqn = type.getId();
        for (ClassOrInterfaceType implemented : n.getImplementedTypes()) {
            addInheritanceRelation(typeFqn, implemented, RelationKind.IMPLEMENTS);
        }
    }

    /**
     * Extract the package name from a type declaration.
     */
    private String getPackageName(TypeDeclaration<?> n) {
        return n.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(pd -> pd.getNameAsString())
                .orElse("");
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
                .collect(Collectors.toList());

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
                    .collect(Collectors.toList());
            return new UmlStereotype(name, values);
        } else if (ann instanceof SingleMemberAnnotationExpr single) {
            return new UmlStereotype(name, List.of(single.getMemberValue().toString()));
        }

        return new UmlStereotype(name);
    }

    /**
     * Resolves a JavaParser Type to its fully-qualified name.
     * Uses the symbol resolver when available, falls back to the simple name.
     * For generic types like List<Foo>, returns the full representation with resolved type arguments.
     *
     * @param type the JavaParser type to resolve
     * @return the fully-qualified type name, or simple name if resolution fails
     */
    private String resolveTypeFqn(Type type) {
        try {
            ResolvedType resolved = type.resolve();
            return resolveTypeToFqnString(resolved, type.asString());
        } catch (Exception e) {
            // Symbol resolution failed, fall back to source representation
            return type.asString();
        }
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
                if (i > 0) sb.append(", ");
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
    private UmlField extractField(FieldDeclaration field, VariableDeclarator var) {
        String name = var.getNameAsString();
        String type = resolveTypeFqn(var.getType());  // FQN for relation detection
        Visibility visibility = extractVisibility(field);

        boolean isStatic = field.isStatic();
        boolean isFinal = field.isFinal();

        UmlField.Builder builder = UmlField.builder()
                .name(name)
                .type(type)
                .visibility(visibility)
                .isStatic(isStatic)
                .isFinal(isFinal);

        // Add annotations
        for (AnnotationExpr ann : field.getAnnotations()) {
            builder.addAnnotation(ann.getNameAsString());
        }

        // Extract initializer if present (for default values)
        var.getInitializer().ifPresent(init -> {
            // Store as annotation for display purposes
            String initStr = init.toString();
            if (initStr.length() <= 50) {  // Truncate long initializers
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
        String returnType = resolveTypeFqn(method.getType());  // FQN for relation detection
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
        EnumSet<no.ntnu.eitri.model.Modifier> modifiers = EnumSet.noneOf(no.ntnu.eitri.model.Modifier.class);
        if (isStatic) modifiers.add(no.ntnu.eitri.model.Modifier.STATIC);
        if (isAbstract) modifiers.add(no.ntnu.eitri.model.Modifier.ABSTRACT);
        if (isFinal) modifiers.add(no.ntnu.eitri.model.Modifier.FINAL);
        if (method.isSynchronized()) modifiers.add(no.ntnu.eitri.model.Modifier.SYNCHRONIZED);
        if (method.isNative()) modifiers.add(no.ntnu.eitri.model.Modifier.NATIVE);
        if (method.isDefault()) modifiers.add(no.ntnu.eitri.model.Modifier.DEFAULT);
        builder.modifiers(modifiers);

        // Extract parameters with FQN types
        method.getParameters().forEach(param -> {
            UmlParameter umlParam = new UmlParameter(
                    param.getNameAsString(),
                    resolveTypeFqn(param.getType())  // FQN for relation detection
            );
            builder.addParameter(umlParam);
        });

        // Extract thrown exceptions with FQN
        method.getThrownExceptions().forEach(exc -> {
            builder.addThrownException(resolveTypeFqn(exc));
        });

        // Add annotations as stereotypes in method
        for (AnnotationExpr ann : method.getAnnotations()) {
            // Store significant annotations
            String annName = ann.getNameAsString();
            if (!annName.equals("Override")) {  // Skip @Override, too common
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
                .name(className)  // Constructor has class name
                .returnType("")    // No return type for constructors
                .visibility(visibility)
                .addAnnotation("constructor");

        // Extract parameters with FQN types
        ctor.getParameters().forEach(param -> {
            UmlParameter umlParam = new UmlParameter(
                    param.getNameAsString(),
                    resolveTypeFqn(param.getType())  // FQN for relation detection
            );
            builder.addParameter(umlParam);
        });

        // Extract thrown exceptions with FQN
        ctor.getThrownExceptions().forEach(exc -> {
            builder.addThrownException(resolveTypeFqn(exc));
        });

        return builder.build();
    }
}
