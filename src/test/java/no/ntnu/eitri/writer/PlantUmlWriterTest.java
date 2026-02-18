package no.ntnu.eitri.writer;

import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.LayoutDirection;
import no.ntnu.eitri.config.PlantUmlConfig;
import no.ntnu.eitri.config.RecordBinder;
import no.ntnu.eitri.config.RunConfig;
import no.ntnu.eitri.model.RelationKind;
import no.ntnu.eitri.model.TypeKind;
import no.ntnu.eitri.model.UmlField;
import no.ntnu.eitri.model.UmlGeneric;
import no.ntnu.eitri.model.UmlMethod;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.model.UmlRelation;
import no.ntnu.eitri.model.UmlType;
import no.ntnu.eitri.model.Visibility;
import no.ntnu.eitri.parser.java.JavaSourceParser;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUmlWriterTest {

        @TempDir
        Path tempDir;

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

                PlantUmlConfig config = config("showVoidReturnTypes", false);

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

                PlantUmlConfig config = config("showVoidReturnTypes", true);

                PlantUmlWriter writer = new PlantUmlWriter();
                String output = writer.render(model, config);

                assertTrue(output.contains(": void"));
        }

        @Test
        void methodGenericBoundsAreShownOnlyWhenShowGenericsEnabled() {
                UmlMethod method = UmlMethod.builder()
                                .name("withConfig")
                                .returnType("void")
                                .visibility(Visibility.PUBLIC)
                                .addGeneric(new UmlGeneric("C", "extends WriterConfig"))
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

                PlantUmlWriter writer = new PlantUmlWriter();
                String withoutGenerics = writer.render(model, config("showGenerics", false));
                String withGenerics = writer.render(model, config("showGenerics", true));

                assertTrue(withoutGenerics.contains("+withConfig()"));
                assertFalse(withoutGenerics.contains("withConfig<C extends"));
                assertTrue(withGenerics.contains("+withConfig<C extends WriterConfig>()"));
        }

        @Test
        void typeGenericBoundsAreShownOnlyWhenShowGenericsEnabled() {
                UmlType type = UmlType.builder()
                                .fqn("com.example.Repository")
                                .simpleName("Repository")
                                .kind(TypeKind.INTERFACE)
                                .visibility(Visibility.PUBLIC)
                                .addGeneric(new UmlGeneric("T", "extends Number"))
                                .build();

                UmlModel model = UmlModel.builder()
                                .addType(type)
                                .build();

                PlantUmlWriter writer = new PlantUmlWriter();
                String withoutGenerics = writer.render(model, config("showGenerics", false));
                String withGenerics = writer.render(model, config("showGenerics", true));

                assertFalse(withoutGenerics.contains("Repository<T extends Number>"));
                assertTrue(withGenerics.contains("+interface com.example.Repository<T extends Number>"));
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

                PlantUmlConfig config = config("showNested", true);

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

                PlantUmlConfig config = config("showNested", false);

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

                PlantUmlConfig config = config("showNested", false, "showUnlinked", false);

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

                PlantUmlConfig config = config("hideEmptyFields", true, "hideEmptyMethods", true);

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

                PlantUmlConfig config = config("direction", LayoutDirection.LEFT_TO_RIGHT);

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

                PlantUmlConfig config = config("direction", LayoutDirection.TOP_TO_BOTTOM);

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

                String output = new PlantUmlWriter().render(model, PlantUmlConfig.defaults());

                assertTrue(output.contains("top to bottom direction"));
        }

        @Test
        void writeCreatesParentDirectoriesWhenMissing() throws Exception {
                UmlType type = UmlType.builder()
                                .fqn("com.example.TestClass")
                                .simpleName("TestClass")
                                .kind(TypeKind.CLASS)
                                .visibility(Visibility.PUBLIC)
                                .build();
                UmlModel model = UmlModel.builder().addType(type).build();

                Path outputFile = tempDir.resolve("nested").resolve("diagram.puml");
                assertFalse(Files.exists(outputFile.getParent()));

                new PlantUmlWriter().write(model, PlantUmlConfig.defaults(), outputFile);

                assertTrue(Files.exists(outputFile.getParent()));
                assertTrue(Files.exists(outputFile));
        }

        @Test
        void writeThrowsWriteExceptionWhenOutputPathIsDirectory() throws Exception {
                UmlModel model = UmlModel.builder().build();
                Path directoryAsOutput = tempDir.resolve("outdir");
                Files.createDirectories(directoryAsOutput);

                WriteException exception = assertThrows(
                                WriteException.class,
                                () -> new PlantUmlWriter().write(model, PlantUmlConfig.defaults(), directoryAsOutput));

                assertTrue(exception.getMessage().contains("Failed to write PlantUML file"));
        }

        @Test
        void hideCommonPackagesRemovesTypeAndRelations() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlType common = UmlType.builder().fqn("java.util.List").simpleName("List")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addType(common)
                                .addRelation(UmlRelation.dependency(source.getFqn(), common.getFqn(), "uses"))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("hideCommonPackages", true);

                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("+class no.ntnu.eitri.parser.Source"));
                assertFalse(output.contains("+class java.util.List"));
                assertFalse(output.contains("no.ntnu.eitri.parser.Source ..> java.util.List"));
        }

        @Test
        void hideExternalPackagesRemovesTypeAndRelations() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlType external = UmlType.builder().fqn("org.external.Library").simpleName("Library")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addType(external)
                                .addRelation(UmlRelation.association(source.getFqn(), external.getFqn(), "uses"))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("hideExternalPackages", true);

                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("+class no.ntnu.eitri.parser.Source"));
                assertFalse(output.contains("+class org.external.Library"));
                assertFalse(output.contains("no.ntnu.eitri.parser.Source -- org.external.Library"));
        }

        @Test
        void hideSiblingPackagesRemovesTypeAndRelations() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlType sibling = UmlType.builder().fqn("no.ntnu.eitri.model.ModelType").simpleName("ModelType")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addType(sibling)
                                .addRelation(UmlRelation.association(source.getFqn(), sibling.getFqn(), "uses"))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("hideSiblingPackages", true);

                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("+class no.ntnu.eitri.parser.Source"));
                assertFalse(output.contains("+class no.ntnu.eitri.model.ModelType"));
                assertFalse(output.contains("no.ntnu.eitri.parser.Source -- no.ntnu.eitri.model.ModelType"));
        }

        @Test
        void rendersRelationToExternalFqnNotInModel() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.dependency(source.getFqn(), "org.external.Library", null))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("showDependency", true);

                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("no.ntnu.eitri.parser.Source ..> org.external.Library"));
                assertTrue(output.contains("class org.external.Library <<external>>"));
        }

        @Test
        void rendersRelationToExternalNestedFqnUsingDollarPath() {
                UmlType source = UmlType.builder().fqn("example.cli.JadxCLIArgs").simpleName("JadxCLIArgs")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.association(source.getFqn(), "example.api.JadxArgs.RenameEnum",
                                                null))
                                .sourcePackages(java.util.Set.of("example.cli"))
                                .build();

                PlantUmlConfig config = config("showAssociation", true, "showNested", true,
                                "hideExternalPackages", false);

                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("example.cli.JadxCLIArgs -- example.api.JadxArgs$RenameEnum"));
                assertFalse(output.contains("example.cli.JadxCLIArgs -- example.api.JadxArgs.RenameEnum"));
                assertTrue(output.contains("class example.api.JadxArgs$RenameEnum <<external>>"));
        }

        @Test
        void hidesRelationToExternalNestedFqnWhenShowNestedIsFalse() {
                UmlType source = UmlType.builder().fqn("example.cli.JadxCLIArgs").simpleName("JadxCLIArgs")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.association(source.getFqn(), "example.api.JadxArgs.RenameEnum",
                                                null))
                                .sourcePackages(java.util.Set.of("example.cli"))
                                .build();

                PlantUmlConfig config = config("showAssociation", true, "showNested", false,
                                "hideExternalPackages", false);

                String output = new PlantUmlWriter().render(model, config);

                assertFalse(output.contains("example.cli.JadxCLIArgs -- example.api.JadxArgs$RenameEnum"));
                assertFalse(output.contains("example.api.JadxArgs.RenameEnum"));
        }

        @Test
        void hidesRelationToExternalFqnWhenHideExternalPackages() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.dependency(source.getFqn(), "org.external.Library", null))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("showDependency", true, "hideExternalPackages", true);

                String output = new PlantUmlWriter().render(model, config);

                assertFalse(output.contains("org.external.Library"));
                assertFalse(output.contains("<<external>>"));
        }

        @Test
        void rendersRelationToCommonFqnNotInModel() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.association(source.getFqn(), "java.util.List", null))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("showAssociation", true, "hideCommonPackages", false);

                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("no.ntnu.eitri.parser.Source -- java.util.List"));
        }

        @Test
        void hidesRelationToCommonFqnWhenHideCommonPackages() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.association(source.getFqn(), "java.util.List", null))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("showAssociation", true, "hideCommonPackages", true);

                String output = new PlantUmlWriter().render(model, config);

                assertFalse(output.contains("java.util.List"));
        }

        @Test
        void rendersRelationToSiblingFqnNotInModel() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.association(source.getFqn(), "no.ntnu.eitri.model.UmlType",
                                                null))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("showAssociation", true);

                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("no.ntnu.eitri.parser.Source -- no.ntnu.eitri.model.UmlType"));
        }

        @Test
        void hidesRelationToSiblingFqnWhenHideSiblingPackages() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addRelation(UmlRelation.association(source.getFqn(), "no.ntnu.eitri.model.UmlType",
                                                null))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("showAssociation", true, "hideSiblingPackages", true);

                String output = new PlantUmlWriter().render(model, config);

                assertFalse(output.contains("no.ntnu.eitri.model.UmlType"));
        }

        @Test
        void relationToFilteredOutModelTypeIsNotRendered() {
                UmlType source = UmlType.builder().fqn("no.ntnu.eitri.parser.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlType common = UmlType.builder().fqn("java.util.List").simpleName("List")
                                .visibility(Visibility.PUBLIC).build();
                UmlModel model = UmlModel.builder()
                                .addType(source)
                                .addType(common)
                                .addRelation(UmlRelation.association(source.getFqn(), common.getFqn(), null))
                                .sourcePackages(java.util.Set.of("no.ntnu.eitri.parser"))
                                .build();

                PlantUmlConfig config = config("hideCommonPackages", true);

                String output = new PlantUmlWriter().render(model, config);

                assertFalse(output.contains("java.util.List"));
        }

        @Test
        void dedupesVisuallyIdenticalRelationsInRenderedOutput() {
                UmlType source = UmlType.builder().fqn("com.example.Source").simpleName("Source")
                                .visibility(Visibility.PUBLIC).build();
                UmlType target = UmlType.builder().fqn("com.example.Target").simpleName("Target")
                                .visibility(Visibility.PUBLIC).build();

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

                String output = new PlantUmlWriter().render(model, PlantUmlConfig.defaults());
                long count = output.lines()
                                .filter(line -> line.equals("com.example.Source -- \"1\" com.example.Target")).count();
                assertEquals(1, count);
        }

        @Test
        void doesNotUseFromMemberAsImplicitRelationLabel() {
                UmlType owner = UmlType.builder()
                                .fqn("com.example.Owner")
                                .simpleName("Owner")
                                .visibility(Visibility.PUBLIC)
                                .addField(UmlField.builder()
                                                .name("repository")
                                                .type("com.example.Dependency")
                                                .visibility(Visibility.PUBLIC)
                                                .build())
                                .build();
                UmlType dependency = UmlType.builder()
                                .fqn("com.example.Dependency")
                                .simpleName("Dependency")
                                .visibility(Visibility.PUBLIC)
                                .build();
                UmlRelation relation = UmlRelation.builder()
                                .fromTypeFqn(owner.getFqn())
                                .toTypeFqn(dependency.getFqn())
                                .kind(RelationKind.ASSOCIATION)
                                .fromMember("repository")
                                .build();
                UmlModel model = UmlModel.builder()
                                .addType(owner)
                                .addType(dependency)
                                .addRelation(relation)
                                .build();

                PlantUmlConfig config = config("showLabels", true);
                String output = new PlantUmlWriter().render(model, config);
                assertTrue(output.contains("com.example.Owner -- com.example.Dependency"));
                assertFalse(output.contains(": repository"));
        }

        @Test
        void rendersExplicitRelationLabelOnly() {
                UmlType owner = UmlType.builder()
                                .fqn("com.example.Owner")
                                .simpleName("Owner")
                                .visibility(Visibility.PUBLIC)
                                .build();
                UmlType dependency = UmlType.builder()
                                .fqn("com.example.Dependency")
                                .simpleName("Dependency")
                                .visibility(Visibility.PUBLIC)
                                .build();

                UmlRelation relation = UmlRelation.builder()
                                .fromTypeFqn(owner.getFqn())
                                .toTypeFqn(dependency.getFqn())
                                .kind(RelationKind.ASSOCIATION)
                                .fromMember("repository")
                                .label("uses")
                                .build();

                UmlModel model = UmlModel.builder()
                                .addType(owner)
                                .addType(dependency)
                                .addRelation(relation)
                                .build();

                PlantUmlConfig config = config("showLabels", true);
                String output = new PlantUmlWriter().render(model, config);

                assertTrue(output.contains("com.example.Owner -- com.example.Dependency : uses"));
                assertFalse(output.contains(": repository"));
        }

        @Test
        void parserAndWriterDoNotRenderGenericTypeParameterPlaceholders(@TempDir Path tempDir) throws Exception {
                Path src = tempDir.resolve("src");
                Files.createDirectories(src);
                Files.writeString(src.resolve("Holder.java"), """
                                package com.example;

                                import java.util.function.Supplier;
                                import no.ntnu.eitri.writer.DiagramWriter;

                                public class Holder<C> {
                                    private DiagramWriter<C> writer;
                                    private Supplier<C> supplier;

                                    public <T> void set(T value) {
                                    }
                                }
                                """);

                JavaSourceParser parser = new JavaSourceParser();
                RunConfig runConfig = new RunConfig(List.of(src), tempDir.resolve("out.puml"), null, null, false,
                                false);
                UmlModel model = parser.parse(List.of(src), runConfig);

                String output = new PlantUmlWriter().render(model, PlantUmlConfig.defaults());

                assertFalse(output.contains("+class C"));
                assertFalse(output.contains("+class T"));
                assertFalse(output.contains("..> C"));
                assertFalse(output.contains("..> T"));
        }

        private static PlantUmlConfig config(Object... keyValues) {
                if (keyValues.length % 2 != 0) {
                        throw new IllegalArgumentException("Key/value pairs expected");
                }
                Map<String, Object> map = new LinkedHashMap<>();
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
