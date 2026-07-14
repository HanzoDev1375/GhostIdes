package ir.hanzodev1375.ghostide.refactor.rename.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {

  private final boolean valid;
  private final List<String> errors;

  private ValidationResult(boolean valid, List<String> errors) {
    this.valid = valid;
    this.errors = Collections.unmodifiableList(errors);
  }

  public static ValidationResult valid() {
    return new ValidationResult(true, new ArrayList<>());
  }

  public static ValidationResult invalid(List<String> errors) {
    return new ValidationResult(false, new ArrayList<>(errors));
  }

  public static ValidationResult invalid(String error) {
    List<String> errors = new ArrayList<>();
    errors.add(error);
    return new ValidationResult(false, errors);
  }

  public boolean isValid() {
    return valid;
  }

  public List<String> getErrors() {
    return errors;
  }

  public String getFirstError() {
    return errors.isEmpty() ? null : errors.get(0);
  }
}
