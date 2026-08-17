package ir.hanzodev1375.ghostide.terminal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.databinding.ActivityTerminalBinding;
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
 * هم {@code TerminalActivity} و هم {@code TerminalBottomSheetFragment} از همون لِی‌اوت
 * ({@code activity_terminal.xml}) استفاده می‌کنن؛ پس این کنترلر مشترک بین هر دوئه.
 */
public class TerminalInputDock {

  public interface SessionProvider {
    TerminalSession currentSession();
  }

  private static final int ANIM_DURATION = 300;
  private static final int FLING_VELOCITY_THRESHOLD = 600;
  private static final int TERMINAL_SWIPE_MAX_MS = 350;
  private static final int KEYBOARD_COLLAPSE_DELAY = 350;

  /** منحنی استاندارد متریال (fast-out / slow-in) برای حس نرم‌تر. */
  private static final TimeInterpolator EASING = new PathInterpolator(0.2f, 0f, 0f, 1f);

  private final ActivityTerminalBinding binding;
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
  private int touchSlop;

  private float terminalDownX;
  private float terminalDownY;
  private long terminalDownTime;

  public TerminalInputDock(ActivityTerminalBinding binding, SessionProvider sessionProvider) {
    this.binding = binding;
    this.sessionProvider = sessionProvider;
    Context context = binding.getRoot().getContext();
    this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    this.handleDetector = createHandleDetector();
    configureInputField();
    setPagesToRestingState();

    ViewGroup.LayoutParams params = binding.dockPages.getLayoutParams();
    params.height = 0;
    binding.dockPages.setLayoutParams(params);
    binding.dockPages.setAlpha(0f);
    binding.dragHandle.setAlpha(0.6f);
    binding.dockPages.post(() -> contentHeight = measureContentHeight());
  }

  /** ژست‌های دستگیره و سویپ افقی روی خودِ ترمینال رو وصل می‌کنه. */
  public void attach() {
    binding.dragHandle.setOnTouchListener(
        (v, event) -> {
          handleDetector.onTouchEvent(event);
          int action = event.getActionMasked();
          if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            settleDrag();
          }
          return true;
        });

    binding.terminalView.setOnTouchListener(
        (v, event) -> {
          detectTerminalSwipe(event);
          return false;
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
    binding.commandInput.clearFocus();
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
    binding.commandInput.setOnEditorActionListener(
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
    binding.commandInputLayout.setEndIconOnClickListener(v -> sendCommand());
  }

  private void sendCommand() {
    TerminalSession session = sessionProvider.currentSession();
    if (session == null) return;

    String text =
        binding.commandInput.getText() == null ? "" : binding.commandInput.getText().toString();
    // اینتر برای تایپ کاربر یعنی "دستور رو اجرا کن"؛ پس Enter خام هم برای اجرای خالی لازمه.
    String command = text.isEmpty() ? "\r" : text + "\r";
    byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
    session.write(bytes, 0, bytes.length);
    binding.commandInput.setText("");
  }

  private void onKeyboardVisibilityChanged(boolean visible) {
    if (visible) {
      cancelPendingCollapse();
      if (!expanded && !dragging) expand();
    } else if (expanded && !dragging) {
      // یه مکث کوتاه تا موقع جابه‌جایی فوکوس بین ترمینال و EditText پنل پرش نکند.
      binding.dockPages.removeCallbacks(collapseRunnable);
      binding.dockPages.postDelayed(collapseRunnable, KEYBOARD_COLLAPSE_DELAY);
    }
  }

  private void cancelPendingCollapse() {
    binding.dockPages.removeCallbacks(collapseRunnable);
  }

  private void expand() {
    heightAnimator.cancel();
    expanded = true;
    if (contentHeight <= 0) {
      contentHeight = measureContentHeight();
      if (contentHeight <= 0) {
        binding.dockPages.post(() -> { if (expanded) expand(); });
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
    ViewGroup.LayoutParams lp = binding.dockPages.getLayoutParams();
    int height = Math.round(contentHeight * progress);
    if (lp.height != height) {
      lp.height = height;
      binding.dockPages.setLayoutParams(lp);
    }
    binding.dockPages.setAlpha(progress);
    binding.handleChevron.setRotation(180f * progress);
  }

  private int measureContentHeight() {
    int width = binding.dockPages.getWidth();
    if (width <= 0) width = binding.inputDock.getWidth();
    if (width <= 0) return 0;
    binding.dockPages.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    return binding.dockPages.getMeasuredHeight();
  }

  /** صفحه‌ها رو به حالت سکون برمی‌گردونه؛ اگه صفحه‌ی ورودی انتخاب شده باشه، همون حفظ می‌شه. */
  private void setPagesToRestingState() {
    if (inputPageShowing) {
      binding.commandInputRow.setVisibility(View.VISIBLE);
      binding.commandInputRow.setTranslationX(0f);
      binding.commandInputRow.setAlpha(1f);
      binding.extraKeysScroll.setVisibility(View.GONE);
      binding.extraKeysScroll.setTranslationX(0f);
      binding.extraKeysScroll.setAlpha(1f);
    } else {
      binding.extraKeysScroll.setVisibility(View.VISIBLE);
      binding.extraKeysScroll.setTranslationX(0f);
      binding.extraKeysScroll.setAlpha(1f);
      binding.commandInputRow.setVisibility(View.INVISIBLE);
      binding.commandInputRow.setTranslationX(0f);
      binding.commandInputRow.setAlpha(0f);
    }
  }

  /** جابه‌جایی نرم بین دکمه‌ها و ورودی متن: صفحه‌ی خروجی از لبه بیرون می‌ره و صفحه‌ی ورودی از لبه‌ی مقابل میاد. */
  private void animatePageSwitch(boolean toInput) {
    pageAnimator.cancel();
    pageAnimator.removeAllUpdateListeners();
    pageAnimator.removeAllListeners();

    View buttons = binding.extraKeysScroll;
    View input = binding.commandInputRow;
    final int width = Math.max(binding.dockPages.getWidth(), 1);

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
            binding.getRoot().getContext(),
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

  /**
   * روی خودِ ترمینال هم سویپ افقی گوش می‌کنیم (بدون خوردن به توشه‌ی معمول ترمینال چون false
   * برمی‌گردونیم). فقط اگه سریع و کوتاه باشه ثبت می‌شه تا با انتخاب متن قاطی نشه.
   */
  private void detectTerminalSwipe(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        terminalDownX = event.getRawX();
        terminalDownY = event.getRawY();
        terminalDownTime = event.getEventTime();
        break;

      case MotionEvent.ACTION_UP:
        if (event.getEventTime() - terminalDownTime > TERMINAL_SWIPE_MAX_MS) return;
        float dx = event.getRawX() - terminalDownX;
        float dy = event.getRawY() - terminalDownY;
        if (Math.abs(dx) <= touchSlop * 2 || Math.abs(dx) <= Math.abs(dy) * 1.6f) return;
        if (dx < 0) {
          showInputPage();
        } else {
          showButtonsPage();
        }
        break;

      default:
        break;
    }
  }

  private static float clamp(float value) {
    return Math.max(0f, Math.min(1f, value));
  }
}
