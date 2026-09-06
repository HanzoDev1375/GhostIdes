package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;

public class ImageChild implements IChild {

  private final ImageView imageView;
  private final String path;

  public ImageChild(Context context, String path) {
    this(context, path, 0f);
  }

  public ImageChild(Context context, String path, float blurSize) {
    this.path = path;
    this.imageView = new ImageView(context);
    this.imageView.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    this.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    Object source = loadSource(path);
    if (blurSize == 0f) {
      Glide.with(context).load(source).into(imageView);
    } else
      Glide.with(context)
          .load(source)
          .transform(new StackBlurTransformation((int) blurSize))
          .into(imageView);
  }

  /** Converts local file paths into file:// uris so Glide always loads them as files. */
  private static Object loadSource(String path) {
    if (path == null || path.trim().isEmpty() || path.startsWith("content:")
        || path.startsWith("http") || path.startsWith("file:")) {
      return path;
    }
    if (path.startsWith("/")) {
      File f = new File(path);
      if (f.exists()) {
        return Uri.fromFile(f);
      }
    }
    return path;
  }

  @Override
  public View view() {
    return imageView;
  }

  @Override
  public String pathTheme() {
    return path;
  }

  @Override
  public void release() {
    Glide.with(imageView).clear(imageView);
  }
}
