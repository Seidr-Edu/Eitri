package no.ntnu.eitri.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PackageClassifierTest {

  // =====================================================================
  // isCommonPackage
  // =====================================================================

  @Nested
  @DisplayName("isCommonPackage")
  class CommonPackageTests {

    @Test
    @DisplayName("java.util is a common package")
    void javaUtil() {
      assertTrue(PackageClassifier.isCommonPackage("java.util"));
    }

    @Test
    @DisplayName("java.lang is a common package")
    void javaLang() {
      assertTrue(PackageClassifier.isCommonPackage("java.lang"));
    }

    @Test
    @DisplayName("java.util.concurrent is a common package")
    void javaUtilConcurrent() {
      assertTrue(PackageClassifier.isCommonPackage("java.util.concurrent"));
    }

    @Test
    @DisplayName("javax.swing is a common package")
    void javaxSwing() {
      assertTrue(PackageClassifier.isCommonPackage("javax.swing"));
    }

    @Test
    @DisplayName("sun.misc is a common package")
    void sunMisc() {
      assertTrue(PackageClassifier.isCommonPackage("sun.misc"));
    }

    @Test
    @DisplayName("jdk.internal is a common package")
    void jdkInternal() {
      assertTrue(PackageClassifier.isCommonPackage("jdk.internal"));
    }

    @Test
    @DisplayName("com.sun.net is a common package")
    void comSunNet() {
      assertTrue(PackageClassifier.isCommonPackage("com.sun.net"));
    }

    @Test
    @DisplayName("'java' root alone is common")
    void javaRoot() {
      assertTrue(PackageClassifier.isCommonPackage("java"));
    }

    @Test
    @DisplayName("org.apache is not a common package")
    void orgApache() {
      assertFalse(PackageClassifier.isCommonPackage("org.apache"));
    }

    @Test
    @DisplayName("com.example is not a common package")
    void comExample() {
      assertFalse(PackageClassifier.isCommonPackage("com.example"));
    }

    @Test
    @DisplayName("null input returns false")
    void nullInput() {
      assertFalse(PackageClassifier.isCommonPackage(null));
    }

    @Test
    @DisplayName("blank input returns false")
    void blankInput() {
      assertFalse(PackageClassifier.isCommonPackage(""));
    }

    @Test
    @DisplayName("javaparser is not a common package (no dot separator)")
    void javaparserNotCommon() {
      assertFalse(PackageClassifier.isCommonPackage("javaparser"));
    }
  }

  // =====================================================================
  // isExternalPackage
  // =====================================================================

  @Nested
  @DisplayName("isExternalPackage")
  class ExternalPackageTests {

    @Test
    @DisplayName("package outside project root is external")
    void outsideProjectRoot() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.model");
      assertTrue(PackageClassifier.isExternalPackage("org.apache.commons", sources));
    }

    @Test
    @DisplayName("source package is not external")
    void sourcePackage() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isExternalPackage("no.ntnu.eitri.parser", sources));
    }

    @Test
    @DisplayName("sibling package under same root is not external")
    void siblingUnderRoot() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isExternalPackage("no.ntnu.eitri.model", sources));
    }

    @Test
    @DisplayName("parent package is not external")
    void parentPackage() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isExternalPackage("no.ntnu.eitri", sources));
    }

    @Test
    @DisplayName("child of source package is not external")
    void childOfSource() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isExternalPackage("no.ntnu.eitri.parser.java", sources));
    }

    @Test
    @DisplayName("completely different root is external")
    void differentRoot() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertTrue(PackageClassifier.isExternalPackage("com.github.javaparser", sources));
    }

    @Test
    @DisplayName("empty source packages means nothing is external")
    void emptySourcePackages() {
      assertFalse(PackageClassifier.isExternalPackage("any.package", Set.of()));
    }

    @Test
    @DisplayName("null package returns false")
    void nullPackage() {
      assertFalse(PackageClassifier.isExternalPackage(null, Set.of("no.ntnu.eitri")));
    }

    @Test
    @DisplayName("null sources returns false")
    void nullSources() {
      assertFalse(PackageClassifier.isExternalPackage("no.ntnu.eitri", null));
    }

    @Test
    @DisplayName("project root itself is not external")
    void projectRootItself() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.model");
      // Root is "no.ntnu.eitri"
      assertFalse(PackageClassifier.isExternalPackage("no.ntnu.eitri", sources));
    }

    @Test
    @DisplayName("sibling package is not external when sources are nested")
    void siblingNotExternalWithNestedSources() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.parser.java");
      // Root goes up from no.ntnu.eitri.parser to no.ntnu.eitri
      assertFalse(PackageClassifier.isExternalPackage("no.ntnu.eitri.model", sources));
    }

    @Test
    @DisplayName("truly external package is external even with nested sources")
    void trueExternalWithNestedSources() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.parser.java");
      assertTrue(PackageClassifier.isExternalPackage("com.github.javaparser", sources));
    }
  }

  // =====================================================================
  // isSiblingPackage
  // =====================================================================

  @Nested
  @DisplayName("isSiblingPackage")
  class SiblingPackageTests {

    @Test
    @DisplayName("model is sibling when parsing parser (same parent)")
    void modelIsSiblingOfParser() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertTrue(PackageClassifier.isSiblingPackage("no.ntnu.eitri.model", sources));
    }

    @Test
    @DisplayName("config is sibling when parsing parser (same parent)")
    void configIsSiblingOfParser() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertTrue(PackageClassifier.isSiblingPackage("no.ntnu.eitri.config", sources));
    }

    @Test
    @DisplayName("source package itself is not a sibling")
    void sourcePackageNotSibling() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isSiblingPackage("no.ntnu.eitri.parser", sources));
    }

    @Test
    @DisplayName("sub-package of source is not a sibling")
    void subPackageNotSibling() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isSiblingPackage("no.ntnu.eitri.parser.java", sources));
    }

    @Test
    @DisplayName("parent package is not a sibling")
    void parentIsNotSibling() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isSiblingPackage("no.ntnu.eitri", sources));
    }

    @Test
    @DisplayName("external package is not a sibling")
    void externalNotSibling() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isSiblingPackage("com.example.other", sources));
    }

    @Test
    @DisplayName("null package returns false")
    void nullPackage() {
      assertFalse(PackageClassifier.isSiblingPackage(null, Set.of("no.ntnu.eitri")));
    }

    @Test
    @DisplayName("empty sources returns false")
    void emptySources() {
      assertFalse(PackageClassifier.isSiblingPackage("any.pkg", Set.of()));
    }

    @Test
    @DisplayName("multiple source packages with shared parent detects sibling")
    void multipleSourcePackages() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.writer");
      assertTrue(PackageClassifier.isSiblingPackage("no.ntnu.eitri.model", sources));
    }

    @Test
    @DisplayName("single-segment package has no parent and cannot be sibling")
    void singleSegmentPackage() {
      Set<String> sources = Set.of("parser");
      assertFalse(PackageClassifier.isSiblingPackage("model", sources));
    }

    @Test
    @DisplayName("deep package under same project root is sibling")
    void deepPackageUnderRootIsSibling() {
      Set<String> sources = Set.of("example.cli");
      assertTrue(PackageClassifier.isSiblingPackage("example.api.plugins", sources));
      assertTrue(PackageClassifier.isSiblingPackage("example.core.export", sources));
    }

    @Test
    @DisplayName("source subtree package is not sibling even when under project root")
    void sourceSubtreeIsNotSiblingUnderRoot() {
      Set<String> sources = Set.of("example.cli");
      assertFalse(PackageClassifier.isSiblingPackage("example.cli.commands", sources));
    }

    @Test
    @DisplayName("common package is never sibling")
    void commonPackageNotSibling() {
      Set<String> sources = Set.of("no.ntnu.eitri.parser");
      assertFalse(PackageClassifier.isSiblingPackage("java.util", sources));
    }

    @Test
    @DisplayName("fallback parent-based sibling detection applies when no common project root")
    void fallbackSiblingDetectionWhenNoCommonProjectRoot() {
      Set<String> sources = Set.of("foo.alpha", "bar.beta");
      assertTrue(PackageClassifier.isSiblingPackage("foo.gamma", sources));
    }
  }

  // =====================================================================
  // computeProjectRoot
  // =====================================================================

  @Nested
  @DisplayName("computeProjectRoot")
  class ProjectRootTests {

    @Test
    @DisplayName("single source package returns parent as root")
    void singlePackage() {
      assertEquals("no.ntnu.eitri",
          PackageClassifier.computeProjectRoot(Set.of("no.ntnu.eitri.parser")));
    }

    @Test
    @DisplayName("two packages with common prefix")
    void twoPackagesCommonPrefix() {
      assertEquals("no.ntnu.eitri",
          PackageClassifier.computeProjectRoot(Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.model")));
    }

    @Test
    @DisplayName("packages with no common prefix returns null")
    void noCommonPrefix() {
      assertNull(PackageClassifier.computeProjectRoot(Set.of("com.example", "org.other")));
    }

    @Test
    @DisplayName("empty set returns null")
    void emptySet() {
      assertNull(PackageClassifier.computeProjectRoot(Set.of()));
    }

    @Test
    @DisplayName("null returns null")
    void nullInput() {
      assertNull(PackageClassifier.computeProjectRoot(null));
    }

    @Test
    @DisplayName("three packages with shared root")
    void threePackages() {
      assertEquals("no.ntnu.eitri",
          PackageClassifier.computeProjectRoot(
              Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.model", "no.ntnu.eitri.config")));
    }

    @Test
    @DisplayName("nested source packages go up one level from common prefix")
    void nestedSourcePackages() {
      assertEquals("no.ntnu.eitri",
          PackageClassifier.computeProjectRoot(
              Set.of("no.ntnu.eitri.parser", "no.ntnu.eitri.parser.java")));
    }
  }

  // =====================================================================
  // extractPackageFromFqn
  // =====================================================================

  @Nested
  @DisplayName("extractPackageFromFqn")
  class ExtractPackageTests {

    @Test
    @DisplayName("extracts package from standard FQN")
    void standardFqn() {
      assertEquals("com.example", PackageClassifier.extractPackageFromFqn("com.example.MyClass"));
    }

    @Test
    @DisplayName("extracts package from nested type FQN")
    void nestedClassFqn() {
      assertEquals("com.example", PackageClassifier.extractPackageFromFqn("com.example.Outer.Inner"));
    }

    @Test
    @DisplayName("extracts package from deep FQN")
    void deepFqn() {
      assertEquals("no.ntnu.eitri.parser.java",
          PackageClassifier.extractPackageFromFqn("no.ntnu.eitri.parser.java.TypeVisitor"));
    }

    @Test
    @DisplayName("returns empty for simple class name")
    void simpleClassName() {
      assertEquals("", PackageClassifier.extractPackageFromFqn("MyClass"));
    }

    @Test
    @DisplayName("returns empty for null")
    void nullInput() {
      assertEquals("", PackageClassifier.extractPackageFromFqn(null));
    }

    @Test
    @DisplayName("returns empty for blank")
    void blankInput() {
      assertEquals("", PackageClassifier.extractPackageFromFqn(""));
    }

    @Test
    @DisplayName("extracts common package")
    void commonPackageFqn() {
      assertEquals("java.util", PackageClassifier.extractPackageFromFqn("java.util.List"));
    }
  }
}
