package no.ntnu.eitri.util;

import java.util.Set;

/**
 * Classifies packages relative to the parsed source packages.
 *
 * <p>
 * Used to determine whether a type's package is:
 * <ul>
 * <li><b>Common</b>: Part of the Java standard library (java.*, javax.*, sun.*,
 * jdk.*)</li>
 * <li><b>External</b>: Not sharing a common root with any source package</li>
 * <li><b>Sibling</b>: Shares a parent package with a source package but is not
 * itself a source package. In practice this is evaluated as "inside the same
 * project root, but outside the parsed source subtree" so deeper branches are
 * also treated as siblings.</li>
 * </ul>
 */
public final class PackageClassifier {

  private static final Set<String> COMMON_ROOTS = Set.of(
      "java", "javax", "sun", "jdk", "com.sun");

  private PackageClassifier() {
    // Utility class
  }

  /**
   * Checks if a package is a common Java platform package.
   *
   * <p>
   * Common packages include java.*, javax.*, sun.*, jdk.*, and com.sun.*.
   *
   * @param packageName the package name to check
   * @return true if the package is a common Java platform package
   */
  public static boolean isCommonPackage(String packageName) {
    if (packageName == null || packageName.isBlank()) {
      return false;
    }
    for (String root : COMMON_ROOTS) {
      if (packageName.equals(root) || packageName.startsWith(root + ".")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a package is external to the project.
   *
   * <p>
   * A package is external if it does not share a common root with any of the
   * source packages. The common root is determined by finding the longest shared
   * package prefix among source packages and checking if the target package
   * starts
   * with that root.
   *
   * <p>
   * If source packages are empty, no package is considered external.
   *
   * @param packageName    the package name to check
   * @param sourcePackages the set of packages parsed from source files
   * @return true if the package is external to the project
   */
  public static boolean isExternalPackage(String packageName, Set<String> sourcePackages) {
    if (packageName == null || packageName.isBlank() || sourcePackages == null || sourcePackages.isEmpty()) {
      return false;
    }

    String projectRoot = computeProjectRoot(sourcePackages);
    if (projectRoot == null || projectRoot.isEmpty()) {
      return false;
    }

    return !packageName.equals(projectRoot) && !packageName.startsWith(projectRoot + ".");
  }

  /**
   * Checks if a package is a sibling package.
   *
   * <p>
   * A sibling package is part of the same project root but outside the parsed
   * source subtree. For example, if parsing {@code example.cli}, then
   * {@code example.api.plugins} and {@code example.core.export} are siblings.
   *
   * <p>
   * If source packages are empty, no package is considered a sibling.
   * Common packages are not considered siblings.
   *
   * @param packageName    the package name to check
   * @param sourcePackages the set of packages parsed from source files
   * @return true if the package is a sibling package
   */
  public static boolean isSiblingPackage(String packageName, Set<String> sourcePackages) {
    if (packageName == null || packageName.isBlank() || sourcePackages == null || sourcePackages.isEmpty()) {
      return false;
    }

    // Common packages are never considered siblings
    if (isCommonPackage(packageName)) {
      return false;
    }
    // If it's already a source package, it's not a sibling
    if (sourcePackages.contains(packageName)) {
      return false;
    }

    // If it's a sub-package of a source package, it's not a sibling
    for (String src : sourcePackages) {
      if (packageName.startsWith(src + ".")) {
        return false;
      }
    }

    // If package is under the project root but outside all source subtrees,
    // classify it as sibling. This captures deeper sibling branches such as
    // 'example.api.plugins' when parsing 'example.cli'.
    String projectRoot = computeProjectRoot(sourcePackages);
    if (projectRoot != null && !projectRoot.isEmpty()) {
      if (packageName.equals(projectRoot)) {
        return false;
      }
      if (packageName.startsWith(projectRoot + ".")) {
        return true;
      }
    }

    // Fallback for edge-cases where a stable project root cannot be computed:
    // keep legacy immediate-parent sibling semantics.
    String targetParent = parentPackage(packageName);
    if (targetParent == null) {
      return false;
    }

    for (String src : sourcePackages) {
      String srcParent = parentPackage(src);
      if (srcParent != null && targetParent.equals(srcParent)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Computes the common root package from a set of source packages.
   *
   * <p>
   * Finds the longest common package prefix shared by all source packages.
   * If the computed prefix is itself one of the source packages (e.g., all
   * sources
   * are nested under one package), the parent is used instead so that sibling
   * packages are not incorrectly classified as external.
   *
   * <p>
   * Examples:
   * <ul>
   * <li>{@code [no.ntnu.eitri.parser]} → {@code no.ntnu.eitri}</li>
   * <li>{@code [no.ntnu.eitri.parser, no.ntnu.eitri.parser.java]} →
   * {@code no.ntnu.eitri}</li>
   * <li>{@code [no.ntnu.eitri.parser, no.ntnu.eitri.model]} →
   * {@code no.ntnu.eitri}</li>
   * </ul>
   *
   * @param sourcePackages the set of source packages
   * @return the common root, or null if no common root exists
   */
  static String computeProjectRoot(Set<String> sourcePackages) {
    if (sourcePackages == null || sourcePackages.isEmpty()) {
      return null;
    }

    String[] packages = sourcePackages.toArray(new String[0]);
    String root = packages[0];

    for (int i = 1; i < packages.length; i++) {
      root = commonPrefix(root, packages[i]);
      if (root.isEmpty()) {
        return null;
      }
    }

    // If the computed root is itself a source package, go up one level
    // so that sibling packages (e.g., model when parsing parser) are not
    // classified as external
    if (sourcePackages.contains(root)) {
      String parent = parentPackage(root);
      return parent != null ? parent : root;
    }

    return root;
  }

  /**
   * Finds the longest common package prefix between two package names.
   *
   * @param a first package name
   * @param b second package name
   * @return the common package prefix, or empty string if none
   */
  private static String commonPrefix(String a, String b) {
    String[] partsA = a.split("\\.");
    String[] partsB = b.split("\\.");

    StringBuilder common = new StringBuilder();
    int minLen = Math.min(partsA.length, partsB.length);

    for (int i = 0; i < minLen; i++) {
      if (!partsA[i].equals(partsB[i])) {
        break;
      }
      if (i > 0) {
        common.append(".");
      }
      common.append(partsA[i]);
    }

    return common.toString();
  }

  /**
   * Returns the parent package of a given package name.
   *
   * @param packageName the package name
   * @return the parent package, or null if the package has no parent
   */
  private static String parentPackage(String packageName) {
    if (packageName == null || packageName.isBlank()) {
      return null;
    }
    int lastDot = packageName.lastIndexOf('.');
    return lastDot > 0 ? packageName.substring(0, lastDot) : null;
  }

  /**
   * Extracts the package name from a fully-qualified type name.
   *
   * <p>
   * Uses the same heuristic as {@code UmlType.computePackageName()}: walks the
   * dot-separated segments left-to-right and treats the first segment starting
   * with an uppercase letter as the beginning of the type hierarchy. Everything
   * before it is the package.
   *
   * @param fqn the fully-qualified type name (e.g., {@code com.example.Foo})
   * @return the package name, or an empty string if no package can be determined
   */
  public static String extractPackageFromFqn(String fqn) {
    if (fqn == null || fqn.isBlank()) {
      return "";
    }
    String[] parts = fqn.split("\\.");
    StringBuilder pkg = new StringBuilder();
    for (String part : parts) {
      if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
        break;
      }
      if (!pkg.isEmpty()) {
        pkg.append(".");
      }
      pkg.append(part);
    }
    return pkg.toString();
  }
}
