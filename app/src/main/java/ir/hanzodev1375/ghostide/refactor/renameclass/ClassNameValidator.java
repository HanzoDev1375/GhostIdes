package ir.hanzodev1375.ghostide.refactor.renameclass;

import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClassNameValidator {

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

  public ValidationResult validate(String oldClassName, String newClassName, File targetFile) {
    List<String> errors = new ArrayList<>();

    if (newClassName == null || newClassName.isEmpty()) {
      errors.add("Class name cannot be empty.");
      return ValidationResult.invalid(errors);
    }
    if (newClassName.equals(oldClassName)) {
      errors.add("New class name must be different from the current name.");
    }
    if (containsWhitespace(newClassName)) {
      errors.add("Class name cannot contain spaces.");
    }
    if (RESERVED_WORDS.contains(newClassName)) {
      errors.add("\"" + newClassName + "\" is a reserved keyword.");
    }
    char first = newClassName.isEmpty() ? ' ' : newClassName.charAt(0);
    if (!errors.isEmpty() && newClassName.isEmpty()) {
      return ValidationResult.invalid(errors);
    }
    if (!Character.isLetter(first) && first != '_' && first != '$') {
      errors.add("Class name must start with a letter.");
    } else if (Character.isDigit(first)) {
      errors.add("Class name cannot start with a digit.");
    }
    for (int i = 0; i < newClassName.length(); i++) {
      char c = newClassName.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
        errors.add("Class name contains an invalid character.");
        break;
      }
    }

    File parentDirectory = targetFile.getParentFile();
    if (parentDirectory != null) {
      String extension = targetFile.getName().endsWith(".kt") ? ".kt" : ".java";
      File collidingFile = new File(parentDirectory, newClassName + extension);
      if (collidingFile.exists()) {
        errors.add("A file named \"" + newClassName + extension + "\" already exists here.");
      }
    }

    if (errors.isEmpty()) {
      return ValidationResult.valid();
    }
    return ValidationResult.invalid(errors);
  }

  private boolean containsWhitespace(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isWhitespace(value.charAt(i))) {
        return true;
      }
    }
    return false;
  }
}
