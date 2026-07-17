package ir.hanzodev1375.ghostide.runer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import ir.hanzodev1375.ghostide.terminal.sheet.TerminalBottomSheetFragment;
import java.io.File;

public class CodeRuner {
  private Context context;

  public CodeRuner(Context context) {
    this.context = context;
  }

  public void bindof(String path, boolean asBottomSheet) {
    String command = buildCommand(path);
    if (command == null) return;

    if (asBottomSheet) {
      runInBottomSheet(command);
    } else {
      runInActivity(command);
    }
  }

  public void runInActivity(String command) {
    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    if (!(context instanceof Activity)) {
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    context.startActivity(i);
  }

   void runInBottomSheet(String command) {
    FragmentManager fm = resolveFragmentManager();
    if (fm == null) {
      runInActivity(command);
      return;
    }
    TerminalBottomSheetFragment.newInstance(command, null).show(fm, "terminal_sheet");
  }

  private FragmentManager resolveFragmentManager() {
    if (context instanceof FragmentActivity) {
      return ((FragmentActivity) context).getSupportFragmentManager();
    }
    return null;
  }

  private String buildCommand(String path) {
    if (path.endsWith(".c")) {
      return c(path);
    } else if (path.endsWith(".cpp")
        || path.endsWith(".h")
        || path.endsWith(".hpp")
        || path.endsWith(".cc")) {
      return cpp(path);
    } else if (path.endsWith(".py")) {
      return python(path);
    } else if (path.endsWith(".php")) {
      return php(path);
    } else if (path.endsWith(".go")) {
      return go(path);
    } else if (path.endsWith(".js")) {
      return node(path);
    } else if (path.endsWith(".ts")) {
      return typescript(path);
    } else if (path.endsWith(".lua")) {
      return lua(path);
    } else if (path.endsWith(".java")) {
      return java(path);
    }
    return null;
  }

  private String python(String path) {
    return "clear; if ! command -v python3 >/dev/null 2>&1; then apt update && apt install python3 -y; fi; python3 "
        + path;
  }

  private String php(String path) {
    return "clear; if ! command -v php >/dev/null 2>&1; then apt update && apt install php -y; fi; php "
        + path;
  }

  private String c(String path) {
    String exe = new File(path).getName();
    exe = exe.substring(0, exe.lastIndexOf('.'));

    return "clear; "
        + "if ! command -v gcc >/dev/null 2>&1; then apt update && apt install gcc -y; fi; "
        + "gcc \""
        + path
        + "\" -o \""
        + exe
        + "\" && ./"
        + exe;
  }

  private String cpp(String path) {
    String exe = new File(path).getName();
    exe = exe.substring(0, exe.lastIndexOf('.'));

    return "clear; "
        + "if ! command -v g++ >/dev/null 2>&1; then apt update && apt install g++ -y; fi; "
        + "g++ \""
        + path
        + "\" -o \""
        + exe
        + "\" && ./"
        + exe;
  }

  private String go(String path) {
    return "clear; "
        + "if ! command -v go >/dev/null 2>&1; then apt update && apt install golang-go -y; fi; "
        + "go run \""
        + path
        + "\"";
  }

  private String node(String path) {
    return "clear; "
        + "if ! command -v node >/dev/null 2>&1; then apt update && apt install nodejs npm -y; fi; "
        + "node \""
        + path
        + "\"";
  }

  private String typescript(String path) {
    return "clear; "
        + "if ! command -v ts-node >/dev/null 2>&1; then apt update && apt install nodejs npm -y && npm install -g typescript ts-node; fi; "
        + "ts-node \""
        + path
        + "\"";
  }

  private String lua(String path) {
    return "clear; "
        + "if ! command -v lua >/dev/null 2>&1; then apt update && apt install lua5.4 -y; fi; "
        + "lua \""
        + path
        + "\"";
  }

  private String java(String path) {
    String className = new File(path).getName();
    className = className.substring(0, className.lastIndexOf('.'));

    return "clear; "
        + "if ! command -v javac >/dev/null 2>&1; then apt update && apt install default-jdk -y; fi; "
        + "java \""
        + path
        + "\"";
  }
}
