package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.model;

import org.eclipse.lsp4j.SymbolKind;

public class BreadcrumbItem {

  private final String name;
  private final SymbolKind kind;
  private final int line;
  private final int column;

  public BreadcrumbItem(String name, SymbolKind kind, int line, int column) {
    this.name = name;
    this.kind = kind;
    this.line = line;
    this.column = column;
  }

  public String getName() {
    return name;
  }

  public SymbolKind getKind() {
    return kind;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }
}
