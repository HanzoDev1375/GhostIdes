package ir.hanzodev1375.ghostide.glide;

import android.content.Context;
import android.graphics.Bitmap;
import ir.hanzodev1375.ghostide.glide.apkicon.ApkIconModelLoader;
import ir.hanzodev1375.ghostide.glide.music.Mp3CoverLoaderFactory;
import java.io.InputStream;
import com.caverock.androidsvg.SVG;
import ir.hanzodev1375.ghostide.glide.svg.SvgDecoder;
import android.graphics.drawable.PictureDrawable;
import ir.hanzodev1375.ghostide.glide.svg.SvgDrawableTranscoder;
import android.graphics.drawable.Drawable;
import ir.hanzodev1375.filetreelib.filetreelibglide.glide.xml.VectorModel;
import ir.hanzodev1375.filetreelib.filetreelibglide.glide.xml.VectorModelLoaderFactory;
import ir.hanzodev1375.filetreelib.filetreelibglide.glide.pdf.ThumbnailBuilderFactory;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import ir.hanzodev1375.filetreelibglide.glide.AppGlideCompat;

@GlideModule
public class AppGlide extends AppGlideCompat {
  @Override
  public void registerComponents(
      @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    registry.append(String.class, Bitmap.class, new Mp3CoverLoaderFactory());
    registry
        .register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
        .append(InputStream.class, SVG.class, new SvgDecoder());
    registry.append(String.class, Drawable.class, new ApkIconModelLoader.Factory(context));
    registry.append(VectorModel.class, Drawable.class, new VectorModelLoaderFactory(context));
    registry.append(String.class, Bitmap.class, new ThumbnailBuilderFactory(context));
  }
}
