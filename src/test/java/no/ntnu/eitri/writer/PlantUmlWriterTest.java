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

    @Test
    void hideCommonPackagesRemovesTypeAndRelations() {
        UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source").visibility(Visibility.PUBLIC).build();
        UmlType common = UmlType.builder().fqn("java.util.List").simpleName("List").visibility(Visibility.PUBLIC).build();
        UmlModel model = UmlModel.builder()
                .addType(source)
                .addType(common)
                .addRelation(UmlRelation.dependency(source.getFqn(), common.getFqn(), "uses"))
                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                .build();

        EitriConfig config = EitriConfig.builder()
                .hideCommonPackages(true)
                .build();

        String output = new PlantUmlWriter().render(model, config);

        assertTrue(output.contains("+class no.ntnu.eitri.parser.Source"));
        assertFalse(output.contains("+class java.util.List"));
        assertFalse(output.contains("no.ntnu.eitri.parser.Source ..> java.util.List"));
    }

    @Test
    void hideExternalPackagesRemovesTypeAndRelations() {
        UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source").visibility(Visibility.PUBLIC).build();
        UmlType external = UmlType.builder().fqn("org.external.Library").simpleName("Library").visibility(Visibility.PUBLIC).build();
        UmlModel model = UmlModel.builder()
                .addType(source)
                .addType(external)
                .addRelation(UmlRelation.association(source.getFqn(), external.getFqn(), "uses"))
                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                .build();

        EitriConfig config = EitriConfig.builder()
                .hideExternalPackages(true)
                .build();

        String output = new PlantUmlWriter().render(model, config);

        assertTrue(output.contains("+class no.ntnu.eitri.parser.Source"));
        assertFalse(output.contains("+class org.external.Library"));
        assertFalse(output.contains("no.ntnu.eitri.parser.Source -- org.external.Library"));
    }

    @Test
    void hideSiblingPackagesRemovesTypeAndRelations() {
        UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source").visibility(Visibility.PUBLIC).build();
        UmlType sibling = UmlType.builder().fqn("no.ntnu.eitri.model.ModelType").simpleName("ModelType").visibility(Visibility.PUBLIC).build();
        UmlModel model = UmlModel.builder()
                .addType(source)
                .addType(sibling)
                .addRelation(UmlRelation.association(source.getFqn(), sibling.getFqn(), "uses"))
                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                .build();

        EitriConfig config = EitriConfig.builder()
                .hideSiblingPackages(true)
                .build();

        String output = new PlantUmlWriter().render(model, config);

        assertTrue(output.contains("+class no.ntnu.eitri.parser.Source"));
        assertFalse(output.contains("+class no.ntnu.eitri.model.ModelType"));
        assertFalse(output.contains("no.ntnu.eitri.parser.Source -- no.ntnu.eitri.model.ModelType"));
    }

    @Test
    void dedupesVisuallyIdenticalRelationsInRenderedOutput() {
        UmlType source = UmlType.builder().fqn("com.example.Source").simpleName("Source").visibility(Visibility.PUBLIC).build();
        UmlType target = UmlType.builder().fqn("com.example.Target").simpleName("Target").visibility(Visibility.PUBLIC).build();

        UmlRelation first = UmlRelation.builder()
                .fromTypeFqn(source.getFqn())
                .toTypeFqn(target.getFqn())
                .kind(no.ntnu.eitri.model.RelationKind.ASSOCIATION)
                .toMultiplicity("1")
                .build();
        UmlRelation second = UmlRelation.builder()
                .fromTypeFqn(source.getFqn())
                .toTypeFqn(target.getFqn())
                .kind(no.ntnu.eitri.model.RelationKind.ASSOCIATION)
                .toMultiplicity("1")
                .build();

        UmlModel model = UmlModel.builder()
                .addType(source)
                .addType(target)
                .addRelation(first)
                .addRelation(second)
                .build();

        String output = new PlantUmlWriter().render(model, EitriConfig.builder().build());
        long count = output.lines().filter(line -> line.equals("com.example.Source -- \"1\" com.example.Target")).count();
        assertEquals(1, count);
    }
}
