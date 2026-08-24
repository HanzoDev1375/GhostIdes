package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

public class ImageChild implements IChild {

  private final ImageView imageView;
  private final String path;

  public ImageChild(Context context, String path) {
    this(context, path, 0f);
  }

  public ImageChild(Context context, String path, float blurSize) {
    this.path = path;
    this.imageView = new ImageView(context);
    this.imageView.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));
    this.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    RequestBuilder<Drawable> request = Glide.with(imageView).load(path);
    if (blurSize > 0f) {
      request = request.transform(new StackBlurTransformation((int) blurSize));
    }
    request.into(imageView);
  }

  @Override
  public View view() {
    return imageView;
  }

  @Override
  public String pathTheme() {
    return path;
  }
}
