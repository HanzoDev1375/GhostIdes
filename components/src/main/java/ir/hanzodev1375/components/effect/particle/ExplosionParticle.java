package ir.hanzodev1375.components.effect.particle;

import android.graphics.Rect;

import java.util.Random;

/**
 *
 *
 * <pre>
 *     author : FaDai
 *     e-mail : i_fadai@163.com
 *     time   : 2017/12/14
 *     desc   : 爆炸粒子
 *     version: 1.0
 * </pre>
 */
public class ExplosionParticle extends Particle {

  /**
   * 生成粒子
   *
   * @param color 粒子颜色
   * @param radius 粒子的半径
   * @param rect View区域的矩形
   * @param endValue 动画的结束值
   * @param random 随机数
   * @param horizontalMultiple 水平变化幅度
   * @param verticalMultiple 垂直变化幅度
   */
  public ExplosionParticle(
      int color,
      int radius,
      Rect rect,
      float endValue,
      Random random,
      float horizontalMultiple,
      float verticalMultiple) {

    this.color = color;
    alpha = 1;

    float nextFloat = random.nextFloat();

    baseRadius = getBaseRadius(radius, random, nextFloat);
    this.radius = baseRadius;

    horizontalElement = getHorizontalElement(rect, random, nextFloat, horizontalMultiple);
    verticalElement = getVerticalElement(rect, random, nextFloat, verticalMultiple);

    int offsetX = rect.width() / 4;
    int offsetY = rect.height() / 4;

    baseCx = rect.centerX() + offsetX * (random.nextFloat() - 0.5f);
    baseCy = rect.centerY() + offsetY * (random.nextFloat() - 0.5f);
    cx = baseCx;
    cy = baseCy;

    font = endValue / 10 * random.nextFloat();
    later = 0.4f * random.nextFloat();
  }

  private static float getBaseRadius(float radius, Random random, float nextFloat) {
    float r = radius + radius * (random.nextFloat() - 0.5f) * 0.5f;
    r = nextFloat < 0.6f ? r : nextFloat < 0.8f ? r * 1.4f : r * 0.8f;
    return r;
  }

  private static float getHorizontalElement(
      Rect rect, Random random, float nextFloat, float horizontalMultiple) {

    float horizontal = rect.width() * (random.nextFloat() - 0.5f);

    horizontal =
        nextFloat < 0.2f ? horizontal : nextFloat < 0.8f ? horizontal * 0.6f : horizontal * 0.3f;

    return horizontal * horizontalMultiple;
  }

  private static float getVerticalElement(
      Rect rect, Random random, float nextFloat, float verticalMultiple) {

    float vertical = rect.height() * (random.nextFloat() * 0.5f + 0.5f);

    vertical = nextFloat < 0.2f ? vertical : nextFloat < 0.8f ? vertical * 1.2f : vertical * 1.4f;

    return vertical * verticalMultiple;
  }

  public void advance(float factor, float endValue) {

    float normalization = factor / endValue;

    if (normalization < font || normalization > 1f - later) {
      alpha = 0;
      return;
    }
    alpha = 1;

    normalization = (normalization - font) / (1f - font - later);

    if (normalization >= 0.7f) {
      alpha = 1f - (normalization - 0.7f) / 0.3f;
    }

    float realValue = normalization * endValue;

    cx = baseCx + horizontalElement * realValue;

    cy = baseCy + verticalElement * (realValue * (realValue - 1));

    radius = baseRadius + baseRadius / 4 * realValue;
  }
}
