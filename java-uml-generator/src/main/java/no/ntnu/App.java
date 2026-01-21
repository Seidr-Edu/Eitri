package no.ntnu;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
// import com.github.javaparser.ast.body.BodyDeclaration;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple CLI:
 *   java -jar java-uml-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
 *       --src /path/to/project/src/main/java \
 *       --out /path/to/output.puml
 */
public class App {

    public static void main(String[] args) throws IOException {
        Map<String, String> argMap = parseArgs(args);

        String srcDir = argMap.get("--src");
        String outFile = argMap.get("--out");

        if (srcDir == null || outFile == null) {
            System.err.println("Usage: java -jar java-uml-generator.jar --src <srcDir> --out <output.puml>");
            System.exit(1);
        }

        Path srcPath = new File(srcDir).toPath();
        if (!Files.isDirectory(srcPath)) {
            System.err.println("Source directory does not exist: " + srcDir);
            System.exit(1);
        }

        UmlModel model = new UmlModel();

        // Configure JavaParser to support modern Java features (switch expressions, etc.)
        StaticJavaParser.getParserConfiguration().setLanguageLevel(
            com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17);

        // Walk all .java files
        try (Stream<Path> paths = Files.walk(srcPath)) {
            List<Path> javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path p : javaFiles) {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(p);
                    cu.accept(new ClassCollectorVisitor(model), null);
                } catch (Exception e) {
                    System.err.println("Failed to parse " + p + ": " + e.getMessage());
                }
            }
        }

        // Write PlantUML
        writePlantUml(model, outFile);
        System.out.println("Wrote PlantUML to " + outFile);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                map.put(args[i], args[i + 1]);
                i++;
            }
        }
        return map;
    }

    private static void writePlantUml(UmlModel model, String outFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            String title = extractTitle(outFile);
            writer.write("@startuml "+ title +"\n");
            
            // First, declare all classes/interfaces/enums
            for (UmlType type : model.types.values()) {
                writer.write(type.toPlantUmlDeclaration());
                writer.newLine();
            }
            writer.newLine();

            // Then relations
            for (UmlRelation rel : model.relations) {
                writer.write(rel.toPlantUmlRelation());
                writer.newLine();
            }

            writer.write("@enduml\n");
        }
    }

    private static String extractTitle(String filePath) {
        // Extract filename from path
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String filename = (lastSeparator >= 0) ? filePath.substring(lastSeparator + 1) : filePath;
        
        // Remove .puml extension if present
        if (filename.endsWith(".puml")) filename = filename.substring(0, filename.length() - 5);
        
        return filename;
    }

    // ===== Model classes =====

    /**
     * Simple in-memory model of types and relations.
     */
    static class UmlModel {
        Map<String, UmlType> types = new LinkedHashMap<>();
        Set<UmlRelation> relations = new LinkedHashSet<>();

        UmlType getOrCreateType(String fullName, UmlType.Kind kind) {
            UmlType existing = types.get(fullName);
            if (existing != null) {
                return existing;
            }
            UmlType t = new UmlType(fullName, kind);
            types.put(fullName, t);
            return t;
        }

        void addRelation(UmlRelation relation) {
            relations.add(relation);
        }
    }

    static class UmlType {
        enum Kind { CLASS, INTERFACE, ENUM }

        String fullName; // e.g. com.example.foo.Bar
        Kind kind;
        List<String> fields = new ArrayList<>();
        List<String> methods = new ArrayList<>();

        UmlType(String fullName, Kind kind) {
            this.fullName = fullName;
            this.kind = kind;
        }

        String simpleName() {
            int idx = fullName.lastIndexOf('.');
            return (idx >= 0) ? fullName.substring(idx + 1) : fullName;
        }

        String toPlantUmlDeclaration() {
            StringBuilder sb = new StringBuilder();
            switch (kind) {
                case INTERFACE -> sb.append("interface ");
                case ENUM -> sb.append("enum ");
                case CLASS -> sb.append("class ");
            }
            sb.append(simpleName()).append(" {\n");
            for (String f : fields) {
                sb.append("  ").append(f).append("\n");
            }
            for (String m : methods) {
                sb.append("  ").append(m).append("\n");
            }
            sb.append("}\n");
            return sb.toString();
        }
    }

    static class UmlRelation {
        enum Type {
            EXTENDS, IMPLEMENTS, ASSOCIATION
        }

        String from; // full name
        String to;   // full name
        Type type;

        UmlRelation(String from, String to, Type type) {
            this.from = from;
            this.to = to;
            this.type = type;
        }

        String simple(String fullName) {
            int idx = fullName.lastIndexOf('.');
            return (idx >= 0) ? fullName.substring(idx + 1) : fullName;
        }

        String toPlantUmlRelation() {
            String f = cleanTypeName(simple(from));
            String t = cleanTypeName(simple(to));
            return switch (type) {
                case EXTENDS -> f + " --|> " + t;
                case IMPLEMENTS -> f + " ..|> " + t;
                case ASSOCIATION -> f + " --> " + t;
            };
        }

        private String cleanTypeName(String typeName) {
            // Remove generic parameters: Set<String> -> Set, List<CaseInfo> -> List
            int idx = typeName.indexOf('<');
            String cleaned = (idx >= 0) ? typeName.substring(0, idx) : typeName;
            // Remove array brackets: byte[] -> byte
            cleaned = cleaned.replace("[]", "");
            // Remove any stray > characters
            cleaned = cleaned.replace(">", "");
            return cleaned;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UmlRelation that = (UmlRelation) o;
            return Objects.equals(from, that.from) &&
                   Objects.equals(to, that.to) &&
                   type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, type);
        }
    }

    // ===== Visitor that fills the UmlModel =====

    static class ClassCollectorVisitor extends VoidVisitorAdapter<Void> {
        private final UmlModel model;

        ClassCollectorVisitor(UmlModel model) {
            this.model = model;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            String pkg = n.findCompilationUnit()
                    .flatMap(CompilationUnit::getPackageDeclaration)
                    .map(pd -> pd.getName().asString())
                    .orElse("");
            String fullName = pkg.isEmpty()
                    ? n.getNameAsString()
                    : pkg + "." + n.getNameAsString();

            UmlType.Kind kind = n.isInterface()
                    ? UmlType.Kind.INTERFACE
                    : UmlType.Kind.CLASS;

            UmlType type = model.getOrCreateType(fullName, kind);

            // Fields
            for (FieldDeclaration field : n.getFields()) {
                for (VariableDeclarator v : field.getVariables()) {
                    String typeName = v.getType().asString();
                    String visibility = toVisibilityPrefix(field);
                    type.fields.add(visibility + v.getNameAsString() + " : " + typeName);

                    // Association: this class "has-a" that type
                    String assocTo = qualifyTypeName(pkg, typeName);
                    model.addRelation(new UmlRelation(fullName, assocTo, UmlRelation.Type.ASSOCIATION));
                }
            }

            // Methods
            for (MethodDeclaration m : n.getMethods()) {
                String visibility = toVisibilityPrefix(m);
                String params = m.getParameters().stream()
                        .map(p -> p.getNameAsString() + " : " + p.getType().asString())
                        .collect(Collectors.joining(", "));
                String sig = visibility + m.getNameAsString() + "(" + params + ") : " + m.getType().asString();
                type.methods.add(sig);
            }

            // Inheritance / implementation
            n.getExtendedTypes().forEach(ext -> {
                String target = qualifyTypeName(pkg, ext.getNameAsString());
                model.addRelation(new UmlRelation(fullName, target, UmlRelation.Type.EXTENDS));
            });
            n.getImplementedTypes().forEach(impl -> {
                String target = qualifyTypeName(pkg, impl.getNameAsString());
                model.addRelation(new UmlRelation(fullName, target, UmlRelation.Type.IMPLEMENTS));
            });

            super.visit(n, arg); // continue to inner classes, etc.
        }

        @Override
        public void visit(EnumDeclaration n, Void arg) {
            String pkg = n.findCompilationUnit()
                    .flatMap(CompilationUnit::getPackageDeclaration)
                    .map(pd -> pd.getName().asString())
                    .orElse("");
            String fullName = pkg.isEmpty()
                    ? n.getNameAsString()
                    : pkg + "." + n.getNameAsString();

            UmlType type = model.getOrCreateType(fullName, UmlType.Kind.ENUM);
            super.visit(n, arg);
        }
private String toVisibilityPrefix(BodyDeclaration<?> decl) {
    if (decl instanceof NodeWithAccessModifiers) {
        NodeWithAccessModifiers<?> node = (NodeWithAccessModifiers<?>) decl;
        AccessSpecifier access = node.getAccessSpecifier();
        switch (access) {
            case PUBLIC:
                return "+ ";
            case PROTECTED:
                return "# ";
            case PRIVATE:
                return "- ";
            case NONE:           // package-private in your version
            default:
                return "~ ";
        }
    }
    return "~ ";
}


        private String qualifyTypeName(String currentPackage, String typeName) {
            // VERY simplistic: if it contains '.', treat as already-qualified.
            // Otherwise, assume same package (good enough for diagrams).
            if (typeName.contains(".")) {
                return typeName;
            }
            if (currentPackage == null || currentPackage.isEmpty()) {
                return typeName;
            }
            return currentPackage + "." + typeName;
        }
    }
}
