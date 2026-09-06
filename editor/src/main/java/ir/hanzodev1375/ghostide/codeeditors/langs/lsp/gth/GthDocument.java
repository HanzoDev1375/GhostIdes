package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/** In-memory representation of an open theme document (port of the JS document store). */
public final class GthDocument {

  public final String uri;
  public final String text;
  public final int[] lineStarts;
  public final List<GthToken> tokens;
  public final int braceBalance;

  public GthDocument(String uri, String text) {
    this.uri = uri;
    this.text = text;
    this.lineStarts = GthScanner.buildLineStarts(text);
    GthScanner.ScanResult scan = GthScanner.scan(text);
    this.tokens = scan.tokens;
    this.braceBalance = scan.braceBalance;
  }

  public Position offsetToPosition(int offset) {
    int[] starts = lineStarts;
    int lo = 0;
    int hi = starts.length - 1;
    while (lo < hi) {
      int mid = (lo + hi + 1) >>> 1;
      if (starts[mid] <= offset) lo = mid;
      else hi = mid - 1;
    }
    int line = lo;
    int character = offset - starts[lo];
    if (line >= starts.length) line = starts.length - 1;
    return new Position(line, Math.max(0, character));
  }

  public int positionToOffset(int line, int character) {
    int[] starts = lineStarts;
    int base =
        line >= 0 && line < starts.length ? starts[line] : starts[starts.length - 1];
    return base + Math.max(0, character);
  }

  public Range rangeOf(int startOff, int endOff) {
    return new Range(offsetToPosition(startOff), offsetToPosition(endOff));
  }

  /** Effective theme = defaults deep-merged with this document's own values. */
  public Map<String, Map<String, String>> effectiveMap() {
    Map<String, Map<String, String>> eff = new HashMap<>();
    for (String block : ThemeSchema.BLOCKS) {
      eff.put(block, new HashMap<>(ThemeSchema.defaultsOf(block)));
    }
    for (GthToken t : tokens) {
      if (t.isKey || t.owner == null || t.block == null) continue;
      Map<String, String> box = eff.get(t.block);
      if (box != null) box.put(t.owner, t.raw);
    }
    return eff;
  }
}