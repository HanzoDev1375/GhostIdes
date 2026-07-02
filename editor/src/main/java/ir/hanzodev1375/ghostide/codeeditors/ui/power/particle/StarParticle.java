package ir.hanzodev1375.ghostide.codeeditors.ui.power;
import android.graphics.Bitmap;

public class StarParticle extends SparkleParticle {
  public StarParticle(
      float x,
      float y,
      float dx,
      float dy,
      int color,
      float size,
      float gravity,
      float friction,
      int life,
      Bitmap starBitmap) {
    super(x, y, dx, dy, color, size, gravity, friction, life, starBitmap);
  }
}
