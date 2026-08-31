package ir.theme;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * Helps storing and loading theme background media paths in a portable way.
 *
 * <p>Old behaviour stored the SAF picker's {@code content://} uri directly. Such a uri is only
 * resolvable on the device that granted the permission, so sharing a theme to another device (or
 * after the grant is revoked) made the background fail to load.
 *
 * <p>Instead of that we store a real filesystem path. When the media lives in the same folder as
 * the theme file we store a small relative reference like {@code ./myimg.png} (or {@code ../..}),
 * which keeps the theme folder shareable. While loading we resolve that reference back to an
 * absolute path.
 *
 * <p>The {@code content://} uri is kept only as a last-resort fallback when the picked document can
 * not be mapped to a real path.
 */
public final class ThemeMediaPath {

  private static final String TAG = "ThemeMediaPath";
  private static final String PREFIX_CONTENT = "content:";
  private static final String PREFIX_RELATIVE_DOT = "./";
  private static final String PREFIX_RELATIVE_PARENT = "../";

  private ThemeMediaPath() {}

  /**
   * Resolves a stored theme media path into something {@code ViewChilder} / Glide can actually
   * load.
   *
   * <ul>
   *   <li>{@code content://...} - returned untouched.
   *   <li>{@code ./name.png} or {@code ../...} - resolved against the theme file's folder.
   *   <li>anything else - assumed to be an absolute path, returned untouched.
   * </ul>
   */
  @Nullable
  public static String resolve(
      @Nullable String themeFilePath, @Nullable String storedPath) {
    if (TextUtils.isEmpty(storedPath)) {
      return storedPath;
    }
    if (storedPath.startsWith(PREFIX_CONTENT) || !storedPath.startsWith(".")) {
      return storedPath;
    }
    File themeDir = themeDirOf(themeFilePath);
    if (themeDir == null) {
      Log.w(TAG, "No theme folder to resolve relative path against: " + storedPath);
      return storedPath;
    }
    String normalized = storedPath;
    while (normalized.startsWith(PREFIX_RELATIVE_PARENT)) {
      normalized = normalized.substring(3);
      themeDir = themeDir.getParentFile();
      if (themeDir == null) {
        return storedPath;
      }
    }
    if (normalized.startsWith(PREFIX_RELATIVE_DOT)) {
      normalized = normalized.substring(2);
    }
    File resolved = new File(themeDir, normalized);
    return resolved.getAbsolutePath();
  }

  /**
   * Converts a picked document uri (and its display name) into the path that should be persisted in
   * the theme file.
   *
   * <ul>
   *   <li>maps {@code content://} to a real absolute path when possible;
   *   <li>shortens it to {@code ./name} when it lives inside the theme's folder;
   *   <li>falls back to the raw {@code content://} uri when it can not be mapped.
   * </ul>
   */
  @NonNull
  public static String fromPickedUri(
      @Nullable Context context,
      @Nullable String themeFilePath,
      @NonNull Uri uri,
      @Nullable String displayName) {
    String realPath = resolveToRealPath(context, uri, displayName);
    if (TextUtils.isEmpty(realPath)) {
      return uri.toString();
    }

    File themeDir = themeDirOf(themeFilePath);
    if (themeDir != null) {
      String relative = toRelative(themeDir, realPath);
      if (relative != null) {
        return relative;
      }
    }
    return realPath;
  }

  @Nullable
  private static File themeDirOf(@Nullable String themeFilePath) {
    if (TextUtils.isEmpty(themeFilePath)) {
      return null;
    }
    File f = new File(themeFilePath);
    File parent = f.getParentFile();
    return parent != null ? parent : f.getAbsoluteFile().getParentFile();
  }

  /**
   * Best-effort mapping of a {@code content://} uri to a real filesystem path. Handles the common
   * media providers (external storage, downloads, media) and falls back to the display name on the
   * primary external storage when the file can not be mapped but its name is known.
   */
  @Nullable
  private static String resolveToRealPath(
      @Nullable Context context, @NonNull Uri uri, @Nullable String displayName) {
    try {
      if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
        String docId = DocumentsContract.getDocumentId(uri);
        String[] split = docId.split(":");
        if (split.length >= 2) {
          String type = split[0];
          String rest = docId.substring(docId.indexOf(':') + 1);
          if ("primary".equals(type)) {
            return new File(Environment.getExternalStorageDirectory(), rest).getAbsolutePath();
          }
          File sdRoot = secondaryStorageRoot(context);
          if (sdRoot != null) {
            return new File(sdRoot, rest).getAbsolutePath();
          }
        }
      }
    } catch (Exception ignored) {
    }

    if (context != null) {
      try (Cursor cursor =
          context
              .getContentResolver()
              .query(uri, new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) {
          String name = cursor.getString(0);
          if (!TextUtils.isEmpty(name)) {
            File dir = new File(Environment.getExternalStorageDirectory(), "Download");
            if (dir.isDirectory()) {
              return new File(dir, name).getAbsolutePath();
            }
          }
        }
      } catch (Exception ignored) {
      }
    }

    if (!TextUtils.isEmpty(displayName)) {
      File dir = new File(Environment.getExternalStorageDirectory(), "Download");
      if (dir.isDirectory()) {
        return new File(dir, displayName).getAbsolutePath();
      }
    }
    return null;
  }

  @Nullable
  private static String toRelative(@NonNull File themeDir, @NonNull String absolutePath) {
    File target = new File(absolutePath).getAbsoluteFile();
    File dir = themeDir.getAbsoluteFile();

    java.util.List<String> t = pathParts(target);
    java.util.List<String> d = pathParts(dir);

    int min = Math.min(t.size(), d.size());
    int common = 0;
    while (common < min && t.get(common).equals(d.get(common))) {
      common++;
    }

    StringBuilder relative = new StringBuilder();
    for (int i = common; i < d.size(); i++) {
      relative.append(PREFIX_RELATIVE_PARENT);
    }
    for (int i = common; i < t.size(); i++) {
      relative.append(t.get(i));
      if (i < t.size() - 1) {
        relative.append('/');
      }
    }

    String result = relative.toString();
    if (result.isEmpty()) {
      return null;
    }
    if (result.startsWith(PREFIX_RELATIVE_PARENT)) {
      return result;
    }
    return PREFIX_RELATIVE_DOT + result;
  }

  private static java.util.List<String> pathParts(@NonNull File file) {
    java.util.ArrayList<String> parts = new java.util.ArrayList<>();
    File current = file;
    while (current != null) {
      parts.add(0, current.getName());
      current = current.getParentFile();
    }
    return parts;
  }

  @Nullable
  private static File secondaryStorageRoot(@NonNull Context context) {
    try {
      for (File f : context.getExternalFilesDirs(null)) {
        if (f != null) {
          String path = f.getAbsolutePath();
          int idx = path.indexOf("/Android/data");
          if (idx > 0) {
            return new File(path.substring(0, idx));
          }
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }
}
