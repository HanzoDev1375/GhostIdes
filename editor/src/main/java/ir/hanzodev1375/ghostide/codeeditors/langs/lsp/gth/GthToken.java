package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth;

/** A string token produced by the tolerant JSON scanner. */
public final class GthToken {
  /** Literal content (escapes already flattened like the JS port). */
  public final String raw;
  /** Start offset in the document (including the opening quote). */
  public final int start;
  /** End offset in the document (including the closing quote). */
  public final int end;
  /** Offset just after the opening quote. */
  public final int contentStart;
  /** Offset just before the closing quote. */
  public final int contentEnd;
  /** Name of the enclosing section ({@code activity|editor|widget|material3}) or null at root. */
  public final String block;
  /** {@code true} when this string is an object key. */
  public final boolean isKey;
  /** For value tokens: the key they are assigned to; null for keys. */
  public final String owner;

  public GthToken(
      String raw,
      int start,
      int end,
      int contentStart,
      int contentEnd,
      String block,
      boolean isKey,
      String owner) {
    this.raw = raw;
    this.start = start;
    this.end = end;
    this.contentStart = contentStart;
    this.contentEnd = contentEnd;
    this.block = block;
    this.isKey = isKey;
    this.owner = owner;
  }
}