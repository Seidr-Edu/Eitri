package no.ntnu.eitri.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UmlNoteTest {

  @Test
  void testGetText() {
    // Arrange
    String expectedText = "This is a test note";
    UmlNote note = UmlNote.builder()
        .text(expectedText)
        .build();

    // Act
    String actualText = note.getText();

    // Assert
    assertEquals(expectedText, actualText, "The text returned by getText() should match the text set in the builder");
  }
}