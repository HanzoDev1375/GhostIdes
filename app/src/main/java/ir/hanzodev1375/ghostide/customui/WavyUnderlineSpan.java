package ir.hanzodev1375.ghostide.customui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.style.LineBackgroundSpan;

public class WavyUnderlineSpan implements LineBackgroundSpan {

  private static final int MAX_SEGMENTS = 48;

  private boolean enabled;
  private int colorText = Color.RED;
  private float amplitude = 3f;
  private float halfWaveLength = 5f;
  private StatosMod mod = StatosMod.DEFAULT;

  private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path wavePath = new Path();
  private int lastRenderedColor = Integer.MIN_VALUE;
  private int cachedLeft = Integer.MIN_VALUE;
  private int cachedRight = Integer.MIN_VALUE;
  private float cachedWaveY = Float.MIN_VALUE;
  private float cachedHalfWaveLength = Float.MIN_VALUE;
  private float cachedAmplitude = Float.MIN_VALUE;

  public enum StatosMod {
    ERROR(0),
    WARNING(1),
    TYPO(2),
    DEFAULT(3);

    final int value;

    StatosMod(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public WavyUnderlineSpan() {
    init();
  }

  public WavyUnderlineSpan(StatosMod mod) {
    this.mod = mod;
    init();
  }

  private void init() {
    switch (mod) {
      case ERROR:
        colorText = Color.RED;
        break;
      case WARNING:
        colorText = Color.YELLOW;
        break;
      case TYPO:
        colorText = Color.GREEN;
        break;
      case DEFAULT:
        colorText = Color.WHITE;
        break;
    }
    wavePaint.setStyle(Paint.Style.STROKE);
    wavePaint.setStrokeWidth(2f);
    wavePaint.setAntiAlias(true);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setMod(StatosMod mod) {
    this.mod = mod;
    init();
  }

  public void setColorText(int colorText) {
    this.colorText = colorText;
  }

  @Override
  public void drawBackground(
      Canvas canvas,
      Paint paint,
      int left,
      int right,
      int top,
      int baseline,
      int bottom,
      CharSequence text,
      int start,
      int end,
      int lnum) {

    if (!enabled || end <= start) return;
    int width = right - left;
    if (width <= 0 || amplitude <= 0 || halfWaveLength <= 1f) return;

    if (lastRenderedColor != colorText) {
      lastRenderedColor = colorText;
      wavePaint.setColor(colorText);
    }

    float waveY = bottom - 2f;

    if (cachedLeft != left
        || cachedRight != right
        || cachedWaveY != waveY
        || cachedHalfWaveLength != halfWaveLength
        || cachedAmplitude != amplitude) {
      buildWavePath(left, width, waveY);
      cachedLeft = left;
      cachedRight = right;
      cachedWaveY = waveY;
      cachedHalfWaveLength = halfWaveLength;
      cachedAmplitude = amplitude;
    }

    canvas.drawPath(wavePath, wavePaint);
  }

  private void buildWavePath(int left, int width, float waveY) {
    int cycles = (int) Math.ceil(width / (4f * halfWaveLength));
    if (cycles > MAX_SEGMENTS) {
      cycles = MAX_SEGMENTS;
    }

    wavePath.rewind();
    wavePath.moveTo(left, waveY);
    for (int i = 0; i < cycles; i++) {
      wavePath.rQuadTo(halfWaveLength, -amplitude, 2 * halfWaveLength, 0);
      wavePath.rQuadTo(halfWaveLength, amplitude, 2 * halfWaveLength, 0);
    }
  }

  public float getAmplitude() {
    return this.amplitude;
  }

  public void setAmplitude(float amplitude) {
    this.amplitude = amplitude;
  }

  public float getHalfWaveLength() {
    return this.halfWaveLength;
  }

  public void setHalfWaveLength(float halfWaveLength) {
    this.halfWaveLength = halfWaveLength;
  }
}