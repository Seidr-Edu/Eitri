package no.ntnu.eitri.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UmlStereotypeTest {

  @Test
  void testConstructorWithAllParameters() {
    UmlStereotype stereotype = new UmlStereotype("Singleton", 'S', "#FF0000", List.of("value1", "value2"));
    assertEquals("Singleton", stereotype.name());
    assertEquals('S', stereotype.spotChar());
    assertEquals("#FF0000", stereotype.spotColor());
    assertEquals(List.of("value1", "value2"), stereotype.values());
  }

  @Test
  void testConstructorWithNameOnly() {
    UmlStereotype stereotype = new UmlStereotype("Singleton");
    assertEquals("Singleton", stereotype.name());
    assertNull(stereotype.spotChar());
    assertNull(stereotype.spotColor());
    assertTrue(stereotype.values().isEmpty());
  }

  @Test
  void testConstructorWithSpotOnly() {
    UmlStereotype stereotype = new UmlStereotype("Service", 'S', "#00FF00");
    assertEquals("Service", stereotype.name());
    assertEquals('S', stereotype.spotChar());
    assertEquals("#00FF00", stereotype.spotColor());
    assertTrue(stereotype.values().isEmpty());
  }

  @Test
  void testConstructorWithValuesOnly() {
    UmlStereotype stereotype = new UmlStereotype("Controller", List.of("value1", "value2"));
    assertEquals("Controller", stereotype.name());
    assertNull(stereotype.spotChar());
    assertNull(stereotype.spotColor());
    assertEquals(List.of("value1", "value2"), stereotype.values());
  }

  @Test
  void testHasValuesWhenValuesArePresent() {
    UmlStereotype stereotype = new UmlStereotype("Service", List.of("value1"));
    assertTrue(stereotype.hasValues());
  }

  @Test
  void testHasValuesWhenValuesAreEmpty() {
    UmlStereotype stereotype = new UmlStereotype("Service", List.of());
    assertFalse(stereotype.hasValues());
  }

  @Test
  void testNullNameThrowsException() {
    assertThrows(NullPointerException.class, () -> new UmlStereotype(null));
  }

  @Test
  void testImmutableValuesList() {
    UmlStereotype stereotype = new UmlStereotype("Service", List.of("value1", "value2"));
    assertThrows(UnsupportedOperationException.class, () -> stereotype.values().add("value3"));
  }
}