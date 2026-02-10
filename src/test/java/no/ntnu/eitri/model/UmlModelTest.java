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
        UmlType a = UmlType.builder().simpleName("A").build();
        UmlType b = UmlType.builder().simpleName("B").build();
        UmlType c = UmlType.builder().simpleName("C").build();

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
        UmlType a = UmlType.builder().simpleName("A").build();
        UmlType b = UmlType.builder().simpleName("B").build();
        UmlType c = UmlType.builder().simpleName("C").build();

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
        UmlType pkgType = UmlType.builder().simpleName("A").build();
        UmlType defaultType = UmlType.builder().simpleName("B").build();

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
