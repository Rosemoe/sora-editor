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

package io.github.rosemoe.sora.compose

import android.content.ClipboardManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.Px
import androidx.annotation.UiThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import io.github.rosemoe.sora.compose.component.DiagnosticTooltipWindow
import io.github.rosemoe.sora.compose.component.TextActionWindow
import io.github.rosemoe.sora.compose.internal.CodeEditorHostImpl
import io.github.rosemoe.sora.compose.internal.createDelegate
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.Event
import io.github.rosemoe.sora.event.EventManager
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRenderer
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Cursor
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.CodeEditorDelegate
import io.github.rosemoe.sora.widget.CursorBlink
import io.github.rosemoe.sora.widget.DirectAccessProps
import io.github.rosemoe.sora.widget.EditorInputConnection
import io.github.rosemoe.sora.widget.EditorScroller
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.EditorTouchEventHandler
import io.github.rosemoe.sora.widget.SelectionMovement
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.snippet.SnippetController
import io.github.rosemoe.sora.widget.style.CursorAnimator
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle
import io.github.rosemoe.sora.widget.style.LineNumberTipTextProvider
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle
import kotlin.math.abs

/**
 * Represents the state of a [CodeEditor] in Jetpack Compose.
 *
 * This class provides methods to control the editor's behavior, manage its content,
 * and query its visual and logical state.
 *
 * ### Row vs Line
 * To handle word-wrapping correctly, this editor distinguishes between these two terms:
 * - **Line**: Refers to a logical line in the source text (delimited by line breaks).
 * - **Row**: Refers to a visual line displayed on the screen. A single logical "Line"
 *   may span multiple "Rows" if word-wrap is enabled.
 *
 * @author Vivek
 */
@Stable
class CodeEditorState @RememberInComposition internal constructor(
    cursorBlinkPeriod: Int,
    internal val host: CodeEditorHostImpl,
    internal val delegate: CodeEditorDelegate
) {

    internal var textActionWindow: TextActionWindow? = null
    internal var diagnosticTooltipWindow: DiagnosticTooltipWindow? = null

    /**
     * Check if the editor is released.
     * When an editor is released, you are not expected to make any changes to it.
     */
    var isReleased = false
        private set

    init {
        setCursorBlinkPeriod(cursorBlinkPeriod)
    }

    val inlayHintRenderers get() = delegate.inlayHintRenderers.toList()

    fun getInlayHintRendererForType(type: InlayHintRendererType?) = delegate.getInlayHintRendererForType(type?.type)

    fun registerInlayHintRenderers(vararg renderers: InlayHintRenderer) =
        delegate.registerInlayHintRenderers(*renderers)

    fun registerInlayHintRenderer(renderer: InlayHintRenderer) = delegate.registerInlayHintRenderer(renderer)
    fun removeInlayHintRenderer(renderer: InlayHintRenderer) = delegate.removeInlayHintRenderer(renderer)

    /**
     * Release some resources held by editor.
     * This will stop completion threads and destroy using [Language] object.
     * Also it prevents future editor tasks (such as posted Runnable) to be executed.
     */
    fun release() {
        hideEditorWindows()
        if (!isReleased) {
            dispatchEvent(EditorReleaseEvent(delegate))
        } else {
            return
        }

        isReleased = true
        editorLanguage.analyzeManager.destroy()
        editorLanguage.formatter.apply {
            setReceiver(null)
            destroy()
        }
        editorLanguage.destroy()
        editorLanguage = EmptyLanguage()

        // avoid access to language related after releasing
        delegate.textStyles = null
        diagnostics = null
        delegate.styleDelegate.reset()

        delegate.text.removeContentListener(delegate)
        delegate.colorScheme.detachEditor(delegate)
        diagnosticTooltipWindow?.release()
    }

    /**
     * Get KeyMetaStates, which manages alt/shift state in editor
     */
    val keyMetaStates get() = delegate.keyMetaStates

    /**
     * Cancel the next animation for [ensurePositionVisible]
     */
    fun cancelAnimation() {
        delegate.cancelAnimation()
    }

    /**
     * Get the width of line number and divider line
     *
     * @return The width
     */
    fun measureTextRegionOffset() = delegate.measureTextRegionOffset()

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getOffset(line: Int, column: Int) = delegate.getOffset(line, column)

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getCharOffsetX(line: Int, column: Int) = delegate.getCharOffsetX(line, column)

    /**
     * Get the character's y offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The y offset on view
     */
    fun getCharOffsetY(line: Int, column: Int) = delegate.getCharOffsetY(line, column)

    val snippetController: SnippetController get() = delegate.snippetController

    /**
     * Get [DirectAccessProps] object of the editor.
     *
     * You can adjust some settings of the editor by modifying the fields in the object directly.
     */
    val props: DirectAccessProps get() = delegate.props

    /**
     * The tip text while formatting
     */
    var formatTip: String
        get() = delegate.formatTip
        set(value) {
            delegate.formatTip = value
        }

    /**
     * whether line number region will scroll together with code region
     */
    var isLineNumberPinned: Boolean
        get() = delegate.isLineNumberPinned
        set(value) {
            delegate.setPinLineNumber(value)
        }

    /**
     * Show first line number in screen in word wrap mode
     */
    var isFirstLineNumberAlwaysVisible: Boolean
        get() = delegate.isFirstLineNumberAlwaysVisible
        set(value) {
            delegate.isFirstLineNumberAlwaysVisible = value
        }

    /**
     * Inserts the given text in the editor.
     *
     * This method allows you to insert texts externally to the content of editor.
     * The content of [text] is not checked to be exactly characters of symbols.
     *
     * Note that this still works when the editor is not editable. But you should not
     * call it at that time due to possible problems, especially when [editable] returns
     * true but [isEditable] returns false
     *
     * @param text            Text to insert, usually a text of symbols
     * @param selectionOffset New selection position relative to the start of text to insert.
     *                        Ranging from 0 to text.length
     * @throws IllegalArgumentException If the [selectionOffset] is invalid
     */
    fun insertText(text: String, selectionOffset: Int) {
        delegate.insertText(text, selectionOffset)
    }

    /**
     * Set cursor blinking period
     * If zero or negative period is passed, the cursor will always be shown.
     *
     * @param period The period time of cursor blinking
     */
    fun setCursorBlinkPeriod(period: Int) {
        if (delegate.cursorBlink == null) {
            delegate.cursorBlink = CursorBlink(delegate, period)
        } else {
            val before = delegate.cursorBlink.period
            delegate.cursorBlink.period = period
            if (before <= 0 && delegate.cursorBlink.valid && host.isAttachedToWindow) {
                host.postInLifecycle(delegate.cursorBlink)
            }
        }
    }

    /**
     * Enable/disable ligature of all types(except 'rlig').
     */
    var isLigatureEnabled: Boolean
        get() = delegate.isLigatureEnabled
        set(value) {
            delegate.isLigatureEnabled = value
        }

    /**
     * Set font feature settings for all paints used by editor
     *
     * @see Paint.setFontFeatureSettings
     */
    fun setFontFeatureSettings(features: String) {
        delegate.setFontFeatureSettings(features)
    }

    /**
     * The style of selection handle.
     *
     * @see SelectionHandleStyle
     * @see io.github.rosemoe.sora.widget.style.builtin.HandleStyleDrop
     * @see io.github.rosemoe.sora.widget.style.builtin.HandleStyleSideDrop
     */
    var selectionHandleStyle: SelectionHandleStyle
        get() = delegate.handleStyle
        set(value) {
            delegate.setSelectionHandleStyle(value)
        }

    /**
     * Whether the editor should use a different color to draw
     * the current code block line and this code block's start line and end line's
     * background.
     */
    var isHighlightCurrentBlock: Boolean
        get() = delegate.isHighlightCurrentBlock
        set(value) {
            delegate.isHighlightCurrentBlock = value
        }

    /**
     * Whether the cursor should stick to the text row while selecting the text.
     */
    var isStickyTextSelection: Boolean
        get() = delegate.isStickyTextSelection
        set(value) {
            delegate.isStickyTextSelection = value
        }

    /**
     * Specify whether the editor should use a different color to draw
     * the background of current line
     */
    var isHighlightCurrentLine: Boolean
        get() = delegate.isHighlightCurrentLine
        set(value) {
            delegate.isHighlightCurrentLine = value
        }

    /**
     * The editor's language.
     *
     * A language is a tool for auto-completion,highlight and auto indent analysis.
     */
    var editorLanguage: Language
        get() = delegate.editorLanguage
        set(value) {
            delegate.setEditorLanguage(value)
        }

    /**
     * The width of code block line in dp unit.
     */
    var blockLineWidth: Float
        get() = delegate.blockLineWidth
        set(value) {
            delegate.blockLineWidth = value
        }

    /**
     * whether text in editor should be wrapped to fit its size, with anti-word-breaking enabled by default
     */
    var isWordwrap: Boolean
        get() = delegate.isWordwrap
        set(value) {
            delegate.setWordwrap(value)
        }

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if anti word breaking is enabled in wordwrap mode.
     */
    val isAntiWordBreaking: Boolean
        get() = delegate.isAntiWordBreaking

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if RTL-based text should display from right of the widget in wordwrap mode.
     */
    val isWordwrapRtlDisplaySupport: Boolean
        get() = delegate.isWordwrapRtlDisplaySupport

    /**
     * Set whether text in editor should be wrapped to fit its size
     *
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @param supportRtlRow    Allow rows with RTL base direction to display from the right side of widget
     * @see isWordwrap
     */
    fun setWordwrap(wordwrap: Boolean, antiWordBreaking: Boolean, supportRtlRow: Boolean = false) {
        delegate.setWordwrap(wordwrap, antiWordBreaking, supportRtlRow)
    }

    /**
     * Whether the cursor's smooth movement animation is enabled.
     */
    var isCursorAnimationEnabled: Boolean
        get() = delegate.isCursorAnimationEnabled
        set(value) {
            delegate.isCursorAnimationEnabled = value
        }

    /**
     * The animator responsible for cursor movement transitions.
     *
     * @see isCursorAnimationEnabled for disabling the animation
     */
    var cursorAnimator: CursorAnimator
        get() = delegate.cursorAnimator
        set(value) {
            delegate.cursorAnimator = value
        }

    /**
     * Set whether scroll bars should be visible when the user scrolls.
     *
     * @param enabled Enabled / disabled
     */
    fun setScrollBarEnabled(enabled: Boolean) {
        delegate.setScrollBarEnabled(enabled)
    }

    // TODO: replace with compose drawables
    var horizontalScrollBarThumbDrawable: Drawable?
        get() = delegate.renderer.horizontalScrollbarThumbDrawable
        set(value) {
            delegate.renderer.horizontalScrollbarThumbDrawable = value
        }

    // TODO: replace with compose drawables
    var horizontalScrollBarTrackDrawable: Drawable?
        get() = delegate.renderer.horizontalScrollbarTrackDrawable
        set(value) {
            delegate.renderer.horizontalScrollbarTrackDrawable = value
        }

    // TODO: replace with compose drawables
    var verticalScrollBarThumbDrawable: Drawable?
        get() = delegate.renderer.verticalScrollbarThumbDrawable
        set(value) {
            delegate.renderer.verticalScrollbarThumbDrawable = value
        }

    // TODO: replace with compose drawables
    var verticalScrollBarTrackDrawable: Drawable?
        get() = delegate.renderer.verticalScrollbarTrackDrawable
        set(value) {
            delegate.renderer.verticalScrollbarTrackDrawable = value
        }

    /**
     * Whether to show a floating line number panel next to the vertical scroll bar
     * while it is being dragged.
     */
    var isDisplayLineNumberPanel: Boolean
        get() = delegate.isDisplayLnPanel
        set(value) {
            delegate.isDisplayLnPanel = value
        }

    /**
     * Sets how the floating line number panel is positioned relative to the scroll bar.
     *
     * Default to [LineNumberPanelPositionMode.Follow]
     *
     * @see LineNumberPanelPositionMode
     */
    var lineNumberPanelPositionMode: LineNumberPanelPositionMode
        get() = LineNumberPanelPositionMode(delegate.lnPanelPositionMode)
        set(value) {
            delegate.lnPanelPositionMode = value.mode
        }

    /**
     * Set display position the line number panel beside vertical scroll bar
     *
     * Only [LineNumberPanelPosition.Top], [LineNumberPanelPosition.Center] and [LineNumberPanelPosition.Bottom]
     * will be effective when position mode is [LineNumberPanelPositionMode.Follow].
     *
     * Default to [LineNumberPanelPosition.Top or LineNumberPanelPosition.Right][LineNumberPanelPosition]
     *
     * @see LineNumberPanelPosition
     * @see lineNumberPanelPositionMode
     */
    var lineNumberPanelPosition: LineNumberPanelPosition
        get() = LineNumberPanelPosition(delegate.lnPanelPosition)
        set(value) {
            delegate.lnPanelPosition = value.position
        }

    /**
     * Provides the text content for the floating line number panel.
     */
    var lineNumberTipTextProvider: LineNumberTipTextProvider
        get() = delegate.lineNumberTipTextProvider
        set(value) {
            delegate.lineNumberTipTextProvider = value
        }

    /**
     * Indicate whether the horizontal scrollbar should be drawn or not. The scrollbar is not drawn by default.
     */
    var isHorizontalScrollBarEnabled: Boolean
        get() = delegate.isHorizontalScrollBarEnabled
        set(value) {
            delegate.isHorizontalScrollBarEnabled = value
        }

    /**
     * Indicate whether the vertical scrollbar should be drawn or not. The scrollbar is not drawn by default.
     */
    var isVerticalScrollBarEnabled: Boolean
        get() = delegate.isVerticalScrollBarEnabled
        set(value) {
            delegate.isVerticalScrollBarEnabled = value
        }

    /**
     * Text size in pixel unit
     */
    @get:Px
    @setparam:Px
    var textSizePx: Float
        get() = delegate.textSizePx
        set(value) {
            delegate.textSizePx = value
        }

    /**
     * Whether the editor is allowed to use [android.graphics.RenderNode] for text rendering.
     *
     * Enabling this improves rendering performance but increases memory usage. It only
     * takes effect when hardware acceleration is enabled on the view.
     */
    var isHardwareAcceleratedDrawAllowed
        get() = delegate.isHardwareAcceleratedDrawAllowed
        set(value) {
            delegate.isHardwareAcceleratedDrawAllowed = value
        }

    /**
     * Get the width of line number region (include line number margin)
     *
     * @return width of line number region
     */
    fun measureLineNumber() = delegate.measureLineNumber()

    /**
     * Indents the selected lines. Does nothing if the text is not selected.
     */
    fun indentSelection() = delegate.indentSelection()

    /**
     * Indents the lines. Does nothing if the [onlyIfSelected] is `true` and
     * no text is selected.
     *
     * @param onlyIfSelected Set to `true` if lines must be indented only if the text is
     *                       selected.
     */
    fun indentLines(onlyIfSelected: Boolean = false) = delegate.indentLines(onlyIfSelected)

    /**
     * Removes indentation from the start of the selected lines. If the text is not selected, or if
     * the start and end selection is on the same line, only the line at the cursor position is
     * unindented.
     */
    fun unindentSelection() = delegate.unindentSelection()

    /**
     * Indents the line if text is not selected and the cursor is at the start of the line. Inserts
     * an indentation string otherwise.
     */
    fun indentOrCommitTab() = delegate.indentOrCommitTab()

    /**
     * Updates the cursor anchor information for the Input Method Editor (IME).
     * This informs the IME of the current cursor coordinates on the screen.
     *
     * @return The offset x of right cursor on view
     */
    fun updateCursorAnchor() = delegate.updateCursorAnchor()

    /**
     * Delete text before cursor or selected text (if there is)
     */
    fun deleteText() = delegate.deleteText()

    /**
     * Commit text to the content from IME
     *
     * @param text                  Text commit by InputConnection
     * @param applyAutoIndent       Apply automatic indentation
     * @param applySymbolCompletion Apply symbol surroundings and completions
     */
    fun commitText(
        text: CharSequence,
        applyAutoIndent: Boolean = true,
        applySymbolCompletion: Boolean = true
    ) = delegate.commitText(text, applyAutoIndent, applySymbolCompletion)

    /**
     * Text size for line info panel in **SP unit**
     */
    var lineInfoTextSize: Float
        get() = delegate.lineInfoTextSize
        set(value) {
            delegate.lineInfoTextSize = value
        }

    /**
     * Sets non-printable painting flags.
     * Specify where they should be drawn and some other properties.
     *
     * Flags can be mixed.
     *
     * @see NonPrintableMarks
     */
    var nonPrintableMarks: NonPrintableMarks
        get() = NonPrintableMarks(delegate.nonPrintablePaintingFlags)
        set(value) {
            delegate.nonPrintablePaintingFlags = value.flag
        }

    /**
     * Make the selection visible
     */
    fun ensureSelectionVisible() = delegate.ensureSelectionVisible()

    /**
     * Scrolls the editor to ensure the specified character position is within the viewport.
     *
     * @param line        Logical line index
     * @param column      Column index in the line
     * @param noAnimation true if no animation should be applied
     */
    fun ensurePositionVisible(
        line: Int,
        column: Int,
        noAnimation: Boolean = false
    ) = delegate.ensurePositionVisible(line, column, noAnimation)

    /**
     * Returns true if there is valid text content in the system clipboard.
     */
    val hasClip get() = delegate.hasClip()

    /**
     * The scroller managing the editor's scroll state and momentum.
     * Usually used for programmatic scrolling.
     *
     * @return The scroller
     */
    val scroller: EditorScroller get() = delegate.scroller

    /**
     * Checks whether the given Y coordinate is beyond the maximum scrollable vertical position.
     *
     * @param positionOnScreen Y position on view
     * @return Whether over max Y
     */
    fun isOverMaxY(positionOnScreen: Float) = delegate.isOverMaxY(positionOnScreen)

    /**
     * Determine character position using positions in scroll coordinate
     *
     * @param offset Position in scroll coordinate
     * @return [Pair<Int, Int>][Pair]. [first][Pair.first] is line and [second][Pair.second] is column
     */
    fun getPointPosition(offset: Offset): Pair<Int, Int> {
        val pair = delegate.getPointPosition(offset.x, offset.y)
        return Pair(IntPair.getFirst(pair), IntPair.getSecond(pair))
    }

    /**
     * Determine character position using positions in scroll coordinate
     *
     * @param x X position in scroll coordinate
     * @param y Y position in scroll coordinate
     * @return [Pair<Int, Int>][Pair]. [first][Pair.first] is line and [second][Pair.second] is column
     */
    fun getPointPosition(x: Float, y: Float) = getPointPosition(Offset(x, y))

    /**
     * Determine character position using positions on view
     *
     * @param x X on view
     * @param y Y on view
     * @return [Pair<Int, Int>][Pair]. [first][Pair.first] is line and [second][Pair.second] is column
     */
    fun getPointPositionOnScreen(x: Float, y: Float): Pair<Int, Int> {
        val packed = delegate.getPointPositionOnScreen(x, y)
        return Pair(IntPair.getFirst(packed), IntPair.getSecond(packed))
    }

    /**
     * Get max scroll y
     */
    val maxScrollY get() = delegate.scrollMaxY

    /**
     * Get max scroll x
     */
    val maxScrollX get() = delegate.scrollMaxX

    /**
     * The factor of extra space in vertical direction. The factor is multiplied with editor
     * height to compute the extra space of vertical viewport. Specially, when factor is zero, no
     * extra space is added.
     *
     * 0.5f by default.
     *
     * @throws IllegalArgumentException if the factor is negative or bigger than 1.0f
     */
    var verticalExtraSpaceFactor: Float
        get() = delegate.verticalExtraSpaceFactor
        set(value) {
            delegate.verticalExtraSpaceFactor = value
        }

    /**
     * Get [EditorSearcher]
     */
    val searcher: EditorSearcher get() = delegate.searcher

    /**
     * Format text Async
     *
     * @return Whether the format task is scheduled
     */
    @Synchronized
    fun formatCodeAsync() = delegate.formatCodeAsync()

    /**
     * Schedules an asynchronous formatting task for the specified text range.
     *
     * Note: Make sure the given positions are valid (line, column and index). Typically, you should
     * obtain a position by an object of [io.github.rosemoe.sora.text.Indexer]
     *
     * @param start Start position created by Indexer
     * @param end   End position created by Indexer
     * @return Whether the format task is scheduled
     */
    @Synchronized
    fun formatCodeAsync(start: CharPosition, end: CharPosition) = delegate.formatCodeAsync(start, end)

    /**
     * Get the cursor range of editor
     */
    val cursorRange: TextRange get() = delegate.cursorRange

    /**
     * If any text is selected
     */
    val isTextSelected get() = delegate.isTextSelected

    /**
     * Tab width compared to space
     */
    var tabWidth: Int
        get() = delegate.tabWidth
        set(value) {
            delegate.tabWidth = value
        }

    /**
     * Sets the boundaries for text size scaling when zooming via pinch gestures.
     *
     * @param minSize Minimum text size in pixels.
     * @param maxSize Maximum text size in pixels.
     */
    fun setScaleTextSizes(minSize: Float, maxSize: Float) {
        delegate.setScaleTextSizes(minSize, maxSize)
    }

    /**
     * When the parent is a scrollable composable,
     * request it not to allow horizontal scrolling to be intercepted.
     * Until the code cannot scroll horizontally
     */
    var isInterceptParentHorizontalScrollEnabled: Boolean
        get() = delegate.isInterceptParentHorizontalScrollEnabled
        set(value) {
            delegate.setInterceptParentHorizontalScrollIfNeeded(value)
        }

    /**
     * Whether to highlight brackets pairs
     */
    var isHighlightBracketPair: Boolean
        get() = delegate.isHighlightBracketPair
        set(value) {
            delegate.isHighlightBracketPair = value
        }

    /**
     * Line separator used when new lines are created in editor (only texts from IME. texts from clipboard
     * or other strategies are not encountered). **Must not be [LineSeparator.NONE]**
     *
     * @see LineSeparator
     */
    var lineSeparator: LineSeparator
        get() = delegate.lineSeparator
        set(value) {
            delegate.lineSeparator = value
        }

    /**
     * Specify input type for the editor
     *
     * Zero for default input type
     *
     * @see android.view.inputmethod.EditorInfo.inputType
     */
    var inputType: Int
        get() = delegate.inputType
        set(value) {
            delegate.inputType = value
        }

    /**
     * Undo last action
     */
    fun undo() = delegate.undo()

    /**
     * Redo last action
     */
    fun redo() = delegate.undo()

    /**
     * Checks whether we can undo
     *
     * @return true if we can undo
     */
    val canUndo get() = delegate.canUndo()

    /**
     * Checks whether we can redo
     *
     * @return true if we can redo
     */
    val canRedo get() = delegate.canRedo()

    /**
     * Enable / disabled undo manager
     */
    var isUndoEnabled: Boolean
        get() = delegate.isUndoEnabled
        set(value) {
            delegate.isUndoEnabled = value
        }

    var diagnosticIndicatorStyle: DiagnosticIndicatorStyle
        get() = delegate.diagnosticIndicatorStyle
        set(value) {
            delegate.diagnosticIndicatorStyle = value
        }

    /**
     * Get [EditorTouchEventHandler] of the editor
     */
    val touchEventHandler: EditorTouchEventHandler get() = delegate.eventHandler

    /**
     * Margin left of divider line
     *
     * @see setDividerMargin
     */
    @get:Px
    val dividerMarginLeft get() = delegate.dividerMarginLeft

    /**
     * Margin right of divider line
     *
     * @see setDividerMargin
     */
    @get:Px
    val dividerMarginRight get() = delegate.dividerMarginRight

    /**
     * Set divider line's left and right margin
     *
     * @param marginLeft  Margin left for divider line
     * @param marginRight Margin right for divider line
     */
    fun setDividerMargin(@Px marginLeft: Float, @Px marginRight: Float) {
        delegate.setDividerMargin(marginLeft, marginRight)
    }

    /**
     * Set divider line's left and right margin
     *
     * @param margin Margin left and right for divider line
     */
    fun setDividerMargin(@Px margin: Float) {
        delegate.setDividerMargin(margin)
    }

    /**
     * line number margin left
     */
    @get:Px
    @setparam:Px
    var lineNumberMarginLeft
        get() = delegate.lineNumberMarginLeft
        set(value) {
            delegate.lineNumberMarginLeft = value
        }

    /**
     * Width of divider line
     */
    @get:Px
    @setparam:Px
    var dividerWidth
        get() = delegate.dividerWidth
        set(value) {
            delegate.dividerWidth = value
        }

    /**
     * Typeface of line number
     */
    var typefaceLineNumber: Typeface
        get() = delegate.typefaceLineNumber
        set(value) {
            delegate.typefaceLineNumber = value
        }

    /**
     * Typeface of text
     */
    var typefaceText: Typeface
        get() = delegate.typefaceText
        set(value) {
            delegate.typefaceText = value
        }

    /**
     * Text scale x of Paint
     *
     * @see Paint.setTextScaleX
     */
    var textScaleX: Float
        get() = delegate.textScaleX
        set(value) {
            delegate.textScaleX = value
        }

    /**
     * Letter spacing of Paint
     *
     * @see Paint.setLetterSpacing
     */
    var textLetterSpacing: Float
        get() = delegate.textLetterSpacing
        set(value) {
            delegate.textLetterSpacing = value
        }

    /**
     * Line number align
     */
    var lineNumberAlign: Paint.Align
        get() = delegate.lineNumberAlign
        set(value) {
            delegate.lineNumberAlign = value
        }

    /**
     * Width for insert cursor
     *
     * @param width Cursor width
     */
    fun setCursorWidth(@Px width: Float) = delegate.setCursorWidth(width)

    /**
     * Border width for text border
     */
    var textBorderWidth
        get() = delegate.textBorderWidth
        set(value) {
            delegate.textBorderWidth = value
        }

    /**
     * Get text cursor.
     *
     * Always set selection position by [setSelection] or [setSelectionRegion].
     * Do not modify the object returned.
     *
     * @return Cursor of text
     */
    val cursor: Cursor get() = delegate.cursor

    /**
     * Get line count
     *
     * @return line count
     */
    val lineCount get() = delegate.lineCount

    /**
     * Get first visible line on screen
     *
     * @return first visible line
     */
    val firstVisibleLine get() = delegate.firstVisibleLine

    /**
     * Get first visible row on screen
     *
     * @return first visible row
     */
    val firstVisibleRow get() = delegate.firstVisibleRow

    /**
     * Get last visible row on screen.
     *
     * @return last visible row
     */
    val lastVisibleRow get() = delegate.lastVisibleRow

    /**
     * Get last visible line on screen
     *
     * @return last visible line
     */
    val lastVisibleLine get() = delegate.lastVisibleLine

    /**
     * Checks whether this row is visible on screen
     *
     * @param row Row to check
     * @return Whether visible
     */
    fun isRowVisible(row: Int) = delegate.isRowVisible(row)

    /**
     * Sets line spacing for this TextView.  Each line other than the last line will have its height
     * multiplied by [mult] and have [add] added to it.
     *
     * @param add  The value in pixels that should be added to each line other than the last line.
     *             This will be applied after the multiplier
     * @param mult The value by which each line height other than the last line will be multiplied
     *             by
     */
    fun setLineSpacing(add: Float, mult: Float) = delegate.setLineSpacing(add, mult)

    /**
     * The value in pixels that should be added to each line other than the last line.
     * This will be applied after the multiplier
     *
     * @return the extra space that is added to the height of each lines of this Text.
     */
    var lineSpacingExtra: Float
        get() = delegate.lineSpacingExtra
        set(value) {
            delegate.lineSpacingExtra = value
        }

    /**
     * The value by which each line height other than the last line will be multiplied by. Default 1.0f
     *
     * @return the value by which each line's height is multiplied to get its actual height.
     */
    var lineSpacingMultiplier: Float
        get() = delegate.lineSpacingMultiplier
        set(value) {
            delegate.lineSpacingMultiplier = value
        }

    /**
     * Get actual line spacing in pixels.
     */
    val lineSpacingPx get() = delegate.lineSpacingPixels

    /**
     * Get baseline directly
     *
     * @param row Row
     * @return baseline y offset
     */
    fun getRowBaseline(row: Int) = delegate.getRowBaseline(row)

    /**
     * Get row height
     *
     * @return height of single row
     */
    val rowHeight get() = delegate.rowHeight

    /**
     * Get row top y offset
     *
     * @param row Row
     * @return top y offset
     */
    fun getRowTop(row: Int) = delegate.getRowTop(row)

    /**
     * Get row bottom y offset
     *
     * @param row Row
     * @return Bottom y offset
     */
    fun getRowBottom(row: Int) = delegate.getRowBottom(row)

    /**
     * Get the top of text in target row
     */
    fun getRowTopOfText(row: Int) = delegate.getRowTopOfText(row)

    /**
     * Get the bottom of text in target row
     */
    fun getRowBottomOfText(row: Int) = delegate.getRowBottomOfText(row)

    /**
     * Get the height of text in row
     */
    val rowHeightOfText get() = delegate.rowHeightOfText

    /**
     * Get scroll x
     *
     * @return scroll x
     */
    val scrollX get() = delegate.offsetX

    /**
     * Get scroll y
     *
     * @return scroll y
     */
    val scrollY get() = delegate.offsetY

    /**
     * Check whether the editor is actually editable. This is not only related to user
     * property 'editable', but also editor states. When the editor is busy at initializing
     * its layout or awaiting the result of format, it is also not editable.
     *
     * Do not modify the text externally in editor when this method returns false.
     *
     * @return Whether the editor is editable, actually.
     * @see isFormatting
     */
    val isEditable: Boolean
        get() = delegate.isEditable

    /**
     * Whether text can be edited
     */
    var editable: Boolean
        get() = delegate.editable
        set(value) {
            delegate.editable = value
        }

    /**
     * Allow scale text size by thumb
     */
    var isScalable: Boolean
        get() = delegate.isScalable
        set(value) {
            delegate.isScalable = value
        }

    var isBlockLineEnabled: Boolean
        get() = delegate.isBlockLineEnabled
        set(value) {
            delegate.isBlockLineEnabled
        }

    /**
     * Begin a rejection on composing texts
     */
    fun beginComposingTextRejection() = delegate.beginComposingTextRejection()

    /**
     * If the editor accepts composing text now, according to composing text rejection count
     */
    fun acceptsComposingText() = delegate.acceptsComposingText()

    /**
     * End a rejection on composing texts
     */
    fun endComposingTextRejection() = delegate.endComposingTextRejection()

    /**
     * Check if there is a mouse inside editor, hovering
     */
    val hasMouseHovering get() = delegate.hasMouseHovering()

    /**
     * Check if there is a mouse inside editor with any button pressed
     */
    val hasMousePressed get() = delegate.hasMousePressed()

    /**
     * Check if editor is in mouse mode.
     *
     * @see DirectAccessProps.mouseMode
     */
    val isInMouseMode get() = delegate.isInMouseMode

    /**
     * Move or extend selection, according to [extend] param.
     *
     * @param extend True if you want to extend selection.
     */
    fun moveOrExtendSelection(movement: SelectionMovement, extend: Boolean) {
        delegate.moveOrExtendSelection(movement, extend)
    }

    /**
     * Extend the selection, based on the selection anchor (select text)
     */
    fun extendSelection(movement: SelectionMovement) = delegate.extendSelection(movement)

    /**
     * Move the selection. Selected text will be de-selected.
     */
    fun moveSelection(movement: SelectionMovement) = delegate.moveSelection(movement)

    /**
     * Move selection to given position
     *
     * @param line   The line to move
     * @param column The column to move
     */
    fun setSelection(line: Int, column: Int, cause: Int = SelectionChangeEvent.CAUSE_UNKNOWN) =
        delegate.setSelection(line, column, cause)

    /**
     * Move selection to given position
     *
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    fun setSelection(line: Int, column: Int, makeItVisible: Boolean, cause: Int = SelectionChangeEvent.CAUSE_UNKNOWN) {
        delegate.setSelection(line, column, makeItVisible, cause)
    }

    /**
     * Select all text
     */
    fun selectAll() = delegate.selectAll()

    /**
     * Set selection region
     *
     * @param lineLeft         Line left
     * @param columnLeft       Column Left
     * @param lineRight        Line right
     * @param columnRight      Column right
     * @param makeRightVisible Whether to make right cursor visible
     */
    fun setSelectionRegion(
        lineLeft: Int,
        columnLeft: Int,
        lineRight: Int,
        columnRight: Int,
        makeRightVisible: Boolean = true,
        cause: Int = SelectionChangeEvent.CAUSE_UNKNOWN
    ) {
        delegate.setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, makeRightVisible, cause)
    }

    /**
     * Get system clipboard manager used by editor
     */
    val clipboardManager: ClipboardManager get() = delegate.clipboardManager

    /**
     * Paste text from clip board
     */
    fun pasteText() = delegate.pasteText()

    /**
     * Paste external text into editor
     */
    fun pasteText(text: CharSequence?) = delegate.pasteText(text)

    /**
     * Copy text to clipboard.
     */
    fun copyText() = delegate.copyText()

    /**
     * Copy text to clipboard.
     *
     * @param shouldCopyLine State whether the editor should select whole line if
     *                       cursor is not in selection mode.
     */
    fun copyText(shouldCopyLine: Boolean) = delegate.copyText(shouldCopyLine)

    /**
     * Copy text to clipboard and delete them
     */
    fun cutText() = delegate.cutText()

    /**
     * Copy the current line to clipboard and delete it.
     */
    fun cutLine() = delegate.cutLine()

    /**
     * Duplicates the current line.
     * Does not selects the duplicated line.
     */
    fun duplicateLine() = delegate.duplicateLine()

    /**
     * Copies the current selection and pastes it at the right selection handle,
     * then selects the duplicated content.
     */
    fun duplicateSelection() = delegate.duplicateSelection()

    /**
     * Copies the current selection and pastes it at the right selection handle.
     *
     * @param selectDuplicate Whether to select the duplicated content.
     */
    fun duplicateSelection(selectDuplicate: Boolean) = delegate.duplicateSelection(selectDuplicate)

    /**
     * Copies the current selection, add the <code>prefix</code> to it
     * and pastes it at the right selection handle.
     *
     * @param prefix          The prefix for the selected content.
     * @param selectDuplicate Whether to select the duplicated content.
     */
    fun duplicateSelection(prefix: String, selectDuplicate: Boolean) {
        delegate.duplicateSelection(prefix, selectDuplicate)
    }

    /**
     * Selects the word at the left selection handle.
     */
    fun selectCurrentWord() = delegate.selectCurrentWord()

    /**
     * Selects the word at the given character position.
     *
     * @param line   The line.
     * @param column The column.
     */
    fun selectWord(line: Int, column: Int) = delegate.selectWord(line, column)

    /**
     * Get the range of the word at given character position.
     *
     * @param line   The line.
     * @param column The column.
     * @param useIcu Whether to use the ICU library to get word edges.
     * @return The word range.
     */
    fun getWordRange(line: Int, column: Int, useIcu: Boolean = props.useICULibToSelectWords): TextRange =
        delegate.getWordRange(line, column, useIcu)

    /**
     * The text displaying.
     *
     * **Changes to this object are expected to be done in main thread
     * due to editor limitations, while the object can be read concurrently.**
     */
    val text get() = delegate.text

    /**
     * Get extra argument set by [setText]
     */
    val extraArgument get() = delegate.extraArguments

    /**
     * Sets the text to be displayed.
     *
     * @param text               the new text you want to display
     * @param reuseContentObject If the given {@code text} is an instance of {@link Content}, reuse it.
     * @param extraArguments     Extra arguments for the document. This {@link Bundle} object is passed
     *                           to all languages and plugins in editor.
     */
    fun setText(text: CharSequence?, extraArguments: Bundle? = null, reuseContentObject: Boolean = true) {
        delegate.setText(text, reuseContentObject, extraArguments)
    }

    /**
     * Set the editor's text size in sp unit. This value must be greater than 0
     *
     * @param textSize the editor's text size in **Sp units**.
     */
    fun setTextSize(textSize: Float) = delegate.setTextSize(textSize)

    /**
     * Render ASCII Function characters
     */
    var isRenderFunctionCharacters
        get() = delegate.isRenderFunctionCharacters
        set(value) {
            delegate.isRenderFunctionCharacters = value
        }

    /**
     * Subscribe event of the given type.
     *
     * @see io.github.rosemoe.sora.event.EventManager.subscribeEvent
     */
    fun <T : Event> subscribeEvent(eventType: Class<T>, receiver: EventReceiver<T>): SubscriptionReceipt<T> =
        delegate.subscribeEvent(eventType, receiver)

    /**
     * Subscribe event of the given type.
     *
     * @see EventManager.subscribeEvent
     */
    inline fun <reified T : Event> subscribeEvent(receiver: EventReceiver<T>) =
        subscribeEvent(T::class.java, receiver)

    /**
     * Subscribe event of the given type, without [io.github.rosemoe.sora.event.Unsubscribe].
     *
     * @see EventManager.subscribeAlways
     */
    fun <T : Event> subscribeAlways(
        eventType: Class<T>,
        receiver: EventManager.NoUnsubscribeReceiver<T>
    ): SubscriptionReceipt<T> = delegate.subscribeAlways(eventType, receiver)

    /**
     * Subscribe event of the given type, without [io.github.rosemoe.sora.event.Unsubscribe].
     *
     * @see EventManager.subscribeAlways
     */
    inline fun <reified T : Event> subscribeAlways(receiver: EventManager.NoUnsubscribeReceiver<T>) =
        subscribeAlways(T::class.java, receiver)

    /**
     * Dispatch the given event
     *
     * @see EventManager.dispatchEvent
     */
    fun <T : Event> dispatchEvent(event: T) = delegate.dispatchEvent(event)

    /**
     * Create a new [EventManager] instance that can be used to subscribe events in editor,
     * as a child instance of editor.
     *
     * @return Child [EventManager] instance
     */
    fun createSubEventManager(): EventManager = delegate.createSubEventManager()

    /**
     * Check whether the editor is currently performing a format operation
     */
    val isFormatting: Boolean
        get() = delegate.isFormatting

    /**
     * Whether line numbers are shown
     */
    var isLineNumberEnabled: Boolean
        get() = delegate.isLineNumberEnabled
        set(value) {
            delegate.isLineNumberEnabled = value
        }

    /**
     * the ColorScheme object of this editor
     *
     * You can config colors of some regions, texts and highlight text
     */
    var colorScheme: EditorColorScheme
        get() = delegate.colorScheme
        set(value) {
            delegate.colorScheme = value
        }

    /**
     * Move selection to line start with scrolling
     *
     * @param line Line index to jump
     */
    fun jumpToLine(line: Int) = delegate.jumpToLine(line)

    /**
     * Mark current selection position as a point of cursor range.
     * When user taps to select another point in text, the text between the marked point and
     * newly chosen point is selected.
     *
     * @see isInLongSelect
     * @see endLongSelect
     */
    fun beginLongSelect() = delegate.beginLongSelect()

    /**
     * Checks whether long select mode is started
     */
    val isInLongSelect get() = delegate.isInLongSelect

    /**
     * Marks long select mode is end.
     * This does nothing but set the flag to false.
     */
    fun endLongSelect() = delegate.endLongSelect()

    /**
     * Rerun analysis forcibly
     */
    fun rerunAnalysis() = delegate.rerunAnalysis()

    /**
     * Get analyze result.
     *
     * **Do not make changes to it or read concurrently**
     */
    @set:UiThread
    var styles: Styles?
        get() = delegate.styles
        set(value) {
            delegate.styles = value
        }

    @UiThread
    fun updateStyles(styles: Styles, range: StyleUpdateRange?) {
        delegate.updateStyles(styles, range)
    }

    @set:UiThread
    var diagnostics: DiagnosticsContainer?
        get() = delegate.diagnostics
        set(value) {
            delegate.diagnostics = value
        }

    @set:UiThread
    var inlayHints: InlayHintsContainer?
        get() = delegate.inlayHints
        set(value) {
            delegate.inlayHints = value
        }

    @set:UiThread
    var highlightTexts: HighlightTextContainer?
        get() = delegate.highlightTexts
        set(value) {
            delegate.highlightTexts = value
        }

    /**
     * Hide auto complete window if shown
     */
    fun hideAutoCompleteWindow() = delegate.hideAutoCompleteWindow()

    /**
     * Get cursor code block index
     *
     * @return index of cursor's code block
     */
    val blockIndex get() = delegate.blockIndex

    /**
     * Display soft input method for self
     */
    fun showSoftInput() = delegate.showSoftInput()

    /**
     * Hide soft input
     */
    fun hideSoftInput() = delegate.hideSoftInput()

    /**
     * Whether the soft keyboard is enabled for this editor. Set to `true` by default.
     */
    var isSoftKeyboardEnabled: Boolean
        get() = delegate.isSoftKeyboardEnabled
        set(value) {
            delegate.isSoftKeyboardEnabled = value
        }

    /**
     * Set whether the soft keyboard should be disabled for this editor if a hardware keyboard is
     * connected to the device. Set to `true` by default.
     */
    var isDisableSoftKbdIfHardKbdAvailable: Boolean
        get() = delegate.isDisableSoftKbdIfHardKbdAvailable
        set(value) {
            delegate.isDisableSoftKbdIfHardKbdAvailable = value
        }

    /**
     * Notify input method that text has been changed for external reason
     */
    fun notifyIMEExternalCursorChange() = delegate.notifyIMEExternalCursorChange()

    /**
     * Restart the input connection.
     * Do not call this method randomly. Please refer to documentation first.
     *
     * @see android.view.inputmethod.InputConnection
     */
    fun restartInput() = host.restartInput()

    /**
     * Send cursor position in text and on screen to input method
     */
    fun updateCursor() = delegate.updateCursor()

    /**
     * Hide all built-in windows of the editor
     */
    fun hideEditorWindows() {
        textActionWindow?.dismiss()
        diagnosticTooltipWindow?.dismiss()
        delegate.hideEditorWindows()
    }

    internal fun computeScroll() {
        val scroller = delegate.touchHandler.scroller

        if (scroller.computeScrollOffset()) {
            val currX = scroller.currX
            val currY = scroller.currY
            val maxX = delegate.scrollMaxX
            val maxY = delegate.scrollMaxY

            var absorbX = 0f
            var absorbY = 0f
            var hitWall = false

            // X-AXIS COLLISION
            // If we are at/past 0, and we STARTED the fling further inside the document
            if (currX <= 0 && scroller.startX > 0) {
                absorbX = scroller.currVelocity
                hitWall = true
            }
            // If we hit the max bound, and we STARTED the fling before the max bound
            else if (currX >= maxX && scroller.startX < maxX) {
                absorbX = -scroller.currVelocity
                hitWall = true
            }

            // Y-AXIS COLLISION
            if (currY <= 0 && scroller.startY > 0) {
                absorbY = scroller.currVelocity
                hitWall = true
            } else if (currY >= maxY && scroller.startY < maxY) {
                absorbY = -scroller.currVelocity
                hitWall = true
            }

            if (hitWall) {
                scroller.forceFinished(true)

                if (delegate.overscrollCallback != null) {
                    delegate.overscrollCallback?.onAbsorb(absorbX, absorbY)
                }
            }

            host.postInvalidateOnAnimation()
        }
    }
}

/**
 * Creates and remembers a [CodeEditorState] instance across recompositions.
 *
 * Use this function to manage the state of a `CodeEditor` in Compose. The state includes
 * the text content, cursor position, scroll state, and editor configurations.
 *
 * @param initialText The initial text to be displayed in the editor. Defaults to `null`.
 */
@Composable
fun rememberCodeEditorState(initialText: String? = null): CodeEditorState {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    return remember(context, view) {
        val host = CodeEditorHostImpl(
            context = context,
            view = view,
            focusRequester = focusRequester,
            coroutineScope = coroutineScope,
            keyboardController = keyboardController
        )
        val delegate = createDelegate(host)
        host.inputConnection = EditorInputConnection(view, delegate)
        CodeEditorState(CodeEditor.DEFAULT_CURSOR_BLINK_PERIOD, host, delegate).apply {
            setText(initialText)
        }
    }
}
