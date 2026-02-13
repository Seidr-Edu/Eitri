package no.ntnu.eitri.parser.resolution;

import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores discovered types and tracked source packages during parsing.
 */
public final class TypeRegistry {

    private final Map<String, UmlType> typesByFqn = new HashMap<>();
    private final Set<String> sourcePackages = new HashSet<>();

    public void addType(UmlType type) {
        String fqn = type.getFqn();
        if (typesByFqn.containsKey(fqn)) {
            throw new IllegalArgumentException("Type already registered: " + fqn);
        }
        typesByFqn.put(fqn, type);
        addSourcePackage(type.getPackageName());
    }

    public UmlType getType(String fqn) {
        return typesByFqn.get(fqn);
    }

    public boolean hasType(String fqn) {
        return typesByFqn.containsKey(fqn);
    }

    public void ensureTypeExists(String fqn) {
        if (fqn == null || fqn.isBlank() || typesByFqn.containsKey(fqn)) {
            return;
        }

        UmlType placeholder = UmlType.builder()
                .fqn(fqn)
                .simpleName(extractSimpleName(fqn))
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        typesByFqn.put(fqn, placeholder);
    }

    public Collection<UmlType> getTypes() {
        return typesByFqn.values();
    }

    public void addSourcePackage(String packageName) {
        if (packageName != null && !packageName.isBlank()) {
            sourcePackages.add(packageName);
        }
    }

    public Set<String> getSourcePackages() {
        return Collections.unmodifiableSet(sourcePackages);
    }

    private String extractSimpleName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;
    }
}
