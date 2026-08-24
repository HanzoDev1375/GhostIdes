package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import java.util.Locale;

/**
 * A container that renders a single piece of media content (image / gif / video / html script)
 * addressed by path or content Uri.
 *
 * <p>{@link #load(String, LifecycleOwner, float)} is idempotent: requesting the same path and
 * blur again while it is already displayed is a no-op, so calling it from both onCreate and
 * onResume does not recreate players or webviews (no flicker, no restart).</p>
 */
public class ViewChilder extends FrameLayout {

  private static final String TAG = "ViewChilder";

  private static final int TYPE_UNKNOWN = 0;
  private static final int TYPE_IMAGE = 1;
  private static final int TYPE_GIF = 2;
  private static final int TYPE_VIDEO = 3;
  private static final int TYPE_SCRIPT = 4;

  private IChild current;
  private String currentPath;
  private float currentBlur = Float.NaN;

  public ViewChilder(@NonNull Context context) {
    super(context);
  }

  public ViewChilder(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
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
  }

  public void clear() {
    clearCurrent();
    setVisibility(INVISIBLE);
  }

  @Nullable
  public IChild current() {
    return current;
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
        return new GifChild(context, path);
      case TYPE_VIDEO:
        if (resolved == null) {
          Log.e(TAG, "Video content requires a LifecycleOwner -> " + path);
          return new ImageChild(context, path);
        }
        return new VideoChild(context, path, resolved);
      case TYPE_SCRIPT:
        return new ScriptChild(context, path, resolved);
      default:
        Log.e(TAG, "Unknown extension \"" + extensionOf(path) + "\" for: "
            + path + ", falling back to ImageChild");
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
      case "avif":
        return TYPE_IMAGE;
      case "gif":
        return TYPE_GIF;
      case "mp4":
      case "mkv":
      case "webm":
      case "3gp":
        return TYPE_VIDEO;
      case "html":
      case "htm":
        return TYPE_SCRIPT;
      default:
        break;
    }
    // No usable extension: sniff the mime type (content:// uris etc.)
    String mime = mimeOf(context, path);
    if (mime == null) return TYPE_UNKNOWN;
    if ("image/gif".equals(mime)) return TYPE_GIF;
    if (mime.startsWith("image/")) return TYPE_IMAGE;
    if (mime.startsWith("video/")) return TYPE_VIDEO;
    if ("text/html".equals(mime) || "application/xhtml+xml".equals(mime)) return TYPE_SCRIPT;
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
}
