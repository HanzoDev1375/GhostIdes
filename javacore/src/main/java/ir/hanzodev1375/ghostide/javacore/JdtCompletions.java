package ir.hanzodev1375.ghostide.javacore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Completion, hover and other AST/binding intelligence used by the language server. All of it runs
 * on the binding-aware {@link CompilationUnit} produced by {@link JdtEngine} (which carries the
 * project's real classpath), so it understands android.jar classes, project packages and members —
 * not just the tokens inside the open file.
 */
public final class JdtCompletions {

  private JdtCompletions() {}

  // ───────────────────────────── Completion ─────────────────────────────

  public static final int COMPLETION_LIMIT = 300;

  /**
   * Computes completions at {@code offset}. After a "." it lists the members of the receiver type
   * (resolved through the classpath); otherwise it lists in-scope locals, project types, used
   * imports, declared types and keywords.
   */
  public static List<CompletionItem> compute(
      CompilationUnit cu, String text, int offset, String[] sourcePaths, String[] classpath) {
    List<CompletionItem> items = new ArrayList<>();
    if (cu == null || text == null) return items;

    String prefix = prefixBefore(text, offset);
    Range range = completionRange(text, offset);

    ITypeBinding receiver = receiverType(cu, text, offset);
    if (receiver != null) {
      collectReceiverMembers(receiver, prefix, range, items);
    } else {
      collectLocals(cu, prefix, range, items);
      collectDeclaredTypes(cu, prefix, range, items);
      collectImportedNames(cu, prefix, range, items);
      collectProjectTypes(sourcePaths, prefix, range, items);
      collectKeywords(prefix, range, items);
    }
    return items;
  }

  /** When completion happens right after ".", the type of the receiver expression. */
  private static ITypeBinding receiverType(CompilationUnit cu, String text, int offset) {
    if (cu == null || offset <= 0) return null;
    if (text.charAt(offset - 1) != '.') return null;
    int i = offset - 2;
    while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i))) i--;
    int start = i + 1;
    if (start >= offset - 1) return null;
    ASTNode n = NodeFinder.perform(cu, start, offset - 1 - start);
    if (n == null) return null;

    if (n instanceof SimpleName) {
      SimpleName sn = (SimpleName) n;
      ASTNode parent = sn.getParent();
      if (parent instanceof FieldAccess) return ((FieldAccess) parent).resolveTypeBinding();
      if (parent instanceof MethodInvocation) return ((MethodInvocation) parent).resolveTypeBinding();
      if (parent instanceof QualifiedName) {
        QualifiedName qn = (QualifiedName) parent;
        return qn.resolveTypeBinding();
      }
      IVariableBinding vb = asVariable(sn.resolveBinding());
      if (vb != null) return vb.getType();
    }
    if (n instanceof Name) {
      IBinding b = ((Name) n).resolveBinding();
      if (b instanceof ITypeBinding) return (ITypeBinding) b;
      IVariableBinding vab = asVariable(b);
      return vab == null ? null : vab.getType();
    }
    return null;
  }

  private static IVariableBinding asVariable(IBinding b) {
    return b instanceof IVariableBinding ? (IVariableBinding) b : null;
  }

  private static IMethodBinding asMethod(IBinding b) {
    return b instanceof IMethodBinding ? (IMethodBinding) b : null;
  }

  private static void collectReceiverMembers(
      ITypeBinding receiver, String prefix, Range range, List<CompletionItem> items) {
    if (receiver == null) return;
    Set<String> seen = new LinkedHashSet<>();
    for (ITypeBinding t = receiver; t != null; t = t.getSuperclass()) {
      if (t.getName() == null) continue;
      for (IVariableBinding field : t.getDeclaredFields()) {
        String name = field.getName();
        if (name == null || !seen.add(name) || !matches(name, prefix)) continue;
        CompletionItem it = base(name, CompletionItemKind.Field, range);
        it.setDetail(shortType(field.getType()));
        if (field.getDeclaringClass() != null) {
          it.setDetail(it.getDetail() + "  // " + field.getDeclaringClass().getQualifiedName());
        }
        if (Modifier.isStatic(field.getModifiers())) it.setSortText("0" + name);
        items.add(it);
      }
      for (IMethodBinding m : t.getDeclaredMethods()) {
        String name = m.getName();
        if (name == null || !seen.add("m" + name) || !matches(name, prefix)) continue;
        CompletionItem it = base(name, CompletionItemKind.Method, range);
        it.setDetail(signatureSimple(m));
        if (Modifier.isStatic(m.getModifiers())) it.setSortText("0" + name);
        items.add(it);
      }
    }
    if (items.size() > COMPLETION_LIMIT) items.subList(COMPLETION_LIMIT, items.size()).clear();
  }

  private static void collectLocals(
      CompilationUnit cu, String prefix, Range range, List<CompletionItem> items) {
    List<String> names = new ArrayList<>();
    cu.accept(
        new ASTVisitor() {
          @Override
          public boolean visit(VariableDeclarationStatement node) {
            for (Object f : node.fragments()) {
              if (f instanceof VariableDeclarationFragment) {
                names.add(((VariableDeclarationFragment) f).getName().getIdentifier());
              }
            }
            return true;
          }

          @Override
          public boolean visit(SingleVariableDeclaration node) {
            names.add(node.getName().getIdentifier());
            return true;
          }

          @Override
          public boolean visit(FieldDeclaration node) {
            for (Object f : node.fragments()) {
              if (f instanceof VariableDeclarationFragment) {
                names.add(((VariableDeclarationFragment) f).getName().getIdentifier());
              }
            }
            return true;
          }
        });
    for (String name : names) {
      if (name != null && matches(name, prefix)) {
        items.add(base(name, CompletionItemKind.Variable, range));
      }
    }
  }

  private static void collectDeclaredTypes(
      CompilationUnit cu, String prefix, Range range, List<CompletionItem> items) {
    cu.accept(
        new ASTVisitor() {
          @Override
          public boolean visit(TypeDeclaration node) {
            add(node.getName());
            return true;
          }

          @Override
          public boolean visit(EnumDeclaration node) {
            add(node.getName());
            return true;
          }

          @Override
          public boolean visit(AnnotationTypeDeclaration node) {
            add(node.getName());
            return true;
          }

          private void add(Name n) {
            String s = n instanceof SimpleName ? ((SimpleName) n).getIdentifier() : n.getFullyQualifiedName();
            if (matches(s, prefix)) items.add(base(s, CompletionItemKind.Class, range));
          }
        });
  }

  private static void collectImportedNames(
      CompilationUnit cu, String prefix, Range range, List<CompletionItem> items) {
    if (cu.imports() == null) return;
    for (Object o : cu.imports()) {
      if (!(o instanceof ImportDeclaration)) continue;
      ImportDeclaration imp = (ImportDeclaration) o;
      String fqn = imp.getName().getFullyQualifiedName();
      if (fqn == null) continue;
      int dot = fqn.lastIndexOf('.');
      String simple = dot >= 0 ? fqn.substring(dot + 1) : fqn;
      if (simple.endsWith("*")) continue;
      if (matches(simple, prefix)) {
        CompletionItem it = base(simple, CompletionItemKind.Class, range);
        it.setDetail(fqn);
        items.add(it);
      }
    }
  }

  private static void collectProjectTypes(
      String[] sourcePaths, String prefix, Range range, List<CompletionItem> items) {
    if (sourcePaths == null || sourcePaths.length == 0) return;
    String[] roots = sourcePaths;
    try {
      JdtEngine tmp = new JdtEngine();
      Set<String> seen = new LinkedHashSet<>();
      int count = 0;
      for (String binary : tmp.projectTypes(roots)) {
        if (++count > 2000) break;
        int dot = binary.lastIndexOf('.');
        String simple = dot >= 0 ? binary.substring(dot + 1) : binary;
        if (!seen.add(simple) || !matches(simple, prefix)) continue;
        CompletionItem it = base(simple, CompletionItemKind.Class, range);
        it.setDetail(binary);
        items.add(it);
      }
    } catch (Throwable ignored) {
      // Best effort only.
    }
  }

  private static void collectKeywords(String prefix, Range range, List<CompletionItem> items) {
    for (String kw : KEYWORDS) {
      if (matches(kw, prefix)) items.add(base(kw, CompletionItemKind.Keyword, range));
    }
  }

  private static final String[] KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
    "for", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
    "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
    "volatile", "while", "null", "true", "false", "var", "record", "sealed", "permits", "yield"
  };

  private static boolean matches(String name, String prefix) {
    if (name == null) return false;
    if (prefix == null || prefix.isEmpty()) return true;
    return name.startsWith(prefix);
  }

  private static CompletionItem base(String name, CompletionItemKind kind, Range range) {
    CompletionItem item = new CompletionItem(name);
    item.setKind(kind);
    item.setSortText(name);
    item.setFilterText(name);
    item.setTextEdit(Either.forLeft(new TextEdit(range, name)));
    return item;
  }

  private static String shortType(ITypeBinding t) {
    if (t == null) return "?";
    return t.isArray() ? shortType(t.getElementType()) + "[]" : t.getName();
  }

  private static String signatureSimple(IMethodBinding m) {
    StringBuilder sb = new StringBuilder();
    sb.append(shortType(m.getReturnType())).append(' ').append(m.getName()).append('(');
    ITypeBinding[] params = m.getParameterTypes();
    if (params != null) {
      for (int i = 0; i < params.length; i++) {
        if (i > 0) sb.append(", ");
        sb.append(shortType(params[i]));
      }
    }
    sb.append(')');
    return sb.toString();
  }

  /** Computes the token prefix that a completion replaces, ending at {@code offset}. */
  public static String prefixBefore(String text, int offset) {
    if (text == null || offset <= 0) return "";
    int i = offset - 1;
    while (i >= 0) {
      char c = text.charAt(i);
      if (!Character.isJavaIdentifierPart(c)) break;
      i--;
    }
    return text.substring(i + 1, offset);
  }

  /** The range of the simple name/token being completed, ending at {@code offset}. */
  public static Range completionRange(String text, int offset) {
    int i = offset - 1;
    while (i >= 0) {
      char c = text.charAt(i);
      if (!Character.isJavaIdentifierPart(c)) break;
      i--;
    }
    return new Range(offsetToPosition(text, i + 1), offsetToPosition(text, offset));
  }

  // ───────────────────────────── Hover ─────────────────────────────

  /** Resolves whatever simple name sits at {@code offset} and returns its binding, if any. */
  public static IBinding bindingAt(CompilationUnit cu, int offset) {
    if (cu == null) return null;
    ASTNode node = NodeFinder.perform(cu, offset, 0);
    if (node == null) return null;
    IBinding binding = null;
    if (node instanceof SimpleName) binding = ((SimpleName) node).resolveBinding();
    else if (node.getParent() instanceof SimpleName) {
      binding = ((SimpleName) node.getParent()).resolveBinding();
    }
    return binding;
  }

  /** Markdown hover text for a bound node (signature + declaring type). */
  public static String signatureMarkdown(IBinding binding) {
    if (binding == null) return null;
    String body;
    switch (binding.getKind()) {
      case IBinding.METHOD:
        body = signatureSimple((IMethodBinding) binding);
        break;
      case IBinding.TYPE:
        body = typeSignature((ITypeBinding) binding);
        break;
      case IBinding.VARIABLE:
        body = variableSignature((IVariableBinding) binding);
        break;
      default:
        body = binding.getName();
    }
    return "```java\n" + body + "\n```";
  }

  private static String typeSignature(ITypeBinding t) {
    StringBuilder sb = new StringBuilder(t.getQualifiedName());
    ITypeBinding sup = t.getSuperclass();
    if (sup != null
        && sup.getQualifiedName() != null
        && !"java.lang.Object".equals(sup.getQualifiedName())) {
      sb.append("\n\nextends ").append(sup.getName());
    }
    ITypeBinding[] ifaces = t.getInterfaces();
    if (ifaces != null && ifaces.length > 0) {
      sb.append('\n');
      for (int i = 0; i < ifaces.length; i++) {
        sb.append(i == 0 ? "implements " : ", ").append(ifaces[i].getName());
      }
    }
    return sb.toString();
  }

  private static String variableSignature(IVariableBinding v) {
    StringBuilder sb = new StringBuilder();
    ITypeBinding type = v.getType();
    sb.append(shortType(type)).append(' ').append(v.getName());
    if (v.isField() && v.getDeclaringClass() != null) {
      sb.append("  // ").append(v.getDeclaringClass().getQualifiedName());
    }
    return sb.toString();
  }

  /** Kept for compatibility with any external caller; wraps {@link #signatureMarkdown}. */
  public static Hover hoverFor(IBinding binding, Range range) {
    String md = signatureMarkdown(binding);
    if (md == null) return null;
    return new Hover(new MarkupContent(MarkupKind.MARKDOWN, md), range);
  }

  // ───────────────────────────── Definitions / references ─────────────────────────────

  /** Binary (source) qualified name of the declaring type of a binding, if it is in source. */
  public static String declaringBinaryName(IBinding binding) {
    if (binding == null) return null;
    ITypeBinding t = null;
    switch (binding.getKind()) {
      case IBinding.TYPE:
        t = (ITypeBinding) binding;
        break;
      case IBinding.METHOD:
        t = ((IMethodBinding) binding).getDeclaringClass();
        break;
      case IBinding.VARIABLE:
        t = ((IVariableBinding) binding).getDeclaringClass();
        break;
      default:
        return null;
    }
    if (t == null) return null;
    if (t.isLocal() || t.isAnonymous()) return null;
    String bin = t.getBinaryName();
    return bin != null ? bin : t.getQualifiedName();
  }

  /** Stable identity of a binding (works across parses with the same environment). */
  public static String bindingKey(IBinding binding) {
    return binding == null ? null : binding.getKey();
  }

  /** The {@link SimpleName}s in a unit whose binding key equals {@code key} (covers decl + uses). */
  public static List<SimpleName> usesOfKey(CompilationUnit cu, String key) {
    List<SimpleName> out = new ArrayList<>();
    if (cu == null || key == null) return out;
    cu.accept(
        new ASTVisitor() {
          @Override
          public boolean visit(SimpleName node) {
            IBinding b = node.resolveBinding();
            if (b != null && key.equals(b.getKey())) out.add(node);
            return true;
          }
        });
    return out;
  }

  /** LSP range of a name node inside its unit's coordinates. */
  public static Range nameRange(CompilationUnit cu, Name name) {
    if (cu == null || name == null) return new Range(new Position(0, 0), new Position(0, 1));
    Position start = offsetToPosition(cu, name.getStartPosition());
    int end = name.getStartPosition() + Math.max(1, name.getLength());
    return new Range(start, offsetToPosition(cu, end));
  }

  // ───────────────────────────── Document symbols ─────────────────────────────

  public static List<DocumentSymbol> documentSymbols(CompilationUnit cu) {
    List<DocumentSymbol> out = new ArrayList<>();
    if (cu == null) return out;
    cu.accept(
        new ASTVisitor() {
          @Override
          public boolean visit(CompilationUnit node) {
            for (Object t : node.types()) {
              if (t instanceof TypeDeclaration) addType((TypeDeclaration) t, out);
              else if (t instanceof EnumDeclaration) addEnum((EnumDeclaration) t, out);
              else if (t instanceof AnnotationTypeDeclaration) {
                AnnotationTypeDeclaration a = (AnnotationTypeDeclaration) t;
                out.add(typeSymbol(a.getName(), SymbolKind.Interface, a));
              }
            }
            return false;
          }
        });
    return out;
  }

  private static void addType(TypeDeclaration t, List<DocumentSymbol> out) {
    DocumentSymbol sym =
        typeSymbol(t.getName(), t.isInterface() ? SymbolKind.Interface : SymbolKind.Class, t);
    List<DocumentSymbol> children = new ArrayList<>();
    for (TypeDeclaration member : t.getTypes()) {
      if (member.resolveBinding() != null) addType(member, children);
    }
    for (Object body : t.bodyDeclarations()) {
      if (body instanceof MethodDeclaration) {
        MethodDeclaration m = (MethodDeclaration) body;
        children.add(
            memberSymbol(
                m.getName(),
                SymbolKind.Method,
                m,
                m.isConstructor() ? "constructor" : detailOf(m),
                m.getName()));
      } else if (body instanceof FieldDeclaration) {
        FieldDeclaration f = (FieldDeclaration) body;
        DocumentSymbol fs = null;
        for (Object frag : f.fragments()) {
          if (frag instanceof VariableDeclarationFragment) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) frag;
            fs =
                memberSymbol(
                    vdf.getName().getStartPosition(),
                    vdf.getName().getIdentifier(),
                    SymbolKind.Field,
                    f,
                    detailOf(f));
            break;
          }
        }
        if (fs == null) fs = memberSymbol(f.getStartPosition(), "field", SymbolKind.Field, f, null);
        children.add(fs);
      } else if (body instanceof Initializer) {
        children.add(memberSymbol(null, SymbolKind.Property, (Initializer) body, "initializer", null));
      }
    }
    sym.setChildren(children);
    out.add(sym);
  }

  private static void addEnum(EnumDeclaration e, List<DocumentSymbol> out) {
    DocumentSymbol sym = typeSymbol(e.getName(), SymbolKind.Enum, e);
    List<DocumentSymbol> children = new ArrayList<>();
    for (Object c : e.enumConstants()) {
      if (c instanceof EnumConstantDeclaration) {
        symbolOf((EnumConstantDeclaration) c, SymbolKind.EnumMember, null)
            .ifPresent(children::add);
      }
    }
    sym.setChildren(children);
    out.add(sym);
  }

  private static DocumentSymbol typeSymbol(Name name, SymbolKind kind, ASTNode node) {
    DocumentSymbol s = new DocumentSymbol();
    s.setName(name == null ? "" : name.getFullyQualifiedName());
    s.setKind(kind);
    s.setRange(rangeOf(node));
    s.setSelectionRange(rangeOf(name));
    return s;
  }

  private static DocumentSymbol memberSymbol(Name name, SymbolKind kind, ASTNode node,
      String detail, ASTNode selection) {
    DocumentSymbol s = new DocumentSymbol();
    s.setName(name == null ? detail : name.getFullyQualifiedName());
    s.setKind(kind);
    s.setDetail(detail);
    s.setRange(rangeOf(node));
    s.setSelectionRange(selection != null ? rangeOf(selection) : rangeOf(node));
    return s;
  }

  private static DocumentSymbol memberSymbol(int startOffset, String name, SymbolKind kind,
      ASTNode node, String detail) {
    DocumentSymbol s = new DocumentSymbol();
    s.setName(name);
    s.setKind(kind);
    s.setDetail(detail);
    s.setRange(rangeOf(node));
    s.setSelectionRange(new Range(offsetToPosition(node, startOffset),
        offsetToPosition(node, startOffset + Math.max(1, name.length()))));
    return s;
  }

  private static java.util.Optional<DocumentSymbol> symbolOf(ASTNode node, SymbolKind kind,
      String detail) {
    DocumentSymbol s = new DocumentSymbol();
    String n = (node instanceof EnumConstantDeclaration)
        ? ((EnumConstantDeclaration) node).getName().getIdentifier()
        : kind.name();
    s.setName(n);
    s.setKind(kind);
    s.setDetail(detail);
    s.setRange(rangeOf(node));
    s.setSelectionRange(rangeOf(node));
    return java.util.Optional.of(s);
  }

  private static String detailOf(MethodDeclaration m) {
    StringBuilder sb = new StringBuilder();
    if (m.isConstructor()) {
      sb.append(m.getName().getIdentifier());
    } else {
      if (m.getReturnType2() != null) sb.append(m.getReturnType2().toString()).append(' ');
      sb.append(m.getName().getIdentifier());
    }
    sb.append('(').append(parametersOf(m)).append(')');
    return sb.toString();
  }

  private static String detailOf(FieldDeclaration f) {
    return f.getType() == null ? "" : f.getType().toString();
  }

  private static String parametersOf(MethodDeclaration m) {
    StringBuilder sb = new StringBuilder();
    for (Object p : m.parameters()) {
      if (p instanceof SingleVariableDeclaration) {
        if (sb.length() > 0) sb.append(", ");
        SingleVariableDeclaration sv = (SingleVariableDeclaration) p;
        sb.append(sv.getType() == null ? "?" : sv.getType().toString())
            .append(' ')
            .append(sv.getName().getIdentifier());
      }
    }
    return sb.toString();
  }

  private static Range rangeOf(ASTNode node) {
    return rangeOf(node, node.getStartPosition(), node.getStartPosition() + node.getLength());
  }

  private static Range rangeOf(Name name) {
    return nameRange(name.getRoot() instanceof CompilationUnit ? (CompilationUnit) name.getRoot() : null, name);
  }

  private static Range rangeOf(ASTNode node, int start, int end) {
    Position s = offsetToPosition(node, start);
    Position e = offsetToPosition(node, Math.max(end, start + 1));
    return new Range(s, e);
  }

  // ───────────────────────────── Folding ─────────────────────────────

  public static List<FoldingRange> foldingRanges(CompilationUnit cu) {
    List<FoldingRange> out = new ArrayList<>();
    if (cu == null) return out;
    cu.accept(
        new ASTVisitor() {
          @Override
          public boolean visit(TypeDeclaration node) {
            fold(node, out);
            return true;
          }

          @Override
          public boolean visit(EnumDeclaration node) {
            fold(node, out);
            return true;
          }

          @Override
          public boolean visit(AnnotationTypeDeclaration node) {
            fold(node, out);
            return true;
          }

          @Override
          public boolean visit(MethodDeclaration node) {
            if (node.getBody() != null) foldBlock(node.getBody(), "region", out);
            return true;
          }

          @Override
          public boolean visit(Initializer node) {
            foldBlock(node.getBody(), "region", out);
            return true;
          }

          @Override
          public boolean visit(TryStatement node) {
            foldBlock(node.getBody(), "region", out);
            return true;
          }

          @Override
          public boolean visit(SwitchStatement node) {
            fold(node, out);
            return true;
          }

          @Override
          public boolean visit(IfStatement node) {
            ifStatement(node, out);
            return true;
          }

          @Override
          public boolean visit(ForStatement node) {
            if (node.getBody() instanceof Block) foldBlock((Block) node.getBody(), "region", out);
            return true;
          }

          @Override
          public boolean visit(EnhancedForStatement node) {
            if (node.getBody() instanceof Block) foldBlock((Block) node.getBody(), "region", out);
            return true;
          }

          @Override
          public boolean visit(WhileStatement node) {
            if (node.getBody() instanceof Block) foldBlock((Block) node.getBody(), "region", out);
            return true;
          }

          @Override
          public boolean visit(DoStatement node) {
            if (node.getBody() instanceof Block) foldBlock((Block) node.getBody(), "region", out);
            return true;
          }

          @Override
          public boolean visit(SynchronizedStatement node) {
            foldBlock(node.getBody(), "region", out);
            return true;
          }
        });
    return out;
  }

  private static void fold(ASTNode node, List<FoldingRange> out) {
    int start = node.getStartPosition();
    int end = start + node.getLength();
    int sLine = lineOf(node, start);
    int eLine = lineOf(node, end - 1);
    if (eLine > sLine) out.add(new FoldingRange(sLine, eLine));
  }

  private static void foldBlock(Block block, String kind, List<FoldingRange> out) {
    if (block == null) return;
    int sLine = lineOf(block, block.getStartPosition() + 1); // after '{'
    int eLine = lineOf(block, block.getStartPosition() + block.getLength() - 2);
    FoldingRange r = new FoldingRange(sLine, eLine);
    r.setKind(kind);
    if (eLine > sLine) out.add(r);
  }

  private static void ifStatement(IfStatement node, List<FoldingRange> out) {
    Statement then = node.getThenStatement();
    if (then instanceof Block) foldBlock((Block) then, "region", out);
    Statement els = node.getElseStatement();
    if (els instanceof Block) foldBlock((Block) els, "region", out);
  }

  // ───────────────────────────── Signature help ─────────────────────────────

  public static SignatureHelp signatureHelp(CompilationUnit cu, String text, int offset) {
    if (cu == null || text == null) return null;
    ASTNode node = NodeFinder.perform(cu, offset, 0);
    if (node == null) return null;

    Invocation info = null;
    for (ASTNode cur = node; cur != null; cur = cur.getParent()) {
      if (cur instanceof MethodInvocation) {
        if (offsetInArgs(text, (MethodInvocation) cur, offset)) {
          info = new Invocation((MethodInvocation) cur);
          break;
        }
      } else if (cur instanceof SuperMethodInvocation) {
        if (offsetInArgs(text, (SuperMethodInvocation) cur, offset)) {
          info = new Invocation((SuperMethodInvocation) cur);
          break;
        }
      } else if (cur instanceof ClassInstanceCreation) {
        if (offsetInArgs(text, (ClassInstanceCreation) cur, offset)) {
          info = new Invocation((ClassInstanceCreation) cur);
          break;
        }
      } else if (cur instanceof ConstructorInvocation) {
        if (offsetInArgs(text, (ConstructorInvocation) cur, offset)) {
          info = new Invocation((ConstructorInvocation) cur);
          break;
        }
      }
    }
    if (info == null) return null;

    List<SignatureInformation> signatures = new ArrayList<>();
    int activeParam = info.activeParameter(text, offset);
    addSignatures(info, signatures);

    SignatureHelp help = new SignatureHelp();
    help.setSignatures(signatures);
    help.setActiveParameter(activeParam);
    if (!signatures.isEmpty()) help.setActiveSignature(0);
    return help;
  }

  private static boolean offsetInArgs(String text, ASTNode inv, int offset) {
    int start = inv.getStartPosition();
    int open = text.indexOf('(', start);
    if (open < 0) return false;
    int close = text.indexOf(')', offset);
    if (close < 0) close = start + inv.getLength();
    return offset > open && offset <= close;
  }

  private static void addSignatures(Invocation inv, List<SignatureInformation> out) {
    String name = inv.name();
    int paramCount;
    IMethodBinding[] bindings = inv.bindings();
    if (bindings != null && bindings.length > 0) {
      for (IMethodBinding b : bindings) {
        SignatureInformation si = new SignatureInformation(signatureSimple(b));
        List<ParameterInformation> params = new ArrayList<>();
        ITypeBinding[] types = b.getParameterTypes();
        if (types != null) {
          for (int i = 0; i < types.length; i++) {
            String pn = b.getParameterNames() != null && i < b.getParameterNames().length
                ? b.getParameterNames()[i]
                : "arg" + i;
            params.add(new ParameterInformation(" / " + pn + ""));
          }
        }
        si.setParameters(params);
        out.add(si);
      }
      return;
    }
    // Unresolved: express the arity from the open file's syntax.
    paramCount = inv.startArgumentCountForSyntax();
    SignatureInformation si = new SignatureInformation(name + "(" + paramsText(paramCount) + ")");
    List<ParameterInformation> params = new ArrayList<>();
    for (int i = 0; i < paramCount; i++) {
      params.add(new ParameterInformation(" arg" + i));
    }
    si.setParameters(params);
    out.add(si);
  }

  private static String paramsText(int n) {
    if (n <= 0) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      if (i > 0) sb.append(", ");
      sb.append("?");
    }
    return sb.toString();
  }

  private static final class Invocation {
    final ASTNode node;
    final String nameText;

    Invocation(MethodInvocation m) {
      node = m;
      nameText = m.getName().getIdentifier();
    }

    Invocation(SuperMethodInvocation m) {
      node = m;
      nameText = m.getName().getIdentifier();
    }

    Invocation(ClassInstanceCreation c) {
      node = c;
      nameText = c.getType().toString();
    }

    Invocation(ConstructorInvocation c) {
      node = c;
      nameText = "this";
    }

    String name() {
      return nameText;
    }

    IMethodBinding[] bindings() {
      if (node instanceof MethodInvocation) {
        IMethodBinding b = ((MethodInvocation) node).resolveMethodBinding();
        return b == null ? null : new IMethodBinding[] {b};
      }
      if (node instanceof SuperMethodInvocation) {
        IMethodBinding b = ((SuperMethodInvocation) node).resolveMethodBinding();
        return b == null ? null : new IMethodBinding[] {b};
      }
      if (node instanceof ClassInstanceCreation) {
        IMethodBinding b = ((ClassInstanceCreation) node).resolveConstructorBinding();
        return b == null ? null : new IMethodBinding[] {b};
      }
      if (node instanceof ConstructorInvocation) {
        IMethodBinding b = ((ConstructorInvocation) node).resolveConstructorBinding();
        return b == null ? null : new IMethodBinding[] {b};
      }
      return null;
    }

    int startArgumentCountForSyntax() {
      if (node instanceof MethodInvocation) return ((MethodInvocation) node).arguments().size();
      if (node instanceof SuperMethodInvocation) {
        return ((SuperMethodInvocation) node).arguments().size();
      }
      if (node instanceof ClassInstanceCreation) return ((ClassInstanceCreation) node).arguments().size();
      if (node instanceof ConstructorInvocation) return ((ConstructorInvocation) node).arguments().size();
      return 0;
    }

    int activeParameter(String text, int offset) {
      int start = node.getStartPosition();
      int open = text.indexOf('(', start);
      if (open < 0) return 0;
      int commas = 0;
      int depth = 0;
      for (int i = open + 1; i < offset; i++) {
        char c = text.charAt(i);
        if (c == '(') depth++;
        else if (c == ')') depth--;
        else if (c == ',' && depth == 0) commas++;
      }
      return commas;
    }
  }

  // ───────────────────────────── Inlay hints ─────────────────────────────

  public static List<InlayHint> inlayHints(CompilationUnit cu) {
    List<InlayHint> out = new ArrayList<>();
    if (cu == null) return out;
    cu.accept(
        new ASTVisitor() {
          @Override
          public boolean visit(MethodInvocation node) {
            addParamHints(node.resolveMethodBinding(), node.arguments(), out);
            return true;
          }

          @Override
          public boolean visit(SuperMethodInvocation node) {
            addParamHints(node.resolveMethodBinding(), node.arguments(), out);
            return true;
          }

          @Override
          public boolean visit(ClassInstanceCreation node) {
            addParamHints(node.resolveConstructorBinding(), node.arguments(), out);
            return true;
          }
        });
    return out;
  }

  private static void addParamHints(IMethodBinding binding, List args,
      List<InlayHint> out) {
    if (binding == null || args == null || args.size() == 0) return;
    String[] names = binding.getParameterNames();
    if (names == null) return;
    int n = Math.min(names.length, args.size());
    for (int i = 0; i < n; i++) {
      Object a = args.get(i);
      if (!(a instanceof Expression)) continue;
      Expression arg = (Expression) a;
      String paramName = names[i];
      if (paramName == null || paramName.isEmpty()) continue;
      Position pos = offsetToPosition(arg, arg.getStartPosition());
      InlayHint hint = new InlayHint(pos, Either.forLeft(paramName + ":  "));
      hint.setPaddingLeft(false);
      hint.setPaddingRight(true);
      out.add(hint);
    }
  }

  // ───────────────────────────── Imports / code actions ─────────────────────────────

  /** Edits that delete import statements whose simple name is never used in the file. */
  public static List<TextEdit> unusedImportEdits(CompilationUnit cu, String text) {
    List<TextEdit> out = new ArrayList<>();
    if (cu == null || text == null) return out;
    Set<String> used = usedNames(cu);
    for (Object o : cu.imports()) {
      if (!(o instanceof ImportDeclaration)) continue;
      ImportDeclaration imp = (ImportDeclaration) o;
      if (imp.isStatic()) continue; // conservative: static imports are hard to track
      String fqn = imp.getName().getFullyQualifiedName();
      if (fqn == null) continue;
      int dot = fqn.lastIndexOf('.');
      String simple = dot >= 0 ? fqn.substring(dot + 1) : fqn;
      if (simple.endsWith("*")) continue;
      if (!used.contains(simple)) {
        Range r = importLineRange(imp, text);
        out.add(new TextEdit(r, ""));
      }
    }
    return out;
  }

  /** Replaces the whole import block with unused imports removed and the rest sorted. */
  public static TextEdit organizeImportsEdit(CompilationUnit cu, String text) {
    if (cu == null || text == null || cu.imports() == null) return null;
    if (cu.imports().isEmpty()) return null;
    Set<String> used = usedNames(cu);
    List<String> kept = new ArrayList<>();
    for (Object o : cu.imports()) {
      if (!(o instanceof ImportDeclaration)) continue;
      ImportDeclaration imp = (ImportDeclaration) o;
      String fqn = imp.getName().getFullyQualifiedName();
      if (fqn == null) continue;
      int dot = fqn.lastIndexOf('.');
      String simple = dot >= 0 ? fqn.substring(dot + 1) : fqn;
      if (imp.isStatic() || simple.endsWith("*") || used.contains(simple)) kept.add(fqn);
    }
    if (kept.isEmpty()) return null;
    kept.sort(String::compareTo);

    ImportDeclaration first = (ImportDeclaration) cu.imports().get(0);
    int start = first.getStartPosition();
    int end = start;
    for (Object o : cu.imports()) {
      if (o instanceof ImportDeclaration) {
        ImportDeclaration imp = (ImportDeclaration) o;
        end = Math.max(end, imp.getStartPosition() + imp.getLength());
      }
    }
    // Extend through the end of line of the last import.
    int lineEnd = text.indexOf('\n', end);
    if (lineEnd >= 0) end = lineEnd + 1;

    StringBuilder sb = new StringBuilder();
    for (String imp : kept) sb.append("import ").append(imp).append(";\n");
    return new TextEdit(
        new Range(offsetToPosition(text, start), offsetToPosition(text, end)), sb.toString());
  }

  private static Set<String> usedNames(CompilationUnit cu) {
    Set<String> used = new LinkedHashSet<>();
    cu.accept(
        new ASTVisitor() {
          @Override
          public boolean visit(SimpleName node) {
            String id = node.getIdentifier();
            if (id != null) used.add(id);
            return true;
          }

          @Override
          public boolean visit(ImportDeclaration node) {
            return false; // don't count the import's own name
          }
        });
    return used;
  }

  private static Range importLineRange(ImportDeclaration imp, String text) {
    int start = imp.getStartPosition();
    int lineEnd = text.indexOf('\n', start);
    int end = lineEnd >= 0 ? lineEnd + 1 : start + imp.getLength();
    return new Range(offsetToPosition(text, start), offsetToPosition(text, end));
  }

  // ───────────────────────────── Position helpers ─────────────────────────────

  /** LSP position of {@code offset} in the unit's source (line+column from JDT's line table). */
  public static Position offsetToPosition(ASTNode node, int offset) {
    if (node == null) return new Position(0, 0);
    CompilationUnit cu =
        node.getRoot() instanceof CompilationUnit ? (CompilationUnit) node.getRoot() : null;
    if (cu != null) {
      int line = cu.getLineNumber(offset);
      int col = cu.getColumnNumber(offset);
      if (line > 0) return new Position(line - 1, Math.max(0, col - 1));
    }
    return new Position(0, 0);
  }

  private static int lineOf(ASTNode node, int offset) {
    if (node == null) return 0;
    Object root = node.getRoot();
    if (root instanceof CompilationUnit) {
      int line = ((CompilationUnit) root).getLineNumber(offset);
      return line > 0 ? line - 1 : 0;
    }
    return 0;
  }

  /** LSP position of {@code offset} via raw string scanning (utf-16 columns). */
  public static Position offsetToPosition(String text, int offset) {
    if (text == null) return new Position(0, 0);
    int line = 0;
    int col = 0;
    int n = Math.min(offset, text.length());
    for (int i = 0; i < n; i++) {
      if (text.charAt(i) == '\n') {
        line++;
        col = 0;
      } else {
        col++;
      }
    }
    return new Position(line, col);
  }

  /** Column (char offset inside the line) of {@code offset} in {@code text}. */
  public static int columnAt(String text, int offset) {
    if (text == null) return 0;
    int lineStart = text.lastIndexOf('\n', Math.min(offset, text.length()) - 1);
    return offset - (lineStart + 1);
  }
}