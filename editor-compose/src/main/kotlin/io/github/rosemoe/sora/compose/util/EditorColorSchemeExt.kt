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

package io.github.rosemoe.sora.compose.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

var EditorColorScheme.problemTypo
    get() = Color(getColor(EditorColorScheme.PROBLEM_TYPO))
    set(value) {
        setColor(EditorColorScheme.PROBLEM_TYPO, value.toArgb())
    }
var EditorColorScheme.problemError
    get() = Color(getColor(EditorColorScheme.PROBLEM_ERROR))
    set(value) {
        setColor(EditorColorScheme.PROBLEM_ERROR, value.toArgb())
    }
var EditorColorScheme.problemWarning
    get() = Color(getColor(EditorColorScheme.PROBLEM_WARNING))
    set(value) {
        setColor(EditorColorScheme.PROBLEM_WARNING, value.toArgb())
    }

var EditorColorScheme.annotation
    get() = Color(getColor(EditorColorScheme.ANNOTATION))
    set(value) {
        setColor(EditorColorScheme.ANNOTATION, value.toArgb())
    }
var EditorColorScheme.functionName
    get() = Color(getColor(EditorColorScheme.FUNCTION_NAME))
    set(value) {
        setColor(EditorColorScheme.FUNCTION_NAME, value.toArgb())
    }
var EditorColorScheme.identifierName
    get() = Color(getColor(EditorColorScheme.IDENTIFIER_NAME))
    set(value) {
        setColor(EditorColorScheme.IDENTIFIER_NAME, value.toArgb())
    }
var EditorColorScheme.identifierVar
    get() = Color(getColor(EditorColorScheme.IDENTIFIER_VAR))
    set(value) {
        setColor(EditorColorScheme.IDENTIFIER_VAR, value.toArgb())
    }
var EditorColorScheme.literal
    get() = Color(getColor(EditorColorScheme.LITERAL))
    set(value) {
        setColor(EditorColorScheme.LITERAL, value.toArgb())
    }
var EditorColorScheme.operator
    get() = Color(getColor(EditorColorScheme.OPERATOR))
    set(value) {
        setColor(EditorColorScheme.OPERATOR, value.toArgb())
    }
var EditorColorScheme.comment
    get() = Color(getColor(EditorColorScheme.COMMENT))
    set(value) {
        setColor(EditorColorScheme.COMMENT, value.toArgb())
    }
var EditorColorScheme.keyword
    get() = Color(getColor(EditorColorScheme.KEYWORD))
    set(value) {
        setColor(EditorColorScheme.KEYWORD, value.toArgb())
    }

var EditorColorScheme.stickyScrollDivider
    get() = Color(getColor(EditorColorScheme.STICKY_SCROLL_DIVIDER))
    set(value) {
        setColor(EditorColorScheme.STICKY_SCROLL_DIVIDER, value.toArgb())
    }
var EditorColorScheme.strikethrough
    get() = Color(getColor(EditorColorScheme.STRIKETHROUGH))
    set(value) {
        setColor(EditorColorScheme.STRIKETHROUGH, value.toArgb())
    }

var EditorColorScheme.diagnosticTooltipAction
    get() = Color(getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION))
    set(value) {
        setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION, value.toArgb())
    }
var EditorColorScheme.diagnosticTooltipDetailedMessage
    get() = Color(getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG))
    set(value) {
        setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG, value.toArgb())
    }
var EditorColorScheme.diagnosticTooltipBriefMessage
    get() = Color(getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG))
    set(value) {
        setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG, value.toArgb())
    }
var EditorColorScheme.diagnosticTooltipBackground
    get() = Color(getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND, value.toArgb())
    }

var EditorColorScheme.functionCharBackgroundStroke
    get() = Color(getColor(EditorColorScheme.FUNCTION_CHAR_BACKGROUND_STROKE))
    set(value) {
        setColor(EditorColorScheme.FUNCTION_CHAR_BACKGROUND_STROKE, value.toArgb())
    }
var EditorColorScheme.hardWrapMarker
    get() = Color(getColor(EditorColorScheme.HARD_WRAP_MARKER))
    set(value) {
        setColor(EditorColorScheme.HARD_WRAP_MARKER, value.toArgb())
    }

var EditorColorScheme.textInlayHintForeground
    get() = Color(getColor(EditorColorScheme.TEXT_INLAY_HINT_FOREGROUND))
    set(value) {
        setColor(EditorColorScheme.TEXT_INLAY_HINT_FOREGROUND, value.toArgb())
    }
var EditorColorScheme.textInlayHintBackground
    get() = Color(getColor(EditorColorScheme.TEXT_INLAY_HINT_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.TEXT_INLAY_HINT_BACKGROUND, value.toArgb())
    }

var EditorColorScheme.snippedBackgroundEditing
    get() = Color(getColor(EditorColorScheme.SNIPPET_BACKGROUND_EDITING))
    set(value) {
        setColor(EditorColorScheme.SNIPPET_BACKGROUND_EDITING, value.toArgb())
    }
var EditorColorScheme.snippedBackgroundRelated
    get() = Color(getColor(EditorColorScheme.SNIPPET_BACKGROUND_RELATED))
    set(value) {
        setColor(EditorColorScheme.SNIPPET_BACKGROUND_RELATED, value.toArgb())
    }
var EditorColorScheme.snippedBackgroundInactive
    get() = Color(getColor(EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE))
    set(value) {
        setColor(EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE, value.toArgb())
    }

var EditorColorScheme.sideBlockLine
    get() = Color(getColor(EditorColorScheme.SIDE_BLOCK_LINE))
    set(value) {
        setColor(EditorColorScheme.SIDE_BLOCK_LINE, value.toArgb())
    }
var EditorColorScheme.nonPrintableChar
    get() = Color(getColor(EditorColorScheme.NON_PRINTABLE_CHAR))
    set(value) {
        setColor(EditorColorScheme.NON_PRINTABLE_CHAR, value.toArgb())
    }
var EditorColorScheme.textSelected
    get() = Color(getColor(EditorColorScheme.TEXT_SELECTED))
    set(value) {
        setColor(EditorColorScheme.TEXT_SELECTED, value.toArgb())
    }

var EditorColorScheme.matchedTextBackground
    get() = Color(getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.matchedTextBorder
    get() = Color(getColor(EditorColorScheme.MATCHED_TEXT_BORDER))
    set(value) {
        setColor(EditorColorScheme.MATCHED_TEXT_BORDER, value.toArgb())
    }

var EditorColorScheme.completionWindowBorder
    get() = Color(getColor(EditorColorScheme.COMPLETION_WND_CORNER))
    set(value) {
        setColor(EditorColorScheme.COMPLETION_WND_CORNER, value.toArgb())
    }
var EditorColorScheme.completionWindowBackground
    get() = Color(getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.COMPLETION_WND_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.completionWindowTextMatched
    get() = Color(getColor(EditorColorScheme.COMPLETION_WND_TEXT_MATCHED))
    set(value) {
        setColor(EditorColorScheme.COMPLETION_WND_TEXT_MATCHED, value.toArgb())
    }
var EditorColorScheme.completionWindowTextPrimary
    get() = Color(getColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY))
    set(value) {
        setColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY, value.toArgb())
    }
var EditorColorScheme.completionWindowTextSecondary
    get() = Color(getColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY))
    set(value) {
        setColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY, value.toArgb())
    }
var EditorColorScheme.completionWindowItemCurrent
    get() = Color(getColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT))
    set(value) {
        setColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT, value.toArgb())
    }

var EditorColorScheme.textHighlightStrongBackground
    get() = Color(getColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.textHighlightStrongBorder
    get() = Color(getColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BORDER))
    set(value) {
        setColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BORDER, value.toArgb())
    }
var EditorColorScheme.textHighlightBackground
    get() = Color(getColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.textHighlightBorder
    get() = Color(getColor(EditorColorScheme.TEXT_HIGHLIGHT_BORDER))
    set(value) {
        setColor(EditorColorScheme.TEXT_HIGHLIGHT_BORDER, value.toArgb())
    }

var EditorColorScheme.highlightedDelimitersBackground
    get() = Color(getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.highlightedDelimitersForeground
    get() = Color(getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND))
    set(value) {
        setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, value.toArgb())
    }
var EditorColorScheme.highlightedDelimitersUnderline
    get() = Color(getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE))
    set(value) {
        setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE, value.toArgb())
    }
var EditorColorScheme.highlightedDelimitersBorder
    get() = Color(getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BORDER))
    set(value) {
        setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BORDER, value.toArgb())
    }

var EditorColorScheme.lineNumberPanel
    get() = Color(getColor(EditorColorScheme.LINE_NUMBER_PANEL))
    set(value) {
        setColor(EditorColorScheme.LINE_NUMBER_PANEL, value.toArgb())
    }
var EditorColorScheme.lineNumberPanelText
    get() = Color(getColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT))
    set(value) {
        setColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT, value.toArgb())
    }
var EditorColorScheme.blockLine
    get() = Color(getColor(EditorColorScheme.BLOCK_LINE))
    set(value) {
        setColor(EditorColorScheme.BLOCK_LINE, value.toArgb())
    }
var EditorColorScheme.blockLineCurrent
    get() = Color(getColor(EditorColorScheme.BLOCK_LINE_CURRENT))
    set(value) {
        setColor(EditorColorScheme.BLOCK_LINE_CURRENT, value.toArgb())
    }

var EditorColorScheme.scrollBarTrack
    get() = Color(getColor(EditorColorScheme.SCROLL_BAR_TRACK))
    set(value) {
        setColor(EditorColorScheme.SCROLL_BAR_TRACK, value.toArgb())
    }
var EditorColorScheme.scrollBarThumb
    get() = Color(getColor(EditorColorScheme.SCROLL_BAR_THUMB))
    set(value) {
        setColor(EditorColorScheme.SCROLL_BAR_THUMB, value.toArgb())
    }
var EditorColorScheme.scrollBarThumbPressed
    get() = Color(getColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED))
    set(value) {
        setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, value.toArgb())
    }

var EditorColorScheme.underline
    get() = Color(getColor(EditorColorScheme.UNDERLINE))
    set(value) {
        setColor(EditorColorScheme.UNDERLINE, value.toArgb())
    }
var EditorColorScheme.currentLine
    get() = Color(getColor(EditorColorScheme.CURRENT_LINE))
    set(value) {
        setColor(EditorColorScheme.CURRENT_LINE, value.toArgb())
    }
var EditorColorScheme.currentRowBorder
    get() = Color(getColor(EditorColorScheme.CURRENT_ROW_BORDER))
    set(value) {
        setColor(EditorColorScheme.CURRENT_ROW_BORDER, value.toArgb())
    }

var EditorColorScheme.selectionHandle
    get() = Color(getColor(EditorColorScheme.SELECTION_HANDLE))
    set(value) {
        setColor(EditorColorScheme.SELECTION_HANDLE, value.toArgb())
    }
var EditorColorScheme.selectionInsert
    get() = Color(getColor(EditorColorScheme.SELECTION_INSERT))
    set(value) {
        setColor(EditorColorScheme.SELECTION_INSERT, value.toArgb())
    }
var EditorColorScheme.selectedTextBackground
    get() = Color(getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.selectedTextBorder
    get() = Color(getColor(EditorColorScheme.SELECTED_TEXT_BORDER))
    set(value) {
        setColor(EditorColorScheme.SELECTED_TEXT_BORDER, value.toArgb())
    }

var EditorColorScheme.textNormal
    get() = Color(getColor(EditorColorScheme.TEXT_NORMAL))
    set(value) {
        setColor(EditorColorScheme.TEXT_NORMAL, value.toArgb())
    }
var EditorColorScheme.wholeBackground
    get() = Color(getColor(EditorColorScheme.WHOLE_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.WHOLE_BACKGROUND, value.toArgb())
    }

var EditorColorScheme.lineNumberBackground
    get() = Color(getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.lineNumberCurrent
    get() = Color(getColor(EditorColorScheme.LINE_NUMBER_CURRENT))
    set(value) {
        setColor(EditorColorScheme.LINE_NUMBER_CURRENT, value.toArgb())
    }
var EditorColorScheme.lineNumber
    get() = Color(getColor(EditorColorScheme.LINE_NUMBER))
    set(value) {
        setColor(EditorColorScheme.LINE_NUMBER, value.toArgb())
    }
var EditorColorScheme.lineDivider
    get() = Color(getColor(EditorColorScheme.LINE_DIVIDER))
    set(value) {
        setColor(EditorColorScheme.LINE_DIVIDER, value.toArgb())
    }

var EditorColorScheme.signatureTextNormal
    get() = Color(getColor(EditorColorScheme.SIGNATURE_TEXT_NORMAL))
    set(value) {
        setColor(EditorColorScheme.SIGNATURE_TEXT_NORMAL, value.toArgb())
    }
var EditorColorScheme.signatureBackground
    get() = Color(getColor(EditorColorScheme.SIGNATURE_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.SIGNATURE_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.signatureBorder
    get() = Color(getColor(EditorColorScheme.SIGNATURE_BORDER))
    set(value) {
        setColor(EditorColorScheme.SIGNATURE_BORDER, value.toArgb())
    }
var EditorColorScheme.signatureTextHighlightedParameter
    get() = Color(getColor(EditorColorScheme.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER))
    set(value) {
        setColor(EditorColorScheme.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER, value.toArgb())
    }
var EditorColorScheme.hoverTextNormal
    get() = Color(getColor(EditorColorScheme.HOVER_TEXT_NORMAL))
    set(value) {
        setColor(EditorColorScheme.HOVER_TEXT_NORMAL, value.toArgb())
    }
var EditorColorScheme.hoverTextHighlighted
    get() = Color(getColor(EditorColorScheme.HOVER_TEXT_HIGHLIGHTED))
    set(value) {
        setColor(EditorColorScheme.HOVER_TEXT_HIGHLIGHTED, value.toArgb())
    }
var EditorColorScheme.hoverBackground
    get() = Color(getColor(EditorColorScheme.HOVER_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.HOVER_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.hoverBorder
    get() = Color(getColor(EditorColorScheme.HOVER_BORDER))
    set(value) {
        setColor(EditorColorScheme.HOVER_BORDER, value.toArgb())
    }

var EditorColorScheme.staticSpanBackground
    get() = Color(getColor(EditorColorScheme.STATIC_SPAN_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.STATIC_SPAN_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.staticSpanForeground
    get() = Color(getColor(EditorColorScheme.STATIC_SPAN_FOREGROUND))
    set(value) {
        setColor(EditorColorScheme.STATIC_SPAN_FOREGROUND, value.toArgb())
    }

var EditorColorScheme.textActionWindowBackground
    get() = Color(getColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.textActionWindowIconColor
    get() = Color(getColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR))
    set(value) {
        setColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR, value.toArgb())
    }
var EditorColorScheme.minimapBackground
    get() = Color(getColor(EditorColorScheme.MINIMAP_BACKGROUND))
    set(value) {
        setColor(EditorColorScheme.MINIMAP_BACKGROUND, value.toArgb())
    }
var EditorColorScheme.minimapViewport
    get() = Color(getColor(EditorColorScheme.MINIMAP_VIEWPORT))
    set(value) {
        setColor(EditorColorScheme.MINIMAP_VIEWPORT, value.toArgb())
    }
var EditorColorScheme.minimapViewportBorder
    get() = Color(getColor(EditorColorScheme.MINIMAP_VIEWPORT_BORDER))
    set(value) {
        setColor(EditorColorScheme.MINIMAP_VIEWPORT_BORDER, value.toArgb())
    }
