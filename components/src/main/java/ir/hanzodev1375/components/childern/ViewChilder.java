package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import ir.hanzodev1375.components.utils.ComponentsPrefs;

import java.util.Locale;

/**
 * A container that renders a single piece of media content (image / gif / video / html script)
 * addressed by path or content Uri.
 *
 * <p>{@link #load(String, LifecycleOwner, float)} is idempotent: requesting the same path and blur
 * again while it is already displayed is a no-op, so calling it from both onCreate and onResume
 * does not recreate players or webviews (no flicker, no restart).
 */
public class ViewChilder extends FrameLayout {

  private static final String TAG = "ViewChilder";

  private static final int TYPE_UNKNOWN = 0;
  private static final int TYPE_IMAGE = 1;
  private static final int TYPE_GIF = 2;
  private static final int TYPE_VIDEO = 3;
  private IChild current;
  private String currentPath;
  private float currentBlur = Float.NaN;

  private WallpaperParallaxEffect parallaxEffect;
  private boolean parallaxPrefEnabled;
  private boolean parallaxRunning;

  private final SharedPreferences.OnSharedPreferenceChangeListener parallaxPrefListener =
      (prefs, key) -> {
        if (ComponentsPrefs.KEY_PARALLAX.equals(key)) {
          setParallaxPrefEnabled(prefs.getBoolean(ComponentsPrefs.KEY_PARALLAX, true));
        }
      };

  public ViewChilder(@NonNull Context context) {
    super(context);
    initParallax();
  }

  public ViewChilder(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initParallax();
  }

  private void initParallax() {
    parallaxPrefEnabled = new ComponentsPrefs(getContext()).isParallaxEnabled();
  }

  public void load(@Nullable String path) {
    load(path, resolveLifecycleOwner(), 0f);
  }

  public void load(@Nullable String path, float blurSize) {
    load(path, resolveLifecycleOwner(), blurSize);
  }

  public void load(@Nullable String path, @Nullable LifecycleOwner owner) {
    load(path, owner, 0f);
  }

  /** Loads content, skipping the work if the same content is already being displayed. */
  public void load(@Nullable String path, @Nullable LifecycleOwner owner, float blurSize) {
    if (isShowing(path, blurSize)) return;
    reload(path, owner, blurSize);
  }

  /** Forces a (re)load even if the same content is already displayed. */
  public void reload(@Nullable String path) {
    reload(path, resolveLifecycleOwner(), 0f);
  }

  public void reload(@Nullable String path, float blurSize) {
    reload(path, resolveLifecycleOwner(), blurSize);
  }

  public void reload(@Nullable String path, @Nullable LifecycleOwner owner, float blurSize) {
    clearCurrent();
    if (path == null || path.trim().isEmpty()) {
      setVisibility(INVISIBLE);
      return;
    }
    setVisibility(VISIBLE);
    current = create(getContext(), path, owner, blurSize);
    currentPath = path;
    currentBlur = blurSize;
    addView(current.view());
    startParallax();
  }

  public void clear() {
    clearCurrent();
    setVisibility(INVISIBLE);
    stopParallax();
  }

  @Nullable
  public IChild current() {
    return current;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
    prefs.registerOnSharedPreferenceChangeListener(parallaxPrefListener);
    startParallax();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
    prefs.unregisterOnSharedPreferenceChangeListener(parallaxPrefListener);
    stopParallax();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (parallaxRunning && current != null) {
      applyParallaxTransform(current.view());
    }
  }

  /**
   * Toggle-state came from the settings screen: re-evaluate immediately so the motion effect
   * turns on/off at that very moment (like Telegram), without needing a restart.
   */
  private void setParallaxPrefEnabled(boolean enabled) {
    parallaxPrefEnabled = enabled;
    if (enabled) {
      startParallax();
    } else {
      stopParallax();
    }
  }

  /** Only runs when the user enabled it AND a background is actually loaded. */
  private void startParallax() {
    if (!parallaxPrefEnabled || parallaxRunning) return;
    if (current == null || getVisibility() != VISIBLE || !isAttachedToWindow()) return;
    if (parallaxEffect == null) {
      parallaxEffect = new WallpaperParallaxEffect(getContext());
      parallaxEffect.setCallback(this::applyParallaxOffset);
    }
    parallaxRunning = true;
    if (getWidth() > 0 && getHeight() > 0) {
      applyParallaxTransform(current.view());
    } else {
      post(
          () -> {
            if (parallaxRunning && current != null) {
              applyParallaxTransform(current.view());
            }
          });
    }
    parallaxEffect.setEnabled(true);
  }

  private void stopParallax() {
    parallaxRunning = false;
    if (parallaxEffect != null) {
      parallaxEffect.setEnabled(false);
    }
    if (current != null) {
      resetParallaxTransform(current.view());
    }
  }

  private void applyParallaxOffset(int offsetX, int offsetY) {
    if (!parallaxRunning || current == null || parallaxEffect == null) return;
    View bg = current.view();
    applyParallaxTransform(bg);
    bg.setTranslationX(offsetX);
    bg.setTranslationY(offsetY);
  }

  private void applyParallaxTransform(View bg) {
    int w = Math.max(getWidth(), 1);
    int h = Math.max(getHeight(), 1);
    float scale = parallaxEffect.getScale(w, h);
    bg.setPivotX(w / 2f);
    bg.setPivotY(h / 2f);
    bg.setScaleX(scale);
    bg.setScaleY(scale);
  }

  private void resetParallaxTransform(View bg) {
    bg.setScaleX(1f);
    bg.setScaleY(1f);
    bg.setTranslationX(0f);
    bg.setTranslationY(0f);
  }

  private boolean isShowing(@Nullable String path, float blurSize) {
    return current != null && path != null && path.equals(currentPath) && blurSize == currentBlur;
  }

  private void clearCurrent() {
    IChild old = current;
    current = null;
    currentPath = null;
    currentBlur = Float.NaN;
    removeAllViews();
    if (old != null) {
      old.release();
    }
  }

  private LifecycleOwner resolveLifecycleOwner() {
    return findOwnerFrom(getContext());
  }

  public static IChild create(Context context, String path, LifecycleOwner owner) {
    return create(context, path, owner, 0f);
  }

  public static IChild create(Context context, String path, LifecycleOwner owner, float blurSize) {
    if (context == null) {
      throw new IllegalArgumentException("ViewChilder: context must not be null");
    }
    LifecycleOwner resolved = owner != null ? owner : findOwnerFrom(context);
    switch (typeOf(context, path)) {
      case TYPE_IMAGE:
        return new ImageChild(context, path, blurSize);
      case TYPE_GIF:
        return new GifChild(context, path, blurSize);
      case TYPE_VIDEO:
        if (resolved == null) {
          Log.e(TAG, "Video content requires a LifecycleOwner -> " + path);
          return new ImageChild(context, path);
        }
        return new VideoChild(context, path, resolved);
      default:
        Log.e(
            TAG,
            "Unknown extension \""
                + extensionOf(path)
                + "\" for: "
                + path
                + ", falling back to ImageChild");
        return new ImageChild(context, path);
    }
  }

  private static int typeOf(Context context, String path) {
    switch (extensionOf(path)) {
      case "png":
      case "jpg":
      case "jpeg":
      case "webp":
      case "bmp":
      case "svg":
      case "avif":
        return TYPE_IMAGE;
      case "gif":
        return TYPE_GIF;
      case "mp4":
      case "mkv":
      case "webm":
      case "3gp":
        return TYPE_VIDEO;
      default:
        break;
    }
    // No usable extension: sniff the mime type (content:// uris etc.)
    String mime = mimeOf(context, path);
    if (mime == null) return TYPE_UNKNOWN;
    if ("image/gif".equals(mime)) return TYPE_GIF;
    if (mime.startsWith("image/")) return TYPE_IMAGE;
    if (mime.startsWith("video/")) return TYPE_VIDEO;
    return TYPE_UNKNOWN;
  }

  @Nullable
  private static String mimeOf(Context context, String path) {
    if (context == null || path == null || !path.startsWith("content:")) return null;
    try {
      return context.getContentResolver().getType(Uri.parse(path));
    } catch (Exception e) {
      return null;
    }
  }

  @Nullable
  private static LifecycleOwner findOwnerFrom(Context c) {
    while (c instanceof ContextWrapper) {
      if (c instanceof LifecycleOwner) {
        return (LifecycleOwner) c;
      }
      c = ((ContextWrapper) c).getBaseContext();
    }
    return null;
  }

  private static String extensionOf(String path) {
    if (path == null) {
      return "";
    }
    int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    int dot = path.lastIndexOf('.');
    if (dot <= slash) {
      return "";
    }
    return path.substring(dot + 1).toLowerCase(Locale.US);
  }

  public String getCurrentPath() {
    return this.currentPath;
  }

  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }
}
