package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class GifChild implements IChild {

  private final ImageView imageView;
  private final String path;

  public GifChild(Context context, String path) {
    this.path = path;
    this.imageView = new ImageView(context);
    this.imageView.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    this.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    Glide.with(imageView).asGif().load(path).into(imageView);
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
