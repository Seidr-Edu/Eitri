package no.ntnu.eitri.degradation;

import no.ntnu.eitri.config.PlantUmlConfig;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlParameter;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates deterministic degraded UML variants from a canonical semantic
 * model.
 */
public final class ModelDegrader {
    private static final int MIN_INBOUND_REFERENCES_FOR_TYPE_REMOVAL = 1;
    private static final int MAX_INBOUND_REFERENCES_FOR_TYPE_REMOVAL = 3;

    public static final DiagramVariantProfile DIAGRAM_V2 = new DiagramVariantProfile(
            "diagram_v2",
            8,
            2,
            EnumSet.of(DegradationKind.OMIT_FIELD, DegradationKind.OMIT_METHOD, DegradationKind.REVERSE_RELATION));
    public static final DiagramVariantProfile DIAGRAM_V3 = new DiagramVariantProfile(
            "diagram_v3",
            12,
            3,
            EnumSet.of(
                    DegradationKind.OMIT_FIELD,
                    DegradationKind.OMIT_METHOD,
                    DegradationKind.REVERSE_RELATION,
                    DegradationKind.OMIT_RELATION,
                    DegradationKind.OMIT_TYPE));

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public List<DiagramDegradationResult> degradeAll(UmlModel model, PlantUmlConfig config) {
        return List.of(
                degrade(model, config, DIAGRAM_V2),
                degrade(model, config, DIAGRAM_V3));
    }

    public DiagramDegradationResult degrade(UmlModel model, PlantUmlConfig config, DiagramVariantProfile profile) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(profile, "profile");

        UmlModel workingModel = UmlModelCloner.cloneModel(model);
        RenderableModelView view = RenderableModelView.analyze(workingModel, config);
        List<DegradationCandidate> eligibleCandidates = discoverCandidates(workingModel, view, profile);
        Map<String, Integer> eligibleKindCounts = kindCounts(eligibleCandidates);
        int desiredCount = desiredAppliedCount(profile, eligibleCandidates.size());
        List<DegradationCandidate> selected = selectCandidates(
                eligibleCandidates,
                desiredCount,
                view,
                config.showUnlinked(),
                profile.variantId());

        List<AppliedDegradation> applied = new ArrayList<>(selected.size());
        for (DegradationCandidate candidate : selected) {
            workingModel = candidate.apply(workingModel);
            applied.add(candidate.toAppliedDegradation());
        }

        return new DiagramDegradationResult(
                profile.variantId(),
                profile.percentage(),
                profile.minimum(),
                eligibleCandidates.size(),
                eligibleKindCounts,
                applied.size(),
                List.copyOf(applied),
                workingModel);
    }

    private Map<String, Integer> kindCounts(List<DegradationCandidate> candidates) {
        Map<String, Integer> counts = new HashMap<>();
        for (DegradationCandidate candidate : candidates) {
            counts.merge(candidate.kind().wireName(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, _right) -> left,
                        LinkedHashMap::new));
    }

    List<DegradationCandidate> discoverCandidates(
            UmlModel model,
            RenderableModelView view,
            DiagramVariantProfile profile) {
        List<DegradationCandidate> candidates = new ArrayList<>();

        for (UmlType type : model.getTypesSorted()) {
            if (!view.isRenderedType(type.getFqn())) {
                continue;
            }
            if (profile.allowedKinds().contains(DegradationKind.OMIT_TYPE)
                    && view.renderedTypeFqns().size() >= 4
                    && isLowCouplingRemovableType(type, view)) {
                int inboundReferenceCount = view.inboundModeledReferenceCount(type.getFqn());
                candidates.add(DegradationCandidate.forType(
                        type,
                        inboundReferenceCount,
                        view.incidentModeledNeighborFqns(type.getFqn())));
            }
            if (profile.allowedKinds().contains(DegradationKind.OMIT_FIELD)) {
                for (UmlField field : type.getFields()) {
                    if (view.isVisibleField(field)) {
                        candidates.add(DegradationCandidate.forField(type, field));
                    }
                }
            }
            if (profile.allowedKinds().contains(DegradationKind.OMIT_METHOD)) {
                for (UmlMethod method : type.getMethods()) {
                    if (view.isVisibleMethod(method)) {
                        candidates.add(DegradationCandidate.forMethod(type, method));
                    }
                }
            }
        }

        for (UmlRelation relation : view.renderedRelations()) {
            if (profile.allowedKinds().contains(DegradationKind.REVERSE_RELATION)
                    && view.isModeledType(relation.getFromTypeFqn())
                    && view.isModeledType(relation.getToTypeFqn())) {
                candidates.add(DegradationCandidate.forRelation(relation, DegradationKind.REVERSE_RELATION));
            }
            if (profile.allowedKinds().contains(DegradationKind.OMIT_RELATION)
                    && !relation.getKind().isHierarchy()
                    && !relation.getKind().isNesting()) {
                candidates.add(DegradationCandidate.forRelation(relation, DegradationKind.OMIT_RELATION));
            }
        }

        return candidates;
    }

    private boolean isLowCouplingRemovableType(UmlType type, RenderableModelView view) {
        if (type.getKind() != TypeKind.CLASS && type.getKind() != TypeKind.RECORD) {
            return false;
        }
        if (view.hasRenderedNestedChildren(type.getFqn())) {
            return false;
        }
        int inboundReferenceCount = view.inboundModeledReferenceCount(type.getFqn());
        return inboundReferenceCount >= MIN_INBOUND_REFERENCES_FOR_TYPE_REMOVAL
                && inboundReferenceCount <= MAX_INBOUND_REFERENCES_FOR_TYPE_REMOVAL;
    }

    int desiredAppliedCount(DiagramVariantProfile profile, int eligibleCount) {
        if (eligibleCount <= 0) {
            return 0;
        }
        int percentageCount = (int) Math.ceil(eligibleCount * (profile.percentage() / 100.0d));
        return Math.min(eligibleCount, Math.max(profile.minimum(), percentageCount));
    }

    static int maxTypeRemovalsForRenderedTypeCount(int renderedTypeCount) {
        if (renderedTypeCount < 30) {
            return 1;
        }
        if (renderedTypeCount < 80) {
            return 3;
        }
        return 5;
    }

    private List<DegradationCandidate> selectCandidates(
            List<DegradationCandidate> eligibleCandidates,
            int desiredCount,
            RenderableModelView view,
            boolean showUnlinked,
            String variantId) {
        SelectionState selectionState = new SelectionState(
                view.renderedTypeFqns(),
                view.visibleFieldCounts(),
                view.visibleMethodCounts(),
                view.renderedRelationCounts(),
                showUnlinked);

        return eligibleCandidates.stream()
                .map(candidate -> candidate.withHash(hash(variantId + ":" + candidate.id())))
                .sorted(Comparator.comparing(DegradationCandidate::hash).thenComparing(DegradationCandidate::id))
                .filter(selectionState::select)
                .limit(desiredCount)
                .toList();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }

    private static UmlModel rebuildWithType(UmlModel model, UmlType updatedType) {
        UmlModel.Builder builder = UmlModel.builder()
                .name(model.getName())
                .sourcePackages(model.getSourcePackages())
                .notes(new ArrayList<>(model.getNotes()))
                .relations(new ArrayList<>(model.getRelations()));

        for (UmlType type : model.getTypesSorted()) {
            builder.addType(type.getFqn().equals(updatedType.getFqn()) ? updatedType : type);
        }
        return builder.build();
    }

    private static UmlModel rebuildWithRelations(UmlModel model, List<UmlRelation> relations) {
        UmlModel.Builder builder = UmlModel.builder()
                .name(model.getName())
                .sourcePackages(model.getSourcePackages())
                .notes(new ArrayList<>(model.getNotes()))
                .relations(relations);
        for (UmlType type : model.getTypesSorted()) {
            builder.addType(type);
        }
        return builder.build();
    }

    public enum DegradationKind {
        OMIT_FIELD("omit_field"),
        OMIT_METHOD("omit_method"),
        REVERSE_RELATION("reverse_relation"),
        OMIT_RELATION("omit_relation"),
        OMIT_TYPE("omit_type");

        private final String wireName;

        DegradationKind(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }
    }

    public record DiagramVariantProfile(
            String variantId,
            int percentage,
            int minimum,
            Set<DegradationKind> allowedKinds) {

        public DiagramVariantProfile {
            allowedKinds = Set.copyOf(allowedKinds);
        }
    }

    public record AppliedDegradation(
            String kind,
            String ownerFqn,
            String target,
            String detail) {
    }

    public record DiagramDegradationResult(
            String variant,
            int percentage,
            int minimum,
            int eligibleCandidateCount,
            Map<String, Integer> eligibleKindCounts,
            int appliedCount,
            List<AppliedDegradation> applied,
            UmlModel model) {

        public DiagramDegradationResult {
            eligibleKindCounts = Map.copyOf(eligibleKindCounts);
            applied = List.copyOf(applied);
        }
    }

    private static final class SelectionState {
        private final Set<String> modeledTypeFqns;
        private final Map<String, Integer> remainingVisibleFieldsByType;
        private final Map<String, Integer> remainingVisibleMethodsByType;
        private final Map<String, Integer> remainingRenderedRelationsByType;
        private final boolean showUnlinked;
        private final int maxTypeRemovals;
        private int remainingRenderedTypeCount;
        private int selectedTypeRemovals;
        private final Set<String> selectedTargets = new java.util.HashSet<>();
        private final Set<String> selectedMemberTypes = new java.util.HashSet<>();
        private final Set<String> selectedRelationTypes = new java.util.HashSet<>();
        private final Set<String> removedTypes = new java.util.HashSet<>();

        private SelectionState(
                Set<String> modeledTypeFqns,
                Map<String, Integer> visibleFieldCounts,
                Map<String, Integer> visibleMethodCounts,
                Map<String, Integer> renderedRelationCounts,
                boolean showUnlinked) {
            this.modeledTypeFqns = Set.copyOf(modeledTypeFqns);
            this.remainingVisibleFieldsByType = new HashMap<>(visibleFieldCounts);
            this.remainingVisibleMethodsByType = new HashMap<>(visibleMethodCounts);
            this.remainingRenderedRelationsByType = new HashMap<>(renderedRelationCounts);
            this.showUnlinked = showUnlinked;
            this.remainingRenderedTypeCount = modeledTypeFqns.size();
            this.maxTypeRemovals = maxTypeRemovalsForRenderedTypeCount(this.remainingRenderedTypeCount);
        }

        private boolean select(DegradationCandidate candidate) {
            if (!selectedTargets.add(candidate.targetKey())) {
                return false;
            }

            boolean selected = switch (candidate.kind()) {
                case OMIT_FIELD -> canRemoveVisibleMember(
                        candidate.typeFqn(),
                        remainingVisibleFieldsByType);
                case OMIT_METHOD -> canRemoveVisibleMember(
                        candidate.typeFqn(),
                        remainingVisibleMethodsByType);
                case REVERSE_RELATION -> canSelectRelation(candidate, false);
                case OMIT_RELATION -> canSelectRelation(candidate, true);
                case OMIT_TYPE -> canOmitType(candidate);
            };
            if (!selected) {
                selectedTargets.remove(candidate.targetKey());
            }
            return selected;
        }

        private boolean canRemoveVisibleMember(String typeFqn, Map<String, Integer> remainingCounts) {
            if (removedTypes.contains(typeFqn)) {
                return false;
            }
            int remaining = remainingCounts.getOrDefault(typeFqn, 0);
            if (remaining <= 1) {
                return false;
            }
            remainingCounts.put(typeFqn, remaining - 1);
            selectedMemberTypes.add(typeFqn);
            return true;
        }

        private boolean canSelectRelation(DegradationCandidate candidate, boolean omitRelation) {
            if (removedTypes.contains(candidate.relation().fromTypeFqn())
                    || removedTypes.contains(candidate.relation().toTypeFqn())) {
                return false;
            }

            if (omitRelation && !showUnlinked) {
                for (String endpoint : List.of(candidate.relation().fromTypeFqn(), candidate.relation().toTypeFqn())) {
                    if (!modeledTypeFqns.contains(endpoint)) {
                        continue;
                    }
                    int remaining = remainingRenderedRelationsByType.getOrDefault(endpoint, 0);
                    if (remaining <= 1) {
                        return false;
                    }
                }

                decrementRelationCount(candidate.relation().fromTypeFqn());
                decrementRelationCount(candidate.relation().toTypeFqn());
            }
            if (modeledTypeFqns.contains(candidate.relation().fromTypeFqn())) {
                selectedRelationTypes.add(candidate.relation().fromTypeFqn());
            }
            if (modeledTypeFqns.contains(candidate.relation().toTypeFqn())) {
                selectedRelationTypes.add(candidate.relation().toTypeFqn());
            }
            return true;
        }

        private boolean canOmitType(DegradationCandidate candidate) {
            String typeFqn = candidate.typeFqn();
            if (selectedTypeRemovals >= maxTypeRemovals
                    || remainingRenderedTypeCount <= 2
                    || removedTypes.contains(typeFqn)
                    || selectedMemberTypes.contains(typeFqn)
                    || selectedRelationTypes.contains(typeFqn)) {
                return false;
            }

            if (!showUnlinked) {
                for (String neighborFqn : candidate.incidentModeledNeighborFqns()) {
                    int remaining = remainingRenderedRelationsByType.getOrDefault(neighborFqn, 0);
                    if (remaining <= 1) {
                        return false;
                    }
                }
            }

            selectedTypeRemovals++;
            removedTypes.add(typeFqn);
            remainingRenderedTypeCount--;
            decrementRelationCount(typeFqn);
            for (String neighborFqn : candidate.incidentModeledNeighborFqns()) {
                decrementRelationCount(neighborFqn);
            }
            return true;
        }

        private void decrementRelationCount(String endpoint) {
            if (!modeledTypeFqns.contains(endpoint)) {
                return;
            }
            remainingRenderedRelationsByType.computeIfPresent(endpoint, (_ignored, remaining) -> remaining - 1);
        }
    }

    record DegradationCandidate(
            String id,
            String targetKey,
            String hash,
            DegradationKind kind,
            String ownerFqn,
            String target,
            String detail,
            String typeFqn,
            String fieldName,
            String fieldType,
            String methodName,
            List<String> methodParameterTypes,
            RelationDescriptor relation,
            List<String> incidentModeledNeighborFqns) {

        private static DegradationCandidate forField(UmlType owner, UmlField field) {
            String target = field.getName();
            return new DegradationCandidate(
                    "field:" + owner.getFqn() + "#" + field.getName() + ":" + field.getType(),
                    "field:" + owner.getFqn() + "#" + field.getName() + ":" + field.getType(),
                    "",
                    DegradationKind.OMIT_FIELD,
                    owner.getFqn(),
                    target,
                    "Removed field " + field.getName() + " : " + field.getTypeSimpleName(),
                    owner.getFqn(),
                    field.getName(),
                    field.getType(),
                    null,
                    List.of(),
                    null,
                    List.of());
        }

        private static DegradationCandidate forMethod(UmlType owner, UmlMethod method) {
            String signature = methodSignature(method, true);
            List<String> parameterTypes = method.getParameters().stream()
                    .map(UmlParameter::type)
                    .toList();
            return new DegradationCandidate(
                    "method:" + owner.getFqn() + "#" + methodSignature(method, false),
                    "method:" + owner.getFqn() + "#" + methodSignature(method, false),
                    "",
                    DegradationKind.OMIT_METHOD,
                    owner.getFqn(),
                    signature,
                    "Removed method " + signature,
                    owner.getFqn(),
                    null,
                    null,
                    method.getName(),
                    parameterTypes,
                    null,
                    List.of());
        }

        private static DegradationCandidate forRelation(UmlRelation relation, DegradationKind kind) {
            RelationDescriptor descriptor = RelationDescriptor.fromRelation(relation);
            String direction = descriptor.fromTypeFqn() + " -> " + descriptor.toTypeFqn();
            String action = kind == DegradationKind.REVERSE_RELATION ? "Reversed relation " : "Removed relation ";
            return new DegradationCandidate(
                    "relation:" + descriptor.identityString() + ":" + kind.wireName(),
                    "relation:" + descriptor.identityString(),
                    "",
                    kind,
                    descriptor.fromTypeFqn(),
                    descriptor.targetString(),
                    action + descriptor.kind().name().toLowerCase() + " " + direction,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    descriptor,
                    List.of());
        }

        private static DegradationCandidate forType(
                UmlType type,
                int inboundReferenceCount,
                Set<String> incidentModeledNeighborFqns) {
            return new DegradationCandidate(
                    "type:" + type.getFqn(),
                    "type:" + type.getFqn(),
                    "",
                    DegradationKind.OMIT_TYPE,
                    type.getFqn(),
                    type.getFqn(),
                    "Removed low-coupling type " + type.getFqn()
                            + " (inbound_modeled_references=" + inboundReferenceCount + ")",
                    type.getFqn(),
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    incidentModeledNeighborFqns.stream().sorted().toList());
        }

        private static String methodSignature(UmlMethod method, boolean simpleTypes) {
            String parameters = method.getParameters().stream()
                    .map(parameter -> simpleTypes ? parameter.typeSimpleName() : parameter.type())
                    .collect(Collectors.joining(", "));
            return method.getName() + "(" + parameters + ")";
        }

        private DegradationCandidate withHash(String hash) {
            return new DegradationCandidate(
                    id,
                    targetKey,
                    hash,
                    kind,
                    ownerFqn,
                    target,
                    detail,
                    typeFqn,
                    fieldName,
                    fieldType,
                    methodName,
                    methodParameterTypes,
                    relation,
                    incidentModeledNeighborFqns);
        }

        private UmlModel apply(UmlModel model) {
            return switch (kind) {
                case OMIT_FIELD -> applyFieldRemoval(model);
                case OMIT_METHOD -> applyMethodRemoval(model);
                case REVERSE_RELATION -> applyRelationReplacement(model, relation.reversed());
                case OMIT_RELATION -> applyRelationRemoval(model);
                case OMIT_TYPE -> applyTypeRemoval(model);
            };
        }

        private UmlModel applyFieldRemoval(UmlModel model) {
            UmlType owner = model.getType(typeFqn)
                    .orElseThrow(() -> new IllegalStateException("Missing type during degradation: " + typeFqn));
            UmlType.Builder builder = UmlType.builder()
                    .fqn(owner.getFqn())
                    .simpleName(owner.getSimpleName())
                    .alias(owner.getAlias())
                    .kind(owner.getKind())
                    .visibility(owner.getVisibility())
                    .style(owner.getStyle())
                    .outerTypeFqn(owner.getOuterTypeFqn());

            owner.getStereotypes().forEach(builder::addStereotype);
            owner.getTags().forEach(builder::addTag);
            owner.getGenerics().forEach(builder::addGeneric);
            for (UmlField field : owner.getFields()) {
                if (!field.getName().equals(fieldName) || !field.getType().equals(fieldType)) {
                    builder.addField(field);
                }
            }
            owner.getMethods().forEach(builder::addMethod);
            return rebuildWithType(model, builder.build());
        }

        private UmlModel applyMethodRemoval(UmlModel model) {
            UmlType owner = model.getType(typeFqn)
                    .orElseThrow(() -> new IllegalStateException("Missing type during degradation: " + typeFqn));
            UmlType.Builder builder = UmlType.builder()
                    .fqn(owner.getFqn())
                    .simpleName(owner.getSimpleName())
                    .alias(owner.getAlias())
                    .kind(owner.getKind())
                    .visibility(owner.getVisibility())
                    .style(owner.getStyle())
                    .outerTypeFqn(owner.getOuterTypeFqn());

            owner.getStereotypes().forEach(builder::addStereotype);
            owner.getTags().forEach(builder::addTag);
            owner.getGenerics().forEach(builder::addGeneric);
            owner.getFields().forEach(builder::addField);
            for (UmlMethod method : owner.getMethods()) {
                if (!matchesMethod(method)) {
                    builder.addMethod(method);
                }
            }
            return rebuildWithType(model, builder.build());
        }

        private boolean matchesMethod(UmlMethod method) {
            return method.getName().equals(methodName)
                    && method.getParameters().stream().map(UmlParameter::type).toList().equals(methodParameterTypes);
        }

        private UmlModel applyRelationRemoval(UmlModel model) {
            List<UmlRelation> relations = model.getRelations().stream()
                    .filter(existing -> !relation.matches(existing))
                    .toList();
            return rebuildWithRelations(model, relations);
        }

        private UmlModel applyRelationReplacement(UmlModel model, UmlRelation replacement) {
            List<UmlRelation> relations = new ArrayList<>(model.getRelations().size());
            for (UmlRelation existing : model.getRelations()) {
                relations.add(relation.matches(existing) ? replacement : existing);
            }
            return rebuildWithRelations(model, relations);
        }

        private UmlModel applyTypeRemoval(UmlModel model) {
            UmlModel.Builder builder = UmlModel.builder()
                    .name(model.getName())
                    .sourcePackages(model.getSourcePackages())
                    .notes(new ArrayList<>(model.getNotes()));

            for (UmlType type : model.getTypesSorted()) {
                if (!type.getFqn().equals(typeFqn)) {
                    builder.addType(type);
                }
            }
            for (UmlRelation existing : model.getRelations()) {
                if (!existing.getFromTypeFqn().equals(typeFqn) && !existing.getToTypeFqn().equals(typeFqn)) {
                    builder.addRelation(existing);
                }
            }
            return builder.build();
        }

        private AppliedDegradation toAppliedDegradation() {
            return new AppliedDegradation(kind.wireName(), ownerFqn, target, detail);
        }
    }

    record RelationDescriptor(
            String fromTypeFqn,
            String toTypeFqn,
            RelationKind kind,
            String fromMember,
            String toMember,
            String label,
            String fromMultiplicity,
            String toMultiplicity) {

        private static RelationDescriptor fromRelation(UmlRelation relation) {
            return new RelationDescriptor(
                    relation.getFromTypeFqn(),
                    relation.getToTypeFqn(),
                    relation.getKind(),
                    relation.getFromMember(),
                    relation.getToMember(),
                    relation.getLabel(),
                    relation.getFromMultiplicity(),
                    relation.getToMultiplicity());
        }

        private String identityString() {
            return String.join(
                    "|",
                    fromTypeFqn,
                    toTypeFqn,
                    kind.name(),
                    nullToEmpty(fromMember),
                    nullToEmpty(toMember),
                    nullToEmpty(label),
                    nullToEmpty(fromMultiplicity),
                    nullToEmpty(toMultiplicity));
        }

        private String targetString() {
            return fromTypeFqn + " -> " + toTypeFqn + " (" + kind.name().toLowerCase() + ")";
        }

        private UmlRelation reversed() {
            return UmlRelation.builder()
                    .fromTypeFqn(toTypeFqn)
                    .toTypeFqn(fromTypeFqn)
                    .kind(kind)
                    .label(label)
                    .fromMultiplicity(toMultiplicity)
                    .toMultiplicity(fromMultiplicity)
                    .fromMember(toMember)
                    .toMember(fromMember)
                    .build();
        }

        private boolean matches(UmlRelation relation) {
            return fromTypeFqn.equals(relation.getFromTypeFqn())
                    && toTypeFqn.equals(relation.getToTypeFqn())
                    && kind == relation.getKind()
                    && Objects.equals(fromMember, relation.getFromMember())
                    && Objects.equals(toMember, relation.getToMember())
                    && Objects.equals(label, relation.getLabel())
                    && Objects.equals(fromMultiplicity, relation.getFromMultiplicity())
                    && Objects.equals(toMultiplicity, relation.getToMultiplicity());
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
