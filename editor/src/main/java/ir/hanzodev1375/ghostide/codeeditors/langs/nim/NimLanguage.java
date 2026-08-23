/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.nim;

import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLangConfig;
import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLanguage;

public class NimLanguage extends SimpleLanguage {

  private static final String[] WORDS = {
    "addr", "and", "as", "asm", "bind", "block", "break", "case", "cast", "concept",
    "const", "continue", "converter", "defer", "discard", "distinct", "div", "do",
    "elif", "else", "end", "enum", "except", "export", "finally", "for", "from",
    "func", "if", "import", "include", "interface", "is", "isnot", "iterator", "let",
    "macro", "method", "mixin", "mod", "not", "notin", "object", "of", "or", "out",
    "proc", "ptr", "raise", "ref", "return", "shl", "shr", "static", "template",
    "try", "tuple", "type", "using", "var", "when", "while", "xor", "yield", "in",
    "true", "false",
    "int", "int8", "int16", "int32", "int64", "uint", "uint8", "uint16", "uint32",
    "uint64", "float", "float32", "float64", "bool", "char", "string", "cstring",
    "seq", "array", "openArray", "varargs", "range", "set", "object", "tuple",
    "pointer", "Natural", "Positive", "void", "auto", "untyped", "typed", "any",
    "Option", "Result", "File", "Table", "OrderedTable", "HashSet",
    "echo", "len", "high", "low", "inc", "dec", "add", "del", "delete", "insert",
    "repr", "alloc", "dealloc", "new", "newSeq", "newString", "quit", "assert",
    "defined", "compileOption", "ord", "chr", "succ", "pred", "abs", "min", "max",
    "clamp", "contains", "find", "filter", "map", "apply", "items", "pairs",
    "mpairs", "fields", "fieldPairs", "toFloat", "toInt", "runnableExamples",
    "pop", "setLen", "zeroDefault", "await", "async"
  };

  private static final SnippetDef[] SNIPPETS = {
    new SnippetDef(
        "proc",
        "Snippet - Procedure",
        "proc ${1:name}(${2:args}: ${3:int}): ${4:int} =\n    $0"),
    new SnippetDef(
        "func",
        "Snippet - Func",
        "func ${1:name}(${2:args}: ${3:int}): ${4:int} =\n    $0"),
    new SnippetDef(
        "ife",
        "Snippet - If/Else",
        "if ${1:condition}:\n    $0\nelse:\n    "),
    new SnippetDef(
        "for",
        "Snippet - For Loop",
        "for ${1:i} in ${2:0 ..< n}:\n    $0"),
    new SnippetDef(
        "type",
        "Snippet - Object Type",
        "type\n  ${1:Name} = object\n    ${2:field}: ${3:int}"),
  };

  public NimLanguage() {
    super(nimConfig(), WORDS, SNIPPETS);
  }

  private static SimpleLangConfig nimConfig() {
    var cfg = new SimpleLangConfig();
    cfg.lineComments = new String[] {"#"};
    cfg.tripleStrings = true;
    return cfg;
  }
}
