package ir.hanzodev1375.ghostide.javacore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
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
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * An LSP4j language server for Java fully backed by in-process Eclipse JDT (via {@link JdtEngine}).
 * It mirrors {@code GthServer} in the editor module: hosted in-memory through paired pipes, so no
 * external jdtls/node/JVM/proot is needed — it ships inside the app.
 *
 * <p>The server feeds the engine the project's real classpath (from {@link JavaClasspathProvider},
 * backed on-device by {@code AndroidClasspathResolver}: android.jar + libs + gradle deps + source
 * roots), so completion/hover/definition understand packages and android types instead of only the
 * tokens in the open file.
 *
 * <p>Capabilities: completion, hover, signature help, definition, references, rename, document
 * symbols, folding, formatting (document + range), code actions (organize/remove imports, format),
 * inlay hints and diagnostics.
 */
public class JdtLanguageServer
    implements LanguageServer, LanguageClientAware, TextDocumentService, WorkspaceService {

  public static final String SERVER_NAME = "ghost-java-lsp";
  public static final String SERVER_VERSION = "1.0.0";

  private static final String SOURCE_GHOST_JAVA = "ghost-java";
  private static final String[] TRIGGER_CHARS = {".", "@"};

  private final String projectRoot;
  private final Map<String, String> documents = new ConcurrentHashMap<>();
  private final JdtEngine engine = new JdtEngine();

  private volatile JavaClasspathProvider classpathProvider;
  private volatile boolean classpathResolved;
  private LanguageClient client;

  public JdtLanguageServer() {
    this(null);
  }

  public JdtLanguageServer(String projectRoot) {
    this.projectRoot = projectRoot;
  }

  /** Installs the classpath provider (supplied by the editor module, Android side). */
  public JdtLanguageServer setClasspathProvider(JavaClasspathProvider provider) {
    this.classpathProvider = provider;
    return this;
  }

  /** Wires the app-side runtime error reporter (logcat + toast). */
  public JdtLanguageServer setErrorListener(JdtErrorListener listener) {
    JdtRuntime.setErrorListener(listener);
    return this;
  }

  public JdtEngine engine() {
    return engine;
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  // ───────────────────────────── Project / classpath ─────────────────────────────

  private synchronized void ensureClasspath() {
    if (classpathResolved) return;
    classpathResolved = true;
    List<String> roots = new ArrayList<>();
    List<String> jars = new ArrayList<>();
    JavaClasspathProvider provider = classpathProvider;
    if (provider != null && projectRoot != null) {
      try {
        for (String s : provider.sourceRoots(projectRoot)) if (s != null) roots.add(s);
      } catch (Throwable t) {
        JdtRuntime.report("classpath", "source roots lookup failed", t, false);
      }
      try {
        for (String s : provider.libraryJars(projectRoot)) if (s != null) jars.add(s);
      } catch (Throwable t) {
        JdtRuntime.report("classpath", "library jars lookup failed", t, false);
      }
    }
    engine.configure(
        roots.toArray(new String[0]), jars.toArray(new String[0]));
  }

  private CompilationUnit parseOf(String uri, String text) {
    ensureClasspath();
    char[] source = text == null ? new char[0] : text.toCharArray();
    return engine.parse(source, fileBaseName(uri), null, null);
  }

  private String pathOf(String uri) {
    if (uri == null) return uri;
    if (uri.startsWith("file://")) return uri.substring("file://".length());
    return uri;
  }

  private String uriOf(String path) {
    return path == null ? null : "file://" + path;
  }

  // ───────────────────────────── Initialize ─────────────────────────────

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    ServerCapabilities caps = new ServerCapabilities();
    caps.setPositionEncoding("utf-16");
    caps.setTextDocumentSync(TextDocumentSyncKind.Full);

    CompletionOptions completion = new CompletionOptions();
    completion.setResolveProvider(false);
    completion.setTriggerCharacters(java.util.Arrays.asList(TRIGGER_CHARS));
    caps.setCompletionProvider(completion);

    caps.setHoverProvider(true);
    caps.setSignatureHelpProvider(new SignatureHelpOptions(java.util.Arrays.asList(".", "(", ",")));
    caps.setDefinitionProvider(true);
    caps.setReferencesProvider(true);
    caps.setDocumentSymbolProvider(true);
    caps.setFoldingRangeProvider(true);
    caps.setRenameProvider(new RenameOptions(true));
    caps.setDocumentFormattingProvider(true);
    caps.setDocumentRangeFormattingProvider(true);
    caps.setCodeActionProvider(
        Either.forRight(
            new CodeActionOptions(
                java.util.Arrays.asList(
                    CodeActionKind.SourceOrganizeImports,
                    CodeActionKind.Source,
                    CodeActionKind.QuickFix))));

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
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    classpathResolved = false;
    for (String uri : documents.keySet()) {
      String text = documents.get(uri);
      if (text != null) publishDiagnostics(uri, text);
    }
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {}

  // ───────────────────────────── Document sync ─────────────────────────────

  private void analyzeDocument(String uri, String text) {
    documents.put(uri, text == null ? "" : text);
    publishDiagnostics(uri, text == null ? "" : text);
  }

  private void publishDiagnostics(String uri, String text) {
    if (client == null) return;
    try {
      CompilationUnit unit = parseOf(uri, text);
      List<JdtEngine.Problem> problems =
          unit == null ? Collections.emptyList() : engine.problems(unit);
      List<Diagnostic> diags = toDiagnostics(text, problems);
      client.publishDiagnostics(new PublishDiagnosticsParams(uri, diags));
    } catch (Throwable t) {
      List<Diagnostic> fatal = new ArrayList<>();
      fatal.add(
          new Diagnostic(
              new Range(new Position(0, 0), new Position(0, 1)),
              "Parser crashed: " + t,
              DiagnosticSeverity.Error,
              SOURCE_GHOST_JAVA));
      try {
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, fatal));
      } catch (Throwable ignored) {
        // client may be gone/disconnected
      }
    }
  }

  private List<Diagnostic> toDiagnostics(String text, List<JdtEngine.Problem> problems) {
    if (problems == null || problems.isEmpty()) return Collections.emptyList();
    List<Diagnostic> out = new ArrayList<>(problems.size());
    for (JdtEngine.Problem p : problems) {
      Position start = JdtCompletions.offsetToPosition(text, p.start);
      Position end = JdtCompletions.offsetToPosition(text, p.start + p.length);
      Diagnostic d =
          new Diagnostic(new Range(start, end), p.message, severityOf(p.severity), SOURCE_GHOST_JAVA);
      out.add(d);
    }
    return out;
  }

  private static DiagnosticSeverity severityOf(int sev) {
    switch (sev) {
      case 1:
        return DiagnosticSeverity.Error;
      case 2:
        return DiagnosticSeverity.Warning;
      default:
        return DiagnosticSeverity.Information;
    }
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    String text = params.getTextDocument().getText();
    analyzeDocument(uri, text == null ? "" : text);
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    if (params.getContentChanges() == null || params.getContentChanges().isEmpty()) return;
    String uri = params.getTextDocument().getUri();
    String text = params.getContentChanges().get(params.getContentChanges().size() - 1).getText();
    analyzeDocument(uri, text == null ? "" : text);
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    documents.remove(params.getTextDocument().getUri());
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    if (client == null) return;
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text != null) publishDiagnostics(uri, text);
  }

  // ───────────────────────────── Completion ─────────────────────────────

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return completedList(Collections.emptyList());

    Position pos = params.getPosition();
    int offset = positionToOffset(text, pos);
    if (offset < 0) return completedList(Collections.emptyList());

    try {
      CompilationUnit unit = parseOf(uri, text);
      if (unit == null) return completedList(Collections.emptyList());
      List<CompletionItem> items =
          JdtCompletions.compute(unit, text, offset, engine.sourcePaths(), engine.classpath());
      return completedList(items);
    } catch (Throwable e) {
      JdtRuntime.report("completion", null, e, false);
      return completedList(Collections.emptyList());
    }
  }

  private static CompletableFuture<Either<List<CompletionItem>, CompletionList>> completedList(
      List<CompletionItem> items) {
    return CompletableFuture.completedFuture(Either.forRight(new CompletionList(false, items)));
  }

  // ───────────────────────────── Hover ─────────────────────────────

  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(null);
    int offset = positionToOffset(text, params.getPosition());
    if (offset < 0) return CompletableFuture.completedFuture(null);
    try {
      CompilationUnit unit = parseOf(uri, text);
      IBinding binding = JdtCompletions.bindingAt(unit, offset);
      String md = JdtCompletions.signatureMarkdown(binding);
      if (md == null) return CompletableFuture.completedFuture(null);
      Range range = tokenRangeAt(text, offset);
      return CompletableFuture.completedFuture(
          new Hover(new MarkupContent(MarkupKind.MARKDOWN, md), range));
    } catch (Throwable e) {
      JdtRuntime.report("hover", null, e, false);
      return CompletableFuture.completedFuture(null);
    }
  }

  // ───────────────────────────── Signature help ─────────────────────────────

  @Override
  public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(null);
    int offset = positionToOffset(text, params.getPosition());
    if (offset < 0) return CompletableFuture.completedFuture(null);
    try {
      CompilationUnit unit = parseOf(uri, text);
      return CompletableFuture.completedFuture(
          JdtCompletions.signatureHelp(unit, text, offset));
    } catch (Throwable e) {
      JdtRuntime.report("signatureHelp", null, e, false);
      return CompletableFuture.completedFuture(null);
    }
  }

  // ───────────────────────────── Definition / references ─────────────────────────────

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(DefinitionParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(null);
    int offset = positionToOffset(text, params.getPosition());
    if (offset < 0) return CompletableFuture.completedFuture(null);
    try {
      CompilationUnit cu = parseOf(uri, text);
      IBinding binding = JdtCompletions.bindingAt(cu, offset);
      Location loc = resolveDefinition(uri, cu, binding);
      if (loc == null) return CompletableFuture.completedFuture(null);
      List<Location> out = new ArrayList<>(1);
      out.add(loc);
      return CompletableFuture.completedFuture(Either.forLeft(out));
    } catch (Throwable e) {
      JdtRuntime.report("definition", null, e, false);
      return CompletableFuture.completedFuture(null);
    }
  }

  /** Definition target of {@code binding}: same file first, else the file declaring its type. */
  private Location resolveDefinition(String uri, CompilationUnit cu, IBinding binding) {
    if (binding == null) return null;
    String key = JdtCompletions.bindingKey(binding);
    if (cu != null && key != null) {
      List<SimpleName> uses = JdtCompletions.usesOfKey(cu, key);
      if (!uses.isEmpty()) {
        SimpleName n = uses.get(0);
        return new Location(uri, JdtCompletions.nameRange(cu, n));
      }
    }
    String binaryName = JdtCompletions.declaringBinaryName(binding);
    if (binaryName == null || binaryName.indexOf('.') < 0) return null;
    String rel = binaryName.replace('.', File.separatorChar) + ".java";
    for (String root : engine.sourcePaths()) {
      if (root == null) continue;
      File target = new File(root, rel);
      if (!target.isFile()) continue;
      CompilationUnit tcu = engine.parseFile(target);
      if (tcu == null) continue;
      List<SimpleName> uses = JdtCompletions.usesOfKey(tcu, key);
      if (!uses.isEmpty()) {
        SimpleName n = uses.get(0);
        return new Location(uriOf(target.getAbsolutePath()), JdtCompletions.nameRange(tcu, n));
      }
      // binding key mismatch across files: fall back to the first name match
      String simple = binaryName.substring(binaryName.lastIndexOf('.') + 1);
      SimpleName decl = findSimpleNamed(tcu, simple);
      if (decl != null) {
        return new Location(uriOf(target.getAbsolutePath()), JdtCompletions.nameRange(tcu, decl));
      }
    }
    return null;
  }

  private static SimpleName findSimpleNamed(CompilationUnit cu, String name) {
    final SimpleName[] found = {null};
    if (cu == null || name == null) return null;
    cu.accept(
        new org.eclipse.jdt.core.dom.ASTVisitor() {
          @Override
          public boolean visit(SimpleName node) {
            if (found[0] == null && name.equals(node.getIdentifier())) {
              found[0] = node;
              return false;
            }
            return true;
          }
        });
    return found[0];
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(Collections.emptyList());
    int offset = positionToOffset(text, params.getPosition());
    if (offset < 0) return CompletableFuture.completedFuture(Collections.emptyList());
    try {
      CompilationUnit cu = parseOf(uri, text);
      IBinding binding = JdtCompletions.bindingAt(cu, offset);
      if (binding == null) return CompletableFuture.completedFuture(Collections.emptyList());
      String key = JdtCompletions.bindingKey(binding);
      if (key == null) return CompletableFuture.completedFuture(Collections.emptyList());

      Map<String, List<Range>> byUri = new LinkedHashMap<>();
      collectUses(uri, cu, key, byUri);
      int scanned = 0;
      for (File file : engine.sourceFiles(engine.sourcePaths())) {
        if (++scanned > 250) break; // avoid ANR on huge projects
        CompilationUnit fcu = engine.parseFile(file);
        if (fcu == null) continue;
        collectUses(uriOf(file.getAbsolutePath()), fcu, key, byUri);
      }
      List<Location> out = new ArrayList<>();
      for (Map.Entry<String, List<Range>> e : byUri.entrySet()) {
        for (Range r : e.getValue()) out.add(new Location(e.getKey(), r));
      }
      return CompletableFuture.completedFuture(out);
    } catch (Throwable e) {
      JdtRuntime.report("references", null, e, false);
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  private static void collectUses(
      String uri, CompilationUnit cu, String key, Map<String, List<Range>> byUri) {
    if (cu == null || key == null) return;
    List<SimpleName> uses = JdtCompletions.usesOfKey(cu, key);
    if (uses.isEmpty()) return;
    List<Range> ranges = byUri.computeIfAbsent(uri, k -> new ArrayList<>());
    for (SimpleName n : uses) ranges.add(JdtCompletions.nameRange(cu, n));
  }

  // ───────────────────────────── Symbols / folding / inlay hints ─────────────────────────────

  @Override
  public CompletableFuture<List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>>>
      documentSymbol(DocumentSymbolParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(Collections.emptyList());
    try {
      CompilationUnit unit = parseOf(uri, text);
      List<DocumentSymbol> symbols = JdtCompletions.documentSymbols(unit);
      List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>> out = new ArrayList<>();
      for (DocumentSymbol s : symbols) out.add(Either.forRight(s));
      return CompletableFuture.completedFuture(out);
    } catch (Throwable e) {
      JdtRuntime.report("documentSymbol", null, e, false);
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  @Override
  public CompletableFuture<List<FoldingRange>> foldingRange(
      FoldingRangeRequestParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(Collections.emptyList());
    try {
      CompilationUnit unit = parseOf(uri, text);
      return CompletableFuture.completedFuture(JdtCompletions.foldingRanges(unit));
    } catch (Throwable e) {
      JdtRuntime.report("foldingRanges", null, e, false);
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(Collections.emptyList());
    try {
      CompilationUnit unit = parseOf(uri, text);
      List<InlayHint> hints = JdtCompletions.inlayHints(unit);
      Range r = params.getRange();
      if (r != null && !hints.isEmpty()) {
        List<InlayHint> filtered = new ArrayList<>();
        for (InlayHint h : hints) {
          if (h.getPosition() != null
              && h.getPosition().getLine() >= r.getStart().getLine()
              && h.getPosition().getLine() <= r.getEnd().getLine()) {
            filtered.add(h);
          }
        }
        hints = filtered;
      }
      return CompletableFuture.completedFuture(hints);
    } catch (Throwable e) {
      JdtRuntime.report("inlayHint", null, e, false);
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  // ───────────────────────────── Rename ─────────────────────────────

  @Override
  public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>>
      prepareRename(PrepareRenameParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(null);
    int offset = positionToOffset(text, params.getPosition());
    if (offset < 0 || offset > text.length()) return CompletableFuture.completedFuture(null);
    Range range = tokenRangeAt(text, offset);
    if (range == null) return CompletableFuture.completedFuture(null);
    return CompletableFuture.completedFuture(Either3.forFirst(range));
  }

  @Override
  public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(null);
    String newName = params.getNewName();
    if (newName == null || newName.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    int offset = positionToOffset(text, params.getPosition());
    if (offset < 0) return CompletableFuture.completedFuture(null);
    try {
      CompilationUnit cu = parseOf(uri, text);
      IBinding binding = JdtCompletions.bindingAt(cu, offset);
      if (binding == null) return CompletableFuture.completedFuture(null);
      String key = JdtCompletions.bindingKey(binding);
      if (key == null) return CompletableFuture.completedFuture(null);

      Map<String, List<TextEdit>> changes = new LinkedHashMap<>();
      collectRenameEdits(uri, cu, key, newName, changes);
      int scanned = 0;
      for (File file : engine.sourceFiles(engine.sourcePaths())) {
        if (++scanned > 250) break; // avoid ANR on huge projects
        CompilationUnit fcu = engine.parseFile(file);
        if (fcu == null) continue;
        collectRenameEdits(uriOf(file.getAbsolutePath()), fcu, key, newName, changes);
      }
      WorkspaceEdit edit = new WorkspaceEdit(changes);
      return CompletableFuture.completedFuture(edit);
    } catch (Throwable e) {
      JdtRuntime.report("rename", null, e, false);
      return CompletableFuture.completedFuture(null);
    }
  }

  private static void collectRenameEdits(
      String uri, CompilationUnit cu, String key, String newName,
      Map<String, List<TextEdit>> changes) {
    if (cu == null) return;
    List<SimpleName> uses = JdtCompletions.usesOfKey(cu, key);
    if (uses.isEmpty()) return;
    List<TextEdit> edits = changes.computeIfAbsent(uri, k -> new ArrayList<>());
    for (SimpleName n : uses) {
      edits.add(new TextEdit(JdtCompletions.nameRange(cu, n), newName));
    }
  }

  // ───────────────────────────── Formatting ─────────────────────────────

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(
      DocumentFormattingParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(Collections.emptyList());
    String formatted = engine.format(text);
    if (formatted == null || formatted.equals(text)) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    Position start = new Position(0, 0);
    Position end = JdtCompletions.offsetToPosition(text, text.length());
    List<TextEdit> edits =
        Collections.singletonList(new TextEdit(new Range(start, end), formatted));
    return CompletableFuture.completedFuture(edits);
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
      DocumentRangeFormattingParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(Collections.emptyList());
    try {
      return CompletableFuture.completedFuture(engine.formatRange(text, params.getRange()));
    } catch (Throwable e) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  // ───────────────────────────── Code actions ─────────────────────────────

  @Override
  public CompletableFuture<List<Either<org.eclipse.lsp4j.Command, CodeAction>>> codeAction(
      CodeActionParams params) {
    String uri = params.getTextDocument().getUri();
    String text = documents.get(uri);
    if (text == null) return CompletableFuture.completedFuture(Collections.emptyList());
    List<Either<org.eclipse.lsp4j.Command, CodeAction>> out = new ArrayList<>();
    try {
      CompilationUnit cu = parseOf(uri, text);
      if (cu == null) return CompletableFuture.completedFuture(out);

      if (cu.imports() != null && !cu.imports().isEmpty()) {
        List<TextEdit> remove = JdtCompletions.unusedImportEdits(cu, text);
        if (!remove.isEmpty()) {
          CodeAction a =
              codeAction("RemoveUnusedImports", "Remove unused imports",
                  CodeActionKind.Source, uri, remove);
          out.add(Either.forRight(a));
        }
        TextEdit organize = JdtCompletions.organizeImportsEdit(cu, text);
        if (organize != null) {
          CodeAction a =
              codeAction(
                  "OrganizeImports",
                  "Organize imports",
                  CodeActionKind.SourceOrganizeImports,
                  uri,
                  Collections.singletonList(organize));
          out.add(Either.forRight(a));
        }
      }

      String formatted = engine.format(text);
      if (formatted != null && !formatted.equals(text)) {
        Position start = new Position(0, 0);
        Position end = JdtCompletions.offsetToPosition(text, text.length());
        CodeAction a =
            codeAction(
                "FormatDocument",
                "Format document",
                CodeActionKind.Source,
                uri,
                Collections.singletonList(new TextEdit(new Range(start, end), formatted)));
        out.add(Either.forRight(a));
      }
    } catch (Throwable t) {
      JdtRuntime.report("codeActions", null, t, false);
      // No actions when the unit cannot be parsed.
    }
    return CompletableFuture.completedFuture(out);
  }

  private static CodeAction codeAction(
      String command, String title, String kind, String uri, List<TextEdit> edits) {
    CodeAction a = new CodeAction(title);
    a.setKind(kind);
    a.setIsPreferred(true);
    Map<String, List<TextEdit>> changes = new LinkedHashMap<>();
    changes.put(uri, edits);
    a.setEdit(new WorkspaceEdit(changes));
    return a;
  }

  // ───────────────────────────── Position helpers ─────────────────────────────

  private static int positionToOffset(String text, Position pos) {
    if (text == null) return -1;
    int line = pos.getLine();
    int col = pos.getCharacter();
    int cur = 0;
    int cl = 0;
    int n = text.length();
    while (cur < n && cl < line) {
      if (text.charAt(cur) == '\n') cl++;
      cur++;
    }
    int start = Math.min(cur, n);
    return Math.min(start + col, n);
  }

  private static Range tokenRangeAt(String text, int offset) {
    if (text == null || offset <= 0) return new Range(new Position(0, 0), new Position(0, 1));
    int i = offset - 1;
    while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i))) i--;
    int start = i + 1;
    int j = offset;
    while (j < text.length() && Character.isJavaIdentifierPart(text.charAt(j))) j++;
    return new Range(
        JdtCompletions.offsetToPosition(text, start),
        JdtCompletions.offsetToPosition(text, Math.max(j, start + 1)));
  }

  static String fileBaseName(String uri) {
    int slash = uri.lastIndexOf('/');
    String name = slash >= 0 ? uri.substring(slash + 1) : uri;
    if (name.contains(".java")) return name;
    return name + ".java";
  }
}