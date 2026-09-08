package ir.hanzodev1375.ghostide.javacore;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-process Java intelligence engine built directly on Eclipse JDT — the same approach CodeAssist
 * uses. No external language server, no JVM, no proot: the eclipse compiler runs inside this
 * process and is error-tolerant, so completion and diagnostics work on broken code.
 *
 * <p>The engine resolves bindings against the project's real classpath ({@code android.jar} +
 * library jars) and source roots, so types, packages and members are understood — unlike a bare
 * parse with no environment. The same environment is reused for every snippet/files parsed, which
 * is what makes cross-file binding keys comparable.
 */
public final class JdtEngine {

  private final Map<String, String> compilerOptions;

  private String[] currentSourcePaths = new String[0];
  private String[] currentClasspath = new String[0];
  private String currentUnitName = "X.java";

  public JdtEngine() {
    this.compilerOptions = defaultCompilerOptions();
  }

  /** Installs the classpath/source-roots that every subsequent parse should bind against. */
  public void configure(String[] sourcePaths, String[] classpath) {
    if (sourcePaths != null) currentSourcePaths = sourcePaths;
    if (classpath != null) currentClasspath = classpath;
  }

  public void setUnitName(String unitName) {
    currentUnitName = unitName;
  }

  /** Installed source roots (java source directories) of the current project. */
  public String[] sourcePaths() {
    return currentSourcePaths;
  }

  /** Installed library classpath (android.jar, libs, ...) of the current project. */
  public String[] classpath() {
    return currentClasspath;
  }

  public static Map<String, String> defaultCompilerOptions() {
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_17);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_17);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_17);
    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
    options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
    options.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.IGNORE);
    options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.IGNORE);
    options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.WARNING);
    options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.IGNORE);
    return options;
  }

  /**
   * Parses Java source into an error-tolerant AST. Positions map directly to UTF-16 offsets
   * (matching LSP's default utf-16 position encoding). If a classpath has been installed the AST
   * is fully binding-aware and can resolve types against android.jar, libs and project sources on
   * disk. Never returns null; a catastrophic failure degrades to an empty compilation unit.
   */
  public CompilationUnit parse(char[] source, String fileName, String[] sourcePaths, String[] classpath) {
    if (sourcePaths != null) currentSourcePaths = sourcePaths;
    if (classpath != null) currentClasspath = classpath;
    if (fileName != null) currentUnitName = fileName;

    // JDT's ASTParser.checkForSystemLibrary() refuses binding resolution for Java 9+ unless the
    // environment truly carries a java.base module (a real JDK). When the terminal JDK's module
    // image (lib/jrt-fs.jar, exposed as ClasspathJrt) is on the classpath we can keep full Java
    // compliance; otherwise we drop to a 1.8 baseline (the module check is skipped for Java 8) and
    // java.lang.* still resolves from the provided jars (android.jar). Either way the parse never
    // throws "Missing system library".
    if (hasJdkSystemLibrary(currentClasspath)) {
      CompilationUnit unit = doParse(source, JavaCore.VERSION_17);
      if (unit != null) return unit;
    }
    return doParse(source, JavaCore.VERSION_1_8);
  }

  /** True when the classpath carries a real JDK system library (jrt-fs.jar or jmods). */
  private static boolean hasJdkSystemLibrary(String[] classpath) {
    if (classpath == null) return false;
    for (String entry : classpath) {
      if (entry == null) continue;
      if (entry.endsWith("jrt-fs.jar") || entry.endsWith(".jmod")) return true;
    }
    return false;
  }

  private CompilationUnit doParse(char[] source, String compliance) {
    Map<String, String> opts = new HashMap<>(defaultCompilerOptions());
    opts.put(JavaCore.COMPILER_COMPLIANCE, compliance);
    opts.put(JavaCore.COMPILER_SOURCE, compliance);
    opts.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, compliance);
    opts.put(CompilerOptions.OPTION_EnablePreviews, CompilerOptions.DISABLED);

    ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setSource(source);
    parser.setResolveBindings(true);
    parser.setBindingsRecovery(true);
    parser.setStatementsRecovery(true);
    parser.setUnitName(currentUnitName);
    parser.setCompilerOptions(opts);
    if (currentClasspath.length > 0) {
      try {
        // sourcePaths are absolute on-disk source roots; classpath are absolute jar/jmod paths. We
        // do NOT include the running VM bootpath (there is no JDK on Android) and let ECJ resolve
        // the library types from the jars themselves.
        parser.setEnvironment(currentClasspath, currentSourcePaths, null, false);
      } catch (Throwable ignored) {
        // Environment failed to install (e.g. a missing class on a partial dex); parse unbound.
      }
    }
    try {
      return (CompilationUnit) parser.createAST(null);
    } catch (Throwable t) {
      JdtRuntime.report("parse", "JDT parse crashed", t, false);
      return null;
    }
  }

  /** Parses a project source file from disk with the currently installed environment. */
  public CompilationUnit parseFile(File file) {
    try {
      byte[] bytes = Files.readAllBytes(file.toPath());
      String text = new String(bytes, StandardCharsets.UTF_8);
      return parse(text.toCharArray(), file.getName(), currentSourcePaths, currentClasspath);
    } catch (IOException e) {
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  // ────────────────────────────── Diagnostics ──────────────────────────────

  /** Collects compiler problems as (start, length, message, severity) tuples. */
  public List<Problem> problems(CompilationUnit unit) {
    List<Problem> out = new ArrayList<>();
    if (unit == null) return out;
    IProblem[] probs = unit.getProblems();
    if (probs == null) return out;
    for (IProblem p : probs) {
      int severity = p.isError() ? 1 : (p.isWarning() ? 2 : 3);
      int start = p.getSourceStart();
      int length = Math.max(0, p.getSourceEnd() - p.getSourceStart() + 1);
      out.add(new Problem(start, length, p.getMessage(), severity, p.getID()));
    }
    return out;
  }

  public static final class Problem {
    public final int start;
    public final int length;
    public final String message;
    public final int severity; // 1=error, 2=warning, 3=info
    public final int id;

    public Problem(int start, int length, String message, int severity, int id) {
      this.start = start;
      this.length = length;
      this.message = message;
      this.severity = severity;
      this.id = id;
    }
  }

  // ────────────────────────────── Formatting ──────────────────────────────

  /**
   * Formats a whole Java source string. Returns the formatted text, or {@code null} if the source
   * could not be formatted (e.g. it is completely broken).
   */
  public String format(String source) {
    try {
      CodeFormatter formatter = ToolFactory.createCodeFormatter(compilerOptions);
      org.eclipse.text.edits.TextEdit edit =
          formatter.format(
              CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, null);
      if (edit == null) return null;
      StringBuffer buf = new StringBuffer(source);
      applyTextEdit(buf, edit);
      return buf.toString();
    } catch (Throwable t) {
      JdtRuntime.report("format", "JDT formatter crashed", t, false);
      return null;
    }
  }

  /**
   * Formats only the selected line range. Uses a line-level prefix/suffix match between the
   * original and the fully-formatted document to carve out the region corresponding to the
   * selection, which is the standard "format selection" heuristic.
   */
  public List<TextEdit> formatRange(String source, Range range) {
    String formatted = format(source);
    if (formatted == null || formatted.equals(source)) return new ArrayList<>();

    String[] orig = source.split("\n", -1);
    String[] frm = formatted.split("\n", -1);
    int startLine = Math.max(0, range.getStart().getLine());
    int endLine = Math.min(orig.length - 1, range.getEnd().getLine());

    int prefix = 0;
    while (prefix < startLine && prefix < frm.length && orig[prefix].equals(frm[prefix])) prefix++;

    int os = orig.length, fs = frm.length;
    int suffix = 0;
    while (suffix < (orig.length - endLine - 1)
        && suffix < frm.length
        && orig[os - 1 - suffix].equals(frm[fs - 1 - suffix])) suffix++;

    int regionStart = prefix;
    int regionEnd = frm.length - suffix; // exclusive
    if (regionEnd <= regionStart) return new ArrayList<>();

    StringBuilder sb = new StringBuilder();
    for (int i = regionStart; i < regionEnd; i++) {
      if (i > regionStart) sb.append('\n');
      sb.append(frm[i]);
    }
    String text = sb.toString();
    Position startPos = JdtCompletions.offsetToPosition(source, offsetOfLine(source, startLine));
    Position endPos = JdtCompletions.offsetToPosition(source, offsetOfLine(source, endLine + 1));
    List<TextEdit> out = new ArrayList<>(1);
    out.add(new TextEdit(new Range(startPos, endPos), text));
    return out;
  }

  private static int offsetOfLine(String text, int line) {
    if (line <= 0) return 0;
    int cur = 0;
    int l = 0;
    int n = text.length();
    while (cur < n && l < line) {
      if (text.charAt(cur) == '\n') l++;
      cur++;
    }
    return Math.min(cur, n);
  }

  /** Applies an Eclipse {@link org.eclipse.text.edits.TextEdit} (ranges half-open, back-to-front). */
  private static void applyTextEdit(StringBuffer buf, org.eclipse.text.edits.TextEdit edit) {
    org.eclipse.text.edits.TextEdit[] children = edit.getChildren();
    if (children != null && children.length > 0) {
      // Siblings are applied in reverse so earlier offsets are not disturbed.
      for (int i = children.length - 1; i >= 0; i--) applyTextEdit(buf, children[i]);
      return;
    }
    if (edit instanceof ReplaceEdit) {
      int offset = edit.getOffset();
      int length = edit.getLength();
      if (offset >= 0 && length >= 0 && offset + length <= buf.length()) {
        buf.replace(offset, offset + length, ((ReplaceEdit) edit).getText());
      }
    }
  }

  // ────────────────────────────── Project scanning ──────────────────────────────

  /**
   * Lists plausible top-level type names of a project by walking its source roots (path → binary
   * name, no parsing). Used by the AST completion fallback so "import"-free completions can still
   * suggest sibling project types.
   */
  public List<String> projectTypes(String[] sourceRoots) {
    List<String> out = new ArrayList<>();
    if (sourceRoots == null) return out;
    for (String root : sourceRoots) {
      if (root == null) continue;
      collectPathTypes(new File(root), root, out);
    }
    return out;
  }

  private static void collectPathTypes(File dir, String root, List<String> out) {
    File[] children = dir.listFiles();
    if (children == null) return;
    for (File child : children) {
      if (child.isDirectory()) collectPathTypes(child, root, out);
      else if (child.getName().endsWith(".java")) {
        String rel = child.getAbsolutePath().substring(root.length());
        if (rel.startsWith("/") || rel.startsWith(File.separator)) rel = rel.substring(1);
        if (!rel.endsWith(".java")) continue;
        String binary = rel.substring(0, rel.length() - 5).replace(File.separatorChar, '.');
        if (binary.isEmpty()) continue;
        out.add(binary);
      }
    }
  }

  /** All {@code .java} files (non-recursive recursion) under the given source roots. */
  public List<File> sourceFiles(String[] sourceRoots) {
    List<File> out = new ArrayList<>();
    if (sourceRoots == null) return out;
    for (String root : sourceRoots) {
      if (root == null) continue;
      collectFiles(new File(root), out);
    }
    return out;
  }

  private static void collectFiles(File dir, List<File> out) {
    File[] children = dir.listFiles();
    if (children == null) return;
    for (File child : children) {
      if (child.isDirectory()) collectFiles(child, out);
      else if (child.getName().endsWith(".java")) out.add(child);
    }
  }
}