package no.ntnu.eitri.visitor;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;

import java.util.stream.Collectors;

/**
 * JavaParser visitor that traverses the AST to collect UML-relevant information.
 * 
 * Uses the Visitor pattern to walk through the parsed Java source code.
 * VoidVisitorAdapter provides empty implementations for all node types,
 * allowing us to override only the ones we care about (classes, interfaces, enums).
 * 
 * For each type declaration found, this visitor:
 * 1. Creates/retrieves the corresponding UmlType in the model
 * 2. Extracts fields with visibility and type information
 * 3. Extracts methods with parameters and return types
 * 4. Records inheritance (extends) and implementation (implements) relationships
 * 5. Records associations based on field types
 */
public class ClassCollectorVisitor extends VoidVisitorAdapter<Void> {

    private final UmlModel model;

    public ClassCollectorVisitor(UmlModel model) {
        this.model = model;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        String pkg = getPackageName(n);
        String fullName = buildFullName(pkg, n.getNameAsString());

        UmlType.Kind kind = n.isInterface()
                ? UmlType.Kind.INTERFACE
                : UmlType.Kind.CLASS;

        UmlType type = model.getOrCreateType(fullName, kind);
        type.setLocal(true);

        collectFields(n, type, pkg, fullName);
        collectMethods(n, type);
        collectInheritance(n, pkg, fullName);

        super.visit(n, arg);
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        String pkg = getPackageName(n);
        String fullName = buildFullName(pkg, n.getNameAsString());

        UmlType type = model.getOrCreateType(fullName, UmlType.Kind.ENUM);
        type.setLocal(true);

        for (EnumConstantDeclaration constant : n.getEntries()) {
            type.addField(constant.getNameAsString());
        }

        super.visit(n, arg);
    }

    private String getPackageName(TypeDeclaration<?> n) {
        return n.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(pd -> pd.getName().asString())
                .orElse("");
    }

    private String buildFullName(String pkg, String simpleName) {
        return pkg.isEmpty() ? simpleName : pkg + "." + simpleName;
    }

    private void collectFields(ClassOrInterfaceDeclaration n, UmlType type, String pkg, String fullName) {
        for (FieldDeclaration field : n.getFields()) {
            for (VariableDeclarator v : field.getVariables()) {
                String typeName = v.getType().asString();
                String visibility = toVisibilityPrefix(field);
                type.addField(visibility + v.getNameAsString() + " : " + typeName);

                String assocTo = qualifyTypeName(pkg, typeName);
                model.addRelation(new UmlRelation(fullName, assocTo, UmlRelation.Type.ASSOCIATION));
            }
        }
    }

    private void collectMethods(ClassOrInterfaceDeclaration n, UmlType type) {
        for (MethodDeclaration m : n.getMethods()) {
            String visibility = toVisibilityPrefix(m);
            String params = m.getParameters().stream()
                    .map(p -> p.getNameAsString() + " : " + p.getType().asString())
                    .collect(Collectors.joining(", "));
            String sig = visibility + m.getNameAsString() + "(" + params + ") : " + m.getType().asString();
            type.addMethod(sig);
        }
    }

    private void collectInheritance(ClassOrInterfaceDeclaration n, String pkg, String fullName) {
        n.getExtendedTypes().forEach(ext -> {
            String target = qualifyTypeName(pkg, ext.getNameAsString());
            // Register the parent type - if extending, it could be a class or interface
            // We use the same kind as the current type (interface extends interface, class extends class)
            UmlType.Kind parentKind = n.isInterface() ? UmlType.Kind.INTERFACE : UmlType.Kind.CLASS;
            model.getOrCreateType(target, parentKind);
            model.addRelation(new UmlRelation(fullName, target, UmlRelation.Type.EXTENDS));
        });
        n.getImplementedTypes().forEach(impl -> {
            String target = qualifyTypeName(pkg, impl.getNameAsString());
            // Types referenced via 'implements' are always interfaces
            // Register them as such so they render correctly in PlantUML
            model.getOrCreateType(target, UmlType.Kind.INTERFACE);
            model.addRelation(new UmlRelation(fullName, target, UmlRelation.Type.IMPLEMENTS));
        });
    }

    /**
     * Converts Java access modifiers to PlantUML visibility symbols.
     * 
     * PlantUML uses these symbols to represent visibility:
     * - + public
     * - # protected
     * - - private
     * - ~ package-private (default)
     * 
     * @param decl a field or method declaration
     * @return the PlantUML visibility prefix with trailing space
     */
    private String toVisibilityPrefix(BodyDeclaration<?> decl) {
        // Pattern matching with instanceof to safely cast and extract
        if (decl instanceof NodeWithAccessModifiers<?> node) {
            AccessSpecifier access = node.getAccessSpecifier();
            return switch (access) {
                case PUBLIC -> "+ ";
                case PROTECTED -> "# ";
                case PRIVATE -> "- ";
                // NONE means package-private in Java
                default -> "~ ";
            };
        }
        return "~ ";
    }

    /**
     * Attempts to construct a fully-qualified type name.
     * 
     * This is a simplified heuristic that assumes types without dots
     * are in the same package as the current class. This works well
     * for project-internal references but may be incorrect for:
     * - Imported types from other packages
     * - Types from java.lang (String, Object, etc.)
     * 
     * For UML diagram purposes, this approximation is usually acceptable
     * since we filter out common Java types anyway.
     * 
     * @param currentPackage the package of the class being analyzed
     * @param typeName the type name as it appears in source code
     * @return best-effort fully-qualified name
     */
    private String qualifyTypeName(String currentPackage, String typeName) {
        // Already qualified (contains a dot)
        if (typeName.contains(".")) {
            return typeName;
        }
        // No package context available
        if (currentPackage == null || currentPackage.isEmpty()) {
            return typeName;
        }
        // Assume same package as current class
        return currentPackage + "." + typeName;
    }
}
