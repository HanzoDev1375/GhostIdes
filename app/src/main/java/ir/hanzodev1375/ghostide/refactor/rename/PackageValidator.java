package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PackageValidator {

  private static final Set<String> RESERVED_WORDS =
      new HashSet<>(
          Arrays.asList(
              "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
              "class", "const", "continue", "default", "do", "double", "else", "enum",
              "extends", "final", "finally", "float", "for", "goto", "if", "implements",
              "import", "instanceof", "int", "interface", "long", "native", "new",
              "package", "private", "protected", "public", "return", "short", "static",
              "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
              "transient", "try", "void", "volatile", "while", "true", "false", "null",
              "var", "yield", "record", "sealed", "permits"));

  public ValidationResult validateSyntax(String newPackage) {
    List<String> errors = new ArrayList<>();

    if (newPackage == null || newPackage.isEmpty()) {
      errors.add("Package name cannot be empty.");
      return ValidationResult.invalid(errors);
    }

    if (containsWhitespace(newPackage)) {
      errors.add("Package name cannot contain spaces.");
    }

    if (newPackage.startsWith(".") || newPackage.endsWith(".") || newPackage.contains("..")) {
      errors.add("Package name cannot contain empty segments.");
    }

    if (containsUppercase(newPackage)) {
      errors.add("Package name must be lowercase.");
    }

    String[] segments = newPackage.split("\\.", -1);
    for (String segment : segments) {
      if (segment.isEmpty()) {
        continue;
      }
      String segmentError = validateSegment(segment);
      if (segmentError != null && !errors.contains(segmentError)) {
        errors.add(segmentError);
      }
    }

    if (errors.isEmpty()) {
      return ValidationResult.valid();
    }
    return ValidationResult.invalid(errors);
  }

  public ValidationResult validateAgainstProject(
      String oldPackage, String newPackage, ScanResult scanResult) {
    ValidationResult syntaxResult = validateSyntax(newPackage);
    List<String> errors = new ArrayList<>(syntaxResult.getErrors());

    if (newPackage != null && newPackage.equals(oldPackage)) {
      errors.add("New package name must be different from the current package.");
    }

    if (syntaxResult.isValid()
        && scanResult != null
        && newPackage != null
        && !newPackage.equals(oldPackage)
        && destinationAlreadyExists(newPackage, scanResult)) {
      errors.add("A package named \"" + newPackage + "\" already exists in this project.");
    }

    if (errors.isEmpty()) {
      return ValidationResult.valid();
    }
    return ValidationResult.invalid(errors);
  }

  private boolean destinationAlreadyExists(String newPackage, ScanResult scanResult) {
    String relativePath = newPackage.replace('.', File.separatorChar);
    for (File sourceRoot : scanResult.getSourceRoots()) {
      File candidate = new File(sourceRoot, relativePath);
      if (candidate.isDirectory()) {
        File[] children = candidate.listFiles();
        if (children != null && children.length > 0) {
          return true;
        }
      }
    }
    return false;
  }

  private String validateSegment(String segment) {
    if (RESERVED_WORDS.contains(segment)) {
      return "\"" + segment + "\" is a reserved keyword and cannot be used as a package segment.";
    }
    char first = segment.charAt(0);
    if (!Character.isLetter(first) && first != '_') {
      return "Package segment \"" + segment + "\" must start with a letter.";
    }
    for (int i = 0; i < segment.length(); i++) {
      char c = segment.charAt(i);
      boolean valid = Character.isLetterOrDigit(c) || c == '_';
      if (!valid) {
        return "Package segment \"" + segment + "\" contains an invalid character.";
      }
    }
    return null;
  }

  private boolean containsWhitespace(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isWhitespace(value.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private boolean containsUppercase(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isUpperCase(value.charAt(i))) {
        return true;
      }
    }
    return false;
  }
}
