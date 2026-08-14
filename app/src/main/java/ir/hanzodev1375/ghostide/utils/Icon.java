package ir.hanzodev1375.ghostide.utils;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import ir.hanzodev1375.ghostide.R;

public class Icon {
  
  public void bind(String pathIcon, ImageView icon) {
    String[] arr = {
      ".jpg", ".png", ".bmp", ".svg", ".apk", ".jpeg", ".avif", ".webp", ".gif", ".mp4", ".ico",
      ".heic", ".heif", ".tiff", ".tif", ".webm", ".mkv", ".mov", ".3gp", ".mp3", ".xml"
    };
    for (var it : arr) {
      if (pathIcon.endsWith(it)) {
        Glide.with(icon.getContext()).load(pathIcon).error(R.drawable.ic_material_xml).into(icon);
      }
    }
  }
}
