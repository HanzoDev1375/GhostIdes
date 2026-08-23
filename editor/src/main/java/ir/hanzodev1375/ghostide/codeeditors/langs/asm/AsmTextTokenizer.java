/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.asm;

import io.github.rosemoe.sora.util.TrieTree;
import java.util.Locale;

public class AsmTextTokenizer {

  private static TrieTree<AsmTokens> instructions;

  private static TrieTree<AsmTokens> registers;

  private static TrieTree<AsmTokens> directives;

  static {
    doStaticInit();
  }

  public static TrieTree<AsmTokens> getTree() {
    return instructions;
  }

  private CharSequence source;
  private int bufferLen;
  public int offset;
  public int length;
  private AsmTokens currToken;

  public AsmTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    offset = 0;
    currToken = AsmTokens.WHITESPACE;
    this.bufferLen = source.length();
  }

  public void reset(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    this.bufferLen = src.length();
    init();
  }

  public CharSequence getTokenText() {
    return source.subSequence(offset, offset + length);
  }

  public int getTokenLength() {
    return length;
  }

  public AsmTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private AsmTokens nextTokenInternal() {
    offset += length;
    if (offset >= bufferLen) return AsmTokens.EOF;
    char ch = source.charAt(offset);
    length = 1;
    if (ch == '\n') return AsmTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return AsmTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return AsmTokens.WHITESPACE;
    }
    // ; and // line comments (NASM, ARM, C style)
    if (ch == ';') {
      return scanToLineEnd(AsmTokens.LINE_COMMENT);
    }
    if (ch == '/') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '/') {
        length++;
        return scanToLineEnd(AsmTokens.LINE_COMMENT);
      }
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '*') {
        length++;
        char pre = 0, cur = 0;
        boolean finished = false;
        while (offset + length < bufferLen) {
          pre = cur;
          cur = source.charAt(offset + length);
          if (pre == '*' && cur == '/') {
            length++;
            finished = true;
            break;
          }
          length++;
        }
        return finished
            ? AsmTokens.BLOCK_COMMENT_COMPLETE
            : AsmTokens.BLOCK_COMMENT_INCOMPLETE;
      }
      return AsmTokens.SLASH;
    }
    if (ch == '"') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '"') {
        if (source.charAt(offset + length) == '\\') {
          length++;
          if (offset + length < bufferLen) length++;
        } else {
          length++;
        }
      }
      if (offset + length < bufferLen) length++;
      return AsmTokens.STRING_LITERAL;
    }
    if (ch == '\'') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\'') {
        if (source.charAt(offset + length) == '\\') {
          length++;
          if (offset + length < bufferLen) length++;
        } else {
          length++;
        }
      }
      if (offset + length < bufferLen) length++;
      return AsmTokens.CHARACTER_LITERAL;
    }
    // .directive (GAS)
    if (ch == '.' && offset + 1 < bufferLen && isIdentifierStart(source.charAt(offset + 1))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      String text =
          source.subSequence(offset, offset + length).toString().toLowerCase(Locale.ROOT);
      AsmTokens known = directives.get(text, 0, text.length());
      return known != null ? known : AsmTokens.DIRECTIVE;
    }
    // %directive (NASM)
    if (ch == '%' && offset + 1 < bufferLen && isIdentifierStart(source.charAt(offset + 1))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      String text =
          source.subSequence(offset, offset + length).toString().toLowerCase(Locale.ROOT);
      AsmTokens known = directives.get(text, 0, text.length());
      return known != null ? known : AsmTokens.DIRECTIVE;
    }
    // $immediate or $register (AT&T)
    if (ch == '$'
        && offset + 1 < bufferLen
        && (isHexDigit(source.charAt(offset + 1))
            || isIdentifierStart(source.charAt(offset + 1)))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      return AsmTokens.INTEGER_LITERAL;
    }
    if (isDigit(ch)) {
      return scanNumber();
    }
    if (isIdentifierStart(ch)) {
      return scanIdentifier(ch);
    }
    switch (ch) {
      case '(':
        return AsmTokens.LPAREN;
      case ')':
        return AsmTokens.RPAREN;
      case '[':
        return AsmTokens.LBRACK;
      case ']':
        return AsmTokens.RBRACK;
      case ',':
        return AsmTokens.COMMA;
      case ':':
        return AsmTokens.COLON;
      case '+':
        return AsmTokens.PLUS;
      case '-':
        return AsmTokens.MINUS;
      case '*':
        return AsmTokens.STAR;
      case '%':
      case '@':
        return ch == '%' ? AsmTokens.PERCENT : AsmTokens.AT;
      case '^':
        return AsmTokens.CARET;
      case '~':
        return AsmTokens.TILDE;
      case '#':
        return AsmTokens.HASH;
      case '=':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return AsmTokens.EQ;
        }
        return AsmTokens.ASSIGN;
      case '!':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return AsmTokens.NOT_EQ;
        }
        return AsmTokens.NOT;
      case '<':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '<') {
          length++;
          return AsmTokens.SHIFT_LEFT;
        }
        return AsmTokens.LT;
      case '>':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '>') {
          length++;
          return AsmTokens.SHIFT_RIGHT;
        }
        return AsmTokens.GT;
      default:
        return AsmTokens.UNKNOWN;
    }
  }

  private AsmTokens scanToLineEnd(AsmTokens target) {
    while (offset + length < bufferLen && source.charAt(offset + length) != '\n') {
      length++;
    }
    return target;
  }

  private AsmTokens scanNumber() {
    boolean isFloat = false;
    if (offset + 1 < bufferLen && source.charAt(offset) == '0') {
      char next = source.charAt(offset + 1);
      if (next == 'x' || next == 'X') {
        length++;
        while (offset + length < bufferLen && isHexDigit(source.charAt(offset + length))) length++;
        return AsmTokens.INTEGER_LITERAL;
      }
      if (next == 'b' || next == 'B') {
        length++;
        while (offset + length < bufferLen
            && (source.charAt(offset + length) == '0' || source.charAt(offset + length) == '1'))
          length++;
        return AsmTokens.INTEGER_LITERAL;
      }
      if (next == 'o' || next == 'O') {
        length++;
        while (offset + length < bufferLen
            && source.charAt(offset + length) >= '0'
            && source.charAt(offset + length) <= '7') length++;
        return AsmTokens.INTEGER_LITERAL;
      }
    }
    while (offset + length < bufferLen
        && (isHexDigit(source.charAt(offset + length)) || source.charAt(offset + length) == '_')) {
      length++;
    }
    if (offset + length < bufferLen
        && (source.charAt(offset + length) == 'h' || source.charAt(offset + length) == 'H')) {
      length++;
      return AsmTokens.INTEGER_LITERAL;
    }
    if (offset + length < bufferLen && source.charAt(offset + length) == '.') {
      isFloat = true;
      length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    if (offset + length < bufferLen
        && (source.charAt(offset + length) == 'e' || source.charAt(offset + length) == 'E')
        && isFloat) {
      length++;
      if (offset + length < bufferLen
          && (source.charAt(offset + length) == '+' || source.charAt(offset + length) == '-'))
        length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    return isFloat ? AsmTokens.FLOATING_LITERAL : AsmTokens.INTEGER_LITERAL;
  }

  private AsmTokens scanIdentifier(char first) {
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      length++;
    }
    String text =
        source.subSequence(offset, offset + length).toString().toLowerCase(Locale.ROOT);
    AsmTokens token = instructions.get(text, 0, text.length());
    if (token != null) return token;
    token = registers.get(text, 0, text.length());
    if (token != null) return token;
    token = directives.get(text, 0, text.length());
    if (token != null) return token;
    return AsmTokens.IDENTIFIER;
  }

  private static boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\f';
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isHexDigit(char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static boolean isIdentifierStart(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '.';
  }

  private static boolean isIdentifierPart(char c) {
    return isIdentifierStart(c) || isDigit(c) || c == '$';
  }

  private static void putAll(TrieTree<AsmTokens> tree, String[] words, AsmTokens token) {
    for (String word : words) {
      tree.put(word.toLowerCase(Locale.ROOT), token);
    }
  }

  private static void doStaticInit() {
    instructions = new TrieTree<>();
    putAll(
        instructions,
        new String[] {
          "aaa", "aad", "aam", "aas", "adc", "adcs", "add", "adds", "adr", "adrp", "and",
          "ands", "arpl", "asr", "asrv", "b", "bic", "bkpt", "bl", "blx", "blr", "bound",
          "br", "brk", "bsf", "bsr", "bswap", "bt", "btc", "btr", "bts", "bx", "bxj", "cbnz",
          "cbz", "cdq", "cdqe", "clac", "clc", "clrex", "cli", "clts", "clz", "cmc", "cmn",
          "cmova", "cmovae", "cmovb", "cmovbe", "cmovc", "cmove", "cmovg", "cmovge", "cmovl",
          "cmovle", "cmovna", "cmovnae", "cmovnb", "cmovnbe", "cmovnc", "cmovne", "cmovng",
          "cmovnge", "cmovnl", "cmovnle", "cmovno", "cmovnp", "cmovns", "cmovnz", "cmovo",
          "cmovp", "cmovpe", "cmovpo", "cmovs", "cmovz", "cmp", "cmpsb", "cmpsd", "cmpsq",
          "cmpsw", "cmpxchg", "cmpxchg8b", "cpuid", "cqo", "csel", "csinc", "csinv", "csneg",
          "cwd", "cwde", "daa", "das", "dbg", "dec", "div", "dmb", "dsb", "emms", "enter",
          "eon", "eor", "eors", "extr", "hlt", "idiv", "imul", "in", "inc", "insb", "insd",
          "insw", "int", "int1", "int3", "into", "invd", "invlpg", "iret", "iretd", "isb",
          "ja", "jae", "jb", "jbe", "jc", "jcxz", "je", "jecxz", "jg", "jge", "jl", "jle",
          "jmp", "jna", "jnae", "jnb", "jnbe", "jnc", "jne", "jng", "jnge", "jnl", "jnle",
          "jno", "jnp", "jns", "jnz", "jo", "jp", "jpe", "jpo", "js", "jz", "lahf", "lar",
          "ldar", "ldarb", "ldaxp", "ldaxr", "lcall", "ldc", "ldm", "ldmda", "ldmdb", "ldmea",
          "ldmfa", "ldmia",           "ldmib", "ldp", "ldr", "ldrb", "ldrbt", "ldrd", "ldrex", "ldrexb", "ldrexd",
          "ldrexh", "ldrh", "ldrsb", "ldrsh", "ldrsw", "ldrt",
          "lds", "lea", "leave", "les", "lfence", "lfs", "lgdt", "lidt", "lldt", "lmsw",
          "lock", "lodsb", "lodsd", "lodsq", "lodsw", "loop", "loope", "loopne", "loopnz",
          "loopz", "lsl", "lss", "ltr", "lsr", "lslv", "lsrv", "madd", "mfence", "mla",
          "mls", "mneg", "mov", "movabs", "movbe", "movd", "movi", "movk", "movn", "movq",
          "movs", "movsb", "movsd", "movsq", "movsw", "movsx", "movsxd", "movt", "movw",
          "movz", "movzx", "msub", "mul", "muls", "mull", "mvn", "mvns", "neg", "negs",
          "ngc", "ngcs", "nop", "not", "orn", "orr", "out", "outsb", "outsd", "outsw",
          "pause", "pkhbt", "pkhtb", "pop", "popa", "popad", "popcnt", "popf", "popfd",
          "popfq", "prfm", "push", "pusha", "pushad", "pushf", "pushfd", "pushfq", "qadd",
          "qsub", "rbit", "rc", "rcl", "rcr", "rdmsr", "rdpmc", "rdtsc", "rep", "repe",
          "repne", "repnz", "repz", "ret", "retf", "retn", "rev", "rev16", "rev32", "revsh",
          "rfe", "rol", "ror", "rorv", "rors", "rorx", "rrx", "rsb", "rsbs", "rsm", "sahf",
          "sal", "sar", "sarv", "sbb", "scasb", "scasd", "scasw", "sbfx", "sdiv", "sel",
          "seta", "setae", "setb", "setbe", "setc", "sete", "setg", "setge", "setl", "setle",
          "setna", "setnae", "setnb", "setnbe", "setnc", "setne", "setng", "setnge", "setnl",
          "setnle", "setno", "setnp", "setns", "setnz", "seto", "setp", "setpe", "setpo",
          "sets", "setz", "sev", "sfence", "sgdt", "shl", "shld", "shr", "shrd", "sidt",
          "sldt", "smsw", "smc", "smaddl", "smnegl", "smsubl", "smulh", "smull", "smlal",
          "ssat", "stac", "stc", "std", "sti", "stlr", "stm", "stmda", "stmdb", "stmea",
          "stmfa", "stmib", "stp", "str", "strb", "strbt", "strd", "strex", "strexb",
          "strexd", "strh", "strt", "stosb", "stosd", "stosq", "stosw", "stur", "sturb",
          "sturh", "stxp", "stxr", "stlxp", "stlxr", "sub", "subs", "svc", "swapgs",
          "syscall", "sysenter", "sysexit", "sysret", "sxtab", "sxtab16", "sxtah", "sxtb",
          "sxth", "sxtw", "tbz", "tbnz", "teq", "test", "tfence", "tpause", "ts", "tst",
          "tzcnt", "ud2", "udiv", "umaddl", "umnegl", "umsubl", "umulh", "umull", "umlal",
          "usat", "verr", "verw", "wait", "wbinvd", "wfe", "wfi", "wrmsr", "xadd", "xchg",
          "xlat", "xlatb", "xor"
        },
        AsmTokens.INSTRUCTION);
    registers = new TrieTree<>();
    var regs = new StringBuilder();
    for (int i = 0; i <= 15; i++) regs.append('r').append(i).append(' ');
    for (int i = 0; i <= 30; i++) {
      regs.append('x').append(i).append(' ');
      regs.append('w').append(i).append(' ');
    }
    for (int i = 0; i <= 31; i++) {
      regs.append('v').append(i).append(' ');
      regs.append('q').append(i).append(' ');
      regs.append('d').append(i).append(' ');
      regs.append('s').append(i).append(' ');
      regs.append('b').append(i).append(' ');
      regs.append('h').append(i).append(' ');
    }
    for (int i = 8; i <= 15; i++) {
      regs.append('r').append(i).append('d').append(' ');
      regs.append('r').append(i).append('w').append(' ');
      regs.append('r').append(i).append('b').append(' ');
    }
    for (int i = 0; i <= 7; i++) {
      regs.append("mm").append(i).append(' ');
      regs.append("st").append(i).append(' ');
      regs.append("cr").append(i).append(' ');
      regs.append("dr").append(i).append(' ');
      regs.append("k").append(i).append(' ');
    }
    putAll(
        registers,
        (regs.toString().trim()
                + " ax bx cx dx si di bp sp ip eax ebx ecx edx esi edi ebp esp eip al ah bl bh "
                + "cl ch dl dh sil dil bpl spl rax rbx rcx rdx rsi rdi rbp rsp rip cs ds es fs gs ss "
                + "pc lr fp sb sl apsr cpsr spsr fpscr fpsr fpcr nzcv xzr wzr elr sp_el0 currentel daif")
            .split("\\s+"),
        AsmTokens.REGISTER);
    directives = new TrieTree<>();
    putAll(
        directives,
        new String[] {
          ".text", ".data", ".bss", ".rodata", ".section", ".global", ".globl", ".extern",
          ".align", ".balign", ".p2align", ".ascii", ".asciz", ".string", ".byte", ".word",
          ".long", ".quad", ".octa", ".short", ".half", ".float", ".double", ".single",
          ".equ", ".set", ".org", ".space", ".skip", ".zero", ".type", ".size", ".func",
          ".endfunc", ".macro", ".endm", ".include", ".incbin", ".if", ".else", ".elseif",
          ".endif", ".ifdef", ".ifndef", ".file", ".line", ".local", ".comm", ".lcomm",
          ".weak", ".protected", ".hidden", ".internal", ".previous", ".popsection",
          ".pushsection", ".rept", ".endr", ".irp", ".irpc", ".err", ".warning", ".message",
          ".title", ".ident", ".arch", ".arm", ".thumb", ".thumb_func", ".thumb_set",
          ".code", ".syntax", ".cpu", ".fpu", ".eabi_attribute", ".fnstart", ".fnend",
          ".personality", ".handlerdata", ".ltorg", ".pool", ".req", ".unreq", ".end",
          "%define", "%include", "%macro", "%endmacro", "%if", "%else", "%endif", "%elif",
          "%assign", "%strlen", "%substr", "%rep", "%endrep", "%exitrep", "%undef",
          "%error", "%warning", "%line", "%local", "%push", "%pop", "%repl",
          "section", "segments?", "global", "extern", "bits", "align", "alignb", "common",
          "absolute", "cpu", "float", "default", "org", "db", "dw", "dd", "dq", "dt", "do",
          "dy", "dz", "resb", "resw", "resd", "resq", "rest", "reso", "resy", "resz",
          "equ", "times", "incbin", "use16", "use32", "use64", "export", "import", "group",
          "unsafe", "nosplit", "strict", "wrapper"
        },
        AsmTokens.DIRECTIVE);
  }
}
