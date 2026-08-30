package ir.hanzodev1375.ghostide.adapters;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import ir.hanzodev1375.ghostide.GhostIdeAppLoader;
import ir.hanzodev1375.ghostide.ide.ui.api.UiFeedbackHost;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
/**
 * App-wide {@link UiFeedbackHost}. Tracks the resumed activity through lifecycle callbacks so
 * dialogs always attach to something visible; toasts only need the application context. Dialog
 * callbacks degrade gracefully (null/false) when no activity is resumed.
 */
public final class UiFeedbackHostImpl implements UiFeedbackHost, Application.ActivityLifecycleCallbacks {

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private Activity resumedActivity;

  @Override
  public void toast(String message, boolean longDuration) {
    runOnMain(
        () ->
            Toast.makeText(
                    GhostIdeAppLoader.getContext(),
                    message,
                    longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
                .show());
  }

  @Override
  public void promptInput(String title, @Nullable String prefill, InputCallback callback) {
    runOnMain(
        () -> {
          Activity activity = resumedActivity;
          if (activity == null || activity.isFinishing()) {
            callback.onInput(null);
            return;
          }
          EditText input = new EditText(activity);
          if (prefill != null) input.setText(prefill);
          int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
          input.setPadding(pad, pad, pad, pad);
          new DialogCompat(activity)
              .setTitle(title)
              .setView(input)
              .setPositiveButton(
                  android.R.string.ok, (d, w) -> callback.onInput(input.getText().toString()))
              .setNegativeButton(android.R.string.cancel, (d, w) -> callback.onInput(null))
              .setOnCancelListener(d -> callback.onInput(null))
              .show();
        });
  }

  @Override
  public void confirm(String title, String message, ConfirmCallback callback) {
    runOnMain(
        () -> {
          Activity activity = resumedActivity;
          if (activity == null || activity.isFinishing()) {
            callback.onResult(false);
            return;
          }
          new DialogCompat(activity)
              .setTitle(title)
              .setMessage(message)
              .setPositiveButton(android.R.string.ok, (d, w) -> callback.onResult(true))
              .setNegativeButton(android.R.string.cancel, (d, w) -> callback.onResult(false))
              .setOnCancelListener(d -> callback.onResult(false))
              .show();
        });
  }

  private void runOnMain(Runnable action) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      action.run();
    } else {
      mainHandler.post(action);
    }
  }

  @Override
  public void onActivityResumed(Activity activity) {
    resumedActivity = activity;
  }

  @Override
  public void onActivityPaused(Activity activity) {
    if (resumedActivity == activity) resumedActivity = null;
  }

  @Override
  public void onActivityCreated(Activity a, Bundle savedInstanceState) {}

  @Override
  public void onActivityStarted(Activity a) {}

  @Override
  public void onActivityStopped(Activity a) {}

  @Override
  public void onActivitySaveInstanceState(Activity a, Bundle outState) {}

  @Override
  public void onActivityDestroyed(Activity a) {}
}
