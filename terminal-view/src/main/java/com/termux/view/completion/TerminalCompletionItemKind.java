package com.termux.view.completion;

/**
 * Completion item kinds.
 *
 * <p>Mirrors {@code io.github.rosemoe.sora.lang.completion.CompletionItemKind} so that LSP
 * completions can be mapped onto the same kind set (both follow the LSP CompletionItemKind spec).
 */
public enum TerminalCompletionItemKind {

  /** Identifier. */
  Identifier(0, 0xffabb6bd),
  /** Plain text. */
  Text(0, 0xffabb6bd),
  /** Method. */
  Method(1, 0xfff4b2be),
  /** Function. */
  Function(2, 0xfff4b2be),
  /** Constructor. */
  Constructor(3, 0xfff4b2be),
  /** Field. */
  Field(4, 0xfff1c883),
  /** Variable. */
  Variable(5, 0xfff1c883),
  /** Class. */
  Class(6, 0xff85cce5),
  /** Interface. */
  Interface(7, 0xff99cb87),
  /** Module. */
  Module(8, 0xff85cce5),
  /** Property. */
  Property(9, 0xffcebcf4),
  /** Unit. */
  Unit(10),
  /** Value. */
  Value(11, 0xfff1c883),
  /** Enum. */
  Enum(12, 0xff85cce5),
  /** Keyword. */
  Keyword(13, 0xffcc7832),
  /** Snippet. */
  Snippet(14),
  /** Color. */
  Color(15, 0xfff4b2be),
  /** File. */
  File(16),
  /** Reference. */
  Reference(17),
  /** Folder. */
  Folder(18),
  /** Enum member. */
  EnumMember(19),
  /** Constant. */
  Constant(20, 0xfff1c883),
  /** Struct. */
  Struct(21, 0xffcebcf4),
  /** Event. */
  Event(22),
  /** Operator. */
  Operator(23, 0xffeaabb6),
  /** Type parameter. */
  TypeParameter(24, 0xfff1c883),
  /** User. */
  User(25),
  /** Issue. */
  Issue(26);

  private final int value;
  private final long defaultDisplayBackgroundColor;
  private final String displayString;

  TerminalCompletionItemKind(int value, long defaultDisplayBackgroundColor) {
    this.value = value;
    this.defaultDisplayBackgroundColor = defaultDisplayBackgroundColor;
    this.displayString = name().substring(0, 1);
  }

  TerminalCompletionItemKind(int value) {
    this(value, 0);
  }

  /** The LSP-compatible numeric value of this kind. */
  public int getValue() {
    return value;
  }

  /** The default background color (as a packed 0xaarrggbb long) used for this kind's icon. */
  public long getDefaultDisplayBackgroundColor() {
    return defaultDisplayBackgroundColor;
  }

  /** The display character (first letter of the kind name) shown on the icon. */
  public String getDisplayChar() {
    return displayString;
  }

  /**
   * Map an LSP {@code CompletionItemKind} numeric value to this enum.
   *
   * @param lspValue The LSP kind integer value, or null to get {@link #Identifier}.
   * @return The matching kind, or {@code Identifier} for unknown values.
   */
  public static TerminalCompletionItemKind fromLspValue(Integer lspValue) {
    if (lspValue == null) {
      return Identifier;
    }
    for (TerminalCompletionItemKind kind : values()) {
      if (kind.value == lspValue) {
        return kind;
      }
    }
    return Identifier;
  }
}
