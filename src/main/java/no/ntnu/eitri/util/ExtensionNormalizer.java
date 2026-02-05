package no.ntnu.eitri.util;

/**
 * Normalizes file extension identifiers.
 */
public final class ExtensionNormalizer {

    private ExtensionNormalizer() {
        // Utility class
    }

    public static String normalizeExtension(String extension) {
        if (extension == null) return null;
        String trimmed = extension.trim().toLowerCase();
        if (trimmed.isEmpty()) return null;
        return trimmed.startsWith(".") ? trimmed : "." + trimmed;
    }
}
