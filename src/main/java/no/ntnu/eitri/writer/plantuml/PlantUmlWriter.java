package no.ntnu.eitri.writer.plantuml;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.config.LayoutDirection;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import no.ntnu.eitri.util.PackageClassifier;
import no.ntnu.eitri.writer.DiagramWriter;
import no.ntnu.eitri.writer.WriteException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PlantUML class diagram writer.
 * 
 * <p>
 * This writer generates PlantUML class diagram syntax from a UmlModel,
 * applying all configuration options for filtering and display.
 */
public class PlantUmlWriter implements DiagramWriter {

    private static final String NAME = "PlantUML";
    private static final String FILE_EXTENSION = ".puml";
    private final PlantUmlRenderer renderer = new PlantUmlRenderer();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
    }

    @Override
    public void write(UmlModel model, EitriConfig config, Path outputPath) throws WriteException {
        try {
            // Ensure parent directory exists
            Path parent = outputPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                write(model, config, writer);
            }
        } catch (IOException e) {
            throw new WriteException("Failed to write PlantUML file", outputPath, e);
        }
    }

    @Override
    public void write(UmlModel model, EitriConfig config, Writer writer) throws IOException {
        String content = render(model, config);
        writer.write(content);
    }

    @Override
    public String render(UmlModel model, EitriConfig config) {
        StringWriter sw = new StringWriter();

        try {
            renderTo(model, config, sw);
        } catch (IOException e) {
            // StringWriter doesn't throw IOException
            throw new WriteException("Unexpected IO error", e);
        }

        return sw.toString();
    }

    /**
     * Renders the model to the given writer.
     */
    private void renderTo(UmlModel model, EitriConfig config, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        RenderContext context = buildRenderContext(model, config);
        renderHeader(model, config, sb);
        renderTypes(model, config, context, sb);
        renderRelations(model, config, context, sb);
        renderFooter(sb);

        writer.write(sb.toString());
    }

    private void renderHeader(UmlModel model, EitriConfig config, StringBuilder sb) {
        sb.append("@startuml");
        if (model.getName() != null && !model.getName().isBlank()) {
            sb.append(" ").append(model.getName());
        }
        sb.append("\n\n");

        if (config.getDirection() != null) {
            sb.append(toPlantUmlDirection(config.getDirection())).append("\n\n");
        }

        renderGlobalSettings(config, sb);
    }

    private void renderTypes(UmlModel model, EitriConfig config, RenderContext context, StringBuilder sb) {
        renderTypesGroupedByPackage(model, config, context, sb);
        sb.append("\n");
    }

    private void renderRelations(UmlModel model, EitriConfig config, RenderContext context, StringBuilder sb) {
        Set<String> renderedRelationLines = new LinkedHashSet<>();
        for (UmlRelation relation : model.getRelations()) {
            if (shouldRenderRelation(relation, config, context.nestedTypeFqns(), context.renderedTypeFqns())) {
                renderedRelationLines.add(renderRelation(relation, config, context));
            }
        }
        renderedRelationLines.forEach(line -> sb.append(line).append("\n"));
    }

    private void renderFooter(StringBuilder sb) {
        sb.append("\n@enduml\n");
    }

    private RenderContext buildRenderContext(UmlModel model, EitriConfig config) {
        Map<String, String> typeNames = new HashMap<>();
        Map<String, UmlType> typesByFqn = new HashMap<>();
        for (UmlType type : model.getTypes()) {
            typeNames.put(type.getFqn(), renderer.displayNameForType(type));
            typesByFqn.put(type.getFqn(), type);
        }

        Set<String> linkedTypes = collectLinkedTypes(model);
        Set<String> sourcePackages = model.getSourcePackages();
        Set<String> renderedTypeFqns = model.getTypes().stream()
                .filter(t -> shouldRenderType(t, config, linkedTypes, sourcePackages))
                .map(UmlType::getFqn)
                .collect(Collectors.toSet());
        Set<String> nestedTypeFqns = collectNestedTypeFqns(model);

        return new RenderContext(typeNames, renderedTypeFqns, nestedTypeFqns, linkedTypes, typesByFqn);
    }

    /**
     * Renders global PlantUML settings based on configuration.
     */
    private void renderGlobalSettings(EitriConfig config, StringBuilder sb) {
        if (config.isHideCircle()) {
            sb.append("hide circle\n");
        }
        if (config.isHideEmptyFields()) {
            sb.append("hide empty fields\n");
        }
        if (config.isHideEmptyMethods()) {
            sb.append("hide empty methods\n");
        }
        if (config.isHideEmptyMembers()) {
            sb.append("hide empty members\n");
        }
        if (config.getGroupInheritance() > 1) {
            sb.append("skinparam groupInheritance ").append(config.getGroupInheritance()).append("\n");
        }
        if (config.getClassAttributeIconSize() != 8 && config.getClassAttributeIconSize() > -1) { // 8 is PlantUML
                                                                                                  // default
            sb.append("skinparam classAttributeIconSize ").append(config.getClassAttributeIconSize()).append("\n");
        }
        if (config.isShowGenerics()) {
            sb.append("skinparam genericDisplay old\n");
        }
        sb.append("\n");
    }

    /**
     * Renders types grouped by their package using PlantUML package syntax.
     */
    private void renderTypesGroupedByPackage(UmlModel model, EitriConfig config,
            RenderContext context, StringBuilder sb) {
        Set<String> sourcePackages = model.getSourcePackages();

        // Group types by package
        Map<String, List<UmlType>> byPackage = model.getTypes().stream()
                .filter(t -> shouldRenderType(t, config, context.linkedTypes(), sourcePackages))
                .collect(Collectors.groupingBy(
                        t -> t.getPackageName() != null ? t.getPackageName() : "",
                        Collectors.toList()));

        // Sort packages for consistent output
        List<String> packages = new ArrayList<>(byPackage.keySet());
        Collections.sort(packages);

        for (String pkg : packages) {
            List<UmlType> types = byPackage.get(pkg);
            if (types == null || types.isEmpty()) {
                continue;
            }

            // Open package block (skip for default/empty package)
            if (!pkg.isEmpty()) {
                sb.append("package ").append(pkg).append(" {\n");
            }

            // Render types in this package
            for (UmlType type : types) {
                if (!pkg.isEmpty()) {
                    sb.append("  "); // Indent within package
                }
                renderType(type, config, sb);
                sb.append("\n");
            }

            // Close package block
            if (!pkg.isEmpty()) {
                sb.append("}\n\n");
            }
        }
    }

    /**
     * Collects all types that participate in at least one relation.
     */
    private Set<String> collectLinkedTypes(UmlModel model) {
        Set<String> linked = new HashSet<>();
        for (UmlRelation relation : model.getRelations()) {
            linked.add(relation.getFromTypeFqn());
            linked.add(relation.getToTypeFqn());
        }
        return linked;
    }

    private Set<String> collectNestedTypeFqns(UmlModel model) {
        return model.getTypes().stream()
                .filter(UmlType::isNested)
                .map(UmlType::getFqn)
                .collect(Collectors.toSet());
    }

    /**
     * Determines if a type should be rendered based on configuration.
     */
    private boolean shouldRenderType(UmlType type, EitriConfig config, Set<String> linkedTypes,
            Set<String> sourcePackages) {
        if (!config.isShowNested() && type.isNested()) {
            return false;
        }

        // Package-based filtering
        String pkg = type.getPackageName();
        if (config.isHideCommonPackages() && PackageClassifier.isCommonPackage(pkg)) {
            return false;
        }
        if (config.isHideExternalPackages() && PackageClassifier.isExternalPackage(pkg, sourcePackages)) {
            return false;
        }
        if (config.isHideSiblingPackages() && PackageClassifier.isSiblingPackage(pkg, sourcePackages)) {
            return false;
        }

        // Check hideUnlinked
        return !config.isHideUnlinked() || linkedTypes.contains(type.getFqn());
    }

    /**
     * Determines if a relation should be rendered based on configuration.
     * Relations to or from hidden types (package-filtered, nested, etc.) are
     * excluded.
     */
    private boolean shouldRenderRelation(UmlRelation relation, EitriConfig config,
            Set<String> nestedTypeFqns, Set<String> renderedTypeFqns) {
        // Skip relations involving types that were filtered out
        if (!renderedTypeFqns.contains(relation.getFromTypeFqn())
                || !renderedTypeFqns.contains(relation.getToTypeFqn())) {
            return false;
        }

        if (!config.isShowNested()
                && (nestedTypeFqns.contains(relation.getFromTypeFqn())
                        || nestedTypeFqns.contains(relation.getToTypeFqn()))) {
            return false;
        }

        RelationKind kind = relation.getKind();

        return switch (kind) {
            case EXTENDS -> config.isShowInheritance();
            case IMPLEMENTS -> config.isShowImplements();
            case COMPOSITION -> config.isShowComposition();
            case AGGREGATION -> config.isShowAggregation();
            case ASSOCIATION -> config.isShowAssociation();
            case DEPENDENCY -> config.isShowDependency();
            case NESTED -> config.isShowNested();
        };
    }

    /**
     * Determines if a member should be rendered based on visibility config.
     */
    private boolean shouldRenderMember(Visibility visibility, EitriConfig config) {
        return switch (visibility) {
            case PRIVATE -> !config.isHidePrivate();
            case PROTECTED -> !config.isHideProtected();
            case PACKAGE -> !config.isHidePackage();
            case PUBLIC -> true;
        };
    }

    /**
     * Renders a type declaration.
     */
    private void renderType(UmlType type, EitriConfig config, StringBuilder sb) {
        // Type declaration line
        sb.append(renderer.renderTypeDeclaration(type));
        sb.append(" {\n");

        // Fields
        if (!config.isHideFields()) {
            for (UmlField field : type.getFields()) {
                if (shouldRenderMember(field.getVisibility(), config)) {
                    sb.append("    ").append(renderer.renderField(field, config.isShowReadOnly())).append("\n");
                }
            }
        }

        // Methods
        if (!config.isHideMethods()) {
            for (UmlMethod method : type.getMethods()) {
                if (shouldRenderMember(method.getVisibility(), config)) {
                    sb.append("    ").append(renderer.renderMethod(method, config.isShowVoidReturnTypes()))
                            .append("\n");
                }
            }
        }

        sb.append("}\n");
    }

    /**
     * Renders a relation.
     */
    private String renderRelation(UmlRelation relation, EitriConfig config, RenderContext context) {
        String fromName = context.typeNames().getOrDefault(relation.getFromTypeFqn(), relation.getFromTypeFqn());
        String toName = context.typeNames().getOrDefault(relation.getToTypeFqn(), relation.getToTypeFqn());
        UmlRelation effectiveRelation = withEffectiveRelationLabel(relation, config, context.typesByFqn());
        return renderer.renderRelation(effectiveRelation, fromName, toName, config.isShowLabels(),
                config.isShowMultiplicities());
    }

    private UmlRelation withEffectiveRelationLabel(UmlRelation relation, EitriConfig config, Map<String, UmlType> typesByFqn) {
        String label = resolveFallbackRelationLabel(relation, config, typesByFqn);
        if (Objects.equals(label, relation.getLabel())) {
            return relation;
        }
        return UmlRelation.builder()
                .fromTypeFqn(relation.getFromTypeFqn())
                .toTypeFqn(relation.getToTypeFqn())
                .kind(relation.getKind())
                .label(label)
                .fromMultiplicity(relation.getFromMultiplicity())
                .toMultiplicity(relation.getToMultiplicity())
                .fromMember(relation.getFromMember())
                .toMember(relation.getToMember())
                .build();
    }

    private String resolveFallbackRelationLabel(UmlRelation relation, EitriConfig config, Map<String, UmlType> typesByFqn) {
        if (!config.isShowLabels()) {
            return relation.getLabel();
        }
        if (relation.getLabel() != null && !relation.getLabel().isBlank()) {
            return relation.getLabel();
        }
        if (relation.isMemberRelation()) {
            return null;
        }
        String fromMember = relation.getFromMember();
        if (fromMember == null || fromMember.isBlank()) {
            return null;
        }
        UmlType fromType = typesByFqn.get(relation.getFromTypeFqn());
        if (fromType == null || !isVisibleMember(fromType, fromMember, config)) {
            return null;
        }
        return fromMember;
    }

    private boolean isVisibleMember(UmlType ownerType, String memberName, EitriConfig config) {
        if (ownerType.getFields().stream()
                .anyMatch(field -> field.getName().equals(memberName) && shouldRenderMember(field.getVisibility(), config))) {
            return true;
        }
        return ownerType.getMethods().stream()
                .anyMatch(method -> method.getName().equals(memberName) && shouldRenderMember(method.getVisibility(), config));
    }

    private String toPlantUmlDirection(LayoutDirection direction) {
        return switch (direction) {
            case TOP_TO_BOTTOM -> "top to bottom direction";
            case LEFT_TO_RIGHT -> "left to right direction";
        };
    }

    private record RenderContext(
            Map<String, String> typeNames,
            Set<String> renderedTypeFqns,
            Set<String> nestedTypeFqns,
            Set<String> linkedTypes,
            Map<String, UmlType> typesByFqn) {
    }
}
