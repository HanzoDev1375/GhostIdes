package ir.hanzodev1375.ghostide.ide.ui.api;

import androidx.annotation.Nullable;

/**
 * Published as a service by the host under {@link IdeHostServices#UI_FEEDBACK}. Lets a plugin show
 * user-visible feedback (toasts, input dialogs, confirmations) without owning any Activity.
 *
 * <p>All methods are safe to call from any thread — the host hops to the main thread internally.
 * Dialog callbacks fire with {@code null}/{@code false} when no Activity is available to host the
 * dialog, so always handle those cases.
 */
public interface UiFeedbackHost {

  /** Shows a short toast. */
  default void toast(String message) {
    toast(message, false);
  }

  void toast(String message, boolean longDuration);

  /**
   * Asks the user for a single line of text.
   *
   * @param prefill initial text, may be {@code null}
   * @param callback invoked on the main thread with the entered text, or {@code null} if the user
   *     cancelled or no dialog could be shown
   */
  void promptInput(String title, @Nullable String prefill, InputCallback callback);

  /**
   * Asks the user to confirm an action.
   *
   * @param callback invoked on the main thread with {@code true} only when the user confirmed
   */
  void confirm(String title, String message, ConfirmCallback callback);

  /** Receives the result of {@link #promptInput(String, String, InputCallback)}. */
  @FunctionalInterface
  interface InputCallback {
    void onInput(@Nullable String text);
  }

  /** Receives the result of {@link #confirm(String, String, ConfirmCallback)}. */
  @FunctionalInterface
  interface ConfirmCallback {
    void onResult(boolean confirmed);
  }
}
