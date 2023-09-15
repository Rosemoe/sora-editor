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
class StaticColorSpan private constructor(var backgroundColor: Int,
                                          var foregroundColor: Int, column: Int,
                                          style: Long
) : Span(column, style) {

  constructor(column: Int, style: Long) : this(0, 0, column, style)

  init {
    validateColorIds()
  }

  companion object {

    private val spanPool = SpanPool(SpanPool.CAPACITY_SMALL, ::StaticColorSpan)

    @JvmStatic
    fun obtain(backgroundColor: Int, foregroundColor: Int, column: Int,
               style: Long
    ): StaticColorSpan {
      return spanPool.obtain(column, style).also {
        it.backgroundColor = backgroundColor
        it.foregroundColor = foregroundColor
        it.validateColorIds()
      }
    }

    @JvmStatic
    private fun StaticColorSpan.validateColorIds() {
      require(
        backgroundColorId == EditorColorScheme.STATIC_SPAN_BACKGROUND || backgroundColorId == 0) {
        "Background color id for ${javaClass.simpleName} must be EditorColorScheme.STATIC_SPAN_BACKGROUND or 0"
      }
      require(
        foregroundColorId == EditorColorScheme.STATIC_SPAN_FOREGROUND || foregroundColorId == 0) {
        "Foreground color id for ${javaClass.simpleName} must be EditorColorScheme.STATIC_SPAN_FOREGROUND or 0"
      }
    }
  }

  override fun recycle(): Boolean {
    this.backgroundColor = 0
    this.foregroundColor = 0
    this.column = 0
    this.underlineColor = 0
    this.style = 0
    return spanPool.offer(this)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is StaticColorSpan) return false
    if (!super.equals(other)) return false

    if (backgroundColor != other.backgroundColor) return false
    if (foregroundColor != other.foregroundColor) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + backgroundColor
    result = 31 * result + foregroundColor
    return result
  }

  override fun toString(): String {
    return "StaticColorSpan{" +
      "column=" + column +
      ", style=" + style +
      ", underlineColor=" + underlineColor +
      ", bgColor=" + backgroundColor +
      ", fgColor=" + foregroundColor +
      "}";
  }
}