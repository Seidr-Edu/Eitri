package no.ntnu.eitri.writer;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlantUmlWriterTest {

    @Test
    void omitsVoidReturnTypeWhenDisabled() {
        UmlMethod method = UmlMethod.builder()
                .name("init")
                .returnType("void")
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType type = UmlType.builder()
                .name("Service")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .addMethod(method)
                .build();

        UmlModel model = UmlModel.builder()
                .addType(type)
                .build();

        EitriConfig config = EitriConfig.builder()
                .showVoidReturnTypes(false)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertTrue(output.contains("+class Service"));
        assertTrue(output.contains("+init()"));
        assertFalse(output.contains(": void"));
    }

    @Test
    void includesVoidReturnTypeWhenEnabled() {
        UmlMethod method = UmlMethod.builder()
                .name("init")
                .returnType("void")
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType type = UmlType.builder()
                .name("Service")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .addMethod(method)
                .build();

        UmlModel model = UmlModel.builder()
                .addType(type)
                .build();

        EitriConfig config = EitriConfig.builder()
                .showVoidReturnTypes(true)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertTrue(output.contains(": void"));
    }

    @Test
    void includesNestedRelationsWhenEnabled() {
        UmlType outer = UmlType.builder()
                .name("Outer")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType inner = UmlType.builder()
                .name("Inner")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlRelation nested = UmlRelation.nestedRelation(outer.getId(), inner.getId());

        UmlModel model = UmlModel.builder()
                .addType(outer)
                .addType(inner)
                .addRelation(nested)
                .build();

        EitriConfig config = EitriConfig.builder()
                .showNested(true)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertTrue(output.contains("Outer +-- Inner"));
        assertTrue(output.contains(": nested"));
    }

    @Test
    void omitsNestedRelationsWhenDisabled() {
        UmlType outer = UmlType.builder()
                .name("Outer")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType inner = UmlType.builder()
                .name("Inner")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlRelation nested = UmlRelation.nestedRelation(outer.getId(), inner.getId());

        UmlModel model = UmlModel.builder()
                .addType(outer)
                .addType(inner)
                .addRelation(nested)
                .build();

        EitriConfig config = EitriConfig.builder()
                .showNested(false)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertFalse(output.contains("Outer +-- Inner"));
        assertFalse(output.contains(": nested"));
    }

    @Test
    void omitsNestedTypesAndTheirRelationsWhenDisabled() {
        UmlType outer = UmlType.builder()
                .name("Outer")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType inner = UmlType.builder()
                .name("Inner")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .outerTypeId(outer.getId())
                .build();

        UmlType other = UmlType.builder()
                .name("Other")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlRelation nested = UmlRelation.nestedRelation(outer.getId(), inner.getId());
        UmlRelation assoc = UmlRelation.association(inner.getId(), other.getId(), "uses");

        UmlModel model = UmlModel.builder()
                .addType(outer)
                .addType(inner)
                .addType(other)
                .addRelation(nested)
                .addRelation(assoc)
                .build();

        EitriConfig config = EitriConfig.builder()
                .showNested(false)
                .hideUnlinked(false)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertFalse(output.contains("+class Inner"));
        assertFalse(output.contains("Inner -- Other"));
        assertFalse(output.contains(": uses"));
    }
}
