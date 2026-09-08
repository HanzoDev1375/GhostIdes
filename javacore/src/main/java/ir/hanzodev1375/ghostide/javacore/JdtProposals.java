package ir.hanzodev1375.ghostide.javacore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Converts raw Eclipse JDT {@link org.eclipse.jdt.core.CompletionProposal}s into LSP4j
 * {@link CompletionItem}s with the correct kind, label, replacement range and doc.
 */
public final class JdtProposals {

  private JdtProposals() {}

  public static CompletionItem toItem(
      org.eclipse.jdt.core.CompletionProposal proposal, String text, int requestOffset) {
    if (proposal == null) return null;

    String name = proposal.getName() != null ? String.valueOf(proposal.getName()) : "";
    if (name.isEmpty() && proposal instanceof org.eclipse.jdt.core.CompletionProposal) {
      // fall back to replacement string
      name = String.valueOf(proposal.getCompletion());
    }
    if (name.isEmpty()) return null;

    int kindId = proposal.getKind();
    CompletionItemKind kind = kindOf(kindId);

    String label;
    String detail = null;
    String doc = null;

    switch (kindId) {
      case org.eclipse.jdt.core.CompletionProposal.METHOD_REF:
        label = name + signature(proposal);
        break;
      case org.eclipse.jdt.core.CompletionProposal.CONSTRUCTOR_INVOCATION:
        label = name + signature(proposal);
        break;
      case org.eclipse.jdt.core.CompletionProposal.FIELD_REF:
        label = name;
        break;
      case org.eclipse.jdt.core.CompletionProposal.TYPE_REF:
        label = name;
        break;
      default:
        label = name;
    }

    CompletionItem item = new CompletionItem(label);
    item.setLabel(label);
    item.setKind(kind);
    item.setSortText(name);
    item.setFilterText(name);

    if (detail != null) item.setDetail(detail);
    if (doc != null) item.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, doc));

    int start = proposal.getReplaceStart();
    int end = proposal.getReplaceEnd();
    if (start >= 0 && end >= start && text != null) {
      Range range = new Range(offsetToPosition(text, start), offsetToPosition(text, end));
      item.setTextEdit(Either.forLeft(new TextEdit(range, String.valueOf(proposal.getCompletion()))));
    }
    return item;
  }

  private static String signature(CompletionProposal proposal) {
    char[] sig = proposal.getSignature();
    if (sig == null) return "()";
    try {
      char[][] raw = Signature.getParameterTypes(sig);
      String[] params = new String[raw.length];
      for (int i = 0; i < raw.length; i++) params[i] = new String(raw[i]);
      StringBuilder sb = new StringBuilder("(");
      for (int i = 0; i < params.length; i++) {
        if (i > 0) sb.append(", ");
        sb.append(simpleType(params[i]));
      }
      sb.append(')');
      return sb.toString();
    } catch (Exception e) {
      return "()";
    }
  }

  private static String simpleType(String sig) {
    String t = org.eclipse.jdt.core.Signature.toString(sig);
    if (t == null) return "?";
    int dot = t.lastIndexOf('.');
    return dot >= 0 ? t.substring(dot + 1) : t;
  }

  private static CompletionItemKind kindOf(int kind) {
    switch (kind) {
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.CONSTRUCTOR_INVOCATION:
        return CompletionItemKind.Method;
      case CompletionProposal.FIELD_REF:
        return CompletionItemKind.Field;
      case CompletionProposal.TYPE_REF:
      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
        return CompletionItemKind.Class;
      case CompletionProposal.KEYWORD:
        return CompletionItemKind.Keyword;
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return CompletionItemKind.Variable;
      case CompletionProposal.PACKAGE_REF:
        return CompletionItemKind.Module;
      default:
        return CompletionItemKind.Property;
    }
  }

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
}
