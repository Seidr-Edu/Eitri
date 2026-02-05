package no.ntnu.eitri.util;

import java.nio.file.Path;

/**
 * Utility for extracting normalized file extensions from paths or names.
 */
public final class PathExtension {

    private PathExtension() {
        // Utility class
    }

    public static String fromPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return null;
        }
        return fromFileName(path.getFileName().toString());
    }

    public static String fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return null;
        }
        String extension = fileName.substring(idx);
        return ExtensionNormalizer.normalizeExtension(extension);
    }
}
