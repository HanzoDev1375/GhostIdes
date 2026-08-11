/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.rosemoe.sora.graphics.inlayHint;

import android.graphics.Canvas;
import io.github.rosemoe.sora.graphics.InlayHintRenderParams;
import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.lang.styling.inlayHint.GhostTextInlayHint;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * A ghost text inlay hint renderer.
 *
 * <p>The ghost text is drawn with the same font size as the editor text but in the original text
 * color with 50% transparency, so it looks like a faded preview of the suggested text.
 *
 * @see GhostTextInlayHint
 * @author Rosemoe
 */
public class GhostTextInlayHintRenderer extends InlayHintRenderer {

  public static final GhostTextInlayHintRenderer DefaultInstance = new GhostTextInlayHintRenderer();

  /** Alpha value of 50% transparency */
  private static final int GHOST_TEXT_ALPHA = 0x80;

  protected final Paint localPaint;

  public GhostTextInlayHintRenderer() {
    localPaint = new Paint();
    localPaint.setAntiAlias(true);
  }

  @Override
  public String getTypeName() {
    return GhostTextInlayHint.TYPE_NAME;
  }

  @Override
  public float onMeasure(InlayHint inlayHint, Paint paint, InlayHintRenderParams params) {
    return paint.measureText(
        inlayHint instanceof GhostTextInlayHint ? ((GhostTextInlayHint) inlayHint).getText() : "");
  }

  @Override
  public void onRender(
      InlayHint inlayHint,
      Canvas canvas,
      Paint paint,
      InlayHintRenderParams params,
      EditorColorScheme colorScheme,
      float measuredWidth) {
    if (!(inlayHint instanceof GhostTextInlayHint)) {
      return;
    }
    GhostTextInlayHint ghostText = (GhostTextInlayHint) inlayHint;
    localPaint.setTypeface(paint.getTypeface());
    localPaint.setTextSize(paint.getTextSize());
    int baseColor = colorScheme.getColor(EditorColorScheme.TEXT_NORMAL);
    localPaint.setColor((baseColor & 0x00FFFFFF) | (GHOST_TEXT_ALPHA << 24));
    canvas.drawText(ghostText.getText(), 0f, (float) params.getTextBaseline(), localPaint);
  }
}
