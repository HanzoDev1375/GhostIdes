package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class StackBlurTransformation extends BitmapTransformation {

  private static final String ID = "ir.hanzodev1375.components.childern.StackBlurTransformation.1";

  private static final int DOWN_SAMPLING = 4;

  private final int radius;

  public StackBlurTransformation(int radius) {
    this.radius = Math.max(1, radius);
  }

  @Override
  protected Bitmap transform(
      @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    int w = Math.max(1, toTransform.getWidth() / DOWN_SAMPLING);
    int h = Math.max(1, toTransform.getHeight() / DOWN_SAMPLING);
    Bitmap scaled = Bitmap.createScaledBitmap(toTransform, w, h, false);
    return stackBlur(scaled, radius);
  }

  private static Bitmap stackBlur(Bitmap bitmap, int radius) {
    int w = bitmap.getWidth();
    int h = bitmap.getHeight();
    int[] pix = new int[w * h];
    bitmap.getPixels(pix, 0, w, 0, 0, w, h);

    int wm = w - 1;
    int hm = h - 1;
    int div = radius + radius + 1;
    int[] dv = new int[256 * div];
    for (int i = 0; i < 256 * div; i++) {
      dv[i] = i / div;
    }

    int[] r = new int[w * h];
    int[] g = new int[w * h];
    int[] b = new int[w * h];
    int[] vmin = new int[Math.max(w, h)];

    int rsum, gsum, bsum, p, yp, yi, yw;
    yw = yi = 0;

    for (int y = 0; y < h; y++) {
      rsum = gsum = bsum = 0;
      for (int i = -radius; i <= radius; i++) {
        p = pix[yi + Math.min(wm, Math.max(i, 0))];
        rsum += (p & 0xff0000) >> 16;
        gsum += (p & 0x00ff00) >> 8;
        bsum += p & 0x0000ff;
      }
      for (int x = 0; x < w; x++) {
        r[yi] = dv[rsum];
        g[yi] = dv[gsum];
        b[yi] = dv[bsum];

        if (y == 0) {
          vmin[x] = Math.min(x + radius + 1, wm);
        }
        p = pix[yw + vmin[x]];

        rsum += (p & 0xff0000) >> 16;
        gsum += (p & 0x00ff00) >> 8;
        bsum += p & 0x0000ff;

        p = pix[yw + Math.max(x - radius, 0)];
        rsum -= (p & 0xff0000) >> 16;
        gsum -= (p & 0x00ff00) >> 8;
        bsum -= p & 0x0000ff;

        yi++;
      }
      yw += w;
    }

    for (int x = 0; x < w; x++) {
      rsum = gsum = bsum = 0;
      yp = -radius * w;
      for (int i = -radius; i <= radius; i++) {
        yi = Math.max(0, yp) + x;
        rsum += r[yi];
        gsum += g[yi];
        bsum += b[yi];
        yp += w;
      }
      yi = x;
      for (int y = 0; y < h; y++) {
        pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
        if (x == 0) {
          vmin[y] = Math.min(y + radius + 1, hm) * w;
        }
        p = x + vmin[y];

        rsum += r[p];
        gsum += g[p];
        bsum += b[p];

        if (y - radius < 0) {
          p = x;
        } else {
          p = x + (y - radius) * w;
        }

        rsum -= r[p];
        gsum -= g[p];
        bsum -= b[p];

        yi += w;
      }
    }

    bitmap.setPixels(pix, 0, w, 0, 0, w, h);
    return bitmap;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof StackBlurTransformation && ((StackBlurTransformation) o).radius == radius;
  }

  @Override
  public int hashCode() {
    return ID.hashCode() + radius * 1000;
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update((ID + radius).getBytes(CHARSET));
  }
}
