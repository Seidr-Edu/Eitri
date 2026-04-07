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

import java.util.List;
import java.util.LinkedHashMap;
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

        ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, config, ModelDegrader.DIAGRAM_V2);

        assertEquals(5, model.getTypes().size());
        assertEquals(3, model.getRelations().size());
        UmlType worker = model.getType("demo.Worker").orElseThrow();
        assertEquals(2, worker.getFields().size());
        assertEquals(2, worker.getMethods().size());
        assertTrue(degraded.appliedCount() > 0);
        assertNotEquals(writer.render(model, config), writer.render(degraded.model(), config));
    }

    @Test
    void degradationSelectionIsDeterministic() {
        UmlModel model = sampleModel();

        ModelDegrader.DiagramDegradationResult first = degrader.degrade(model, config, ModelDegrader.DIAGRAM_V3);
        ModelDegrader.DiagramDegradationResult second = degrader.degrade(model, config, ModelDegrader.DIAGRAM_V3);

        assertEquals(first.applied(), second.applied());
        assertEquals(writer.render(first.model(), config), writer.render(second.model(), config));
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
    void v2NeverCreatesOmitRelationCandidatesOrApplications() {
        UmlModel model = sampleModel();
        RenderableModelView view = RenderableModelView.analyze(model, config);

        List<ModelDegrader.DegradationCandidate> candidates = degrader.discoverCandidates(model, view, ModelDegrader.DIAGRAM_V2);
        ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, config, ModelDegrader.DIAGRAM_V2);

        assertFalse(candidates.stream().anyMatch(candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_RELATION));
        assertFalse(degraded.applied().stream().anyMatch(applied -> "omit_relation".equals(applied.kind())));
    }

    @Test
    void v3OnlyAllowsOmittingNonHierarchyAndNonNestedRelations() {
        UmlModel model = sampleModel();
        RenderableModelView view = RenderableModelView.analyze(model, config);

        List<ModelDegrader.DegradationCandidate> candidates = degrader.discoverCandidates(model, view, ModelDegrader.DIAGRAM_V3);

        List<ModelDegrader.DegradationCandidate> omitRelationCandidates = candidates.stream()
                .filter(candidate -> candidate.kind() == ModelDegrader.DegradationKind.OMIT_RELATION)
                .toList();

        assertFalse(omitRelationCandidates.isEmpty());
        assertTrue(omitRelationCandidates.stream()
                .noneMatch(candidate -> candidate.relation().kind().isHierarchy() || candidate.relation().kind().isNesting()));
    }

    @Test
    void hierarchyAndNestedRelationsAreEligibleForReversal() {
        UmlModel model = sampleModel();
        PlantUmlConfig nestedConfig = config("showNested", true);
        RenderableModelView view = RenderableModelView.analyze(model, nestedConfig);

        Set<RelationKind> reversibleKinds = degrader.discoverCandidates(model, view, ModelDegrader.DIAGRAM_V3).stream()
                .filter(candidate -> candidate.kind() == ModelDegrader.DegradationKind.REVERSE_RELATION)
                .map(candidate -> candidate.relation().kind())
                .collect(Collectors.toSet());

        assertTrue(reversibleKinds.contains(RelationKind.EXTENDS));
        assertTrue(reversibleKinds.contains(RelationKind.NESTED));
        assertTrue(reversibleKinds.contains(RelationKind.ASSOCIATION));
    }

    @Test
    void noRelationReceivesConflictingDegradations() {
        UmlModel model = sampleModel();

        ModelDegrader.DiagramDegradationResult degraded = degrader.degrade(model, config, ModelDegrader.DIAGRAM_V3);

        Set<String> relationTargets = degraded.applied().stream()
                .filter(applied -> applied.kind().contains("relation"))
                .map(ModelDegrader.AppliedDegradation::target)
                .collect(Collectors.toSet());

        long relationApplications = degraded.applied().stream()
                .filter(applied -> applied.kind().contains("relation"))
                .count();

        assertEquals(relationTargets.size(), relationApplications);
    }

    private UmlModel sampleModel() {
        UmlType base = UmlType.builder()
                .fqn("demo.Base")
                .simpleName("Base")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType helper = UmlType.builder()
                .fqn("demo.Helper")
                .simpleName("Helper")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .addField(field("status", "java.lang.String"))
                .addMethod(method("help"))
                .build();

        UmlType worker = UmlType.builder()
                .fqn("demo.Worker")
                .simpleName("Worker")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .addField(field("name", "java.lang.String"))
                .addField(field("count", "int"))
                .addMethod(method("start"))
                .addMethod(method("stop"))
                .build();

        UmlType outer = UmlType.builder()
                .fqn("demo.Outer")
                .simpleName("Outer")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType inner = UmlType.builder()
                .fqn("demo.Outer$Inner")
                .simpleName("Inner")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .outerTypeFqn(outer.getFqn())
                .build();

        return UmlModel.builder()
                .addType(base)
                .addType(helper)
                .addType(worker)
                .addType(outer)
                .addType(inner)
                .addRelation(UmlRelation.extendsRelation(worker.getFqn(), base.getFqn()))
                .addRelation(UmlRelation.association(worker.getFqn(), helper.getFqn(), "uses"))
                .addRelation(UmlRelation.nestedRelation(outer.getFqn(), inner.getFqn()))
                .sourcePackages(Set.of("demo"))
                .build();
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
            return RecordBinder.bindFlatRecord(map, PlantUmlConfig.class, PlantUmlConfig.defaults(), "test");
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }
}
