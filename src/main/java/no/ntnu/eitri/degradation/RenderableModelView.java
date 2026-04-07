package no.ntnu.eitri.degradation;

import no.ntnu.eitri.config.PlantUmlConfig;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import no.ntnu.eitri.util.PackageClassifier;
import no.ntnu.eitri.writer.plantuml.PlantUmlRenderer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal view of the semantic model after PlantUML config filtering.
 *
 * <p>
 * This mirrors the writer's filtering logic closely enough for degradation
 * candidate discovery without changing the existing writer implementation.
 */
final class RenderableModelView {

    private final PlantUmlConfig config;
    private final Map<String, UmlType> typesByFqn;
    private final Set<String> renderedTypeFqns;
    private final Set<String> nestedTypeFqns;
    private final Set<String> sourcePackages;
    private final List<UmlRelation> renderedRelations;
    private final Map<String, Integer> visibleFieldCounts;
    private final Map<String, Integer> visibleMethodCounts;
    private final Map<String, Integer> renderedRelationCounts;
    private final PlantUmlRenderer renderer;

    private RenderableModelView(
            PlantUmlConfig config,
            Map<String, UmlType> typesByFqn,
            Set<String> renderedTypeFqns,
            Set<String> nestedTypeFqns,
            Set<String> sourcePackages,
            List<UmlRelation> renderedRelations,
            Map<String, Integer> visibleFieldCounts,
            Map<String, Integer> visibleMethodCounts,
            Map<String, Integer> renderedRelationCounts,
            PlantUmlRenderer renderer) {
        this.config = config;
        this.typesByFqn = typesByFqn;
        this.renderedTypeFqns = renderedTypeFqns;
        this.nestedTypeFqns = nestedTypeFqns;
        this.sourcePackages = sourcePackages;
        this.renderedRelations = renderedRelations;
        this.visibleFieldCounts = visibleFieldCounts;
        this.visibleMethodCounts = visibleMethodCounts;
        this.renderedRelationCounts = renderedRelationCounts;
        this.renderer = renderer;
    }

    static RenderableModelView analyze(UmlModel model, PlantUmlConfig config) {
        PlantUmlRenderer renderer = new PlantUmlRenderer();
        Map<String, UmlType> typesByFqn = new HashMap<>();
        for (UmlType type : model.getTypes()) {
            typesByFqn.put(type.getFqn(), type);
        }

        Set<String> linkedTypes = collectLinkedTypes(model);
        Set<String> nestedTypeFqns = collectNestedTypeFqns(model);
        Set<String> sourcePackages = model.getSourcePackages();

        Set<String> renderedTypeFqns = new HashSet<>();
        for (UmlType type : model.getTypes()) {
            if (shouldRenderType(type, config, linkedTypes, sourcePackages)) {
                renderedTypeFqns.add(type.getFqn());
            }
        }

        List<UmlRelation> renderedRelations = model.getRelationsSorted().stream()
                .filter(relation -> shouldRenderRelation(
                        relation,
                        config,
                        renderedTypeFqns,
                        nestedTypeFqns,
                        typesByFqn,
                        sourcePackages,
                        renderer))
                .toList();

        Map<String, Integer> visibleFieldCounts = new LinkedHashMap<>();
        Map<String, Integer> visibleMethodCounts = new LinkedHashMap<>();
        for (UmlType type : model.getTypesSorted()) {
            if (!renderedTypeFqns.contains(type.getFqn())) {
                continue;
            }
            int fieldCount = 0;
            for (UmlField field : type.getFields()) {
                if (shouldRenderMember(field.getVisibility(), config)) {
                    fieldCount++;
                }
            }
            int methodCount = 0;
            for (UmlMethod method : type.getMethods()) {
                if (shouldRenderMember(method.getVisibility(), config)) {
                    methodCount++;
                }
            }
            visibleFieldCounts.put(type.getFqn(), fieldCount);
            visibleMethodCounts.put(type.getFqn(), methodCount);
        }

        Map<String, Integer> renderedRelationCounts = new LinkedHashMap<>();
        for (UmlRelation relation : renderedRelations) {
            increment(renderedRelationCounts, relation.getFromTypeFqn(), typesByFqn);
            increment(renderedRelationCounts, relation.getToTypeFqn(), typesByFqn);
        }

        return new RenderableModelView(
                config,
                typesByFqn,
                renderedTypeFqns,
                nestedTypeFqns,
                sourcePackages,
                renderedRelations,
                visibleFieldCounts,
                visibleMethodCounts,
                renderedRelationCounts,
                renderer);
    }

    private static void increment(Map<String, Integer> counts, String typeFqn, Map<String, UmlType> typesByFqn) {
        if (!typesByFqn.containsKey(typeFqn)) {
            return;
        }
        counts.merge(typeFqn, 1, Integer::sum);
    }

    boolean isRenderedType(String typeFqn) {
        return renderedTypeFqns.contains(typeFqn);
    }

    boolean isVisibleField(UmlField field) {
        return shouldRenderMember(field.getVisibility(), config);
    }

    boolean isVisibleMethod(UmlMethod method) {
        return shouldRenderMember(method.getVisibility(), config);
    }

    boolean isModeledType(String typeFqn) {
        return typesByFqn.containsKey(typeFqn);
    }

    List<UmlRelation> renderedRelations() {
        return renderedRelations;
    }

    Map<String, Integer> visibleFieldCounts() {
        return visibleFieldCounts;
    }

    Map<String, Integer> visibleMethodCounts() {
        return visibleMethodCounts;
    }

    Map<String, Integer> renderedRelationCounts() {
        return renderedRelationCounts;
    }

    Set<String> renderedTypeFqns() {
        return renderedTypeFqns;
    }

    private static Set<String> collectLinkedTypes(UmlModel model) {
        Set<String> linked = new HashSet<>();
        for (UmlRelation relation : model.getRelations()) {
            linked.add(relation.getFromTypeFqn());
            linked.add(relation.getToTypeFqn());
        }
        return linked;
    }

    private static Set<String> collectNestedTypeFqns(UmlModel model) {
        Set<String> nested = new HashSet<>();
        for (UmlType type : model.getTypes()) {
            if (type.isNested()) {
                nested.add(type.getFqn());
            }
        }
        return nested;
    }

    private static boolean shouldRenderType(
            UmlType type,
            PlantUmlConfig config,
            Set<String> linkedTypes,
            Set<String> sourcePackages) {
        if (!config.showNested() && type.isNested()) {
            return false;
        }

        String pkg = type.getPackageName();
        if (config.hideCommonPackages() && PackageClassifier.isCommonPackage(pkg)) {
            return false;
        }
        if (config.hideExternalPackages() && PackageClassifier.isExternalPackage(pkg, sourcePackages)) {
            return false;
        }
        if (config.hideSiblingPackages() && PackageClassifier.isSiblingPackage(pkg, sourcePackages)) {
            return false;
        }

        return config.showUnlinked() || linkedTypes.contains(type.getFqn());
    }

    private static boolean shouldRenderMember(Visibility visibility, PlantUmlConfig config) {
        return switch (visibility) {
            case PRIVATE -> !config.hidePrivate();
            case PROTECTED -> !config.hideProtected();
            case PACKAGE -> !config.hidePackage();
            case PUBLIC -> true;
        };
    }

    private static boolean shouldRenderRelation(
            UmlRelation relation,
            PlantUmlConfig config,
            Set<String> renderedTypeFqns,
            Set<String> nestedTypeFqns,
            Map<String, UmlType> typesByFqn,
            Set<String> sourcePackages,
            PlantUmlRenderer renderer) {
        String fromFqn = relation.getFromTypeFqn();
        String toFqn = relation.getToTypeFqn();

        if (!renderedTypeFqns.contains(fromFqn)) {
            return false;
        }

        if (!renderedTypeFqns.contains(toFqn)) {
            if (typesByFqn.containsKey(toFqn)) {
                return false;
            }
            if (!isExternalFqnAllowed(toFqn, config, sourcePackages)) {
                return false;
            }
        }

        if (!config.showNested()
                && (isNestedTypeReference(fromFqn, nestedTypeFqns, typesByFqn, renderer)
                        || isNestedTypeReference(toFqn, nestedTypeFqns, typesByFqn, renderer))) {
            return false;
        }

        RelationKind kind = relation.getKind();
        return switch (kind) {
            case EXTENDS -> config.showInheritance();
            case IMPLEMENTS -> config.showImplements();
            case COMPOSITION -> config.showComposition();
            case AGGREGATION -> config.showAggregation();
            case ASSOCIATION -> config.showAssociation();
            case DEPENDENCY -> config.showDependency();
            case NESTED -> config.showNested();
        };
    }

    private static boolean isExternalFqnAllowed(String fqn, PlantUmlConfig config, Set<String> sourcePackages) {
        String pkg = PackageClassifier.extractPackageFromFqn(fqn);

        if (PackageClassifier.isCommonPackage(pkg)) {
            return !config.hideCommonPackages();
        }
        if (PackageClassifier.isExternalPackage(pkg, sourcePackages)) {
            return !config.hideExternalPackages();
        }
        if (PackageClassifier.isSiblingPackage(pkg, sourcePackages)) {
            return !config.hideSiblingPackages();
        }
        return true;
    }

    private static boolean isNestedTypeReference(
            String fqn,
            Set<String> nestedTypeFqns,
            Map<String, UmlType> typesByFqn,
            PlantUmlRenderer renderer) {
        if (nestedTypeFqns.contains(fqn)) {
            return true;
        }
        if (typesByFqn.containsKey(fqn)) {
            return false;
        }
        String displayName = renderer.displayNameForFqn(fqn);
        return displayName != null && displayName.contains("$");
    }
}
