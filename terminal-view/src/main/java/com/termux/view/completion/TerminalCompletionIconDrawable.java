package com.termux.view.completion;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import ir.theme.M3Theme;

/**
 * A circular icon drawable showing the first letter of a {@link TerminalCompletionItemKind},
 * colored per kind (like Sora's {@code SimpleCompletionIconDrawer} / {@code CustomCircleDrawable}).
 */
public class TerminalCompletionIconDrawable extends Drawable {

  private static final Map<TerminalCompletionItemKind, TerminalCompletionIconDrawable> CACHE =
      new HashMap<>();

  private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final char displayChar;

  /** Get (a cached) drawable for the given kind. */
  public static TerminalCompletionIconDrawable forKind(TerminalCompletionItemKind kind) {
    TerminalCompletionItemKind k = kind != null ? kind : TerminalCompletionItemKind.Identifier;
    TerminalCompletionIconDrawable drawable = CACHE.get(k);
    if (drawable == null) {
      drawable = new TerminalCompletionIconDrawable(k);
      CACHE.put(k, drawable);
    }
    return drawable;
  }

  private TerminalCompletionIconDrawable(TerminalCompletionItemKind kind) {
    displayChar = kind.getDisplayChar().charAt(0);

    long bgLong = kind.getDefaultDisplayBackgroundColor();
    int bg = bgLong != 0 ? (int) (0xFF000000 | bgLong) : fallbackColor(kind);
    bgPaint.setColor(bg);

    textPaint.setColor(Color.WHITE);
    textPaint.setFakeBoldText(true);
    textPaint.setTextAlign(Paint.Align.CENTER);
  }

  private static int fallbackColor(TerminalCompletionItemKind kind) {
    if (kind == TerminalCompletionItemKind.Keyword && M3Theme.onSurfaceVariant() != null) {
      return M3Theme.onSurfaceVariant();
    }
    return M3Theme.primary() != null ? M3Theme.primary() : 0xFFF70170;
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    final int w = getBounds().width();
    final int h = getBounds().height();
    if (w <= 0 || h <= 0) {
      return;
    }
    final float cx = w / 2f;
    final float cy = h / 2f;
    final float radius = Math.min(w, h) / 2f;

    canvas.drawCircle(cx, cy, radius, bgPaint);

    // Letter sized relative to the icon.
    textPaint.setTextSize(Math.min(w, h) * 0.6f);
    final float baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
    canvas.drawText(String.valueOf(displayChar), cx, baseline, textPaint);
  }

  @Override
  public void setAlpha(int alpha) {
    bgPaint.setAlpha(alpha);
    textPaint.setAlpha(alpha);
    invalidateSelf();
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    bgPaint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
