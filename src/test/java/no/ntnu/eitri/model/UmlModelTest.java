package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UmlModelTest {

    @Test
    @DisplayName("Default name is diagram")
    void defaultName() {
        UmlModel model = UmlModel.builder().build();
        assertEquals("diagram", model.getName());
    }

    @Test
    @DisplayName("Types are keyed by fqn and retrievable")
    void typesByFqn() {
        UmlType type = UmlType.builder()
                .fqn("com.example.Customer")
                .simpleName("Customer")
                .build();

        UmlModel model = UmlModel.builder()
                .addType(type)
                .build();

        assertTrue(model.hasType(type.getFqn()));
        assertEquals(type, model.getType(type.getFqn()).orElseThrow());
    }

    @Test
    @DisplayName("Types are sorted by package then name")
    void typesSorted() {
        UmlType a = UmlType.builder().fqn("com.example.A").simpleName("A").build();
        UmlType b = UmlType.builder().fqn("com.example.B").simpleName("B").build();
        UmlType c = UmlType.builder().fqn("acme.example.C").simpleName("C").build();

        UmlModel model = UmlModel.builder()
                .addType(b)
                .addType(c)
                .addType(a)
                .build();

        List<UmlType> sorted = model.getTypesSorted();
        assertEquals(List.of(c, a, b), sorted);
    }

    @Test
    @DisplayName("Packages list is distinct and sorted")
    void packagesList() {
        UmlType a = UmlType.builder().fqn("com.example.A").simpleName("A").build();
        UmlType b = UmlType.builder().fqn("com.example.B").simpleName("B").build();
        UmlType c = UmlType.builder().fqn("com.example.C").simpleName("C").build();

        UmlModel model = UmlModel.builder()
                .addType(a)
                .addType(b)
                .addType(c)
                .build();

        assertEquals(List.of("com.example"), model.getPackages());
    }

    @Test
    @DisplayName("Types in package and default package")
    void typesInPackage() {
        UmlType pkgType = UmlType.builder().fqn("com.example.A").simpleName("A").build();
        UmlType defaultType = UmlType.builder().fqn("B").simpleName("B").build();

        UmlModel model = UmlModel.builder()
                .addType(pkgType)
                .addType(defaultType)
                .build();

        assertEquals(List.of(pkgType), model.getTypesInPackage("com.example"));
        assertEquals(List.of(defaultType), model.getTypesInDefaultPackage());
    }

    @Test
    @DisplayName("Relations and notes are unmodifiable")
    void relationsAndNotesUnmodifiable() {
        UmlRelation relation = UmlRelation.association("A", "B", null);
        UmlNote note = UmlNote.builder().text("note").build();

        UmlModel model = UmlModel.builder()
                .addRelation(relation)
                .addNote(note)
                .build();

        List<UmlRelation> relations = model.getRelations();
        List<UmlNote> notes = model.getNotes();

        assertThrows(UnsupportedOperationException.class, () -> relations.add(relation));
        assertThrows(UnsupportedOperationException.class, () -> notes.add(note));
    }

    @Test
    @DisplayName("Types collection is unmodifiable")
    void typesCollectionUnmodifiable() {
        UmlType type = UmlType.builder().fqn("com.example.A").simpleName("A").build();
        UmlModel model = UmlModel.builder().addType(type).build();
        assertThrows(UnsupportedOperationException.class, () -> model.getTypes().clear());
    }

    @Test
    @DisplayName("Relations are sorted with hierarchy first")
    void relationsSortedHierarchyFirst() {
        UmlRelation assoc = UmlRelation.association("com.example.A", "com.example.B", null);
        UmlRelation impl = UmlRelation.implementsRelation("com.example.C", "com.example.D");
        UmlRelation ext = UmlRelation.extendsRelation("com.example.E", "com.example.F");

        UmlModel model = UmlModel.builder()
                .addRelation(assoc)
                .addRelation(impl)
                .addRelation(ext)
                .build();

        List<UmlRelation> sorted = model.getRelationsSorted();
        assertEquals(RelationKind.EXTENDS, sorted.get(0).getKind());
        assertEquals(RelationKind.IMPLEMENTS, sorted.get(1).getKind());
        assertEquals(RelationKind.ASSOCIATION, sorted.get(2).getKind());
    }

    @Test
    @DisplayName("Source packages are defensive copied and unmodifiable")
    void sourcePackagesCopiedAndUnmodifiable() {
        java.util.Set<String> sourcePackages = new java.util.HashSet<>(List.of("com.example"));
        UmlModel model = UmlModel.builder()
                .sourcePackages(sourcePackages)
                .build();
        sourcePackages.add("org.other");

        assertEquals(java.util.Set.of("com.example"), model.getSourcePackages());
        assertThrows(UnsupportedOperationException.class, () -> model.getSourcePackages().add("x.y"));
    }
}
