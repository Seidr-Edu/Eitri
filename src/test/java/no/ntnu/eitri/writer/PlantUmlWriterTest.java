package no.ntnu.eitri.writer;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.config.LayoutDirection;
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
                .fqn("com.example.Service")
                .simpleName("Service")
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

        assertTrue(output.contains("+class com.example.Service"));
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
                .fqn("com.example.Service")
                .simpleName("Service")
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
                .fqn("com.example.Outer")
                .simpleName("Outer")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType inner = UmlType.builder()
                .fqn("com.example.Outer$Inner")
                .simpleName("Inner")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlRelation nested = UmlRelation.nestedRelation(outer.getFqn(), inner.getFqn());

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

        assertTrue(output.contains("com.example.Outer +-- com.example.Outer$Inner"));
        assertTrue(output.contains(": nested"));
    }

    @Test
    void omitsNestedRelationsWhenDisabled() {
        UmlType outer = UmlType.builder()
                .fqn("com.example.Outer")
                .simpleName("Outer")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType inner = UmlType.builder()
                .fqn("com.example.Outer$Inner")
                .simpleName("Inner")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .outerTypeFqn(outer.getFqn())
                .build();

        UmlRelation nested = UmlRelation.nestedRelation(outer.getFqn(), inner.getFqn());

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

        assertFalse(output.contains("com.example.Outer +-- com.example.Outer$Inner"));
        assertFalse(output.contains(": nested"));
    }

    @Test
    void omitsNestedTypesAndTheirRelationsWhenDisabled() {
        UmlType outer = UmlType.builder()
                .fqn("com.example.Outer")
                .simpleName("Outer")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlType inner = UmlType.builder()
                .fqn("com.example.Outer$Inner")
                .simpleName("Inner")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .outerTypeFqn(outer.getFqn())
                .build();

        UmlType other = UmlType.builder()
                .fqn("com.example.Other")
                .simpleName("Other")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlRelation nested = UmlRelation.nestedRelation(outer.getFqn(), inner.getFqn());
        UmlRelation assoc = UmlRelation.association(inner.getFqn(), other.getFqn(), "uses");

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

        assertFalse(output.contains("+class com.example.Outer$Inner"));
        assertFalse(output.contains("com.example.Outer$Inner -- com.example.Other"));
        assertFalse(output.contains(": uses"));
    }

    @Test
    void includesHideEmptyFieldsAndMethods() {
        UmlModel model = UmlModel.builder()
                .build();

        EitriConfig config = EitriConfig.builder()
                .hideEmptyFields(true)
                .hideEmptyMethods(true)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertTrue(output.contains("hide empty fields"));
        assertTrue(output.contains("hide empty methods"));
    }


    @Test
    void rendersLeftToRightDirection() {
        UmlType type = UmlType.builder()
                .fqn("com.example.TestClass")
                .simpleName("TestClass")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlModel model = UmlModel.builder()
                .addType(type)
                .build();

        EitriConfig config = EitriConfig.builder()
                .direction(LayoutDirection.LEFT_TO_RIGHT)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertTrue(output.contains("left to right direction"));
    }

    @Test
    void rendersTopToBottomDirection() {
        UmlType type = UmlType.builder()
                .fqn("com.example.TestClass")
                .simpleName("TestClass")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlModel model = UmlModel.builder()
                .addType(type)
                .build();

        EitriConfig config = EitriConfig.builder()
                .direction(LayoutDirection.TOP_TO_BOTTOM)
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertTrue(output.contains("top to bottom direction"));
    }

    @Test
    void rendersDefaultDirectionWhenNotSet() {
        UmlType type = UmlType.builder()
                .fqn("com.example.TestClass")
                .simpleName("TestClass")
                .kind(TypeKind.CLASS)
                .visibility(Visibility.PUBLIC)
                .build();

        UmlModel model = UmlModel.builder()
                .addType(type)
                .build();

        EitriConfig config = EitriConfig.builder()
                .build();

        PlantUmlWriter writer = new PlantUmlWriter();
        String output = writer.render(model, config);

        assertTrue(output.contains("top to bottom direction"));
    }
}
