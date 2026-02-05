package no.ntnu.eitri.model;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ModifierTest {

  @Test
  void testNone() {
    // Test that none() returns an empty EnumSet
    Set<Modifier> modifiers = Modifier.none();
    assertNotNull(modifiers, "The returned set should not be null");
    assertTrue(modifiers.isEmpty(), "The returned set should be empty");
  }

  @Test
  void testOfWithNoModifiers() {
    // Test that of() with no arguments returns an empty set
    Set<Modifier> modifiers = Modifier.of();
    assertNotNull(modifiers, "The returned set should not be null");
    assertTrue(modifiers.isEmpty(), "The returned set should be empty");
  }

  @Test
  void testOfWithSingleModifier() {
    // Test that of() with a single modifier returns a set containing that modifier
    Set<Modifier> modifiers = Modifier.of(Modifier.STATIC);
    assertNotNull(modifiers, "The returned set should not be null");
    assertEquals(1, modifiers.size(), "The set should contain exactly one modifier");
    assertTrue(modifiers.contains(Modifier.STATIC), "The set should contain the STATIC modifier");
  }

  @Test
  void testOfWithMultipleModifiers() {
    // Test that of() with multiple modifiers returns a set containing all of them
    Set<Modifier> modifiers = Modifier.of(Modifier.STATIC, Modifier.FINAL, Modifier.ABSTRACT);
    assertNotNull(modifiers, "The returned set should not be null");
    assertEquals(3, modifiers.size(), "The set should contain exactly three modifiers");
    assertTrue(modifiers.contains(Modifier.STATIC), "The set should contain the STATIC modifier");
    assertTrue(modifiers.contains(Modifier.FINAL), "The set should contain the FINAL modifier");
    assertTrue(modifiers.contains(Modifier.ABSTRACT), "The set should contain the ABSTRACT modifier");
  }
}