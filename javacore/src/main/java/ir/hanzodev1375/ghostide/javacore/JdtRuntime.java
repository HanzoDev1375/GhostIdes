package ir.hanzodev1375.ghostide.javacore;

/**
 * Process-wide funnel for JDT runtime issues. Kept free of any Android type so the engine and the
 * language server can report failures without knowing what the app does with them.
 */
public final class JdtRuntime {

  private JdtRuntime() {}

  private static volatile JdtErrorListener listener;

  /** Installs the app-side reporter (logcat + toast). Only one is used at a time. */
  public static void setErrorListener(JdtErrorListener listener) {
    JdtRuntime.listener = listener;
  }

  public static void report(String stage, String message, Throwable error, boolean critical) {
    JdtErrorListener l = listener;
    if (l == null) return;
    try {
      l.onReport(stage, message, error, critical);
    } catch (Throwable ignored) {
      // the reporter itself must never crash the server.
    }
  }
}