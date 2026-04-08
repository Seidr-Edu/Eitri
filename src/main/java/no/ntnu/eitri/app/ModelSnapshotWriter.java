package no.ntnu.eitri.app;

import no.ntnu.eitri.model.Modifier;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlGeneric;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlParameter;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes a deterministic JSON snapshot of the semantic UML model.
 */
final class ModelSnapshotWriter {

    static final String SCHEMA_VERSION = "uml_model_snapshot.v1";
    private static final Comparator<UmlField> FIELD_COMPARATOR = Comparator
            .comparing(UmlField::getName)
            .thenComparing(UmlField::getType)
            .thenComparing(field -> enumName(field.getVisibility()));
    private static final Comparator<UmlMethod> METHOD_COMPARATOR = Comparator
            .comparing(UmlMethod::getName)
            .thenComparing(method -> methodParameterTypes(method).toString())
            .thenComparing(UmlMethod::getReturnType)
            .thenComparing(method -> enumName(method.getVisibility()))
            .thenComparing(UmlMethod::isConstructor);
    private static final Comparator<UmlRelation> RELATION_COMPARATOR = Comparator
            .comparing((UmlRelation relation) -> enumName(relation.getKind()))
            .thenComparing(UmlRelation::getFromTypeFqn)
            .thenComparing(UmlRelation::getToTypeFqn)
            .thenComparing(relation -> nullable(relation.getFromMember()))
            .thenComparing(relation -> nullable(relation.getToMember()))
            .thenComparing(relation -> nullable(relation.getLabel()))
            .thenComparing(relation -> nullable(relation.getFromMultiplicity()))
            .thenComparing(relation -> nullable(relation.getToMultiplicity()));

    private ModelSnapshotWriter() {
    }

    static void write(UmlModel model, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, toJson(document(model)) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    static Path defaultPath(Path diagramPath) {
        Path parent = diagramPath.getParent();
        return parent == null ? Path.of("model_snapshot.json") : parent.resolve("model_snapshot.json");
    }

    static Map<String, Object> document(UmlModel model) {
        LinkedHashMap<String, Object> document = new LinkedHashMap<>();
        document.put("schema_version", SCHEMA_VERSION);
        document.put("model_name", model.getName());
        document.put("packages", model.getPackages());
        document.put("types", typeDocuments(model));
        document.put("fields", fieldDocuments(model));
        document.put("methods", methodDocuments(model));
        document.put("relations", relationDocuments(model));
        return document;
    }

    private static List<Map<String, Object>> typeDocuments(UmlModel model) {
        List<Map<String, Object>> types = new ArrayList<>();
        for (UmlType type : model.getTypesSorted()) {
            LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
            entry.put("fqn", type.getFqn());
            entry.put("simple_name", type.getSimpleName());
            entry.put("package_name", type.getPackageName());
            entry.put("kind", enumName(type.getKind()));
            entry.put("visibility", enumName(type.getVisibility()));
            entry.put("outer_type_fqn", type.getOuterTypeFqn());
            entry.put("generics", genericDocuments(type.getGenerics()));
            types.add(entry);
        }
        return types;
    }

    private static List<Map<String, Object>> fieldDocuments(UmlModel model) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (UmlType type : model.getTypesSorted()) {
            List<UmlField> sortedFields = type.getFields().stream()
                    .sorted(FIELD_COMPARATOR)
                    .toList();
            for (UmlField field : sortedFields) {
                LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
                entry.put("owner_fqn", type.getFqn());
                entry.put("name", field.getName());
                entry.put("type", field.getType());
                entry.put("visibility", enumName(field.getVisibility()));
                entry.put("modifiers", modifierNames(field.getModifiers()));
                entry.put("read_only", field.isReadOnly());
                fields.add(entry);
            }
        }
        return fields;
    }

    private static List<Map<String, Object>> methodDocuments(UmlModel model) {
        List<Map<String, Object>> methods = new ArrayList<>();
        for (UmlType type : model.getTypesSorted()) {
            List<UmlMethod> sortedMethods = type.getMethods().stream()
                    .sorted(METHOD_COMPARATOR)
                    .toList();
            for (UmlMethod method : sortedMethods) {
                LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
                entry.put("owner_fqn", type.getFqn());
                entry.put("name", method.getName());
                entry.put("parameter_types", methodParameterTypes(method));
                entry.put("return_type", method.getReturnType());
                entry.put("visibility", enumName(method.getVisibility()));
                entry.put("modifiers", modifierNames(method.getModifiers()));
                entry.put("constructor", method.isConstructor());
                entry.put("thrown_exceptions", method.getThrownExceptions().stream().sorted().toList());
                entry.put("generic_arity", method.getGenerics().size());
                methods.add(entry);
            }
        }
        return methods;
    }

    private static List<Map<String, Object>> relationDocuments(UmlModel model) {
        List<Map<String, Object>> relations = new ArrayList<>();
        for (UmlRelation relation : model.getRelations().stream().sorted(RELATION_COMPARATOR).toList()) {
            LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
            entry.put("kind", enumName(relation.getKind()));
            entry.put("from_type_fqn", relation.getFromTypeFqn());
            entry.put("to_type_fqn", relation.getToTypeFqn());
            entry.put("from_member", relation.getFromMember());
            entry.put("to_member", relation.getToMember());
            entry.put("label", relation.getLabel());
            entry.put("from_multiplicity", relation.getFromMultiplicity());
            entry.put("to_multiplicity", relation.getToMultiplicity());
            relations.add(entry);
        }
        return relations;
    }

    private static List<Map<String, Object>> genericDocuments(List<UmlGeneric> generics) {
        return generics.stream()
                .sorted(Comparator.comparing(UmlGeneric::identifier)
                        .thenComparing(generic -> nullable(generic.bounds())))
                .<Map<String, Object>>map(generic -> {
                    LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
                    entry.put("identifier", generic.identifier());
                    entry.put("bounds", generic.bounds());
                    return entry;
                })
                .toList();
    }

    private static List<String> modifierNames(Collection<Modifier> modifiers) {
        return modifiers.stream()
                .map(ModelSnapshotWriter::enumName)
                .sorted()
                .toList();
    }

    private static List<String> methodParameterTypes(UmlMethod method) {
        return method.getParameters().stream()
                .map(UmlParameter::type)
                .toList();
    }

    private static String enumName(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String nullable(String value) {
        return value != null ? value : "";
    }

    private static String toJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, value);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String stringValue) {
            builder.append('"').append(escape(stringValue)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        if (value instanceof Map<?, ?> rawMap) {
            builder.append('{');
            Iterator<Map.Entry<Object, Object>> iterator = ((Map<Object, Object>) rawMap).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, Object> entry = iterator.next();
                builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                appendJson(builder, entry.getValue());
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                appendJson(builder, iterator.next());
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return;
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
