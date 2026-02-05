package no.ntnu.eitri.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ExtensionNormalizer class.
 */
class ExtensionNormalizerTest {

  @Test
  void testNormalizeExtensionWithNull() {
    assertNull(ExtensionNormalizer.normalizeExtension(null), "Null input should return null");
  }

  @Test
  void testNormalizeExtensionWithEmptyString() {
    assertNull(ExtensionNormalizer.normalizeExtension(""), "Empty string should return null");
  }

  @Test
  void testNormalizeExtensionWithWhitespaceOnly() {
    assertNull(ExtensionNormalizer.normalizeExtension("   "), "Whitespace-only string should return null");
  }

  @Test
  void testNormalizeExtensionWithValidExtensionWithoutDot() {
    assertEquals(".txt", ExtensionNormalizer.normalizeExtension("txt"), "Extension without dot should prepend a dot");
  }

  @Test
  void testNormalizeExtensionWithValidExtensionWithDot() {
    assertEquals(".jpg", ExtensionNormalizer.normalizeExtension(".jpg"), "Extension with dot should remain unchanged");
  }

  @Test
  void testNormalizeExtensionWithUpperCaseExtension() {
    assertEquals(".png", ExtensionNormalizer.normalizeExtension("PNG"), "Uppercase extension should be converted to lowercase");
  }

  @Test
  void testNormalizeExtensionWithWhitespaceAround() {
    assertEquals(".doc", ExtensionNormalizer.normalizeExtension("  doc  "), "Whitespace around extension should be trimmed");
  }
}