package no.ntnu.eitri.writer;

import no.ntnu.eitri.config.EitriConfig;
import no.ntnu.eitri.config.LayoutDirection;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
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
    void rendersLeftToRightDirection() {
        UmlType type = UmlType.builder()
                .name("TestClass")
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
                .name("TestClass")
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
                .name("TestClass")
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
