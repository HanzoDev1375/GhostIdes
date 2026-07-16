package ir.hanzodev1375.ghostide.runer;

import android.content.Context;
import android.content.Intent;
import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import java.io.File;

public class CodeRuner {
  private Context context;

  public CodeRuner(Context context) {
    this.context = context;
  }

  public void bindof(String path) {
    if (path.endsWith(".c")) {
      c(path);
    } else if (path.endsWith(".cpp")
        || path.endsWith(".h")
        || path.endsWith(".hpp")
        || path.endsWith(".cc")) {
      cpp(path);
    } else if (path.endsWith(".py")) {
      python(path);
    } else if (path.endsWith(".php")) {
      php(path);
    } else if (path.endsWith(".go")) {
      go(path);
    } else if (path.endsWith(".js")) {
      node(path);
    } else if (path.endsWith(".ts")) {
      typescript(path);
    } else if (path.endsWith(".lua")) {
      lua(path);
    } else if (path.endsWith(".java")) {
      java(path);
    }
  }

  public void python(String path) {
    String command =
        "clear; if ! command -v python3 >/dev/null 2>&1; then apt update && apt install python3 -y; fi; python3 "
            + path;
    Intent i = new Intent(context, TerminalActivity.class);
    i.setClass(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void php(String path) {
    String command =
        "clear; if ! command -v php >/dev/null 2>&1; then apt update && apt install php -y; fi; php "
            + path;
    Intent i = new Intent(context, TerminalActivity.class);
    i.setClass(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void c(String path) {
    String exe = new File(path).getName();
    exe = exe.substring(0, exe.lastIndexOf('.'));

    String command =
        "clear; "
            + "if ! command -v gcc >/dev/null 2>&1; then apt update && apt install gcc -y; fi; "
            + "gcc \""
            + path
            + "\" -o \""
            + exe
            + "\" && ./"
            + exe;

    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void cpp(String path) {
    String exe = new File(path).getName();
    exe = exe.substring(0, exe.lastIndexOf('.'));

    String command =
        "clear; "
            + "if ! command -v g++ >/dev/null 2>&1; then apt update && apt install g++ -y; fi; "
            + "g++ \""
            + path
            + "\" -o \""
            + exe
            + "\" && ./"
            + exe;

    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void go(String path) {
    String command =
        "clear; "
            + "if ! command -v go >/dev/null 2>&1; then apt update && apt install golang-go -y; fi; "
            + "go run \""
            + path
            + "\"";

    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void node(String path) {
    String command =
        "clear; "
            + "if ! command -v node >/dev/null 2>&1; then apt update && apt install nodejs npm -y; fi; "
            + "node \""
            + path
            + "\"";

    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void typescript(String path) {
    String command =
        "clear; "
            + "if ! command -v ts-node >/dev/null 2>&1; then apt update && apt install nodejs npm -y && npm install -g typescript ts-node; fi; "
            + "ts-node \""
            + path
            + "\"";

    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void lua(String path) {
    String command =
        "clear; "
            + "if ! command -v lua >/dev/null 2>&1; then apt update && apt install lua5.4 -y; fi; "
            + "lua \""
            + path
            + "\"";

    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }

  public void java(String path) {
    String className = new File(path).getName();
    className = className.substring(0, className.lastIndexOf('.'));

    String command =
        "clear; "
            + "if ! command -v javac >/dev/null 2>&1; then apt update && apt install default-jdk -y; fi; "
            + "java \""
            + path
            + "\"";

    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    context.startActivity(i);
  }
}
