package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.Color;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.ColorPresentation;
import org.eclipse.lsp4j.ColorPresentationParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentColorParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * GhostIDE theme language server ({@code .gth}), a Java/LSP4j port of the original Node.js {@code
 * ghost-theme-lsp}.
 *
 * <p>Features: JSON diagnostics, section/key completions, {@code @section.key} reference
 * completions, hover with resolved values, go-to-definition for references, document colors, and
 * whole-document / range formatting with a built-in tolerant JSON printer.
 */
public class GhostThemeLanguageServer
    implements LanguageServer, LanguageClientAware, TextDocumentService, WorkspaceService {

  static final String SERVER_NAME = "ghost-theme-lsp";
  static final String SERVER_VERSION = "0.1.0";

  private final Map<String, GthDocument> documents = new ConcurrentHashMap<>();
  private LanguageClient client;

  public GhostThemeLanguageServer() {}

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    ServerCapabilities caps = new ServerCapabilities();
    caps.setPositionEncoding("utf-16");
    caps.setTextDocumentSync(TextDocumentSyncKind.Full);
    CompletionOptions completion = new CompletionOptions();
    completion.setResolveProvider(false);
    completion.setTriggerCharacters(Arrays.asList("@", ".", "\"", ":"));
    caps.setCompletionProvider(completion);
    caps.setHoverProvider(true);
    caps.setDefinitionProvider(true);
    caps.setDocumentFormattingProvider(true);
    caps.setDocumentRangeFormattingProvider(true);
    caps.setColorProvider(true);
    caps.setInlayHintProvider(true);

    InitializeResult result = new InitializeResult(caps);
    result.setServerInfo(new ServerInfo(SERVER_NAME, SERVER_VERSION));
    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void exit() {}

  @Override
  public TextDocumentService getTextDocumentService() {
    return this;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return this;
  }

  // ───────────────────────────── Workspace ─────────────────────────────

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {}

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {}

  // ───────────────────────────── Document sync ─────────────────────────────

  private void indexDocument(String uri, String text) {
    GthDocument doc = new GthDocument(uri, text);
    documents.put(uri, doc);
    publishDiagnostics(uri, GthDiagnosticEngine.compute(doc));
  }

  private void publishDiagnostics(String uri, List<Diagnostic> diagnostics) {
    if (client == null) return;
    client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    indexDocument(params.getTextDocument().getUri(), params.getTextDocument().getText());
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
    if (changes == null || changes.isEmpty()) return;
    indexDocument(params.getTextDocument().getUri(), changes.get(changes.size() - 1).getText());
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    documents.remove(params.getTextDocument().getUri());
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {}

  // ───────────────────────────── Completion ─────────────────────────────

  private enum Kind {
    PROPERTY(10),
    VALUE(12),
    COLOR(17);

    final int value;

    Kind(int value) {
      this.value = value;
    }
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams params) {
    GthDocument doc = documents.get(params.getTextDocument().getUri());
    if (doc == null) return completedList(Collections.emptyList());

    Position pos = params.getPosition();
    int offset = doc.positionToOffset(pos.getLine(), pos.getCharacter());
    GthToken token = null;
    for (GthToken t : doc.tokens) {
      if (offset >= t.start && offset <= t.end && (offset != t.start || t.start == t.end)) {
        token = t;
        break;
      }
    }
    if (token == null) return completedList(Collections.emptyList());

    int contentUpTo = Math.min(offset, token.contentEnd);
    int prefixLen = Math.max(0, contentUpTo - token.contentStart);
    String prefix = token.raw.length() <= prefixLen ? token.raw : token.raw.substring(0, prefixLen);

    List<CompletionItem> items = new ArrayList<>();

    if (token.isKey) {
      if (token.block == null) {
        for (String b : ThemeSchema.BLOCKS) {
          if (!b.startsWith(prefix)) continue;
          items.add(
              keyItem(
                  b,
                  Kind.PROPERTY,
                  null,
                  doc.rangeOf(token.contentStart, token.contentEnd),
                  "Section `" + b + "` of the theme"));
        }
      } else {
        for (String key : ThemeSchema.keysOf(token.block)) {
          if (!key.startsWith(prefix)) continue;
          items.add(
              keyItem(
                  key,
                  Kind.PROPERTY,
                  token.block,
                  doc.rangeOf(token.contentStart, token.contentEnd),
                  ThemeSchema.description(token.block, key)));
        }
      }
      return completedList(items);
    }

    if (token.owner == null || token.block == null) return completedList(items);

    int at = prefix.lastIndexOf('@');
    if (at < 0) {
      addValueCompletions(doc, items, token, prefix);
      return completedList(items);
    }

    String afterAt = prefix.substring(at + 1);
    int dot = afterAt.indexOf('.');
    Map<String, Map<String, String>> eff = doc.effectiveMap();
    Range replaceRange = doc.rangeOf(token.contentStart + at, token.contentEnd);

    if (dot >= 0) {
      String block = afterAt.substring(0, dot);
      String partialKey = afterAt.substring(dot + 1);
      if (ThemeSchema.isBlock(block)) {
        for (String key : ThemeSchema.keysOf(block)) {
          if (!key.startsWith(partialKey)) continue;
          String ref = "@" + block + "." + key;
          String resolved = ThemeSchema.resolveRef(eff, ref);
          String resolvedHex = (resolved != null && !resolved.startsWith("@")) ? resolved : "";
          String docs = ThemeSchema.description(block, key);
          if (!resolvedHex.isEmpty()) {
            docs = docs + "\n\nResolved value: `" + resolvedHex + "`";
          }
          items.add(
              refItem(block + "." + key, Kind.VALUE, "Color reference", replaceRange, ref, docs));
        }
      }
    } else {
      for (String b : ThemeSchema.BLOCKS) {
        if (!b.startsWith(afterAt)) continue;
        items.add(
            refItem(
                b,
                Kind.VALUE,
                "Section",
                replaceRange,
                "@" + b + ".",
                "Keys of section `" + b + "` will be suggested after the dot."));
      }
    }
    return completedList(items);
  }

  private static CompletableFuture<Either<List<CompletionItem>, CompletionList>> completedList(
      List<CompletionItem> items) {
    return CompletableFuture.completedFuture(Either.forRight(new CompletionList(false, items)));
  }

  private void addValueCompletions(
      GthDocument doc, List<CompletionItem> items, GthToken token, String prefix) {
    String type = ThemeSchema.typeOf(token.block, token.owner);
    Range replaceRange = doc.rangeOf(token.contentStart, token.contentEnd);
    Map<String, Map<String, String>> eff = doc.effectiveMap();

    if ("number".equals(type)) {
      if (prefix.isEmpty() || prefix.charAt(0) != '#') {
        String def = ThemeSchema.defaultsOf(token.block).getOrDefault(token.owner, "");
        items.add(
            keyItem(
                def,
                Kind.VALUE,
                token.block + "." + token.owner,
                replaceRange,
                "Default blur amount (0-25)"));
      }
      return;
    }

    if ("path".equals(type)) {
      items.add(
          keyItem(
              "sdcard/Download/background.jpg",
              Kind.VALUE,
              token.block + "." + token.owner,
              replaceRange,
              "Path to the blurred background image/GIF/video"));
      return;
    }

    if (!"color".equals(type)) return;

    String def = ThemeSchema.defaultsOf(token.block).getOrDefault(token.owner, "");
    if (!def.isEmpty() && (prefix.isEmpty() || def.startsWith(prefix))) {
      items.add(
          keyItem(
              def,
              Kind.COLOR,
              "Default color for " + token.owner,
              replaceRange,
              "Default value: `" + def + "`"));
    }

    if (prefix.isEmpty() || "#".startsWith(prefix) || "#".startsWith("")) {
      items.add(
          keyItem(
              "#RRGGBB",
              Kind.COLOR,
              "Hex color",
              replaceRange,
              "Type a 6-digit hex color, e.g. `#3e4452`, or an 8-digit `#AARRGGBB`."));
    }

    if (prefix.isEmpty() || prefix.startsWith("@")) {
      for (String b : ThemeSchema.BLOCKS) {
        for (String key : ThemeSchema.keysOf(b)) {
          String ref = "@" + b + "." + key;
          if (ref.equals(def)) continue;
          String resolved = ThemeSchema.resolveRef(eff, ref);
          if (resolved == null || resolved.startsWith("@")) continue;
          items.add(
              refItem(
                  b + "." + key,
                  Kind.COLOR,
                  "Reference to another color",
                  replaceRange,
                  ref,
                  ThemeSchema.description(b, key)));
        }
      }
    }
  }

  private static CompletionItem keyItem(
      String label, Kind kind, String detail, Range editRange, String documentation) {
    CompletionItem item = new CompletionItem(label);
    item.setKind(mapKind(kind));
    if (detail != null) item.setDetail(detail);
    item.setTextEdit(Either.forLeft(new TextEdit(editRange, label)));
    if (documentation != null && !documentation.isEmpty()) {
      item.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, documentation));
    }
    return item;
  }

  private static CompletionItem refItem(
      String label,
      Kind kind,
      String detail,
      Range editRange,
      String newText,
      String documentation) {
    CompletionItem item = new CompletionItem(label);
    item.setKind(mapKind(kind));
    item.setDetail(detail);
    item.setTextEdit(Either.forLeft(new TextEdit(editRange, newText)));
    if (documentation != null && !documentation.isEmpty()) {
      item.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, documentation));
    }
    return item;
  }

  private static CompletionItemKind mapKind(Kind kind) {
    switch (kind) {
      case PROPERTY:
        return CompletionItemKind.Property;
      case VALUE:
        return CompletionItemKind.Value;
      case COLOR:
        return CompletionItemKind.Color;
      default:
        return CompletionItemKind.Property;
    }
  }

  // ───────────────────────────── Hover ─────────────────────────────

  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    GthDocument doc = documents.get(params.getTextDocument().getUri());
    if (doc == null) return CompletableFuture.completedFuture(null);
    Position pos = params.getPosition();
    int offset = doc.positionToOffset(pos.getLine(), pos.getCharacter());
    GthToken token = null;
    for (GthToken t : doc.tokens) {
      if (offset >= t.start && offset <= t.end) {
        token = t;
        break;
      }
    }
    if (token == null) return CompletableFuture.completedFuture(null);

    Map<String, Map<String, String>> eff = doc.effectiveMap();
    StringBuilder md = new StringBuilder();
    Range range = doc.rangeOf(token.start, token.end);

    if (token.isKey) {
      String name =
          token.block != null
              ? "**" + token.block + "." + token.raw + "**"
              : "**" + token.raw + "**";
      md.append(name);
      if (token.block != null) {
        String desc = ThemeSchema.description(token.block, token.raw);
        if (!desc.isEmpty()) md.append("\n\n").append(desc);
        String def = ThemeSchema.defaultsOf(token.block).get(token.raw);
        if (def != null) md.append("\n\nDefault: `").append(def).append("`");
      }
    } else if (token.owner != null && token.block != null) {
      String ownDesc = ThemeSchema.description(token.block, token.owner);
      md.append("**").append(token.block).append(".").append(token.owner).append("**");
      if (!ownDesc.isEmpty()) md.append("\n\n").append(ownDesc);
      if (token.raw.startsWith("@")) {
        String resolved = ThemeSchema.resolveRef(eff, token.raw);
        ColorRgb hex = (resolved != null && !resolved.startsWith("@")) ? hexToRgb(resolved) : null;
        md.append("\n\nReference: `").append(token.raw).append("`");
        if (hex != null) md.append("\nResolved value: `").append(hex.hex).append("`");
      } else if ("color".equals(ThemeSchema.typeOf(token.block, token.owner))
          && token.raw.matches("#([0-9a-fA-F]{3,8}).*")) {
        ColorRgb hex = hexToRgb(token.raw);
        if (hex != null) md.append("\n`").append(hex.hex).append("`");
      }
    } else {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.completedFuture(
        new Hover(new MarkupContent(MarkupKind.MARKDOWN, md.toString()), range));
  }

  // ───────────────────────────── Definition ─────────────────────────────

  @Override
  public CompletableFuture<
          Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(DefinitionParams params) {
    GthDocument doc = documents.get(params.getTextDocument().getUri());
    if (doc == null) return CompletableFuture.completedFuture(null);
    Position pos = params.getPosition();
    int offset = doc.positionToOffset(pos.getLine(), pos.getCharacter());
    GthToken token = null;
    for (GthToken t : doc.tokens) {
      if (offset >= t.start && offset <= t.end) {
        token = t;
        break;
      }
    }
    if (token == null || token.isKey || token.raw == null || !token.raw.startsWith("@")) {
      return CompletableFuture.completedFuture(null);
    }
    String[] parts = token.raw.substring(1).split("\\.", 2);
    if (parts.length != 2) return CompletableFuture.completedFuture(null);

    GthToken target = null;
    for (GthToken t : doc.tokens) {
      if (t.isKey && t.raw.equals(parts[1]) && java.util.Objects.equals(t.block, parts[0])) {
        target = t;
        break;
      }
    }
    if (target == null) return CompletableFuture.completedFuture(null);
    Location loc = new Location(doc.uri, doc.rangeOf(target.contentStart, target.contentEnd));
    return CompletableFuture.completedFuture(Either.forLeft(Collections.singletonList(loc)));
  }

  // ───────────────────────────── Document colors ─────────────────────────────

  @Override
  public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
    GthDocument doc = documents.get(params.getTextDocument().getUri());
    if (doc == null) return CompletableFuture.completedFuture(Collections.emptyList());

    Map<String, Map<String, String>> eff = doc.effectiveMap();
    List<ColorInformation> colors = new ArrayList<>();
    for (GthToken t : doc.tokens) {
      if (t.isKey || t.owner == null || t.block == null) continue;
      if (!"color".equals(ThemeSchema.typeOf(t.block, t.owner))) continue;
      String hex = t.raw;
      if (hex.startsWith("@")) {
        String resolved = ThemeSchema.resolveRef(eff, hex);
        if (resolved == null || resolved.startsWith("@")) continue;
        hex = resolved;
      }
      ColorRgb rgb = hexToRgb(hex);
      if (rgb == null) continue;
      colors.add(
          new ColorInformation(
              doc.rangeOf(t.contentStart, t.contentEnd),
              new Color(rgb.red, rgb.green, rgb.blue, rgb.alpha)));
    }
    return CompletableFuture.completedFuture(colors);
  }

  @Override
  public CompletableFuture<List<ColorPresentation>> colorPresentation(
      ColorPresentationParams params) {
    Color c = params.getColor();
    double red = c != null ? c.getRed() : 0;
    double green = c != null ? c.getGreen() : 0;
    double blue = c != null ? c.getBlue() : 0;
    double alpha = c != null && c.getAlpha() >= 0 ? c.getAlpha() : 1;

    String r = toHex(red);
    String g = toHex(green);
    String b = toHex(blue);
    String hh = r + g + b;
    int alpha8 = Math.max(0, Math.min(255, (int) Math.round(alpha * 255)));

    List<ColorPresentation> pres = new ArrayList<>();
    String argb = alpha8 < 255 ? "#" + toHex8(alpha8) + hh : "#" + hh;
    pres.add(new ColorPresentation(argb));
    pres.add(
        new ColorPresentation(
            "rgba("
                + Math.round(red * 255)
                + ", "
                + Math.round(green * 255)
                + ", "
                + Math.round(blue * 255)
                + ", "
                + (Math.round(alpha8 / 255.0 * 100.0) / 100.0)
                + ")"));
    return CompletableFuture.completedFuture(pres);
  }

  private static String toHex(double v) {
    int i = Math.max(0, Math.min(255, (int) Math.round(v * 255)));
    return String.format("%02X", i);
  }

  private static String toHex8(int v) {
    return String.format("%02X", v);
  }

  // ───────────────────────────── Inlay hints ─────────────────────────────

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
    GthDocument doc = documents.get(params.getTextDocument().getUri());
    if (doc == null) return CompletableFuture.completedFuture(Collections.emptyList());
    if (params.getRange() == null) return CompletableFuture.completedFuture(Collections.emptyList());

    Position startRange = params.getRange().getStart();
    Position endRange = params.getRange().getEnd();
    List<InlayHint> hints = new ArrayList<>();

    for (GthToken t : doc.tokens) {
      if (t.isKey || t.owner == null || t.block == null) continue;
      Position pos = doc.offsetToPosition(t.contentEnd);
      if (!inWindow(pos, startRange, endRange)) continue;
      String type = ThemeSchema.typeOf(t.block, t.owner);
      InlayHint hint = new InlayHint();
      hint.setPosition(pos);
      if ("number".equals(type)) {
        hint.setLabel("0-25");
      } else if ("path".equals(type)) {
        hint.setLabel("file path");
      } else if ("color".equals(type)) {
        Map<String, Map<String, String>> eff = doc.effectiveMap();
        String raw = t.raw;
        if (raw.startsWith("@")) {
          String resolved = ThemeSchema.resolveRef(eff, raw);
          if (resolved != null && !resolved.startsWith("@")) raw = resolved;
        }
        ColorRgb rgb = hexToRgb(raw);
        if (rgb != null) hint.setLabel(rgb.hex);
      }
      if (hint.getLabel() != null) hints.add(hint);
    }
    return CompletableFuture.completedFuture(hints);
  }

  private static boolean inWindow(Position p, Position start, Position end) {
    if (p == null) return false;
    if (p.getLine() < start.getLine() || p.getLine() > end.getLine()) return false;
    return p.getLine() > start.getLine()
        || p.getLine() < end.getLine()
        || p.getCharacter() >= start.getCharacter()
            && p.getCharacter() <= end.getCharacter();
  }

  // ───────────────────────────── Formatting ─────────────────────────────

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
    return CompletableFuture.completedFuture(
        formatDocument(documents.get(params.getTextDocument().getUri())));
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
      DocumentRangeFormattingParams params) {
    return CompletableFuture.completedFuture(
        formatDocument(documents.get(params.getTextDocument().getUri())));
  }

  /** Formats the whole document; returns a single TextEdit or an empty list when unchanged. */
  private static List<TextEdit> formatDocument(GthDocument doc) {
    if (doc == null) return Collections.emptyList();
    String out = formatJson(doc.text);
    if (out == null || out.equals(doc.text)) return Collections.emptyList();
    Range full =
        new Range(
            new Position(0, 0),
            new Position(
                doc.lineStarts.length - 1,
                doc.text.length() - doc.lineStarts[doc.lineStarts.length - 1]));
    return Collections.singletonList(new TextEdit(full, out));
  }

  /**
   * Tolerant JSON pretty-printer (drop-in for the JS built-in printer / Prettier fallback). Keeps
   * strings verbatim, never throws, works on slightly-broken documents.
   */
  static String formatJson(String text) {
    if (text == null || text.isEmpty()) return null;
    StringBuilder out = new StringBuilder(text.length() + 16);
    int depth = 0;
    boolean inString = false;
    int n = text.length();
    for (int i = 0; i < n; i++) {
      char c = text.charAt(i);
      if (inString) {
        out.append(c);
        if (c == '\\' && i + 1 < n) {
          out.append(text.charAt(i + 1));
          i++;
        } else if (c == '"') {
          inString = false;
        }
        continue;
      }
      switch (c) {
        case '"':
          inString = true;
          out.append(c);
          break;
        case '{':
        case '[':
          out.append(c);
          depth++;
          newline(out, depth);
          break;
        case '}':
        case ']':
          depth = Math.max(0, depth - 1);
          newline(out, depth);
          out.append(c);
          break;
        case ',':
          out.append(c);
          newline(out, depth);
          break;
        case ':':
          out.append(": ");
          break;
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          break;
        default:
          out.append(c);
      }
    }
    String result = out.toString().trim();
    return result.isEmpty() ? null : result + "\n";
  }

  private static void newline(StringBuilder out, int depth) {
    out.append('\n');
    for (int i = 0; i < depth * 2; i++) out.append(' ');
  }

  // ───────────────────────────── Color helpers ─────────────────────────────

  private static final class ColorRgb {
    final double red;
    final double green;
    final double blue;
    final double alpha;
    final String hex;

    ColorRgb(double red, double green, double blue, double alpha, String hex) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.alpha = alpha;
      this.hex = hex;
    }
  }

  /**
   * Parses a hex color into {@code 0..1} RGBA. Theme colors use the Android {@code #AARRGGBB}
   * convention: 3/4-digit forms are expanded ({@code #RGB -> #RRGGBB}, {@code #ARGB -> #AARRGGBB}),
   * a 6-digit value is opaque ({@code alpha = 1}), and an 8-digit value is treated as alpha-first.
   */
  static ColorRgb hexToRgb(String hex) {
    if (hex == null) return null;
    String h = hex.trim();
    if (h.startsWith("#")) h = h.substring(1);
    if (!h.matches("[0-9a-fA-F]{3,8}") || h.length() == 5 || h.length() == 7) return null;
    if (h.length() == 3 || h.length() == 4) {
      StringBuilder expanded = new StringBuilder();
      for (int i = 0; i < h.length(); i++) {
        char ch = h.charAt(i);
        expanded.append(ch).append(ch);
      }
      h = expanded.toString();
    }
    if (h.length() == 6) h = "ff" + h;

    int a = Integer.parseInt(h.substring(0, 2), 16);
    int r = Integer.parseInt(h.substring(2, 4), 16);
    int g = Integer.parseInt(h.substring(4, 6), 16);
    int b = Integer.parseInt(h.substring(6, 8), 16);
    return new ColorRgb(r / 255.0, g / 255.0, b / 255.0, a / 255.0, "#" + h.toUpperCase());
  }
}
