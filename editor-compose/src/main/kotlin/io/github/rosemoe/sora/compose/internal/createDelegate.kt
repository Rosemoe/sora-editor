/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.compose.internal

import io.github.rosemoe.sora.I18nConfig
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.widget.CodeEditorDelegate
import io.github.rosemoe.sora.widget.CodeEditorHost
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode
import io.github.rosemoe.sora.widget.style.builtin.MoveCursorAnimator

internal fun createDelegate(host: CodeEditorHost) = CodeEditorDelegate(host).apply {
    formatTip = I18nConfig.getString(host.context, R.string.sora_editor_editor_formatting)
    cursorAnimator = MoveCursorAnimator(this)
    setText(null)

    isHorizontalScrollBarEnabled = true
    isVerticalScrollBarEnabled = true
    lnPanelPositionMode = LineInfoPanelPositionMode.FOLLOW
    lnPanelPosition = LineInfoPanelPosition.CENTER

    isHighlightCurrentLine = true
    isHighlightBracketPair = true
    isHighlightCurrentBlock = true

    isLigatureEnabled = true
    props.symbolPairAutoCompletion = true
    props.overScrollEnabled = false
}
