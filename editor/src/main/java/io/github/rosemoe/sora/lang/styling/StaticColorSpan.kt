/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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

package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * A static color span.
 *
 * The span has pre-defined colors for background and foreground (i.e. the colors
 * are not resolved from the color scheme, but are specified directly in the span).
 * The [span style][AdvancedSpan.style] for this type of span must specify
 * [EditorColorScheme.STATIC_SPAN_BACKGROUND] as its background color id and
 * [EditorColorScheme.STATIC_SPAN_FOREGROUND] as its foreground color id.
 *
 * @property backgroundColor The background color integer for the span.
 * @property foregroundColor The foreground color integer for the span.
 * @author Akash Yadav
 */
class StaticColorSpan(val backgroundColor: Int,
                      val foregroundColor: Int,
                      column: Int,
                      style: Long
) : Span(column, style) {

  init {
    require(backgroundColorId == EditorColorScheme.STATIC_SPAN_BACKGROUND) {
      "Background color id for ${javaClass.simpleName} must be EditorColorScheme.STATIC_SPAN_BACKGROUND"
    }
    require(foregroundColorId == EditorColorScheme.STATIC_SPAN_FOREGROUND) {
      "Foreground color id for ${javaClass.simpleName} must be EditorColorScheme.STATIC_SPAN_FOREGROUND"
    }
  }
}