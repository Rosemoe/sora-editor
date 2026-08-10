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

package io.github.rosemoe.sora.lang.styling.inlayHint

/**
 * A ghost text inlay hint. The [text] is rendered inline like a suggestion
 * preview, in the original text color with 50% transparency.
 *
 * Ghost text is a suggestion preview displayed in place of the text that would
 * be inserted if the user accepts the suggestion. It does not modify the
 * document content.
 *
 * For multi-line suggestions, use [split] to create one hint per line, or
 * [addGhostText] to add a multi-line ghost text to an [InlayHintsContainer].
 * The first line is anchored at the given (line, column) and following lines
 * are anchored to the start of the following lines.
 *
 * @see io.github.rosemoe.sora.graphics.inlayHint.GhostTextInlayHintRenderer
 * @author Rosemoe
 */
open class GhostTextInlayHint(
    line: Int,
    column: Int,
    val text: String,
    displaySide: CharacterSide = CharacterSide.LEFT
) : InlayHint(line, column, TYPE_NAME, displaySide) {

    companion object {
        const val TYPE_NAME = "ghost-text"

        /**
         * Split a (potentially) multi-line ghost text into a list of
         * [GhostTextInlayHint], one per line.
         *
         * The first line is anchored at the given [line] and [column], while
         * every following line is anchored to the start (column 0) of the
         * corresponding following line.
         */
        fun split(line: Int, column: Int, text: String): List<GhostTextInlayHint> {
            val lines = text.split('\n')
            return lines.mapIndexed { index, lineText ->
                GhostTextInlayHint(
                    line = line + index,
                    column = if (index == 0) column else 0,
                    text = lineText
                )
            }
        }
    }

}

/**
 * Add a (potentially) multi-line ghost text to this container.
 *
 * @see GhostTextInlayHint.split
 */
fun InlayHintsContainer.addGhostText(line: Int, column: Int, text: String) {
    GhostTextInlayHint.split(line, column, text).forEach(::add)
}
