/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.scala;

import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLangConfig;
import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLanguage;

public class ScalaLanguage extends SimpleLanguage {

  private static final String[] WORDS = {
    "abstract", "case", "catch", "class", "def", "do", "else", "extends", "false", "final",
    "finally", "for", "forSome", "given", "if", "implicit", "import", "lazy", "match",
    "new", "null", "object", "override", "package", "private", "protected", "return",
    "sealed", "super", "this", "throw", "trait", "true", "try", "type", "val", "var",
    "while", "with", "yield", "enum", "export", "then", "using", "opaque", "inline",
    "transparent", "end", "extension", "derives", "into", "as", "macro",
    "Int", "Long", "Short", "Byte", "Double", "Float", "Boolean", "Char", "Unit",
    "String", "Any", "AnyRef", "AnyVal", "Nothing", "Null", "Option", "Some", "None",
    "List", "Vector", "Seq", "IndexedSeq", "ArrayBuffer", "ListBuffer", "Map",
    "HashMap", "Set", "HashSet", "SortedMap", "SortedSet", "Tuple1", "Tuple2", "Tuple3",
    "Either", "Left", "Right", "Try", "Success", "Failure", "Future", "Promise",
    "Function0", "Function1", "Function2", "Iterable", "Iterator", "StringBuilder",
    "println", "print", "printf", "require", "assume", "implicitly", "identity",
    "summon", "error", "locally", "readLine", "readInt", "readDouble", "readBoolean"
  };

  private static final SnippetDef[] SNIPPETS = {
    new SnippetDef(
        "def",
        "Snippet - Function",
        "def ${1:name}(${2:params}): ${3:Unit} = {\n    $0\n}"),
    new SnippetDef(
        "class",
        "Snippet - Class",
        "class ${1:Name}(${2:params}) {\n    $0\n}"),
    new SnippetDef(
        "object",
        "Snippet - Object",
        "object ${1:Name} {\n    $0\n}"),
    new SnippetDef(
        "case",
        "Snippet - Case Class",
        "case class ${1:Name}(${2:fields})"),
    new SnippetDef(
        "match",
        "Snippet - Match",
        "${1:value} match {\n    case ${2:pattern} => $0\n    case _ => \n}"),
  };

  public ScalaLanguage() {
    super(scalaConfig(), WORDS, SNIPPETS);
  }

  private static SimpleLangConfig scalaConfig() {
    var cfg = new SimpleLangConfig();
    cfg.lineComments = new String[] {"//"};
    cfg.blockComment = true;
    cfg.tripleStrings = true;
    return cfg;
  }
}
