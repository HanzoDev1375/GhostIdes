package ir.hanzodev1375.components.effect.particle;

public abstract class Particle {

  public int color;
  public float radius;
  public float alpha;
  public float cx;
  public float cy;
  public float horizontalElement;
  public float verticalElement;
  public float baseRadius;
  public float baseCx;
  public float baseCy;
  public float font;
  public float later;
  public void advance(float factor, float endValue) {}
}
