package ir.hanzodev1375.ghostide.terminal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.android.material.textfield.TextInputLayout;
import com.termux.terminal.TerminalSession;
import java.nio.charset.StandardCharsets;

/**
 * پنل ورودی پایین ترمینال که مثل ترموکس با سویپ باز/بسته میشه:
 *
 * <ul>
 *   <li>سویپ به چپ روی ترمینال یا دستگیره → ورودی متن (EditText) نشان داده میشه.</li>
 *   <li>سویپ به راست روی ترمینال یا دستگیره → دکمه‌های میانبر (ESC/TAB/...) نشان داده میشن.</li>
 *   <li>سویپ به بالا/پایین روی دستگیره یا تپ روش → باز/بسته شدن.</li>
 *   <li>وقتی کیبورد بسته بشه، پنل بعد از یه مکث کوتاه خودش جمع میشه.</li>
 * </ul>
 *
 * برای اینکه هم {@code TerminalActivity} و هم {@code TerminalBottomSheetFragment} (که هرکدوم لِی‌اوت
 * مخصوص خودشون رو دارن) بتونن باهاش کار کنن، این کلاس یاد همون ویوها به‌صورت صریح می‌کنه و به هیچ
 * بایندینگ خاصی وابسته نیست.
 */
public class TerminalInputDock {

  public interface SessionProvider {
    TerminalSession currentSession();
  }

  private static final int ANIM_DURATION = 300;
  private static final int FLING_VELOCITY_THRESHOLD = 600;
  private static final int KEYBOARD_COLLAPSE_DELAY = 350;

  /** منحنی استاندارد متریال (fast-out / slow-in) برای حس نرم‌تر. */
  private static final TimeInterpolator EASING = new PathInterpolator(0.2f, 0f, 0f, 1f);

  private final View inputDock;
  private final View dockPages;
  private final View extraKeysScroll;
  private final View commandInputRow;
  private final EditText commandInput;
  private final TextInputLayout commandInputLayout;
  private final View dragHandle;
  private final View handleChevron;
  private final SessionProvider sessionProvider;

  private final ValueAnimator heightAnimator = new ValueAnimator();
  private final ValueAnimator pageAnimator = new ValueAnimator();
  private final GestureDetector handleDetector;

  private final Runnable collapseRunnable = this::collapse;

  private int contentHeight;
  private float progress;
  private boolean expanded;
  private boolean inputPageShowing;
  private boolean dragging;
  private boolean dragged;
  private boolean flingHandled;

  public TerminalInputDock(
      View inputDock,
      View dockPages,
      View extraKeysScroll,
      View commandInputRow,
      EditText commandInput,
      TextInputLayout commandInputLayout,
      View dragHandle,
      View handleChevron,
      SessionProvider sessionProvider) {
    this.inputDock = inputDock;
    this.dockPages = dockPages;
    this.extraKeysScroll = extraKeysScroll;
    this.commandInputRow = commandInputRow;
    this.commandInput = commandInput;
    this.commandInputLayout = commandInputLayout;
    this.dragHandle = dragHandle;
    this.handleChevron = handleChevron;
    this.sessionProvider = sessionProvider;
    this.handleDetector = createHandleDetector();
    configureInputField();
    setPagesToRestingState();

    ViewGroup.LayoutParams params = dockPages.getLayoutParams();
    params.height = 0;
    dockPages.setLayoutParams(params);
    dockPages.setAlpha(0f);
    dragHandle.setAlpha(0.6f);
    dockPages.post(() -> contentHeight = measureContentHeight());
  }

  /** ژست‌های دستگیره رو وصل می‌کنه. سویپ روی خودِ ترمینال هم خودش هندل می‌شه (دسته‌ی سوییپ روی‌توش). */
  public void attach() {
    dragHandle.setOnTouchListener(
        (v, event) -> {
          handleDetector.onTouchEvent(event);
          int action = event.getActionMasked();
          if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            settleDrag();
          }
          return true;
        });
  }

  /** تغییرات کیبورد رو از روی تغییر ارتفاع دیدِ root تشخیص می‌ده. */
  public void attachKeyboardWatcher(View rootView) {
    rootView
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            () -> {
              Rect visibleFrame = new Rect();
              rootView.getWindowVisibleDisplayFrame(visibleFrame);
              int screenHeight = rootView.getRootView().getHeight();
              int hiddenArea = screenHeight - visibleFrame.bottom;
              onKeyboardVisibilityChanged(hiddenArea > (int) (screenHeight * 0.15f));
            });
  }

  /** سویپ به چپ → صفحه‌ی ورودی متن. */
  public void showInputPage() {
    cancelPendingCollapse();
    if (expanded) {
      if (!inputPageShowing) animatePageSwitch(true);
    } else {
      inputPageShowing = true;
      expand();
    }
  }

  /** سویپ به راست → صفحه‌ی دکمه‌های میانبر. */
  public void showButtonsPage() {
    cancelPendingCollapse();
    if (expanded) {
      if (inputPageShowing) animatePageSwitch(false);
    } else {
      inputPageShowing = false;
      expand();
    }
  }

  public void collapse() {
    cancelPendingCollapse();
    heightAnimator.cancel();
    pageAnimator.cancel();
    expanded = false;
    commandInput.clearFocus();
    setPagesToRestingState();
    animateHeight(progress, 0f);
  }

  public void toggle() {
    cancelPendingCollapse();
    if (expanded) {
      collapse();
    } else {
      expand();
    }
  }

  public boolean isExpanded() {
    return expanded;
  }

  private void configureInputField() {
    commandInput.setOnEditorActionListener(
        (v, actionId, event) -> {
          boolean enterPressed =
              event != null
                  && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                  && event.getAction() == KeyEvent.ACTION_DOWN;
          if (actionId == EditorInfo.IME_ACTION_SEND || enterPressed) {
            sendCommand();
            return true;
          }
          return false;
        });
    commandInputLayout.setEndIconOnClickListener(v -> sendCommand());
  }

  private void sendCommand() {
    TerminalSession session = sessionProvider.currentSession();
    if (session == null) return;

    String text = commandInput.getText() == null ? "" : commandInput.getText().toString();
    // اینتر برای تایپ کاربر یعنی "دستور رو اجرا کن"؛ پس Enter خام هم برای اجرای خالی لازمه.
    String command = text.isEmpty() ? "\r" : text + "\r";
    byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
    session.write(bytes, 0, bytes.length);
    commandInput.setText("");
  }

  private void onKeyboardVisibilityChanged(boolean visible) {
    if (visible) {
      cancelPendingCollapse();
      if (!expanded && !dragging) expand();
    } else if (expanded && !dragging) {
      // یه مکث کوتاه تا موقع جابه‌جایی فوکوس بین ترمینال و EditText پنل پرش نکند.
      dockPages.removeCallbacks(collapseRunnable);
      dockPages.postDelayed(collapseRunnable, KEYBOARD_COLLAPSE_DELAY);
    }
  }

  private void cancelPendingCollapse() {
    dockPages.removeCallbacks(collapseRunnable);
  }

  private void expand() {
    heightAnimator.cancel();
    expanded = true;
    if (contentHeight <= 0) {
      contentHeight = measureContentHeight();
      if (contentHeight <= 0) {
        dockPages.post(() -> { if (expanded) expand(); });
        return;
      }
    }
    animateHeight(progress, 1f);
  }

  private void animateHeight(float from, float to) {
    heightAnimator.cancel();
    heightAnimator.removeAllUpdateListeners();
    heightAnimator.removeAllListeners();
    heightAnimator.setDuration(ANIM_DURATION);
    heightAnimator.setInterpolator(EASING);
    heightAnimator.setFloatValues(from, to);
    heightAnimator.addUpdateListener(a -> applyHeight((float) a.getAnimatedValue()));
    if (to >= 1f) {
      heightAnimator.addListener(
          new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              onExpandEnd();
            }
          });
    }
    heightAnimator.start();
  }

  private void onExpandEnd() {
    setPagesToRestingState();
  }

  private void applyHeight(float p) {
    progress = clamp(p);
    ViewGroup.LayoutParams lp = dockPages.getLayoutParams();
    int height = Math.round(contentHeight * progress);
    if (lp.height != height) {
      lp.height = height;
      dockPages.setLayoutParams(lp);
    }
    dockPages.setAlpha(progress);
    handleChevron.setRotation(180f * progress);
  }

  private int measureContentHeight() {
    int width = dockPages.getWidth();
    if (width <= 0) width = inputDock.getWidth();
    if (width <= 0) return 0;
    dockPages.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    return dockPages.getMeasuredHeight();
  }

  /** صفحه‌ها رو به حالت سکون برمی‌گردونه؛ اگه صفحه‌ی ورودی انتخاب شده باشه، همون حفظ می‌شه. */
  private void setPagesToRestingState() {
    if (inputPageShowing) {
      commandInputRow.setVisibility(View.VISIBLE);
      commandInputRow.setTranslationX(0f);
      commandInputRow.setAlpha(1f);
      extraKeysScroll.setVisibility(View.GONE);
      extraKeysScroll.setTranslationX(0f);
      extraKeysScroll.setAlpha(1f);
    } else {
      extraKeysScroll.setVisibility(View.VISIBLE);
      extraKeysScroll.setTranslationX(0f);
      extraKeysScroll.setAlpha(1f);
      commandInputRow.setVisibility(View.INVISIBLE);
      commandInputRow.setTranslationX(0f);
      commandInputRow.setAlpha(0f);
    }
  }

  /** جابه‌جایی نرم بین دکمه‌ها و ورودی متن: صفحه‌ی خروجی از لبه بیرون می‌ره و صفحه‌ی ورودی از لبه‌ی مقابل میاد. */
  private void animatePageSwitch(boolean toInput) {
    pageAnimator.cancel();
    pageAnimator.removeAllUpdateListeners();
    pageAnimator.removeAllListeners();

    final int width = Math.max(dockPages.getWidth(), 1);
    final View buttons = extraKeysScroll;
    final View input = commandInputRow;

    buttons.setVisibility(View.VISIBLE);
    input.setVisibility(View.VISIBLE);
    if (toInput) {
      buttons.setTranslationX(0f);
      buttons.setAlpha(1f);
      input.setTranslationX(width);
      input.setAlpha(0f);
    } else {
      buttons.setTranslationX(-width);
      buttons.setAlpha(0f);
      input.setTranslationX(0f);
      input.setAlpha(1f);
    }

    pageAnimator.setDuration(ANIM_DURATION);
    pageAnimator.setInterpolator(EASING);
    pageAnimator.setFloatValues(0f, 1f);
    pageAnimator.addUpdateListener(
        a -> {
          float v = (float) a.getAnimatedValue();
          if (toInput) {
            input.setTranslationX(width * (1f - v));
            input.setAlpha(v);
            buttons.setTranslationX(-width * v);
            buttons.setAlpha(1f - v);
          } else {
            buttons.setTranslationX(-width * (1f - v));
            buttons.setAlpha(v);
            input.setTranslationX(width * v);
            input.setAlpha(1f - v);
          }
        });
    pageAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            inputPageShowing = toInput;
            setPagesToRestingState();
          }
        });
    pageAnimator.start();
  }

  private void settleDrag() {
    if (flingHandled) {
      // اگه fling خودش اکشن رو اجرا کرده، دیگه اینجا چیزی رو تصحیح نکن.
      flingHandled = false;
      return;
    }
    // یه تپ ساده قبلاً توسط onSingleTapConfirmed (toggle) مدیریت شده؛ پس کاری نکن.
    if (!dragged) return;
    if (contentHeight <= 0) return;
    if (progress >= 0.5f) {
      expanded = true;
      animateHeight(progress, 1f);
    } else {
      expanded = false;
      animateHeight(progress, 0f);
    }
  }

  private GestureDetector createHandleDetector() {
    GestureDetector detector =
        new GestureDetector(
            inputDock.getContext(),
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(MotionEvent e) {
                dragging = true;
                dragged = false;
                cancelPendingCollapse();
                heightAnimator.cancel();
                return true;
              }

              @Override
              public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (contentHeight <= 0) return true;
                dragged = true;
                applyHeight(progress - dy / contentHeight);
                return true;
              }

              @Override
              public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                return handleFling(vx, vy);
              }

              @Override
              public boolean onSingleTapConfirmed(MotionEvent e) {
                toggle();
                return true;
              }
            });
    return detector;
  }

  private boolean handleFling(float velocityX, float velocityY) {
    if (Math.abs(velocityX) > Math.abs(velocityY)
        && Math.abs(velocityX) > FLING_VELOCITY_THRESHOLD) {
      flingHandled = true;
      if (velocityX > 0) {
        // سویپ به راست → دکمه‌های میانبر
        showButtonsPage();
      } else {
        // سویپ به چپ → ورودی متن
        showInputPage();
      }
    } else if (Math.abs(velocityY) > FLING_VELOCITY_THRESHOLD) {
      flingHandled = true;
      if (velocityY < 0) {
        expand();
      } else {
        collapse();
      }
    } else {
      settleDrag();
    }
    return true;
  }

  private static float clamp(float value) {
    return Math.max(0f, Math.min(1f, value));
  }
}