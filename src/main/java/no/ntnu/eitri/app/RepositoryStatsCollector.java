package no.ntnu.eitri.app;

import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Derives repository-level statistics from parsed sources and the UML model.
 */
final class RepositoryStatsCollector {

    private RepositoryStatsCollector() {
    }

    static RepositoryStats collect(UmlModel model, List<Path> sourcePaths, List<String> supportedExtensions) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(sourcePaths, "sourcePaths");

        LinkedHashMap<String, Integer> typeKindCounts = new LinkedHashMap<>();
        Arrays.stream(TypeKind.values())
                .forEach(kind -> typeKindCounts.put(kind.name().toLowerCase(Locale.ROOT), 0));

        int nestedTypeCount = 0;
        LinkedHashMap<String, Integer> packageTypeCounts = new LinkedHashMap<>();
        for (UmlType type : model.getTypesSorted()) {
            typeKindCounts.computeIfPresent(type.getKind().name().toLowerCase(Locale.ROOT), (_key, count) -> count + 1);
            if (type.isNested()) {
                nestedTypeCount++;
            }

            String packageName = type.getPackageName();
            if (packageName == null || packageName.isBlank()) {
                continue;
            }
            packageTypeCounts.merge(packageName, 1, Integer::sum);
        }

        List<String> packages = new ArrayList<>(packageTypeCounts.keySet());
        packages.sort(Comparator.naturalOrder());
        LinkedHashMap<String, Integer> sortedPackageTypeCounts = new LinkedHashMap<>();
        for (String packageName : packages) {
            sortedPackageTypeCounts.put(packageName, packageTypeCounts.get(packageName));
        }

        return new RepositoryStats(
                sourcePaths.size(),
                countSourceFiles(sourcePaths, supportedExtensions),
                model.getTypes().size(),
                model.getTypes().size() - nestedTypeCount,
                nestedTypeCount,
                packages.size(),
                packages,
                sortedPackageTypeCounts,
                typeKindCounts);
    }

    private static int countSourceFiles(List<Path> sourcePaths, List<String> supportedExtensions) {
        Set<Path> files = new LinkedHashSet<>();
        for (Path sourcePath : sourcePaths) {
            if (sourcePath == null || !Files.exists(sourcePath)) {
                continue;
            }

            Path normalized = sourcePath.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                if (matchesSupportedExtension(normalized, supportedExtensions)) {
                    files.add(normalized);
                }
                continue;
            }

            try (Stream<Path> stream = Files.walk(normalized)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> matchesSupportedExtension(path, supportedExtensions))
                        .map(path -> path.toAbsolutePath().normalize())
                        .forEach(files::add);
            } catch (IOException _ignored) {
                // Keep stats best effort; parsing already succeeded, so report what we can.
            }
        }
        return files.size();
    }

    private static boolean matchesSupportedExtension(Path path, List<String> supportedExtensions) {
        if (supportedExtensions == null || supportedExtensions.isEmpty()) {
            return true;
        }

        String filename = path.getFileName() != null ? path.getFileName().toString().toLowerCase(Locale.ROOT) : "";
        for (String extension : supportedExtensions) {
            if (extension == null || extension.isBlank()) {
                continue;
            }
            if (filename.endsWith(extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
