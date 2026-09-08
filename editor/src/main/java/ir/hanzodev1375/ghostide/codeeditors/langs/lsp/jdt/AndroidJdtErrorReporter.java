package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.jdt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import ir.hanzodev1375.ghostide.javacore.JdtErrorListener;

/**
 * App-side sink for in-process JDT runtime failures. Every issue is written to logcat under a
 * stable tag; critical ones (server start, classpath) also surface as a Toast so the failure is
 * visible while developing without digging through logs.
 */
public final class AndroidJdtErrorReporter implements JdtErrorListener {

  private static final String TAG = "GhostJdt";

  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private long lastToastAt;

  public AndroidJdtErrorReporter(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public void onReport(String stage, String message, Throwable error, boolean critical) {
    String text = "[" + stage + "] " + (message != null ? message : describe(error));
    Log.e(TAG, text, error);
    if (critical) {
      toast(text);
    } else {
      long now = System.currentTimeMillis();
      if (now - lastToastAt > 20_000) {
        lastToastAt = now;
        toast(text);
      }
    }
  }

  private static String describe(Throwable t) {
    return t == null ? "unknown error" : t.getClass().getSimpleName() + ": " + t.getMessage();
  }

  private void toast(String text) {
    final String s = text;
    mainHandler.post(
        () -> {
          try {
            Toast.makeText(context, s, Toast.LENGTH_LONG).show();
          } catch (Throwable ignored) {
            // never crash the caller from a toast
          }
        });
  }
}
