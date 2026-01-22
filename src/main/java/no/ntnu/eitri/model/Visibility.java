package no.ntnu.eitri.model;

/**
 * Visibility modifiers for UML elements.
 * Maps to PlantUML visibility prefixes: + (public), - (private), # (protected), ~ (package).
 */
public enum Visibility {
    PUBLIC("+"),
    PRIVATE("-"),
    PROTECTED("#"),
    PACKAGE("~");

    private final String plantUmlSymbol;

    Visibility(String plantUmlSymbol) {
        this.plantUmlSymbol = plantUmlSymbol;
    }

    /**
     * Returns the PlantUML symbol for this visibility.
     * @return the symbol (+, -, #, or ~)
     */
    public String toPlantUml() {
        return plantUmlSymbol;
    }
}
