package no.ntnu.eitri.writer;

import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * Writes a UmlModel to PlantUML format.
 * 
 * Generates a .puml file that can be rendered using PlantUML tools.
 * The output includes type declarations (classes, interfaces, enums)
 * and relationships (inheritance, implementation, associations).
 * 
 * Filters out common Java types to keep diagrams focused on
 * domain-specific classes rather than cluttering with String, List, etc.
 */
public class PlantUmlWriter {

    /**
     * Types to exclude from association relationships.
     * These are ubiquitous Java types that would create noise in diagrams.
     * We still allow inheritance/implementation relationships with these.
     */
    private static final Set<String> COMMON_TYPES = Set.of(
            // Primitives and wrappers
            "boolean", "byte", "char", "short", "int", "long", "float", "double",
            "Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float", "Double",
            "String", "Object", "Class", "Void",
            // Collections
            "List", "Set", "Map", "Collection", "Queue", "Deque",
            "ArrayList", "LinkedList", "HashSet", "TreeSet", "HashMap", "TreeMap",
            "LinkedHashMap", "LinkedHashSet", "Vector", "Stack",
            // Common utilities
            "Optional", "Stream", "Iterator", "Iterable", "Comparable", "Comparator",
            "StringBuilder", "StringBuffer", "Pattern", "Matcher"
    );

    public void write(UmlModel model, String outFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            String title = extractTitle(outFile);
            writer.write("@startuml " + title + "\n");

            // Declare all local types
            for (UmlType type : model.getTypes()) {
                if (type.isLocal()) {
                    writer.write(type.toPlantUmlDeclaration());
                    writer.newLine();
                }
            }
            writer.newLine();

            // Write relations
            for (UmlRelation rel : model.getRelations()) {
                if (shouldIncludeRelation(rel, model)) {
                    writer.write(rel.toPlantUmlRelation());
                    writer.newLine();
                }
            }

            writer.write("@enduml\n");
        }
    }

    /**
     * Determines whether a relationship should appear in the diagram.
     * 
     * Filtering strategy:
     * 1. Always include EXTENDS and IMPLEMENTS - these are fundamental to understanding class hierarchy
     * 2. For ASSOCIATION (field references):
     *    - Exclude common Java types (String, List, Map, etc.) to reduce noise
     *    - Include references to types defined in the scanned source
     *    - Include references to external types that aren't from java.* or javax.*
     * 
     * @param rel the relationship to evaluate
     * @param model the UML model for checking if target is local
     * @return true if the relationship should be included in output
     */
    private boolean shouldIncludeRelation(UmlRelation rel, UmlModel model) {
        // Always include inheritance and interface implementation
        if (rel.getType() != UmlRelation.Type.ASSOCIATION) {
            return true;
        }

        String targetType = rel.getTo();

        // Filter out String, List, Map, etc.
        if (isCommonJavaType(targetType)) {
            return false;
        }

        // Always include associations to locally-defined types
        UmlType targetTypeObj = model.getType(targetType);
        if (targetTypeObj != null && targetTypeObj.isLocal()) {
            return true;
        }

        // Include external dependencies that aren't standard Java libraries
        return !targetType.startsWith("java.") && !targetType.startsWith("javax.");
    }

    private boolean isCommonJavaType(String fullTypeName) {
        String simpleName = fullTypeName;
        int lastDot = fullTypeName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = fullTypeName.substring(lastDot + 1);
        }

        int genericStart = simpleName.indexOf('<');
        if (genericStart >= 0) {
            simpleName = simpleName.substring(0, genericStart);
        }

        return COMMON_TYPES.contains(simpleName) 
                || fullTypeName.startsWith("java.")
                || fullTypeName.startsWith("javax.");
    }

    private String extractTitle(String filePath) {
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String filename = (lastSeparator >= 0) ? filePath.substring(lastSeparator + 1) : filePath;

        if (filename.endsWith(".puml")) {
            filename = filename.substring(0, filename.length() - 5);
        }

        return filename;
    }
}
