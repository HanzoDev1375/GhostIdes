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

package io.github.rosemoe.sora.lang.styling.inlayHint;

import java.util.ArrayList;
import java.util.List;

/**
 * A ghost text inlay hint. The {@link #getText() text} is rendered inline like a suggestion
 * preview, in the original text color with 50% transparency.
 *
 * <p>Ghost text is a suggestion preview displayed in place of the text that would be inserted if
 * the user accepts the suggestion. It does not modify the document content.
 *
 * <p>For multi-line suggestions, use {@link #split} to create one hint per line, or {@link
 * #addGhostText(InlayHintsContainer, int, int, String)} to add a multi-line ghost text to an {@link
 * InlayHintsContainer}. The first line is anchored at the given (line, column) and following lines
 * are anchored to the start of the following lines.
 *
 * @see io.github.rosemoe.sora.graphics.inlayHint.GhostTextInlayHintRenderer
 * @author Rosemoe
 */
public class GhostTextInlayHint extends InlayHint {

  public static final String TYPE_NAME = "ghost-text";

  private final String text;

  public GhostTextInlayHint(int line, int column, String text) {
    this(line, column, text, CharacterSide.LEFT);
  }

  public GhostTextInlayHint(int line, int column, String text, CharacterSide displaySide) {
    super(line, column, TYPE_NAME, displaySide);
    this.text = text;
  }

  public String getText() {
    return text;
  }

  /**
   * Split a (potentially) multi-line ghost text into a list of {@link GhostTextInlayHint}, one per
   * line.
   *
   * <p>The first line is anchored at the given {@code line} and {@code column}, while every
   * following line is anchored to the start (column 0) of the corresponding following line.
   */
  public static List<GhostTextInlayHint> split(int line, int column, String text) {
    String[] lines = text.split("\n");
    List<GhostTextInlayHint> hints = new ArrayList<>(lines.length);
    for (int i = 0; i < lines.length; i++) {
      hints.add(new GhostTextInlayHint(line + i, i == 0 ? column : 0, lines[i]));
    }
    return hints;
  }

  /**
   * Add a (potentially) multi-line ghost text to the given container.
   *
   * @see GhostTextInlayHint#split
   */
  public static void addGhostText(
      InlayHintsContainer container, int line, int column, String text) {
    for (GhostTextInlayHint hint : split(line, column, text)) {
      container.add(hint);
    }
  }
}
