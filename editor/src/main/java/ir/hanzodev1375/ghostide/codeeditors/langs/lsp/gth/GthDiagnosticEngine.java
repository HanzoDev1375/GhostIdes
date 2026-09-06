package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/** Diagnostics computation for theme documents (port of the JS {@code computeDiagnostics}). */
public final class GthDiagnosticEngine {

  public static final String SOURCE = "ghost-theme-lsp";

  private GthDiagnosticEngine() {}

  public static List<Diagnostic> compute(GthDocument doc) {
    List<Diagnostic> diags = new ArrayList<>();

    if (doc.braceBalance != 0) {
      diags.add(
          new Diagnostic(
              new Range(new Position(0, 0), new Position(0, 1)),
              "JSON is incomplete (braces `{` and `}` are unbalanced).",
              DiagnosticSeverity.Warning,
              SOURCE));
    }

    Map<String, Map<String, Integer>> seenKeys = new HashMap<>();
    for (GthToken t : doc.tokens) {
      if (!t.isKey) continue;
      String k = t.block != null ? t.block : "_root";
      seenKeys.computeIfAbsent(k, x -> new HashMap<>()).merge(t.raw, 1, Integer::sum);
    }

    for (GthToken t : doc.tokens) {
      Range range = doc.rangeOf(t.start, t.end);

      if (t.isKey) {
        if (t.block == null) {
          if (!ThemeSchema.isBlock(t.raw) && !t.raw.trim().isEmpty()) {
            diags.add(
                new Diagnostic(
                    range,
                    "Section \""
                        + t.raw
                        + "\" is unknown. Valid sections: "
                        + String.join(", ", ThemeSchema.BLOCKS)
                        + ".",
                    DiagnosticSeverity.Warning,
                    SOURCE));
          }
        } else if (!ThemeSchema.isValidKey(t.block, t.raw)) {
          diags.add(
              new Diagnostic(
                  range,
                  "Key \"" + t.raw + "\" does not exist in section " + t.block + ".",
                  DiagnosticSeverity.Warning,
                  SOURCE));
        }
        continue;
      }

      if ("imagepath".equals(t.owner) && t.raw.isEmpty()) continue;
      if ("blursize".equals(t.owner) && t.raw.isEmpty()) continue;

      if (t.owner == null || t.block == null) continue;
      String type = ThemeSchema.typeOf(t.block, t.owner);

      if ("number".equals(type)) {
        String trimmed = t.raw.trim();
        if (trimmed.isEmpty()) continue;
        if (!trimmed.matches("-?\\d+(\\.\\d+)?")) {
          diags.add(
              new Diagnostic(
                  range,
                  "Value \"" + t.raw + "\" must be a number (blursize).",
                  DiagnosticSeverity.Error,
                  SOURCE));
        } else {
          double num;
          try {
            num = Double.parseDouble(trimmed);
          } catch (NumberFormatException e) {
            num = -1;
          }
          if (num < 0 || num > 25) {
            diags.add(
                new Diagnostic(
                    range,
                    "blursize must be between 0 and 25.",
                    DiagnosticSeverity.Warning,
                    SOURCE));
          }
        }
        continue;
      }

      if ("path".equals(type)) continue;

      // color
      if (t.raw.startsWith("@")) {
        String[] parts = t.raw.substring(1).split("\\.", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
          diags.add(
              new Diagnostic(
                  range,
                  "Invalid reference. Expected format: \"@section.key\", e.g. \"@material3.surface\".",
                  DiagnosticSeverity.Error,
                  SOURCE));
          continue;
        }
        if (!ThemeSchema.isBlock(parts[0])) {
          diags.add(
              new Diagnostic(
                  range,
                  "Reference section \""
                      + parts[0]
                      + "\" does not exist. Valid sections: "
                      + String.join(", ", ThemeSchema.BLOCKS)
                      + ".",
                  DiagnosticSeverity.Error,
                  SOURCE));
          continue;
        }
        if (!ThemeSchema.isValidKey(parts[0], parts[1])) {
          diags.add(
              new Diagnostic(
                  range,
                  "Reference key \"" + parts[1] + "\" does not exist in section " + parts[0] + ".",
                  DiagnosticSeverity.Error,
                  SOURCE));
          continue;
        }
        Map<String, Map<String, String>> eff = doc.effectiveMap();
        String resolved = ThemeSchema.resolveRef(eff, t.raw);
        if (resolved.equals(t.raw) || resolved.startsWith("@")) {
          diags.add(
              new Diagnostic(
                  range,
                  "Reference cannot resolve to a final color (cycle or incomplete).",
                  DiagnosticSeverity.Error,
                  SOURCE));
        }
      } else if (!t.raw.trim().matches("#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})")) {
        diags.add(
            new Diagnostic(
                range,
                "Color \""
                    + t.raw
                    + "\" is not valid. Format: #RRGGBB or #RRGGBBAA, "
                    + "or an @section.key reference.",
                DiagnosticSeverity.Warning,
                SOURCE));
      }
    }

    return diags;
  }
}
