package ir.hanzodev1375.ghostide.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import ir.hanzodev1375.ghostide.materialfileicon.core.JsonFileIconHelper;
import com.caverock.androidsvg.SVG;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.FileManagerActivity;
import ir.hanzodev1375.ghostide.models.FileManagerModel;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
/**
 * code by ghost
 */
public class ShortcutHelper {

  private static final Set<String> IMAGE_EXTENSIONS =
      new HashSet<>(Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".bmp"));

  public static void showShortcutDialog(Activity activity, FileManagerModel model) {
    if (model == null) return;

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
    builder.setTitle(R.string.shortcut_dialog_title);
    builder.setMessage(activity.getString(R.string.shortcut_dialog_message, model.getName()));
    builder.setPositiveButton(R.string.yes, (dialog, which) -> createShortcut(activity, model));
    builder.setNegativeButton(R.string.no, null);
    builder.show();
  }

  public static void createShortcut(Context context, FileManagerModel model) {
    if (model == null) return;

    Intent shortcutIntent = buildShortcutIntent(context, model);
    IconCompat icon = buildIcon(context, model);
    String shortcutId = model.getPath();

    ShortcutInfoCompat shortcut =
        new ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(model.getName())
            .setLongLabel(model.getName())
            .setIntent(shortcutIntent)
            .setIcon(icon)
            .build();

    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
  }

  private static Intent buildShortcutIntent(Context context, FileManagerModel model) {
    Intent intent = new Intent(context, FileManagerActivity.class);
    intent.setAction(Intent.ACTION_VIEW);

    if (model.isDirectory()) {
      intent.putExtra("start_path", model.getPath());
    } else {
      String parentPath = new File(model.getPath()).getParent();
      intent.putExtra("start_path", parentPath);
    }

    return intent;
  }

  private static IconCompat buildIcon(Context context, FileManagerModel model) {
    String filePath = model.getPath();
    String fileName = model.getName();
    int lastDot = fileName.lastIndexOf(".");
    String extension = (lastDot > 0) ? fileName.substring(lastDot).toLowerCase() : "";
    if (IMAGE_EXTENSIONS.contains(extension)) {
      Bitmap bitmap = resizeBitmap(filePath, 256);
      if (bitmap != null) {
        return IconCompat.createWithAdaptiveBitmap(bitmap);
      }
    }

    var iconHelper = new JsonFileIconHelper(filePath);
    Bitmap iconBitmap = renderSvg(context, iconHelper.getIconPath(), 108);
    if (iconBitmap != null) {
      return IconCompat.createWithAdaptiveBitmap(iconBitmap);
    }
    return IconCompat.createWithResource(context, R.drawable.ic_fileicon);
  }

  private static Bitmap renderSvg(Context context, String assetPath, int size) {
    try (InputStream is = context.getAssets().open(assetPath)) {
      SVG svg = SVG.getFromInputStream(is);
      Picture picture = svg.renderToPicture(size, size);
      Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
      new Canvas(bitmap).drawPicture(picture);
      return bitmap;
    } catch (Exception e) {
      return null;
    }
  }

  private static Bitmap resizeBitmap(String filePath, int maxSize) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(filePath, options);

    int scale = 1;
    while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) {
      scale *= 2;
    }

    options.inJustDecodeBounds = false;
    options.inSampleSize = scale;
    return BitmapFactory.decodeFile(filePath, options);
  }
}
