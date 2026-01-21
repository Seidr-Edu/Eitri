package no.ntnu.eitri.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a UML type (class, interface, or enum).
 * 
 * Stores the type's metadata including its fully-qualified name,
 * kind (class/interface/enum), and its members (fields and methods)
 * in PlantUML notation.
 */
public class UmlType {

    /** The three kinds of types supported in UML class diagrams */
    public enum Kind {
        CLASS, INTERFACE, ENUM
    }

    /** Fully-qualified name, e.g., "no.ntnu.eitri.model.UmlType" */
    private final String fullName;
    
    private final Kind kind;
    
    /**
     * True if this type was found in the scanned source code.
     * External dependencies (e.g., java.util.List) are marked as non-local
     * and excluded from the diagram declarations.
     */
    private boolean local;
    
    /** Fields in PlantUML format, e.g., "+ name : String" */
    private final List<String> fields = new ArrayList<>();
    
    /** Methods in PlantUML format, e.g., "+ getName() : String" */
    private final List<String> methods = new ArrayList<>();

    public UmlType(String fullName, Kind kind) {
        this.fullName = fullName;
        this.kind = kind;
        this.local = false;
    }

    public String getFullName() {
        return fullName;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void addField(String field) {
        fields.add(field);
    }

    public void addMethod(String method) {
        methods.add(method);
    }

    public String getSimpleName() {
        int idx = fullName.lastIndexOf('.');
        return (idx >= 0) ? fullName.substring(idx + 1) : fullName;
    }

    /**
     * Generates the PlantUML declaration for this type.
     * 
     * Output format:
     * <pre>
     * class ClassName {
     *   + field : Type
     *   + method() : ReturnType
     * }
     * </pre>
     * 
     * @return PlantUML declaration string
     */
    public String toPlantUmlDeclaration() {
        StringBuilder sb = new StringBuilder();
        // PlantUML keyword based on type kind
        switch (kind) {
            case INTERFACE -> sb.append("interface ");
            case ENUM -> sb.append("enum ");
            case CLASS -> sb.append("class ");
        }
        // Use simple name to keep diagrams readable (no package prefix)
        sb.append(getSimpleName()).append(" {\n");
        for (String f : fields) {
            sb.append("  ").append(f).append("\n");
        }
        for (String m : methods) {
            sb.append("  ").append(m).append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Generates a minimal PlantUML declaration for external types.
     * 
     * Used for types referenced in inheritance relationships but not
     * defined in the scanned source. Declares only the type kind and name,
     * ensuring PlantUML renders them with the correct stereotype (interface vs class).
     * 
     * Output format: "interface IFoo" or "class Bar"
     * 
     * @return PlantUML type declaration without body
     */
    public String toPlantUmlDeclarationHeader() {
        StringBuilder sb = new StringBuilder();
        switch (kind) {
            case INTERFACE -> sb.append("interface ");
            case ENUM -> sb.append("enum ");
            case CLASS -> sb.append("class ");
        }
        sb.append(getSimpleName());
        return sb.toString();
    }
}
