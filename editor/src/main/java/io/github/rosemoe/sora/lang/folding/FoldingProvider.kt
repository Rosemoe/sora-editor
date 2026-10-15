/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.lang.folding

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Supplies foldable regions to the editor.
 *
 * Register the provider with [io.github.rosemoe.sora.lang.folding.FoldingManager.registerProvider]
 * (or `editor.foldingManager.registerProvider`) and call
 * [io.github.rosemoe.sora.lang.folding.FoldingManager.invalidateRegions] whenever the source of
 * the regions changes so that the editor picks the new regions up.
 *
 * Regions produced by the language analyze manager (its code blocks) are used automatically as
 * well. Set [FoldingManager.useCodeBlocks] to false when only your own regions should be offered.
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
fun interface FoldingProvider {

    /**
     * Compute the foldable regions of the current content.
     *
     * @param editor The editor that owns the content
     * @param out    Sink for the computed regions. Regions that do not hide anything are ignored.
     */
    fun computeFoldingRegions(editor: CodeEditor, out: MutableList<FoldingRegion>)

}
