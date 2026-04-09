package no.ntnu.eitri.degradation;

import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.PlantUmlConfig;
import no.ntnu.eitri.config.RecordBinder;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelDegraderTest {

        private final ModelDegrader degrader = new ModelDegrader();
        private final PlantUmlConfig config = PlantUmlConfig.defaults();
        private final PlantUmlWriter writer = new PlantUmlWriter();

        @Test
        void cloneAndDegradeDoesNotMutateOriginalModel() {
                UmlModel model = sampleModel();

                ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, config("showNested", true),
                                ModelDegrader.DIAGRAM_V2);

                assertEquals(6, model.getTypes().size());
                assertEquals(4, model.getRelations().size());
                UmlType worker = model.getType("demo.Worker").orElseThrow();
                assertEquals(2, worker.getFields().size());
                assertEquals(2, worker.getMethods().size());
                assertTrue(degraded.appliedCount() > 0);
                assertNotEquals(writer.render(model, config("showNested", true)),
                                writer.render(degraded.model(), config("showNested", true)));
        }

        @Test
        void degradationSelectionIsDeterministic() {
                UmlModel model = sampleModel();
                PlantUmlConfig nestedConfig = config("showNested", true);

                ModelDegrader.DiagramDegradationResult first = degrader.degrade(model, nestedConfig,
                                ModelDegrader.DIAGRAM_V3);
                ModelDegrader.DiagramDegradationResult second = degrader.degrade(model, nestedConfig,
                                ModelDegrader.DIAGRAM_V3);

                assertEquals(first.applied(), second.applied());
                assertEquals(writer.render(first.model(), nestedConfig), writer.render(second.model(), nestedConfig));
        }

        @Test
        void desiredAppliedCountUsesPercentageAndMinimumWithCap() {
                assertEquals(0, degrader.desiredAppliedCount(ModelDegrader.DIAGRAM_V2, 0));
                assertEquals(1, degrader.desiredAppliedCount(ModelDegrader.DIAGRAM_V2, 1));
                assertEquals(2, degrader.desiredAppliedCount(ModelDegrader.DIAGRAM_V2, 5));
                assertEquals(4, degrader.desiredAppliedCount(ModelDegrader.DIAGRAM_V2, 50));
                assertEquals(3, degrader.desiredAppliedCount(ModelDegrader.DIAGRAM_V3, 10));
                assertEquals(3, degrader.desiredAppliedCount(ModelDegrader.DIAGRAM_V3, 25));
        }

        @Test
        void v2NeverCreatesOmitRelationOrOmitTypeCandidatesOrApplications() {
                UmlModel model = sampleModel();
                RenderableModelView view = RenderableModelView.analyze(model, config("showNested", true));

                List<ModelDegrader.DegradationCandidate> candidates = degrader.discoverCandidates(model, view,
                                ModelDegrader.DIAGRAM_V2);
                ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, config("showNested", true),
                                ModelDegrader.DIAGRAM_V2);

                assertFalse(candidates.stream().anyMatch(
                                candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_RELATION));
                assertFalse(candidates.stream()
                                .anyMatch(candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_TYPE));
                assertFalse(degraded.applied().stream().anyMatch(applied -> "omit_relation".equals(applied.kind())));
                assertFalse(degraded.applied().stream().anyMatch(applied -> "omit_type".equals(applied.kind())));
        }

        @Test
        void v3OnlyAllowsOmittingNonHierarchyAndNonNestedRelations() {
                UmlModel model = sampleModel();
                RenderableModelView view = RenderableModelView.analyze(model, config("showNested", true));

                List<ModelDegrader.DegradationCandidate> candidates = degrader.discoverCandidates(model, view,
                                ModelDegrader.DIAGRAM_V3);

                List<ModelDegrader.DegradationCandidate> omitRelationCandidates = candidates.stream()
                                .filter(candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_RELATION)
                                .toList();

                assertFalse(omitRelationCandidates.isEmpty());
                assertTrue(omitRelationCandidates.stream()
                                .noneMatch(candidate -> candidate.relation().kind().isHierarchy()
                                                || candidate.relation().kind().isNesting()));
        }

        @Test
        void hierarchyAndNestedRelationsAreEligibleForReversal() {
                UmlModel model = sampleModel();
                PlantUmlConfig nestedConfig = config("showNested", true);
                RenderableModelView view = RenderableModelView.analyze(model, nestedConfig);

                Set<RelationKind> reversibleKinds = degrader.discoverCandidates(model, view, ModelDegrader.DIAGRAM_V3)
                                .stream()
                                .filter(candidate -> candidate.kind() == ModelDegrader.DegradationKind.REVERSE_RELATION)
                                .map(candidate -> candidate.relation().kind())
                                .collect(Collectors.toSet());

                assertTrue(reversibleKinds.contains(RelationKind.EXTENDS));
                assertTrue(reversibleKinds.contains(RelationKind.NESTED));
                assertTrue(reversibleKinds.contains(RelationKind.ASSOCIATION));
        }

        @Test
        void lowCouplingTypeCandidatesIncludeRecordsAndNestedTypesButExcludeUnreferencedOwners() {
                UmlModel model = sampleModel();
                RenderableModelView view = RenderableModelView.analyze(model, config("showNested", true));

                Set<String> removableTypes = degrader.discoverCandidates(model, view, ModelDegrader.DIAGRAM_V3).stream()
                                .filter(candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_TYPE)
                                .map(ModelDegrader.DegradationCandidate::target)
                                .collect(Collectors.toSet());

                assertTrue(removableTypes.contains("demo.Base"));
                assertTrue(removableTypes.contains("demo.Helper"));
                assertTrue(removableTypes.contains("demo.Projection"));
                assertTrue(removableTypes.contains("demo.Outer$Inner"));
                assertFalse(removableTypes.contains("demo.Worker"));
                assertFalse(removableTypes.contains("demo.Outer"));
        }

        @Test
        void typesWithMoreThanThreeInboundReferencesAreNotRemovalCandidates() {
                UmlModel model = highInboundModel();
                RenderableModelView view = RenderableModelView.analyze(model, config);

                Set<String> removableTypes = degrader.discoverCandidates(model, view, ModelDegrader.DIAGRAM_V3).stream()
                                .filter(candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_TYPE)
                                .map(ModelDegrader.DegradationCandidate::target)
                                .collect(Collectors.toSet());

                assertFalse(removableTypes.contains("demo.Shared"));
        }

        @Test
        void typeRemovalCapScalesWithRenderedDiagramSize() {
                UmlModel model = sampleModel();
                ModelDegrader.DiagramVariantProfile typeOnlyProfile = new ModelDegrader.DiagramVariantProfile(
                                "type-only",
                                100,
                                100,
                                EnumSet.of(ModelDegrader.DegradationKind.OMIT_TYPE));

                ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, config("showNested", true),
                                typeOnlyProfile);
                ModelDegrader.DiagramDegradationResult mediumDegraded = degrader.degrade(
                                lowInboundPoolModel(40),
                                config,
                                typeOnlyProfile);
                ModelDegrader.DiagramDegradationResult largeDegraded = degrader.degrade(
                                lowInboundPoolModel(100),
                                config,
                                typeOnlyProfile);

                long removedTypes = degraded.applied().stream()
                                .filter(applied -> "omit_type".equals(applied.kind()))
                                .count();
                long mediumRemovedTypes = mediumDegraded.applied().stream()
                                .filter(applied -> "omit_type".equals(applied.kind()))
                                .count();
                long largeRemovedTypes = largeDegraded.applied().stream()
                                .filter(applied -> "omit_type".equals(applied.kind()))
                                .count();

                assertEquals(1, removedTypes);
                assertEquals(3, mediumRemovedTypes);
                assertEquals(5, largeRemovedTypes);
        }

        @Test
        void showUnlinkedFalseBlocksTypeRemovalThatWouldHideRemainingNeighbor() {
                UmlModel model = pairedLowInboundModel();
                PlantUmlConfig linkedOnlyConfig = config("showUnlinked", false);
                ModelDegrader.DiagramVariantProfile typeOnlyProfile = new ModelDegrader.DiagramVariantProfile(
                                "type-only-show-unlinked-false",
                                100,
                                100,
                                EnumSet.of(ModelDegrader.DegradationKind.OMIT_TYPE));

                RenderableModelView view = RenderableModelView.analyze(model, linkedOnlyConfig);
                List<ModelDegrader.DegradationCandidate> candidates = degrader.discoverCandidates(model, view,
                                typeOnlyProfile);
                ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, linkedOnlyConfig,
                                typeOnlyProfile);

                assertEquals(2, candidates.size());
                assertTrue(degraded.applied().isEmpty());
        }

        @Test
        void removingNestedLowCouplingTypeDropsItAndItsIncidentRelationsOnly() {
                UmlModel model = uniqueNestedLeafModel();
                PlantUmlConfig nestedConfig = config("showNested", true);
                ModelDegrader.DiagramVariantProfile typeOnlyProfile = new ModelDegrader.DiagramVariantProfile(
                                "type-only-nested-leaf",
                                100,
                                100,
                                EnumSet.of(ModelDegrader.DegradationKind.OMIT_TYPE));

                ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, nestedConfig,
                                typeOnlyProfile);

                assertEquals(List.of("omit_type"),
                                degraded.applied().stream().map(ModelDegrader.AppliedDegradation::kind).toList());
                assertEquals("demo.Outer$Inner", degraded.applied().getFirst().target());
                assertTrue(model.hasType("demo.Outer$Inner"));
                assertFalse(degraded.model().hasType("demo.Outer$Inner"));
                assertEquals(1, model.getRelations().size());
                assertEquals(0, degraded.model().getRelations().size());
        }

        @Test
        void outerTypesWithRenderedNestedChildrenAreNeverRemovalCandidates() {
                UmlModel model = uniqueNestedLeafModel();
                RenderableModelView view = RenderableModelView.analyze(model, config("showNested", true));

                Set<String> omitTypeCandidates = degrader.discoverCandidates(model, view, ModelDegrader.DIAGRAM_V3)
                                .stream()
                                .filter(candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_TYPE)
                                .map(ModelDegrader.DegradationCandidate::target)
                                .collect(Collectors.toSet());

                assertFalse(omitTypeCandidates.contains("demo.Outer"));
                assertTrue(omitTypeCandidates.contains("demo.Outer$Inner"));
        }

        @Test
        void omittedTypeDoesNotAlsoReceiveMemberOrRelationDegradations() {
                UmlModel model = helperLeafConflictModel();
                PlantUmlConfig nestedConfig = config("showNested", true);
                ModelDegrader.DiagramVariantProfile broadProfile = firstProfileSelectingType(model, nestedConfig);

                ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, nestedConfig, broadProfile);
                ModelDegrader.AppliedDegradation removedType = degraded.applied().stream()
                                .filter(applied -> "omit_type".equals(applied.kind()))
                                .findFirst()
                                .orElseThrow();

                assertEquals("demo.Helper", removedType.target());
                assertTrue(degraded.applied().stream()
                                .filter(applied -> !"omit_type".equals(applied.kind()))
                                .noneMatch(applied -> applied.ownerFqn().equals("demo.Helper")
                                                || applied.target().contains("demo.Helper")));
        }

        @Test
        void noRelationReceivesConflictingDegradations() {
                UmlModel model = sampleModel();

                ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, config("showNested", true),
                                ModelDegrader.DIAGRAM_V3);

                Set<String> relationTargets = degraded.applied().stream()
                                .filter(applied -> applied.kind().contains("relation"))
                                .map(ModelDegrader.AppliedDegradation::target)
                                .collect(Collectors.toSet());

                long relationApplications = degraded.applied().stream()
                                .filter(applied -> applied.kind().contains("relation"))
                                .count();

                assertEquals(relationTargets.size(), relationApplications);
        }

        private ModelDegrader.DiagramVariantProfile firstProfileSelectingType(UmlModel model, PlantUmlConfig config) {
                EnumSet<ModelDegrader.DegradationKind> kinds = EnumSet.of(
                                ModelDegrader.DegradationKind.OMIT_TYPE,
                                ModelDegrader.DegradationKind.OMIT_FIELD,
                                ModelDegrader.DegradationKind.OMIT_METHOD,
                                ModelDegrader.DegradationKind.REVERSE_RELATION,
                                ModelDegrader.DegradationKind.OMIT_RELATION);
                for (int i = 0; i < 2000; i++) {
                        ModelDegrader.DiagramVariantProfile profile = new ModelDegrader.DiagramVariantProfile(
                                        "helper-conflict-" + i,
                                        100,
                                        100,
                                        kinds);
                        if (degrader.degrade(model, config, profile).applied().stream()
                                        .anyMatch(applied -> "omit_type".equals(applied.kind()))) {
                                return profile;
                        }
                }
                throw new AssertionError("Expected to find a deterministic profile that selects omit_type");
        }

        private UmlModel sampleModel() {
                UmlType base = type("demo.Base", "Base", TypeKind.CLASS).build();

                UmlType helper = type("demo.Helper", "Helper", TypeKind.CLASS)
                                .addField(field("status", "java.lang.String"))
                                .addMethod(method("help"))
                                .build();

                UmlType projection = type("demo.Projection", "Projection", TypeKind.RECORD)
                                .build();

                UmlType worker = type("demo.Worker", "Worker", TypeKind.CLASS)
                                .addField(field("name", "java.lang.String"))
                                .addField(field("count", "int"))
                                .addMethod(method("start"))
                                .addMethod(method("stop"))
                                .build();

                UmlType outer = type("demo.Outer", "Outer", TypeKind.CLASS)
                                .build();

                UmlType inner = type("demo.Outer$Inner", "Inner", TypeKind.CLASS)
                                .outerTypeFqn(outer.getFqn())
                                .build();

                return UmlModel.builder()
                                .addType(base)
                                .addType(helper)
                                .addType(projection)
                                .addType(worker)
                                .addType(outer)
                                .addType(inner)
                                .addRelation(UmlRelation.extendsRelation(worker.getFqn(), base.getFqn()))
                                .addRelation(UmlRelation.association(worker.getFqn(), helper.getFqn(), "uses"))
                                .addRelation(UmlRelation.association(worker.getFqn(), projection.getFqn(), "projects"))
                                .addRelation(UmlRelation.nestedRelation(outer.getFqn(), inner.getFqn()))
                                .sourcePackages(Set.of("demo"))
                                .build();
        }

        private UmlModel pairedLowInboundModel() {
                UmlType alpha = type("demo.Alpha", "Alpha", TypeKind.CLASS).build();
                UmlType beta = type("demo.Beta", "Beta", TypeKind.CLASS).build();
                UmlType gamma = type("demo.Gamma", "Gamma", TypeKind.CLASS).build();
                UmlType delta = type("demo.Delta", "Delta", TypeKind.CLASS).build();

                return UmlModel.builder()
                                .addType(alpha)
                                .addType(beta)
                                .addType(gamma)
                                .addType(delta)
                                .addRelation(UmlRelation.association(alpha.getFqn(), beta.getFqn(), "ab"))
                                .addRelation(UmlRelation.association(gamma.getFqn(), delta.getFqn(), "cd"))
                                .sourcePackages(Set.of("demo"))
                                .build();
        }

        private UmlModel highInboundModel() {
                UmlType shared = type("demo.Shared", "Shared", TypeKind.CLASS).build();
                UmlType one = type("demo.One", "One", TypeKind.CLASS).build();
                UmlType two = type("demo.Two", "Two", TypeKind.CLASS).build();
                UmlType three = type("demo.Three", "Three", TypeKind.CLASS).build();
                UmlType four = type("demo.Four", "Four", TypeKind.CLASS).build();
                UmlType marker = type("demo.Marker", "Marker", TypeKind.INTERFACE).build();

                return UmlModel.builder()
                                .addType(shared)
                                .addType(one)
                                .addType(two)
                                .addType(three)
                                .addType(four)
                                .addType(marker)
                                .addRelation(UmlRelation.association(one.getFqn(), shared.getFqn(), "one"))
                                .addRelation(UmlRelation.association(two.getFqn(), shared.getFqn(), "two"))
                                .addRelation(UmlRelation.association(three.getFqn(), shared.getFqn(), "three"))
                                .addRelation(UmlRelation.association(four.getFqn(), shared.getFqn(), "four"))
                                .sourcePackages(Set.of("demo"))
                                .build();
        }

        private UmlModel uniqueNestedLeafModel() {
                UmlType outer = type("demo.Outer", "Outer", TypeKind.CLASS).build();
                UmlType inner = type("demo.Outer$Inner", "Inner", TypeKind.CLASS)
                                .outerTypeFqn(outer.getFqn())
                                .build();
                UmlType hub = type("demo.Hub", "Hub", TypeKind.ABSTRACT_CLASS).build();
                UmlType api = type("demo.Api", "Api", TypeKind.INTERFACE).build();

                return UmlModel.builder()
                                .addType(outer)
                                .addType(inner)
                                .addType(hub)
                                .addType(api)
                                .addRelation(UmlRelation.nestedRelation(outer.getFqn(), inner.getFqn()))
                                .sourcePackages(Set.of("demo"))
                                .build();
        }

        private UmlModel helperLeafConflictModel() {
                UmlType owner = type("demo.Owner", "Owner", TypeKind.CLASS).build();
                UmlType nestedPort = type("demo.Owner$NestedPort", "NestedPort", TypeKind.INTERFACE)
                                .outerTypeFqn(owner.getFqn())
                                .build();
                UmlType hub = type("demo.Hub", "Hub", TypeKind.CLASS).build();
                UmlType service = type("demo.Service", "Service", TypeKind.INTERFACE).build();
                UmlType helper = type("demo.Helper", "Helper", TypeKind.CLASS)
                                .addField(field("cache", "java.lang.String"))
                                .addMethod(method("help"))
                                .build();

                return UmlModel.builder()
                                .addType(owner)
                                .addType(nestedPort)
                                .addType(hub)
                                .addType(service)
                                .addType(helper)
                                .addRelation(UmlRelation.nestedRelation(owner.getFqn(), nestedPort.getFqn()))
                                .addRelation(UmlRelation.association(hub.getFqn(), service.getFqn(), "serves"))
                                .addRelation(UmlRelation.association(hub.getFqn(), helper.getFqn(), "uses"))
                                .sourcePackages(Set.of("demo"))
                                .build();
        }

        private UmlModel lowInboundPoolModel(int typeCount) {
                UmlModel.Builder builder = UmlModel.builder()
                                .sourcePackages(Set.of("demo"));

                for (int i = 0; i < typeCount; i++) {
                        builder.addType(type("demo.Type" + i, "Type" + i, TypeKind.CLASS).build());
                }
                for (int i = 0; i + 1 < typeCount; i += 2) {
                        builder.addRelation(UmlRelation.association(
                                        "demo.Type" + i,
                                        "demo.Type" + (i + 1),
                                        "uses" + i));
                }

                return builder.build();
        }

        private UmlType.Builder type(String fqn, String simpleName, TypeKind kind) {
                return UmlType.builder()
                                .fqn(fqn)
                                .simpleName(simpleName)
                                .kind(kind)
                                .visibility(Visibility.PUBLIC);
        }

        private UmlField field(String name, String type) {
                return UmlField.builder()
                                .name(name)
                                .type(type)
                                .visibility(Visibility.PUBLIC)
                                .build();
        }

        private UmlMethod method(String name) {
                return UmlMethod.builder()
                                .name(name)
                                .returnType("void")
                                .visibility(Visibility.PUBLIC)
                                .build();
        }

        private static PlantUmlConfig config(Object... keyValues) {
                if (keyValues.length % 2 != 0) {
                        throw new IllegalArgumentException("Key/value pairs expected");
                }
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < keyValues.length; i += 2) {
                        map.put((String) keyValues[i], keyValues[i + 1]);
                }
                try {
                        return RecordBinder.bindFlatRecord(map, PlantUmlConfig.class, PlantUmlConfig.defaults(),
                                        "test");
                } catch (ConfigException e) {
                        throw new RuntimeException(e);
                }
        }
}
