package no.ntnu.eitri.config;

/**
 * Diagram layout direction.
 */
public enum LayoutDirection {
    TOP_TO_BOTTOM,
    LEFT_TO_RIGHT;

    /**
     * Parses a direction from string (case-insensitive).
     * Accepts: "tb", "top-to-bottom", "lr", "left-to-right"
     */
    public static LayoutDirection fromString(String value) {
        if (value == null) {
            return TOP_TO_BOTTOM;
        }
        String normalized = value.toLowerCase().trim();
        return switch (normalized) {
            case "lr", "left-to-right", "lefttoright", "ltr" -> LEFT_TO_RIGHT;
            case "tb", "top-to-bottom", "toptobottom", "ttb" -> TOP_TO_BOTTOM;
            default -> TOP_TO_BOTTOM;
        };
    }
}
