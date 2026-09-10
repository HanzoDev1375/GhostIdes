package ir.theme;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ThemeBus {

  public interface ThemeChangeListener {
    void onThemeChanged(GhostTheme oldTheme, GhostTheme newTheme, boolean animated);
  }

  private static final ThemeBus INSTANCE = new ThemeBus();
  private final CopyOnWriteArrayList<ThemeChangeListener> listeners = new CopyOnWriteArrayList<>();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private ThemeBus() {}

  public static ThemeBus getInstance() {
    return INSTANCE;
  }

  public void register(ThemeChangeListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public void unregister(ThemeChangeListener listener) {
    listeners.remove(listener);
  }

  public void notifyThemeChanged(GhostTheme oldTheme, GhostTheme newTheme, boolean animated) {
    for (ThemeChangeListener l : listeners) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        l.onThemeChanged(oldTheme, newTheme, animated);
      } else {
        mainHandler.post(() -> l.onThemeChanged(oldTheme, newTheme, animated));
      }
    }
  }
}
