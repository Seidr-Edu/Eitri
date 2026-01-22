package no.ntnu.eitri.model;

/**
 * The kind/category of a UML type element.
 * Each kind maps to a specific PlantUML declaration keyword.
 */
public enum TypeKind {
    CLASS("class"),
    ABSTRACT_CLASS("abstract class"),
    INTERFACE("interface"),
    ENUM("enum"),
    ANNOTATION("annotation"),
    RECORD("class"); // PlantUML doesn't have record keyword; rendered as class with stereotype

    private final String plantUmlKeyword;

    TypeKind(String plantUmlKeyword) {
        this.plantUmlKeyword = plantUmlKeyword;
    }

    /**
     * Returns the PlantUML keyword for declaring this type.
     * @return the PlantUML declaration keyword
     */
    public String toPlantUml() {
        return plantUmlKeyword;
    }

    /**
     * Whether this kind represents an abstract type (abstract class or interface).
     * @return true if abstract
     */
    public boolean isAbstract() {
        return this == ABSTRACT_CLASS || this == INTERFACE;
    }
}
