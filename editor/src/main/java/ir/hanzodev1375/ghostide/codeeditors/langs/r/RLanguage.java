/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.r;

import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLangConfig;
import ir.hanzodev1375.ghostide.codeeditors.langs.simple.SimpleLanguage;

public class RLanguage extends SimpleLanguage {

  private static final String[] WORDS = {
    "if",
    "else",
    "repeat",
    "while",
    "function",
    "for",
    "in",
    "next",
    "break",
    "TRUE",
    "FALSE",
    "NULL",
    "Inf",
    "NaN",
    "NA",
    "NA_integer_",
    "NA_real_",
    "NA_character_",
    "NA_complex_",
    "library",
    "require",
    "attach",
    "detach",
    "source",
    "with",
    "within",
    "assign",
    "rm",
    "ls",
    "exists",
    "return",
    "character",
    "numeric",
    "integer",
    "logical",
    "complex",
    "list",
    "data.frame",
    "matrix",
    "vector",
    "factor",
    "environment",
    "formula",
    "expression",
    "call",
    "c",
    "cat",
    "paste",
    "paste0",
    "print",
    "summary",
    "str",
    "head",
    "tail",
    "length",
    "names",
    "colnames",
    "rownames",
    "nrow",
    "ncol",
    "dim",
    "mean",
    "median",
    "sd",
    "var",
    "sum",
    "min",
    "max",
    "range",
    "unique",
    "table",
    "which",
    "order",
    "sort",
    "apply",
    "lapply",
    "sapply",
    "tapply",
    "mapply",
    "aggregate",
    "merge",
    "subset",
    "transform",
    "plot",
    "hist",
    "boxplot",
    "barplot",
    "lm",
    "glm",
    "predict",
    "fitted",
    "residuals",
    "abline",
    "lines",
    "points",
    "text",
    "par",
    "read.csv",
    "write.csv",
    "read.table",
    "data.frame",
    "as.character",
    "as.numeric",
    "as.integer",
    "as.logical",
    "as.factor",
    "is.na",
    "na.omit",
    "complete.cases",
    "sample",
    "set.seed",
    "rnorm",
    "runif",
    "dnorm",
    "pnorm",
    "qnorm",
    "rbinom"
  };

  private static final SnippetDef[] SNIPPETS = {
    new SnippetDef("fun", "Snippet - Function", "${1:name} <- function(${2:args}) {\n    $0\n}"),
    new SnippetDef("ife", "Snippet - If/Else", "if (${1:condition}) {\n    $0\n} else {\n    \n}"),
    new SnippetDef("for", "Snippet - For Loop", "for (${1:i} in ${2:seq}) {\n    $0\n}"),
  };

  public RLanguage() {
    super(rConfig(), WORDS, SNIPPETS);
  }

  private static SimpleLangConfig rConfig() {
    var cfg = new SimpleLangConfig();
    cfg.lineComments = new String[] {"#"};
    return cfg;
  }
}
