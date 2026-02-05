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
    @DisplayName("Types are keyed by id and retrievable")
    void typesById() {
        UmlType type = UmlType.builder()
                .name("Customer")
                .packageName("com.example")
                .build();

        UmlModel model = UmlModel.builder()
                .addType(type)
                .build();

        assertTrue(model.hasType(type.getId()));
        assertEquals(type, model.getType(type.getId()).orElseThrow());
    }

    @Test
    @DisplayName("Types are sorted by package then name")
    void typesSorted() {
        UmlType a = UmlType.builder().name("A").packageName("pkg").build();
        UmlType b = UmlType.builder().name("B").packageName("pkg").build();
        UmlType c = UmlType.builder().name("C").packageName("aaa").build();

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
        UmlType a = UmlType.builder().name("A").packageName("pkg").build();
        UmlType b = UmlType.builder().name("B").packageName("pkg").build();
        UmlType c = UmlType.builder().name("C").packageName("aaa").build();

        UmlModel model = UmlModel.builder()
                .addType(a)
                .addType(b)
                .addType(c)
                .build();

        assertEquals(List.of("aaa", "pkg"), model.getPackages());
    }

    @Test
    @DisplayName("Types in package and default package")
    void typesInPackage() {
        UmlType pkgType = UmlType.builder().name("A").packageName("pkg").build();
        UmlType defaultType = UmlType.builder().name("B").build();

        UmlModel model = UmlModel.builder()
                .addType(pkgType)
                .addType(defaultType)
                .build();

        assertEquals(List.of(pkgType), model.getTypesInPackage("pkg"));
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
}
