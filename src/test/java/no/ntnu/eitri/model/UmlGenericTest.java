package no.ntnu.eitri.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UmlGenericTest {

  @Test
  void testConstructorWithValidIdentifier() {
    UmlGeneric generic = new UmlGeneric("T");
    assertEquals("T", generic.identifier());
    assertNull(generic.bounds());
  }

  @Test
  void testConstructorWithIdentifierAndBounds() {
    UmlGeneric generic = new UmlGeneric("E", "extends Comparable");
    assertEquals("E", generic.identifier());
    assertEquals("extends Comparable", generic.bounds());
  }

  @Test
  void testConstructorWithNullIdentifierThrowsException() {
    Exception exception = assertThrows(NullPointerException.class, () -> new UmlGeneric(null));
    assertEquals("Generic identifier cannot be null", exception.getMessage());
  }

  @Test
  void testConstructorWithNullBounds() {
    UmlGeneric generic = new UmlGeneric("T", null);
    assertEquals("T", generic.identifier());
    assertNull(generic.bounds());
  }

  @Test
  void testEqualityAndHashCode() {
    UmlGeneric generic1 = new UmlGeneric("T", "extends Comparable");
    UmlGeneric generic2 = new UmlGeneric("T", "extends Comparable");
    UmlGeneric generic3 = new UmlGeneric("E", "extends Number");

    assertEquals(generic1, generic2);
    assertNotEquals(generic1, generic3);
    assertEquals(generic1.hashCode(), generic2.hashCode());
    assertNotEquals(generic1.hashCode(), generic3.hashCode());
  }

  @Test
  void testEmptyIdentifierIsAllowed() {
    UmlGeneric generic = new UmlGeneric("", "extends java.lang.Object");
    assertEquals("", generic.identifier());
    assertEquals("extends java.lang.Object", generic.bounds());
  }
}
