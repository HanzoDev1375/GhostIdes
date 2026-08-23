/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.elixir;

import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLangConfig;
import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLanguage;

public class ElixirLanguage extends SimpleLanguage {

  private static final String[] WORDS = {
    "def", "defp", "defmodule", "defmacro", "defmacrop", "defstruct", "defprotocol",
    "defimpl", "defdelegate", "defexception", "defoverridable", "defguard",
    "defguardp", "defcallback", "import", "alias", "require", "use", "quote",
    "unquote", "unquote_splicing", "with", "case", "cond", "if", "unless", "for",
    "while", "receive", "try", "catch", "rescue", "after", "raise", "throw", "fn",
    "end", "and", "or", "not", "in", "when", "true", "false", "nil", "do", "else",
    "inspect", "hd", "tl", "elem", "put_elem", "length", "map_size", "tuple_size",
    "round", "trunc", "abs", "div", "rem", "spawn", "spawn_link", "send", "self",
    "apply", "exit", "link", "unlink", "monitor", "demonitor", "process_flag",
    "put_env", "get_env", "make_ref", "is_atom", "is_binary", "is_boolean", "is_float",
    "is_function", "is_integer", "is_list", "is_map", "is_nil", "is_number", "is_pid",
    "is_port", "is_reference", "is_tuple", "is_struct", "match?", "byte_size",
    "bit_size", "function_exported?", "macro_exported?", "struct", "put_struct"
  };

  private static final SnippetDef[] SNIPPETS = {
    new SnippetDef(
        "def",
        "Snippet - Public Function",
        "def ${1:name}(${2:args}) do\n    $0\nend"),
    new SnippetDef(
        "defp",
        "Snippet - Private Function",
        "defp ${1:name}(${2:args}) do\n    $0\nend"),
    new SnippetDef(
        "defmodule",
        "Snippet - Module",
        "defmodule ${1:Name} do\n    $0\nend"),
    new SnippetDef(
        "case",
        "Snippet - Case",
        "case ${1:value} do\n    ${2:pattern} -> $0\n    _ -> \nend"),
  };

  public ElixirLanguage() {
    super(elixirConfig(), WORDS, SNIPPETS);
  }

  private static SimpleLangConfig elixirConfig() {
    var cfg = new SimpleLangConfig();
    cfg.lineComments = new String[] {"#"};
    cfg.tripleStrings = true;
    return cfg;
  }
}
