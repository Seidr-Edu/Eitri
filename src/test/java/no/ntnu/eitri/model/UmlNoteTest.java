package no.ntnu.eitri.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UmlNoteTest {

  @Test
  void testBuilderAndGetters() {
    UmlNote note = UmlNote.builder()
        .text("Note text")
        .targetTypeId("TypeA")
        .targetMember("fieldA")
        .position(UmlNote.NotePosition.LEFT)
        .build();
    assertEquals("Note text", note.getText());
    assertEquals("TypeA", note.getTargetTypeId());
    assertEquals("fieldA", note.getTargetMember());
    assertEquals(UmlNote.NotePosition.LEFT, note.getPosition());
    assertFalse(note.isFloating());
    assertTrue(note.isMemberNote());
  }

  @Test
  void testFloatingNoteDefaults() {
    UmlNote note = UmlNote.builder()
        .text("Floating note")
        .build();
    assertTrue(note.isFloating());
    assertFalse(note.isMemberNote());
    assertEquals(UmlNote.NotePosition.RIGHT, note.getPosition());
    assertTrue(note.toString().contains("floating"));
  }

  @Test
  void testMemberNote() {
    UmlNote note = UmlNote.builder()
        .text("Member note")
        .targetTypeId("TypeB")
        .targetMember("methodB()")
        .build();
    assertFalse(note.isFloating());
    assertTrue(note.isMemberNote());
    assertEquals("methodB()", note.getTargetMember());
  }

  @Test
  void testToStringIncludesTargetAndText() {
    UmlNote note = UmlNote.builder()
        .text("ToString note")
        .targetTypeId("TypeC")
        .build();
    assertTrue(note.toString().contains("TypeC"));
    assertTrue(note.toString().contains("ToString note"));
  }
}