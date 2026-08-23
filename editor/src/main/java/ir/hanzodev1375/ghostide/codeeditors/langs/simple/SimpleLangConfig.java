/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.simple;

import io.github.rosemoe.sora.util.TrieTree;

public class SimpleLangConfig {

  public final TrieTree<SimpleTokens> keywords = new TrieTree<>();

  public final TrieTree<SimpleTokens> types = new TrieTree<>();

  public final TrieTree<SimpleTokens> builtins = new TrieTree<>();

  /** Line comment markers, e.g. "//" , "#" , "--" . Empty means disabled. */
  public String[] lineComments = {};

  /** Whether slash-star block comments are enabled. */
  public boolean blockComment = false;

  /** Whether triple-quoted (""") multi-line strings are enabled. */
  public boolean tripleStrings = false;

  /** Whether @ident annotations/macros/attributes are highlighted. */
  public boolean annotations = false;

  /** Whether #ident directives are highlighted (# is not a comment then). */
  public boolean hashDirectives = false;

  /** Whether $var @var %var sigil variables are scanned as identifiers. */
  public boolean sigilVars = false;

  public void keyword(String[] words) {
    for (String word : words) keywords.put(word, SimpleTokens.KEYWORD);
  }

  public void type(String[] words) {
    for (String word : words) types.put(word, SimpleTokens.TYPE_KEYWORD);
  }

  public void builtin(String[] words) {
    for (String word : words) builtins.put(word, SimpleTokens.BUILTIN);
  }
}
