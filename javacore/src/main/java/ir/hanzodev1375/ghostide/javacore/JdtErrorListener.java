package ir.hanzodev1375.ghostide.javacore;

/**
 * Receives runtime failures from the in-process JDT engine/server. The editor module wires this to
 * logcat (via {@code android.util.Log}) and, for critical ones, a Toast — so problems that only
 * appear at runtime on-device become visible instead of silently swallowed.
 */
public interface JdtErrorListener {

  /**
   * @param stage    which subsystem failed ("parse", "completion", "definition", "classpath"...)
   * @param message  an optional human-readable message (may be null)
   * @param error    the underlying exception (may be null)
   * @param critical true for failures that make the whole server unusable (boot, start, classpath)
   */
  void onReport(String stage, String message, Throwable error, boolean critical);
}