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
 */
package io.github.rosemoe.sora.widget;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.collection.MutableLongLongMap;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.github.rosemoe.sora.I18nConfig;
import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage;
import io.github.rosemoe.sora.event.BuildEditorInfoEvent;
import io.github.rosemoe.sora.event.CreateContextMenuEvent;
import io.github.rosemoe.sora.event.EditorAttachStateChangeEvent;
import io.github.rosemoe.sora.event.EditorFocusChangeEvent;
import io.github.rosemoe.sora.event.EditorReleaseEvent;
import io.github.rosemoe.sora.event.Event;
import io.github.rosemoe.sora.event.EventManager;
import io.github.rosemoe.sora.event.EventReceiver;
import io.github.rosemoe.sora.event.HoverEvent;
import io.github.rosemoe.sora.event.SubscriptionReceipt;
import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRenderer;
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRendererProvider;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.LineSeparator;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.text.method.KeyMetaStates;
import io.github.rosemoe.sora.util.ClipDataUtils;
import io.github.rosemoe.sora.util.EditorHandler;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.Logger;
import io.github.rosemoe.sora.util.LongArrayList;
import io.github.rosemoe.sora.util.ThemeUtils;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent;
import io.github.rosemoe.sora.widget.component.EditorContextMenuCreator;
import io.github.rosemoe.sora.widget.layout.Layout;
import io.github.rosemoe.sora.widget.layout.ViewMeasureHelper;
import io.github.rosemoe.sora.widget.rendering.RenderContext;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.snippet.SnippetController;
import io.github.rosemoe.sora.widget.style.CursorAnimator;
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle;
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition;
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode;
import io.github.rosemoe.sora.widget.style.LineNumberTipTextProvider;
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;
import io.github.rosemoe.sora.widget.style.builtin.HandleStyleDrop;
import io.github.rosemoe.sora.widget.style.builtin.HandleStyleSideDrop;
import io.github.rosemoe.sora.widget.style.builtin.MoveCursorAnimator;

/**
 * CodeEditor is an editor that can highlight text regions by doing basic syntax analyzing
 * This project in <a href="https://github.com/Rosemoe/sora-editor">GitHub</a>
 * <p>
 * Note:
 * Row and line are different in this editor
 * When we say 'row', it means a line displayed on screen. It can be a part of a line in the text object.
 * When we say 'line', it means a real line in the original text.
 *
 * @author Rosemoe
 */
@SuppressWarnings({"unused"})
public class CodeEditor extends View implements InlayHintRendererProvider, CodeEditorHost {

    /**
     * The default text size when creating the editor object. Unit is sp.
     */
    public static final int DEFAULT_TEXT_SIZE = 18;
    /**
     * The default line info text size when creating the editor object. Unit is sp.
     */
    public static final int DEFAULT_LINE_INFO_TEXT_SIZE = 21;
    /**
     * The default cursor blinking period
     */
    public static final int DEFAULT_CURSOR_BLINK_PERIOD = 500;
    /**
     * Draw whitespace characters before line content start
     * <strong>Whitespace here only means space and tab</strong>
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_WHITESPACE_LEADING = 1;
    /**
     * Draw whitespace characters inside line content
     * <strong>Whitespace here only means space and tab</strong>
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_WHITESPACE_INNER = 1 << 1;
    /**
     * Draw whitespace characters after line content end
     * <strong>Whitespace here only means space and tab</strong>
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_WHITESPACE_TRAILING = 1 << 2;
    /**
     * Draw whitespace characters even if it is a line full of whitespaces
     * To apply this, you must enable {@link #FLAG_DRAW_WHITESPACE_LEADING}
     * <strong>Whitespace here only means space and tab</strong>
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE = 1 << 3;
    /**
     * Draw newline signals in text
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_LINE_SEPARATOR = 1 << 4;
    /**
     * Draw the tab character the same as space.
     * If not set, tab will be display to be a line.
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_TAB_SAME_AS_SPACE = 1 << 5;
    /**
     * Draw the whitespaces in selected text
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_WHITESPACE_IN_SELECTION = 1 << 6;
    /**
     * Draw soft-wrap indicator in text
     *
     * @see #setNonPrintablePaintingFlags(int)
     */
    public static final int FLAG_DRAW_SOFT_WRAP = 1 << 7;
    /*
     * Internal state identifiers of action mode
     */
    static final int ACTION_MODE_NONE = 0;
    static final int ACTION_MODE_SEARCH_TEXT = 1;
    static final int ACTION_MODE_SELECT_TEXT = 2;
    private final static Logger logger = Logger.instance("CodeEditor");
    /**
     * Digits for line number measuring
     */
    static final String NUMBER_DIGITS = "0 1 2 3 4 5 6 7 8 9";
    static final String LOG_TAG = "CodeEditor";
    static final String COPYRIGHT = "sora-editor\nCopyright (C) Rosemoe roses2020@qq.com\nThis project is distributed under the LGPL v2.1 license";

    private int downX = 0;
    private int completionWndPosMode;
    private long availableFloatArrayRegion;
    private boolean released;
    private GestureDetector basicDetector;
    private ScaleGestureDetector scaleDetector;
    private InputMethodManager inputMethodManager;
    private int startedActionMode;

    EditorInputConnection inputConnection;
    CodeEditorDelegate delegate;

    public CodeEditor(Context context) {
        this(context, null);
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.codeEditorStyle);
    }

    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(attrs, defStyleAttr, defStyleRes);
        applyAttributeSets(attrs, defStyleAttr, defStyleRes);
    }

    public CodeEditorDelegate getDelegate() {
        return delegate;
    }

    /**
     * Tries to retrieve the {@link CodeEditor} instance associated with the given delegate.
     * <p>
     * <strong>This only returns null when using Compose CodeEditor.</strong>
     *
     * @param delegate The delegate to check
     * @return The host {@link CodeEditor} if the delegate is attached to one, otherwise null.
     */
    @Nullable
    public static CodeEditor fromDelegate(@NonNull CodeEditorDelegate delegate) {
        var view = delegate.host.getAttachedView();
        if (view instanceof CodeEditor) {
            return (CodeEditor) view;
        } else {
            return null;
        }
    }

    /**
     * Get builtin component so that you can enable/disable them or do some other actions.
     *
     * @see io.github.rosemoe.sora.widget.component
     */
    @NonNull
    public <T extends EditorBuiltinComponent> T getComponent(@NonNull Class<T> clazz) {
        return delegate.getComponent(clazz);
    }

    /**
     * Replace the built-in component to the given one.
     * The new component's enabled state will extend the old one.
     *
     * @param clazz       Built-in class type. Such as {@code EditorAutoCompletion.class}
     * @param replacement The new component to apply
     * @param <T>         Type of built-in component
     */
    public <T extends EditorBuiltinComponent> void replaceComponent(@NonNull Class<T> clazz, @NonNull T replacement) {
        delegate.replaceComponent(clazz, replacement);
    }

    public void registerInlayHintRenderers(InlayHintRenderer... renderers) {
        delegate.registerInlayHintRenderers(renderers);
    }

    public void registerInlayHintRenderer(@NonNull InlayHintRenderer renderer) {
        delegate.registerInlayHintRenderer(renderer);
    }

    public void removeInlayHintRenderer(@NonNull InlayHintRenderer renderer) {
        delegate.removeInlayHintRenderer(renderer);
    }

    @NonNull
    public List<InlayHintRenderer> getInlayHintRenderers() {
        return delegate.getInlayHintRenderers();
    }

    @Nullable
    public InlayHintRenderer getInlayHintRendererForType(@Nullable String type) {
        return delegate.getInlayHintRendererForType(type);
    }

    /**
     * Get KeyMetaStates, which manages alt/shift state in editor
     */
    @NonNull
    public KeyMetaStates getKeyMetaStates() {
        return delegate.getKeyMetaStates();
    }

    /**
     * Cancel the next animation for {@link CodeEditor#ensurePositionVisible(int, int)}
     */
    protected void cancelAnimation() {
        delegate.cancelAnimation();
    }

    /**
     * Get the width of line number and divider line
     *
     * @return The width
     */
    public float measureTextRegionOffset() {
        return delegate.measureTextRegionOffset();
    }

    /**
     * Get the rect of left selection handle painted on view
     *
     * @return Descriptor of left handle
     */
    public SelectionHandleStyle.HandleDescriptor getLeftHandleDescriptor() {
        return delegate.getLeftHandleDescriptor();
    }

    /**
     * Get the rect of right selection handle painted on view
     *
     * @return Descriptor of right handle
     */
    public SelectionHandleStyle.HandleDescriptor getRightHandleDescriptor() {
        return delegate.getRightHandleDescriptor();
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    public float getOffset(int line, int column) {
        return delegate.getOffset(line, column);
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    public float getCharOffsetX(int line, int column) {
        return delegate.getCharOffsetX(line, column);
    }

    /**
     * Get the character's y offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The y offset on view
     */
    public float getCharOffsetY(int line, int column) {
        return delegate.getCharOffsetY(line, column);
    }

    /**
     * Prepare editor
     * <p>
     * Initialize variants
     */
    protected void initialize(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        Log.v(LOG_TAG, COPYRIGHT);
        delegate = new CodeEditorDelegate(this);

        delegate.setFormatTip(I18nConfig.getString(getContext(), R.string.sora_editor_editor_formatting));
        delegate.setCursorAnimator(new MoveCursorAnimator(delegate));
        setCursorBlinkPeriod(DEFAULT_CURSOR_BLINK_PERIOD);

        startedActionMode = ACTION_MODE_NONE;

        basicDetector = new GestureDetector(getContext(), delegate.touchHandler);
        basicDetector.setOnDoubleTapListener(delegate.touchHandler);
        scaleDetector = new ScaleGestureDetector(getContext(), delegate.touchHandler);

        inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        setFocusable(true);
        setFocusableInTouchMode(true);

        inputConnection = new EditorInputConnection(this, delegate);
        delegate.contextMenuCreator = new EditorContextMenuCreator(this);
        delegate.setText(null);
        setVerticalScrollBarEnabled(true);
        setHorizontalScrollBarEnabled(true);

        // Issue #41 View being highlighted when focused on Android 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false);
        }
        if (getContext() instanceof ContextThemeWrapper) {
            setEdgeEffectColor(ThemeUtils.getColorPrimary((ContextThemeWrapper) getContext()));
        }

        // Config scale detector
        scaleDetector.setQuickScaleEnabled(false);
    }

    /**
     * Apply attributes from XML
     */
    protected void applyAttributeSets(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        var array = getContext().obtainStyledAttributes(attrs, R.styleable.CodeEditor, defStyleAttr, defStyleRes);

        setHorizontalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbHorizontal));
        setHorizontalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackHorizontal));
        setVerticalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbVertical));
        setVerticalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackVertical));

        delegate.setLnPanelPositionMode(array.getInt(R.styleable.CodeEditor_lnPanelPositionMode, LineInfoPanelPositionMode.FOLLOW));
        delegate.setLnPanelPosition(array.getInt(R.styleable.CodeEditor_lnPanelPosition, LineInfoPanelPosition.CENTER));

        delegate.setDividerWidth(array.getDimension(R.styleable.CodeEditor_dividerWidth, delegate.getDividerWidth()));
        delegate.setDividerMargin(array.getDimension(R.styleable.CodeEditor_dividerMargin, this.delegate.getDividerMarginLeft()), array.getDimension(R.styleable.CodeEditor_dividerMargin, this.delegate.getDividerMarginRight()));
        delegate.setPinLineNumber(array.getBoolean(R.styleable.CodeEditor_pinLineNumber, false));

        delegate.setHighlightCurrentBlock(array.getBoolean(R.styleable.CodeEditor_highlightCurrentBlock, true));
        delegate.setHighlightCurrentLine(array.getBoolean(R.styleable.CodeEditor_highlightCurrentLine, true));
        delegate.setHighlightBracketPair(array.getBoolean(R.styleable.CodeEditor_highlightBracketPair, true));

        delegate.setLigatureEnabled(array.getBoolean(R.styleable.CodeEditor_ligatures, true));
        delegate.setLineNumberEnabled(array.getBoolean(R.styleable.CodeEditor_lineNumberVisible, delegate.isLineNumberEnabled()));
        delegate.getComponent(EditorAutoCompletion.class).setEnabled(array.getBoolean(R.styleable.CodeEditor_autoCompleteEnabled, true));
        delegate.getProps().symbolPairAutoCompletion = array.getBoolean(R.styleable.CodeEditor_symbolCompletionEnabled, true);
        delegate.setRenderFunctionCharacters(array.getBoolean(R.styleable.CodeEditor_renderFunctionChars, delegate.isRenderFunctionCharacters()));
        delegate.setScalable(array.getBoolean(R.styleable.CodeEditor_scalable, delegate.isScalable()));

        delegate.setTextSizePx(array.getDimension(R.styleable.CodeEditor_textSize, delegate.getTextSizePx()));
        setCursorBlinkPeriod(array.getInt(R.styleable.CodeEditor_cursorBlinkPeriod, delegate.getCursorBlink().period));
        delegate.setTabWidth(array.getInt(R.styleable.CodeEditor_tabWidth, delegate.getTabWidth()));

        int wordwrapMode = array.getInt(R.styleable.CodeEditor_wordwrapMode, 0);
        if (wordwrapMode != 0) {
            delegate.setWordwrap(true, wordwrapMode > 1, false);
        }

        delegate.setText(array.getString(R.styleable.CodeEditor_text));

        array.recycle();
    }

    @NonNull
    public SnippetController getSnippetController() {
        return delegate.getSnippetController();
    }

    /**
     * Get {@code DirectAccessProps} object of the editor.
     * <p>
     * You can adjust some settings of the editor by modifying the fields in the object direcly.
     */
    @NonNull
    public DirectAccessProps getProps() {
        return delegate.getProps();
    }

    /**
     * @see #setFormatTip(String)
     */
    public String getFormatTip() {
        return delegate.getFormatTip();
    }

    /**
     * Set the tip text while formatting
     */
    public void setFormatTip(@NonNull String formatTip) {
        delegate.setFormatTip(formatTip);
    }

    /**
     * Set whether line number region will scroll together with code region
     *
     * @see CodeEditor#isLineNumberPinned()
     */
    public void setPinLineNumber(boolean pinLineNumber) {
        delegate.setPinLineNumber(pinLineNumber);
    }

    /**
     * @see CodeEditor#setPinLineNumber(boolean)
     */
    public boolean isLineNumberPinned() {
        return delegate.isLineNumberPinned();
    }

    /**
     * @see CodeEditor#setFirstLineNumberAlwaysVisible(boolean)
     */
    public boolean isFirstLineNumberAlwaysVisible() {
        return delegate.isFirstLineNumberAlwaysVisible();
    }

    /**
     * Show first line number in screen in word wrap mode
     *
     * @see CodeEditor#isFirstLineNumberAlwaysVisible()
     */
    public void setFirstLineNumberAlwaysVisible(boolean enabled) {
        delegate.setFirstLineNumberAlwaysVisible(enabled);
    }

    /**
     * Inserts the given text in the editor.
     * <p>
     * This method allows you to insert texts externally to the content of editor.
     * The content of {@param text} is not checked to be exactly characters of symbols.
     * <p>
     * Note that this still works when the editor is not editable. But you should not
     * call it at that time due to possible problems, especially when {@link #getEditable()} returns
     * true but {@link #isEditable()} returns false
     * </p>
     *
     * @param text            Text to insert, usually a text of symbols
     * @param selectionOffset New selection position relative to the start of text to insert.
     *                        Ranging from 0 to text.length()
     * @throws IllegalArgumentException If the {@param selectionRegion} is invalid
     */
    public void insertText(String text, int selectionOffset) {
        delegate.insertText(text, selectionOffset);
    }

    /**
     * Set cursor blinking period
     * If zero or negative period is passed, the cursor will always be shown.
     *
     * @param period The period time of cursor blinking
     */
    public void setCursorBlinkPeriod(int period) {
        if (delegate.getCursorBlink() == null) {
            delegate.cursorBlink = new CursorBlink(delegate, period);
        } else {
            int before = delegate.getCursorBlink().period;
            delegate.getCursorBlink().setPeriod(period);
            if (before <= 0 && delegate.getCursorBlink().valid && isAttachedToWindow()) {
                postInLifecycle(delegate.getCursorBlink());
            }
        }
    }

    protected CursorBlink getCursorBlink() {
        return delegate.getCursorBlink();
    }

    /**
     * @see CodeEditor#setLigatureEnabled(boolean)
     */
    public boolean isLigatureEnabled() {
        return delegate.isLigatureEnabled();
    }

    /**
     * Enable/disable ligature of all types(except 'rlig').
     */
    public void setLigatureEnabled(boolean enabled) {
        delegate.setLigatureEnabled(enabled);
    }

    /**
     * Set font feature settings for all paints used by editor
     *
     * @see Paint#setFontFeatureSettings(String)
     */
    public void setFontFeatureSettings(String features) {
        delegate.setFontFeatureSettings(features);
    }

    /**
     * Set the style of selection handler.
     *
     * @see SelectionHandleStyle
     * @see HandleStyleDrop
     * @see HandleStyleSideDrop
     */
    public void setSelectionHandleStyle(@NonNull SelectionHandleStyle style) {
        delegate.setSelectionHandleStyle(style);
    }

    @NonNull
    public SelectionHandleStyle getHandleStyle() {
        return delegate.getHandleStyle();
    }

    /**
     * Returns whether highlight current code block
     *
     * @return This module enabled / disabled
     * @see CodeEditor#setHighlightCurrentBlock(boolean)
     */
    public boolean isHighlightCurrentBlock() {
        return delegate.isHighlightCurrentBlock();
    }

    /**
     * Whether the editor should use a different color to draw
     * the current code block line and this code block's start line and end line's
     * background.
     *
     * @param highlightCurrentBlock Enabled / Disabled this module
     */
    public void setHighlightCurrentBlock(boolean highlightCurrentBlock) {
        delegate.setHighlightCurrentBlock(highlightCurrentBlock);
    }

    /**
     * Returns whether the cursor should stick to the text row while selecting the text
     *
     * @see CodeEditor#setStickyTextSelection(boolean)
     */
    public boolean isStickyTextSelection() {
        return delegate.isStickyTextSelection();
    }

    /**
     * Whether the cursor should stick to the text row while selecting the text.
     *
     * @param stickySelection value
     */
    public void setStickyTextSelection(boolean stickySelection) {
        delegate.setStickyTextSelection(stickySelection);
    }

    /**
     * @see CodeEditor#setHighlightCurrentLine(boolean)
     */
    public boolean isHighlightCurrentLine() {
        return delegate.isHighlightCurrentLine();
    }

    /**
     * Specify whether the editor should use a different color to draw
     * the background of current line
     */
    public void setHighlightCurrentLine(boolean highlightCurrentLine) {
        delegate.setHighlightCurrentLine(highlightCurrentLine);
    }

    /**
     * Get the editor's language.
     *
     * @return EditorLanguage
     */
    @NonNull
    public Language getEditorLanguage() {
        return delegate.getEditorLanguage();
    }

    /**
     * Set the editor's language.
     * A language is a tool for auto-completion,highlight and auto indent analysis.
     *
     * @param lang New EditorLanguage for editor
     */
    public void setEditorLanguage(@Nullable Language lang) {
        delegate.setEditorLanguage(lang);
    }

    /**
     * Internal callback to check if the editor is capable of handling the given
     * keybinding {@link KeyEvent}
     *
     * @param keyCode      The keycode for the keybinding event.
     * @param ctrlPressed  Is 'Ctrl' key pressed?
     * @param shiftPressed Is 'Shift' key pressed?
     * @param altPressed   Is 'Alt' key pressed?
     * @return <code>true</code> if the editor can handle the keybinding, <code>false</code> otherwise.
     */
    protected boolean canHandleKeyBinding(int keyCode, boolean ctrlPressed, boolean shiftPressed, boolean altPressed) {
        return delegate.canHandleKeyBinding(keyCode, ctrlPressed, shiftPressed, altPressed);
    }

    /**
     * Getter
     *
     * @return The width in dp unit
     * @see CodeEditor#setBlockLineWidth(float)
     */
    public float getBlockLineWidth() {
        return delegate.getBlockLineWidth();
    }

    /**
     * Set the width of code block line
     *
     * @param dp Width in dp unit
     */
    public void setBlockLineWidth(float dp) {
        delegate.setBlockLineWidth(dp);
    }

    /**
     * @see #setWordwrap(boolean)
     * @see #setWordwrap(boolean, boolean)
     */
    public boolean isWordwrap() {
        return delegate.isWordwrap();
    }

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if anti word breaking is enabled in wordwrap mode.
     */
    public boolean isAntiWordBreaking() {
        return delegate.isAntiWordBreaking();
    }

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if RTL-based text should display from right of the widget in wordwrap mode.
     */
    public boolean isWordwrapRtlDisplaySupport() {
        return delegate.isWordwrapRtlDisplaySupport();
    }

    /**
     * Set whether text in editor should be wrapped to fit its size, with anti-word-breaking enabled
     * by default
     *
     * @param wordwrap Whether to wrap words
     * @see #setWordwrap(boolean, boolean, boolean)
     * @see #isWordwrap()
     */
    public void setWordwrap(boolean wordwrap) {
        delegate.setWordwrap(wordwrap);
    }

    /**
     * Set whether text in editor should be wrapped to fit its size
     *
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @see #isWordwrap()
     */
    public void setWordwrap(boolean wordwrap, boolean antiWordBreaking) {
        delegate.setWordwrap(wordwrap, antiWordBreaking);
    }

    /**
     * Set whether text in editor should be wrapped to fit its size
     *
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @param supportRtlRow    Allow rows with RTL base direction to display from the right side of widget
     * @see #isWordwrap()
     */
    public void setWordwrap(boolean wordwrap, boolean antiWordBreaking, boolean supportRtlRow) {
        delegate.setWordwrap(wordwrap, antiWordBreaking, supportRtlRow);
    }

    /**
     * @see #setCursorAnimationEnabled(boolean)
     */
    public boolean isCursorAnimationEnabled() {
        return delegate.isCursorAnimationEnabled();
    }

    /**
     * Set cursor animation enabled
     */
    public void setCursorAnimationEnabled(boolean enabled) {
        delegate.setCursorAnimationEnabled(enabled);
    }

    /**
     * @see #setCursorAnimator(CursorAnimator)
     */
    public CursorAnimator getCursorAnimator() {
        return delegate.getCursorAnimator();
    }

    /**
     * Set cursor animation
     *
     * @see CursorAnimator
     * @see #getCursorAnimator()
     * @see #setCursorAnimationEnabled(boolean)  for disabling the animation
     */
    public void setCursorAnimator(@NonNull CursorAnimator cursorAnimator) {
        delegate.setCursorAnimator(cursorAnimator);
    }

    /**
     * Whether display vertical scroll bar when scrolling
     *
     * @param enabled Enabled / disabled
     */
    public void setScrollBarEnabled(boolean enabled) {
        delegate.setScrollBarEnabled(enabled);
    }

    public void setHorizontalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        delegate.getRenderer().setHorizontalScrollbarThumbDrawable(drawable);
    }

    @Nullable
    public Drawable getHorizontalScrollbarThumbDrawable() {
        return delegate.getRenderer().getHorizontalScrollbarThumbDrawable();
    }

    public void setHorizontalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        delegate.getRenderer().setHorizontalScrollbarTrackDrawable(drawable);
    }

    @Nullable
    public Drawable getHorizontalScrollbarTrackDrawable() {
        return delegate.getRenderer().getHorizontalScrollbarTrackDrawable();
    }

    public void setVerticalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        delegate.getRenderer().setVerticalScrollbarThumbDrawable(drawable);
    }

    @Nullable
    public Drawable getVerticalScrollbarThumbDrawable() {
        return delegate.getRenderer().getVerticalScrollbarThumbDrawable();
    }

    public void setVerticalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        delegate.getRenderer().setVerticalScrollbarTrackDrawable(drawable);
    }

    @Nullable
    public Drawable getVerticalScrollbarTrackDrawable() {
        return delegate.getRenderer().getVerticalScrollbarTrackDrawable();
    }

    /**
     * @return Enabled / disabled
     * @see CodeEditor#setDisplayLnPanel(boolean)
     */
    public boolean isDisplayLnPanel() {
        return delegate.isDisplayLnPanel();
    }

    /**
     * Whether display the line number panel beside vertical scroll bar
     * when the scroll bar is touched by user
     *
     * @param displayLnPanel Enabled / disabled
     */
    public void setDisplayLnPanel(boolean displayLnPanel) {
        delegate.setDisplayLnPanel(displayLnPanel);
    }

    /**
     * @return LineInfoPanelPosition.FOLLOW or LineInfoPanelPosition.FIXED
     * @see CodeEditor#setLnPanelPosition(int)
     */
    public int getLnPanelPositionMode() {
        return delegate.getLnPanelPositionMode();
    }

    /**
     * Set display position mode the line number panel beside vertical scroll bar
     *
     * @param mode Default LineInfoPanelPosition.FOLLOW
     * @see io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode
     */
    public void setLnPanelPositionMode(int mode) {
        delegate.setLnPanelPositionMode(mode);
    }

    /**
     * @return position
     * @see CodeEditor#setLnPanelPosition(int)
     */
    public int getLnPanelPosition() {
        return delegate.getLnPanelPosition();
    }

    /**
     * Set display position the line number panel beside vertical scroll bar <br/>
     * Only TOP,CENTER and BOTTOM will be effective when position mode is follow.
     *
     * @param position default TOP|RIGHT
     * @see io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
     */
    public void setLnPanelPosition(int position) {
        delegate.setLnPanelPosition(position);
    }

    /**
     * @see CodeEditor#setLineNumberTipTextProvider(LineNumberTipTextProvider)
     */
    public LineNumberTipTextProvider getLineNumberTipTextProvider() {
        return delegate.getLineNumberTipTextProvider();
    }

    /**
     * Set the tip text before line number for the line number panel
     */
    public void setLineNumberTipTextProvider(LineNumberTipTextProvider provider) {
        delegate.setLineNumberTipTextProvider(provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHorizontalScrollBarEnabled() {
        return delegate.horizontalScrollBarEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        this.delegate.horizontalScrollBarEnabled = horizontalScrollBarEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVerticalScrollBarEnabled() {
        return delegate.verticalScrollBarEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        this.delegate.verticalScrollBarEnabled = verticalScrollBarEnabled;
    }

    /**
     * Get the rect of insert cursor handle on view
     *
     * @return Rect of insert handle
     */
    public SelectionHandleStyle.HandleDescriptor getInsertHandleDescriptor() {
        return delegate.getInsertHandleDescriptor();
    }

    /**
     * Get text size in pixel unit
     *
     * @return Text size in pixel unit
     * @see CodeEditor#setTextSize(float)
     * @see CodeEditor#setTextSizePx(float)
     */
    @Px
    public float getTextSizePx() {
        return delegate.getTextSizePx();
    }

    /**
     * Set text size in pixel unit
     *
     * @param size Text size in pixel unit
     */
    public void setTextSizePx(@Px float size) {
        delegate.setTextSizePx(size);
    }

    /**
     * Set text size directly without creating layout or invalidating view
     *
     * @param size Text size in pixel unit
     */
    protected void setTextSizePxDirect(@Px float size) {
        delegate.setTextSizePxDirect(size);
    }

    protected void requestLayoutIfNeeded() {
        delegate.requestLayoutIfNeeded();
    }

    protected void checkForRelayout() {
        delegate.checkForRelayout();
    }

    public EditorRenderer getRenderer() {
        return delegate.getRenderer();
    }

    public RenderContext getRenderContext() {
        return delegate.getRenderContext();
    }

    public Paint.FontMetricsInt getLineNumberMetrics() {
        return delegate.getLineNumberMetrics();
    }

    /**
     * Whether non-printable is to be drawn
     */
    protected boolean shouldInitializeNonPrintable() {
        return delegate.shouldInitializeNonPrintable();
    }

    /**
     * @see #setHardwareAcceleratedDrawAllowed(boolean)
     */
    public boolean isHardwareAcceleratedDrawAllowed() {
        return delegate.isHardwareAcceleratedDrawAllowed();
    }

    /**
     * Set whether allow the editor to use RenderNode to draw its text.
     * Enabling this can cause more memory usage, but the editor can display text
     * much quicker.
     * However, only when hardware accelerate is enabled on this view can the switch
     * make a difference.
     */
    public void setHardwareAcceleratedDrawAllowed(boolean acceleratedDraw) {
        delegate.setHardwareAcceleratedDrawAllowed(acceleratedDraw);
    }

    /**
     * As the name is, we find where leading spaces end and trailing spaces start
     *
     * @param line The line to search
     */
    protected long findLeadingAndTrailingWhitespacePos(ContentLine line) {
        return delegate.findLeadingAndTrailingWhitespacePos(line);
    }

    /**
     * A quick method to predicate whitespace character
     */
    private boolean isWhitespace(char ch) {
        return delegate.isWhitespace(ch);
    }

    /**
     * Get matched text regions on the given line
     *
     * @param line      Target line
     * @param positions Output start positions
     */
    protected void computeMatchedPositions(int line, LongArrayList positions) {
        delegate.computeMatchedPositions(line, positions);
    }


    protected void computeHighlightPositions(int line, MutableLongLongMap positions) {
        delegate.computeHighlightPositions(line, positions);
    }

    /**
     * Get the color of EdgeEffect
     *
     * @return The color of EdgeEffect.
     */
    public int getEdgeEffectColor() {
        return delegate.getVerticalEdgeEffect().getColor();
    }

    /**
     * Set the color of EdgeEffect
     *
     * @param color The color of EdgeEffect
     */
    public void setEdgeEffectColor(int color) {
        delegate.getVerticalEdgeEffect().setColor(color);
        delegate.getHorizontalEdgeEffect().setColor(color);
    }

    /**
     * Get the layout of editor
     */
    @UnsupportedUserUsage
    public Layout getLayout() {
        return delegate.getLayout();
    }

    /**
     * Get EdgeEffect for vertical direction
     *
     * @return EdgeEffect
     */
    protected EdgeEffect getVerticalEdgeEffect() {
        return delegate.getVerticalEdgeEffect();
    }

    /**
     * Get EdgeEffect for horizontal direction
     *
     * @return EdgeEffect
     */
    protected EdgeEffect getHorizontalEdgeEffect() {
        return delegate.getHorizontalEdgeEffect();
    }

    /**
     * Find the smallest code block that cursor is in
     *
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    private int findCursorBlock() {
        return delegate.findCursorBlock();
    }

    /**
     * Find the cursor code block internal
     *
     * @param blocks Current code blocks
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    private int findCursorBlock(List<CodeBlock> blocks) {
        return delegate.findCursorBlock(blocks);
    }

    /**
     * @see #findCursorBlock()
     */
    public int getCurrentCursorBlock() {
        return delegate.getCurrentCursorBlock();
    }

    /**
     * Find the first code block that maybe seen on screen Because the code blocks is sorted by its
     * end line position we can use binary search to quicken this process in order to decrease the
     * time we use on finding
     *
     * @param firstVis The first visible line
     * @param blocks   Current code blocks
     * @return The index of the block we found or <code>-1</code> if no code block is found.
     */
    int binarySearchEndBlock(int firstVis, List<CodeBlock> blocks) {
        return delegate.binarySearchEndBlock(firstVis, blocks);
    }


    /**
     * Get spans on the given line
     */
    @NonNull
    public List<Span> getSpansForLine(int line) {
        return delegate.getSpansForLine(line);
    }

    /**
     * Get the width of line number region (include line number margin)
     *
     * @return width of line number region
     */
    public float measureLineNumber() {
        return delegate.measureLineNumber();
    }

    protected void createLayout() {
        delegate.createLayout();
    }

    /**
     * Create layout for text
     */
    protected void createLayout(boolean clearWordwrapCache) {
        delegate.createLayout(clearWordwrapCache);
    }

    /**
     * Indents the selected lines. Does nothing if the text is not selected.
     */
    public void indentSelection() {
        delegate.indentSelection();
    }

    /**
     * Indents the lines. Does nothing if the <code>onlyIfSelected</code> is <code>true</code> and
     * no text is selected.
     *
     * @param onlyIfSelected Set to <code>true</code> if lines must be indented only if the text is
     *                       selected.
     */
    public void indentLines(boolean onlyIfSelected) {
        delegate.indentLines(onlyIfSelected);
    }

    /**
     * Removes indentation from the start of the selected lines. If the text is not selected, or if
     * the start and end selection is on the same line, only the line at the cursor position is
     * unindented.
     */
    public void unindentSelection() {
        delegate.unindentSelection();
    }

    /**
     * Commit a tab to cursor
     */
    protected void commitTab() {
        delegate.commitTab();
    }

    /**
     * Indents the line if text is not selected and the cursor is at the start of the line. Inserts
     * an indentation string otherwise.
     */
    public void indentOrCommitTab() {
        delegate.indentOrCommitTab();
    }

    /**
     * Creates the string to insert when <code>KEYCODE_TAB</code> key event is received from the IME.
     *
     * @return The string to insert for tab character.
     */
    protected String createTabString() {
        return delegate.createTabString();
    }

    /**
     * Update the information of cursor
     * Such as the position of cursor on screen(For input method that can go to any position on screen like PC input method)
     *
     * @return The offset x of right cursor on view
     */
    public float updateCursorAnchor() {
        return delegate.updateCursorAnchor();
    }

    /**
     * Delete text before cursor or selected text (if there is)
     */
    public void deleteText() {
        delegate.deleteText();
    }

    /**
     * Commit text to the content from IME
     */
    public void commitText(@NonNull CharSequence text) {
        delegate.commitText(text);
    }

    /**
     * Commit text at current state from IME
     *
     * @param text            Text commit by InputConnection
     * @param applyAutoIndent Apply automatic indentation
     */
    public void commitText(@NonNull CharSequence text, boolean applyAutoIndent) {
        delegate.commitText(text, applyAutoIndent);
    }

    /**
     * Commit text with given options
     *
     * @param text                  Text commit by InputConnection
     * @param applyAutoIndent       Apply automatic indentation
     * @param applySymbolCompletion Apply symbol surroundings and completions
     */
    public void commitText(@NonNull CharSequence text, boolean applyAutoIndent, boolean applySymbolCompletion) {
        delegate.commitText(text, applyAutoIndent, applySymbolCompletion);
    }

    /**
     * @see #setLineInfoTextSize(float)
     */
    public float getLineInfoTextSize() {
        return delegate.getLineInfoTextSize();
    }

    /**
     * Set text size for line info panel
     *
     * @param size Text size for line information, <strong>unit is SP</strong>
     */
    public void setLineInfoTextSize(float size) {
        delegate.setLineInfoTextSize(size);
    }

    /**
     * @see #setNonPrintablePaintingFlags(int)
     * @see #FLAG_DRAW_WHITESPACE_LEADING
     * @see #FLAG_DRAW_WHITESPACE_INNER
     * @see #FLAG_DRAW_WHITESPACE_TRAILING
     * @see #FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
     * @see #FLAG_DRAW_LINE_SEPARATOR
     * @see #FLAG_DRAW_WHITESPACE_IN_SELECTION
     */
    public int getNonPrintablePaintingFlags() {
        return delegate.getNonPrintablePaintingFlags();
    }

    /**
     * Sets non-printable painting flags.
     * Specify where they should be drawn and some other properties.
     * <p>
     * Flags can be mixed.
     *
     * @param flags Flags
     * @see #FLAG_DRAW_WHITESPACE_LEADING
     * @see #FLAG_DRAW_WHITESPACE_INNER
     * @see #FLAG_DRAW_WHITESPACE_TRAILING
     * @see #FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
     * @see #FLAG_DRAW_LINE_SEPARATOR
     * @see #FLAG_DRAW_WHITESPACE_IN_SELECTION
     * @see #FLAG_DRAW_SOFT_WRAP
     */
    public void setNonPrintablePaintingFlags(int flags) {
        delegate.setNonPrintablePaintingFlags(flags);
    }

    public boolean hasComposingText() {
        return delegate.hasComposingText();
    }

    /**
     * Make the selection visible
     */
    public void ensureSelectionVisible() {
        delegate.ensureSelectionVisible();
    }

    /**
     * Make the given character position visible
     *
     * @param line   Line in text
     * @param column Column in text
     */
    public void ensurePositionVisible(int line, int column) {
        delegate.ensurePositionVisible(line, column);
    }

    /**
     * Make the given character position visible
     *
     * @param line        Line in text
     * @param column      Column in text
     * @param noAnimation true if no animation should be applied
     */
    public void ensurePositionVisible(int line, int column, boolean noAnimation) {
        delegate.ensurePositionVisible(line, column, noAnimation);
    }

    /**
     * Whether there is clip
     *
     * @return whether clip in clip board
     */
    public boolean hasClip() {
        return delegate.hasClip();
    }

    /**
     * Get 1dp = ?px
     *
     * @return 1dp in pixel
     */
    public float getDpUnit() {
        return delegate.getDpUnit();
    }

    /**
     * Get scroller from EventHandler
     * You would better not use it for your own scrolling
     *
     * @return The scroller
     */
    public EditorScroller getScroller() {
        return delegate.getScroller();
    }

    /**
     * Checks whether the position is over max Y position
     *
     * @param posOnScreen Y position on view
     * @return Whether over max Y
     */
    public boolean isOverMaxY(float posOnScreen) {
        return delegate.isOverMaxY(posOnScreen);
    }

    /**
     * Check if the point on editor view, is inside text region on that row
     */
    public boolean isScreenPointOnText(float x, float y) {
        return delegate.isScreenPointOnText(x, y);
    }

    /**
     * Determine character position using positions in scroll coordinate
     *
     * @param xOffset Horizontal position in scroll coordinate
     * @param yOffset Vertical position in scroll coordinate
     * @return IntPair. first is line and second is column
     * @see IntPair
     */
    public long getPointPosition(float xOffset, float yOffset) {
        return delegate.getPointPosition(xOffset, yOffset);
    }

    /**
     * Determine character position using positions on view
     *
     * @param x X on view
     * @param y Y on view
     * @return IntPair. first is line and second is column
     * @see IntPair
     */
    public long getPointPositionOnScreen(float x, float y) {
        return delegate.getPointPositionOnScreen(x, y);
    }

    public Layout.VisualLocation getPointVisualPosition(float xOffset, float yOffset) {
        return delegate.getPointVisualPosition(xOffset, yOffset);
    }

    public Layout.VisualLocation getPointVisualPositionOnScreen(float x, float y) {
        return delegate.getPointVisualPositionOnScreen(x, y);
    }

    /**
     * Get max scroll y
     *
     * @return max scroll y
     */
    public int getScrollMaxY() {
        return delegate.getScrollMaxY();
    }

    /**
     * Get max scroll x
     *
     * @return max scroll x
     */
    public int getScrollMaxX() {
        return delegate.getScrollMaxX();
    }

    /**
     * Set the factor of extra space in vertical direction. The factor is multiplied with editor
     * height to compute the extra space of vertical viewport. Specially, when factor is zero, no
     * extra space is added.
     *
     * @param extraSpaceFactor the factor. 0.5 by default.
     * @throws IllegalArgumentException if the factor is negative or bigger than 1.0f
     * @see #getVerticalExtraSpaceFactor()
     */
    public void setVerticalExtraSpaceFactor(float extraSpaceFactor) {
        delegate.setVerticalExtraSpaceFactor(extraSpaceFactor);
    }

    /**
     * Get the factor used to compute extra space of vertical viewport.
     *
     * @see #setVerticalExtraSpaceFactor(float)
     */
    public float getVerticalExtraSpaceFactor() {
        return delegate.getVerticalExtraSpaceFactor();
    }

    /**
     * Get EditorSearcher
     *
     * @return EditorSearcher
     */
    public EditorSearcher getSearcher() {
        return delegate.getSearcher();
    }

    /**
     * Set selection around the given position
     * It will try to set selection as near as possible (Exactly the position if that position exists)
     */
    protected void setSelectionAround(int line, int column) {
        delegate.setSelectionAround(line, column);
    }

    /**
     * Format text Async
     *
     * @return Whether the format task is scheduled
     */
    public synchronized boolean formatCodeAsync() {
        return delegate.formatCodeAsync();
    }

    /**
     * Format text in the given region.
     * <p>
     * Note: Make sure the given positions are valid (line, column and index). Typically, you should
     * obtain a position by an object of {@link io.github.rosemoe.sora.text.Indexer}
     *
     * @param start Start position created by Indexer
     * @param end   End position created by Indexer
     * @return Whether the format task is scheduled
     */
    public synchronized boolean formatCodeAsync(CharPosition start, CharPosition end) {
        return delegate.formatCodeAsync(start, end);
    }

    /**
     * Get the cursor range of editor
     */
    public TextRange getCursorRange() {
        return delegate.getCursorRange();
    }

    /**
     * If any text is selected
     */
    public boolean isTextSelected() {
        return delegate.isTextSelected();
    }

    /**
     * Get tab width
     *
     * @return tab width
     */
    public int getTabWidth() {
        return delegate.getTabWidth();
    }

    /**
     * Set tab width
     *
     * @param width tab width compared to space
     */
    public void setTabWidth(int width) {
        delegate.setTabWidth(width);
    }

    /**
     * Set max and min text size that can be used by user zooming.
     * <p>
     * Unit is px.
     */
    public void setScaleTextSizes(float minSize, float maxSize) {
        delegate.setScaleTextSizes(minSize, maxSize);
    }

    /**
     * @see CodeEditor#setInterceptParentHorizontalScrollIfNeeded(boolean)
     */
    public boolean isInterceptParentHorizontalScrollEnabled() {
        return delegate.isInterceptParentHorizontalScrollEnabled();
    }

    /**
     * When the parent is a scrollable view group,
     * request it not to allow horizontal scrolling to be intercepted.
     * Until the code cannot scroll horizontally
     *
     * @param forceHorizontalScrollable Whether force horizontal scrolling
     */
    public void setInterceptParentHorizontalScrollIfNeeded(boolean forceHorizontalScrollable) {
        delegate.setInterceptParentHorizontalScrollIfNeeded(forceHorizontalScrollable);
    }

    /**
     * @see #setHighlightBracketPair(boolean)
     */
    public boolean isHighlightBracketPair() {
        return delegate.isHighlightBracketPair();
    }

    /**
     * Whether to highlight brackets pairs
     */
    public void setHighlightBracketPair(boolean highlightBracketPair) {
        delegate.setHighlightBracketPair(highlightBracketPair);
    }

    /**
     * Set line separator when new lines are created in editor (only texts from IME. texts from clipboard
     * or other strategies are not encountered). Must not be{@link LineSeparator#NONE}
     *
     * @see #getLineSeparator()
     * @see LineSeparator
     */
    public void setLineSeparator(@NonNull LineSeparator lineSeparator) {
        delegate.setLineSeparator(lineSeparator);
    }

    /**
     * @see #setLineSeparator(LineSeparator)
     */
    public LineSeparator getLineSeparator() {
        return delegate.getLineSeparator();
    }

    /**
     * @see CodeEditor#setInputType(int)
     */
    public int getInputType() {
        return delegate.getInputType();
    }

    /**
     * Specify input type for the editor
     * <p>
     * Zero for default input type
     *
     * @see EditorInfo#inputType
     */
    public void setInputType(int inputType) {
        delegate.setInputType(inputType);
    }

    /**
     * Undo last action
     */
    public void undo() {
        delegate.undo();
    }

    /**
     * Redo last action
     */
    public void redo() {
        delegate.redo();
    }

    /**
     * Checks whether we can undo
     *
     * @return true if we can undo
     */
    public boolean canUndo() {
        return delegate.canUndo();
    }

    /**
     * Checks whether we can redo
     *
     * @return true if we can redo
     */
    public boolean canRedo() {
        return delegate.canRedo();
    }

    /**
     * @return Enabled/Disabled
     * @see CodeEditor#setUndoEnabled(boolean)
     */
    public boolean isUndoEnabled() {
        return delegate.isUndoEnabled();
    }

    /**
     * Enable / disabled undo manager
     *
     * @param enabled Enable/Disable
     */
    public void setUndoEnabled(boolean enabled) {
        delegate.setUndoEnabled(enabled);
    }

    public DiagnosticIndicatorStyle getDiagnosticIndicatorStyle() {
        return delegate.getDiagnosticIndicatorStyle();
    }

    public void setDiagnosticIndicatorStyle(@NonNull DiagnosticIndicatorStyle
                                                    diagnosticIndicatorStyle) {
        delegate.setDiagnosticIndicatorStyle(diagnosticIndicatorStyle);
    }

    /**
     * Start search action mode
     */
    public void beginSearchMode() {
        delegate.beginSearchMode();
    }

    /**
     * Get {@link EditorTouchEventHandler} of the editor
     */
    public EditorTouchEventHandler getEventHandler() {
        return delegate.getEventHandler();
    }

    /**
     * @return Margin left of divider line
     * @see CodeEditor#setDividerMargin(float, float)
     */
    @Px
    public float getDividerMarginLeft() {
        return delegate.getDividerMarginLeft();
    }

    /**
     * @return Margin right of divider line
     * @see CodeEditor#setDividerMargin(float, float)
     */
    @Px
    public float getDividerMarginRight() {
        return delegate.getDividerMarginRight();
    }

    /**
     * Set divider line's left and right margin
     *
     * @param marginLeft  Margin left for divider line
     * @param marginRight Margin right for divider line
     */
    public void setDividerMargin(@Px float marginLeft, @Px float marginRight) {
        delegate.setDividerMargin(marginLeft, marginRight);
    }

    /**
     * Set divider line's left and right margin
     *
     * @param margin Margin left and right for divider line
     */
    public void setDividerMargin(@Px float margin) {
        delegate.setDividerMargin(margin);
    }

    /**
     * Set line number margin left
     */
    public void setLineNumberMarginLeft(@Px float lineNumberMarginLeft) {
        delegate.setLineNumberMarginLeft(lineNumberMarginLeft);
    }

    /**
     * @see #setLineNumberMarginLeft(float)
     */
    @Px
    public float getLineNumberMarginLeft() {
        return delegate.getLineNumberMarginLeft();
    }

    /**
     * @return Width of divider line
     * @see CodeEditor#setDividerWidth(float)
     */
    @Px
    public float getDividerWidth() {
        return delegate.getDividerWidth();
    }

    /**
     * Set divider line's width
     *
     * @param dividerWidth Width of divider line
     */
    public void setDividerWidth(@Px float dividerWidth) {
        delegate.setDividerWidth(dividerWidth);
    }

    /**
     * @return Typeface of line number
     * @see CodeEditor#setTypefaceLineNumber(Typeface)
     */
    public Typeface getTypefaceLineNumber() {
        return delegate.getTypefaceLineNumber();
    }

    /**
     * Set line number's typeface
     *
     * @param typefaceLineNumber New typeface
     */
    public void setTypefaceLineNumber(Typeface typefaceLineNumber) {
        delegate.setTypefaceLineNumber(typefaceLineNumber);
    }

    /**
     * @return Typeface of text
     * @see CodeEditor#setTypefaceText(Typeface)
     */
    public Typeface getTypefaceText() {
        return delegate.getTypefaceText();
    }

    /**
     * Set text's typeface
     *
     * @param typefaceText New typeface
     */
    public void setTypefaceText(Typeface typefaceText) {
        delegate.setTypefaceText(typefaceText);
    }

    /**
     * @see #setTextScaleX(float)
     */
    public float getTextScaleX() {
        return delegate.getTextScaleX();
    }

    /**
     * Set text scale x of Paint
     *
     * @see Paint#setTextScaleX(float)
     * @see #getTextScaleX()
     */
    public void setTextScaleX(float textScaleX) {
        delegate.setTextScaleX(textScaleX);
    }

    /**
     * @see #setTextLetterSpacing(float)
     */
    public float getTextLetterSpacing() {
        return delegate.getTextLetterSpacing();
    }

    /**
     * Set letter spacing of Paint
     *
     * @see Paint#setLetterSpacing(float)
     * @see #getTextLetterSpacing()
     */
    public void setTextLetterSpacing(float textLetterSpacing) {
        delegate.setTextLetterSpacing(textLetterSpacing);
    }

    /**
     * @return Line number align
     * @see CodeEditor#setLineNumberAlign(Paint.Align)
     */
    public Paint.Align getLineNumberAlign() {
        return delegate.getLineNumberAlign();
    }

    /**
     * Set line number align
     *
     * @param align Align for line number
     */
    public void setLineNumberAlign(Paint.Align align) {
        delegate.setLineNumberAlign(align);
    }

    /**
     * Width for insert cursor
     *
     * @param width Cursor width
     */
    public void setCursorWidth(@Px float width) {
        delegate.setCursorWidth(width);
    }

    @Px
    public float getInsertSelectionWidth() {
        return delegate.getInsertSelectionWidth();
    }

    /**
     * Border width for text border
     */
    public void setTextBorderWidth(@Px float width) {
        delegate.setTextBorderWidth(width);
    }

    /**
     * @see #setTextBorderWidth(float)
     */
    @Px
    public float getTextBorderWidth() {
        return delegate.getTextBorderWidth();
    }

    /**
     * Get text cursor.
     * <p>
     * Always set selection position by {@link #setSelection} or {@link #setSelectionRegion}.
     * Do not modify the object returned.
     *
     * @return Cursor of text
     */
    public Cursor getCursor() {
        return delegate.getCursor();
    }

    /**
     * Get line count
     *
     * @return line count
     */
    public int getLineCount() {
        return delegate.getLineCount();
    }

    /**
     * Get first visible line on screen
     *
     * @return first visible line
     */
    public int getFirstVisibleLine() {
        return delegate.getFirstVisibleLine();
    }

    /**
     * Get first visible row on screen
     *
     * @return first visible row
     */
    public int getFirstVisibleRow() {
        return delegate.getFirstVisibleRow();
    }

    /**
     * Get last visible row on screen.
     *
     * @return last visible row
     */
    public int getLastVisibleRow() {
        return delegate.getLastVisibleRow();
    }

    /**
     * Get last visible line on screen
     *
     * @return last visible line
     */
    public int getLastVisibleLine() {
        return delegate.getLastVisibleLine();
    }

    /**
     * Checks whether this row is visible on screen
     *
     * @param row Row to check
     * @return Whether visible
     */
    public boolean isRowVisible(int row) {
        return delegate.isRowVisible(row);
    }

    /**
     * Sets line spacing for this TextView.  Each line other than the last line will have its height
     * multiplied by {@code mult} and have {@code add} added to it.
     *
     * @param add  The value in pixels that should be added to each line other than the last line.
     *             This will be applied after the multiplier
     * @param mult The value by which each line height other than the last line will be multiplied
     *             by
     */
    public void setLineSpacing(float add, float mult) {
        delegate.setLineSpacing(add, mult);
    }

    /**
     * Gets the line spacing extra space
     *
     * @return the extra space that is added to the height of each lines of this TextView.
     * @see #setLineSpacing(float, float)
     * @see #getLineSpacingMultiplier()
     */
    public float getLineSpacingExtra() {
        return delegate.getLineSpacingExtra();
    }

    /**
     * @param lineSpacingExtra The value in pixels that should be added to each line other than the last line.
     *                         *            This will be applied after the multiplier
     */
    public void setLineSpacingExtra(float lineSpacingExtra) {
        delegate.setLineSpacingExtra(lineSpacingExtra);
    }

    /**
     * @return the value by which each line's height is multiplied to get its actual height.
     * @see #setLineSpacingMultiplier(float)
     */
    public float getLineSpacingMultiplier() {
        return delegate.getLineSpacingMultiplier();
    }

    /**
     * @param lineSpacingMultiplier The value by which each line height other than the last line will be multiplied
     *                              *             by. Default 1.0f
     */
    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        delegate.setLineSpacingMultiplier(lineSpacingMultiplier);
    }

    /**
     * Get actual line spacing in pixels.
     */
    public int getLineSpacingPixels() {
        return delegate.getLineSpacingPixels();
    }

    /**
     * Get baseline directly
     *
     * @param row Row
     * @return baseline y offset
     */
    public int getRowBaseline(int row) {
        return delegate.getRowBaseline(row);
    }

    /**
     * Get row height
     *
     * @return height of single row
     */
    public int getRowHeight() {
        return delegate.getRowHeight();
    }

    /**
     * Get row top y offset
     *
     * @param row Row
     * @return top y offset
     */
    public int getRowTop(int row) {
        return delegate.getRowTop(row);
    }

    /**
     * Get row bottom y offset
     *
     * @param row Row
     * @return Bottom y offset
     */
    public int getRowBottom(int row) {
        return delegate.getRowBottom(row);
    }

    /**
     * Get the top of text in target row
     */
    public int getRowTopOfText(int row) {
        return delegate.getRowTopOfText(row);
    }

    /**
     * Get the bottom of text in target row
     */
    public int getRowBottomOfText(int row) {
        return delegate.getRowBottomOfText(row);
    }

    /**
     * Get the height of text in row
     */
    public int getRowHeightOfText() {
        return delegate.getRowHeightOfText();
    }

    /**
     * Get scroll x
     *
     * @return scroll x
     */
    public int getOffsetX() {
        return delegate.getOffsetX();
    }

    /**
     * Get scroll y
     *
     * @return scroll y
     */
    public int getOffsetY() {
        return delegate.getOffsetY();
    }

    /**
     * Indicate whether the layout is working
     */
    @UnsupportedUserUsage
    public void setLayoutBusy(boolean busy) {
        delegate.setLayoutBusy(busy);
    }

    /**
     * Check whether the editor is actually editable. This is not only related to user
     * property 'editable', but also editor states. When the editor is busy at initializing
     * its layout or awaiting the result of format, it is also not editable.
     * <p>
     * Do not modify the text externally in editor when this method returns false.
     *
     * @return Whether the editor is editable, actually.
     * @see CodeEditor#setEditable(boolean)
     * @see CodeEditor#setLayoutBusy(boolean)
     * @see #isFormatting()
     */
    public boolean isEditable() {
        return delegate.isEditable();
    }

    /**
     * @see #setEditable(boolean)
     */
    public boolean getEditable() {
        return delegate.getEditable();
    }

    /**
     * Set whether text can be edited
     *
     * @param editable Editable
     */
    public void setEditable(boolean editable) {
        delegate.setEditable(editable);
    }

    /**
     * @return Whether allow scaling
     * @see CodeEditor#setScalable(boolean)
     */
    public boolean isScalable() {
        return delegate.isScalable();
    }

    /**
     * Allow scale text size by thumb
     *
     * @param scale Whether allow
     */
    public void setScalable(boolean scale) {
        delegate.setScalable(scale);
    }

    public boolean isBlockLineEnabled() {
        return delegate.isBlockLineEnabled();
    }

    public void setBlockLineEnabled(boolean enabled) {
        delegate.setBlockLineEnabled(enabled);
    }

    /**
     * Begin a rejection on composing texts
     */
    public void beginComposingTextRejection() {
        delegate.beginComposingTextRejection();
    }

    /**
     * If the editor accepts composing text now, according to composing text rejection count
     */
    public boolean acceptsComposingText() {
        return delegate.acceptsComposingText();
    }

    /**
     * End a rejection on composing texts
     */
    public void endComposingTextRejection() {
        delegate.endComposingTextRejection();
    }

    /**
     * Check if there is a mouse inside editor, hovering
     */
    public boolean hasMouseHovering() {
        return delegate.hasMouseHovering();
    }

    /**
     * Check if there is a mouse inside editor with any button pressed
     */
    public boolean hasMousePressed() {
        return delegate.hasMousePressed();
    }

    /**
     * Check if editor is in mouse mode.
     *
     * @see DirectAccessProps#mouseMode
     */
    public boolean isInMouseMode() {
        return delegate.isInMouseMode();
    }

    /**
     * Get the target cursor to move when shift is pressed
     */
    protected CharPosition getSelectingTarget() {
        return delegate.getSelectingTarget();
    }

    /**
     * Make sure the moving selection is visible
     */
    protected void ensureSelectingTargetVisible() {
        delegate.ensureSelectingTargetVisible();
    }

    protected void ensureSelectionAnchorAvailable() {
        delegate.ensureSelectionAnchorAvailable();
    }

    /**
     * Move or extend selection, according to {@code extend} param.
     *
     * @param extend True if you want to extend selection.
     */
    public void moveOrExtendSelection(@NonNull SelectionMovement movement, boolean extend) {
        delegate.moveOrExtendSelection(movement, extend);
    }

    /**
     * Extend the selection, based on the selection anchor (select text)
     */
    public void extendSelection(@NonNull SelectionMovement movement) {
        delegate.extendSelection(movement);
    }

    /**
     * Move the selection. Selected text will be de-selected.
     */
    public void moveSelection(@NonNull SelectionMovement movement) {
        delegate.moveSelection(movement);
    }

    /**
     * Move selection to given position
     *
     * @param line   The line to move
     * @param column The column to move
     */
    public void setSelection(int line, int column) {
        delegate.setSelection(line, column);
    }

    /**
     * Move selection to given position
     *
     * @param line   The line to move
     * @param column The column to move
     */
    public void setSelection(int line, int column, int cause) {
        delegate.setSelection(line, column, cause);
    }

    /**
     * Move selection to given position
     *
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    public void setSelection(int line, int column, boolean makeItVisible) {
        delegate.setSelection(line, column, makeItVisible);
    }

    /**
     * Move selection to given position
     *
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    public void setSelection(int line, int column, boolean makeItVisible, int cause) {
        delegate.setSelection(line, column, makeItVisible, cause);
    }

    /**
     * Select all text
     */
    public void selectAll() {
        delegate.selectAll();
    }

    /**
     * Set selection region with a call to {@link CodeEditor#ensureSelectionVisible()}
     *
     * @param lineLeft    Line left
     * @param columnLeft  Column Left
     * @param lineRight   Line right
     * @param columnRight Column right
     */
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight,
                                   int columnRight, int cause) {
        delegate.setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, cause);
    }

    /**
     * Set selection region with a call to {@link CodeEditor#ensureSelectionVisible()}
     *
     * @param lineLeft    Line left
     * @param columnLeft  Column Left
     * @param lineRight   Line right
     * @param columnRight Column right
     */
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight,
                                   int columnRight) {
        delegate.setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight);
    }

    /**
     * Set selection region
     *
     * @param lineLeft         Line left
     * @param columnLeft       Column Left
     * @param lineRight        Line right
     * @param columnRight      Column right
     * @param makeRightVisible Whether to make right cursor visible
     */
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight,
                                   int columnRight, boolean makeRightVisible) {
        delegate.setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, makeRightVisible);
    }

    /**
     * Set selection region
     *
     * @param lineLeft         Line left
     * @param columnLeft       Column Left
     * @param lineRight        Line right
     * @param columnRight      Column right
     * @param makeRightVisible Whether to make right cursor visible
     */
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight,
                                   int columnRight, boolean makeRightVisible, int cause) {
        delegate.setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, makeRightVisible, cause);
    }

    /**
     * Get system clipboard manager used by editor
     */
    public ClipboardManager getClipboardManager() {
        return delegate.getClipboardManager();
    }

    /**
     * Paste text from clip board
     */
    public void pasteText() {
        delegate.pasteText();
    }

    /**
     * Paste external text into editor
     */
    public void pasteText(@Nullable CharSequence text) {
        delegate.pasteText(text);
    }

    /**
     * Copy text to clipboard.
     */
    public void copyText() {
        delegate.copyText();
    }

    /**
     * Copy text to clipboard.
     *
     * @param shouldCopyLine State whether the editor should select whole line if
     *                       cursor is not in selection mode.
     */
    public void copyText(boolean shouldCopyLine) {
        delegate.copyText(shouldCopyLine);
    }

    /**
     * Copy the given text region to clipboard, and follow editor's IPC properties.
     */
    protected void copyTextToClipboard(@NonNull CharSequence text, int start, int end) {
        delegate.copyTextToClipboard(text, start, end);
    }

    /**
     * Copies the current line to clipboard.
     */
    private void copyLine() {
        delegate.copyLine();
    }

    /**
     * Copy text to clipboard and delete them
     */
    public void cutText() {
        delegate.cutText();
    }

    /**
     * Copy the current line to clipboard and delete it.
     */
    public void cutLine() {
        delegate.cutLine();
    }

    /**
     * Duplicates the current line.
     * Does not selects the duplicated line.
     */
    public void duplicateLine() {
        delegate.duplicateLine();
    }

    /**
     * Copies the current selection and pastes it at the right selection handle,
     * then selects the duplicated content.
     */
    public void duplicateSelection() {
        delegate.duplicateSelection();
    }

    /**
     * Copies the current selection and pastes it at the right selection handle.
     *
     * @param selectDuplicate Whether to select the duplicated content.
     */
    public void duplicateSelection(boolean selectDuplicate) {
        delegate.duplicateSelection(selectDuplicate);
    }

    /**
     * Copies the current selection, add the <code>prefix</code> to it
     * and pastes it at the right selection handle.
     *
     * @param prefix          The prefix for the selected content.
     * @param selectDuplicate Whether to select the duplicated content.
     */
    public void duplicateSelection(String prefix, boolean selectDuplicate) {
        delegate.duplicateSelection(prefix, selectDuplicate);
    }

    /**
     * Selects the word at the left selection handle.
     */
    public void selectCurrentWord() {
        delegate.selectCurrentWord();
    }

    /**
     * Selects the word at the given character position.
     *
     * @param line   The line.
     * @param column The column.
     */
    public void selectWord(int line, int column) {
        delegate.selectWord(line, column);
    }

    /**
     * @see #getWordRange(int, int, boolean)
     */
    public TextRange getWordRange(final int line, final int column) {
        return delegate.getWordRange(line, column);
    }

    /**
     * Get the range of the word at given character position.
     *
     * @param line   The line.
     * @param column The column.
     * @param useIcu Whether to use the ICU library to get word edges.
     * @return The word range.
     */
    public TextRange getWordRange(final int line, final int column, final boolean useIcu) {
        return delegate.getWordRange(line, column, useIcu);
    }

    /**
     * @return The text displaying.
     * <strong>Changes to this object are expected to be done in main thread
     * due to editor limitations, while the object can be read concurrently.</strong>
     * @see CodeEditor#setText(CharSequence)
     * @see CodeEditor#setText(CharSequence, Bundle)
     */
    @NonNull
    public Content getText() {
        return delegate.getText();
    }

    /**
     * Set the text to be displayed.
     * With no extra arguments.
     *
     * @param text the new text you want to display
     */
    public void setText(@Nullable CharSequence text) {
        delegate.setText(text);
    }

    /**
     * Get extra argument set by {@link CodeEditor#setText(CharSequence, Bundle)}
     */
    @NonNull
    public Bundle getExtraArguments() {
        return delegate.getExtraArguments();
    }

    /**
     * Sets the text to be displayed.
     *
     * @param text           the new text you want to display
     * @param extraArguments Extra arguments for the document. This {@link Bundle} object is passed
     *                       to all languages and plugins in editor.
     */
    public void setText(@Nullable CharSequence text, @Nullable Bundle extraArguments) {
        delegate.setText(text, extraArguments);
    }

    /**
     * Sets the text to be displayed.
     *
     * @param text               the new text you want to display
     * @param reuseContentObject If the given {@code text} is an instance of {@link Content}, reuse it.
     * @param extraArguments     Extra arguments for the document. This {@link Bundle} object is passed
     *                           to all languages and plugins in editor.
     */
    public void setText(@Nullable CharSequence text, boolean reuseContentObject,
                        @Nullable Bundle extraArguments) {
        delegate.setText(text, reuseContentObject, extraArguments);
    }

    /**
     * Set the editor's text size in sp unit. This value must be greater than 0
     *
     * @param textSize the editor's text size in <strong>Sp</strong> units.
     */
    public void setTextSize(float textSize) {
        delegate.setTextSize(textSize);
    }

    /**
     * Render ASCII Function characters
     *
     * @see #isRenderFunctionCharacters()
     */
    public void setRenderFunctionCharacters(boolean renderFunctionCharacters) {
        delegate.setRenderFunctionCharacters(renderFunctionCharacters);
    }

    /**
     * @see #setRenderFunctionCharacters(boolean)
     */
    public boolean isRenderFunctionCharacters() {
        return delegate.isRenderFunctionCharacters();
    }

    /**
     * Subscribe event of the given type.
     *
     * @see EventManager#subscribeEvent(Class, EventReceiver)
     */
    public <T extends
            Event> SubscriptionReceipt<T> subscribeEvent(Class<T> eventType, EventReceiver<T> receiver) {
        return delegate.subscribeEvent(eventType, receiver);
    }

    /**
     * Subscribe event of the given type, without {@link io.github.rosemoe.sora.event.Unsubscribe}.
     *
     * @see EventManager#subscribeEvent(Class, EventReceiver)
     */
    public <T extends
            Event> SubscriptionReceipt<T> subscribeAlways(Class<T> eventType, EventManager.NoUnsubscribeReceiver<T> receiver) {
        return delegate.subscribeAlways(eventType, receiver);
    }

    /**
     * Dispatch the given event
     *
     * @see EventManager#dispatchEvent(Event)
     */
    public <T extends Event> int dispatchEvent(T event) {
        return delegate.dispatchEvent(event);
    }

    /**
     * Create a new {@link EventManager} instance that can be used to subscribe events in editor,
     * as a child instance of editor.
     *
     * @return Child EventManager instance
     */
    @NonNull
    public EventManager createSubEventManager() {
        return delegate.createSubEventManager();
    }

    /**
     * Check whether the editor is currently performing a format operation
     *
     * @return whether the editor is currently formatting
     */
    public boolean isFormatting() {
        return delegate.isFormatting();
    }

    /**
     * Check whether line numbers are shown
     *
     * @return The state of line number displaying
     */
    public boolean isLineNumberEnabled() {
        return delegate.isLineNumberEnabled();
    }

    /**
     * Set whether we should display line numbers
     *
     * @param lineNumberEnabled The state of line number displaying
     */
    public void setLineNumberEnabled(boolean lineNumberEnabled) {
        delegate.setLineNumberEnabled(lineNumberEnabled);
    }

    /**
     * Get the paint of the editor
     * You should not change text size and other attributes that are related to text measuring by the object
     *
     * @return The paint which is used by the editor now
     */
    @NonNull
    public Paint getTextPaint() {
        return delegate.getTextPaint();
    }

    public Paint getOtherPaint() {
        return delegate.getOtherPaint();
    }

    public Paint getGraphPaint() {
        return delegate.getGraphPaint();
    }

    /**
     * Get the ColorScheme object of this editor
     * You can config colors of some regions, texts and highlight text
     *
     * @return ColorScheme object using
     */
    @NonNull
    public EditorColorScheme getColorScheme() {
        return delegate.getColorScheme();
    }

    /**
     * Set a new color scheme for editor.
     * <p>
     * It can be a subclass of {@link EditorColorScheme}.
     * The scheme object can only be applied to one editor instance.
     * Otherwise, an IllegalStateException is thrown.
     *
     * @param colors A non-null and free EditorColorScheme
     */
    public void setColorScheme(@NonNull EditorColorScheme colors) {
        delegate.setColorScheme(colors);
    }

    /**
     * Move selection to line start with scrolling
     *
     * @param line Line index to jump
     */
    public void jumpToLine(int line) {
        delegate.jumpToLine(line);
    }

    /**
     * Mark current selection position as a point of cursor range.
     * When user taps to select another point in text, the text between the marked point and
     * newly chosen point is selected.
     *
     * @see #isInLongSelect()
     * @see #endLongSelect()
     */
    public void beginLongSelect() {
        delegate.beginLongSelect();
    }

    /**
     * Checks whether long select mode is started
     */
    public boolean isInLongSelect() {
        return delegate.isInLongSelect();
    }

    /**
     * Marks long select mode is end.
     * This does nothing but set the flag to false.
     */
    public void endLongSelect() {
        delegate.endLongSelect();
    }


    //-------------------------------------------------------------------------------
    //-------------------------IME Interaction---------------------------------------
    //-------------------------------------------------------------------------------

    /**
     * Rerun analysis forcibly
     */
    public void rerunAnalysis() {
        delegate.rerunAnalysis();
    }

    /**
     * Get analyze result.
     * <strong>Do not make changes to it or read concurrently</strong>
     */
    @Nullable
    public Styles getStyles() {
        return delegate.getStyles();
    }

    @UiThread
    public void setStyles(@Nullable Styles styles) {
        delegate.setStyles(styles);
    }

    @UiThread
    public void updateStyles(@NonNull Styles styles, @Nullable StyleUpdateRange range) {
        delegate.updateStyles(styles, range);
    }

    @Nullable
    public DiagnosticsContainer getDiagnostics() {
        return delegate.getDiagnostics();
    }

    @UiThread
    public void setDiagnostics(@Nullable DiagnosticsContainer diagnostics) {
        delegate.setDiagnostics(diagnostics);
    }

    public void setInlayHints(@Nullable InlayHintsContainer inlayHints) {
        delegate.setInlayHints(inlayHints);
    }

    @Nullable
    public InlayHintsContainer getInlayHints() {
        return delegate.getInlayHints();
    }

    @UiThread
    public void setHighlightTexts(@Nullable HighlightTextContainer highlightTexts) {
        delegate.setHighlightTexts(highlightTexts);
    }

    @Nullable
    public HighlightTextContainer getHighlightTexts() {
        return delegate.getHighlightTexts();
    }

    /**
     * Hide auto complete window if shown
     */
    public void hideAutoCompleteWindow() {
        delegate.hideAutoCompleteWindow();
    }

    /**
     * Get cursor code block index
     *
     * @return index of cursor's code block
     */
    public int getBlockIndex() {
        return delegate.getBlockIndex();
    }

    /**
     * Display soft input method for self
     */
    public void showSoftInput() {
        delegate.showSoftInput();
    }

    /**
     * Hide soft input
     */
    public void hideSoftInput() {
        delegate.hideSoftInput();
    }

    /**
     * Check whether the soft keyboard is enabled for this editor. Unlike {@link #isSoftKeyboardEnabled()},
     * this method also checks whether a hardware keyboard is connected.
     *
     * @return Whether the editor should show soft keyboard.
     * @see #isSoftKeyboardEnabled()
     * @see #isDisableSoftKbdIfHardKbdAvailable()
     */
    protected boolean checkSoftInputEnabled() {
        return delegate.checkSoftInputEnabled();
    }

    /**
     * Set whether the soft keyboard is enabled for this editor. Set to {@code true} by default.
     *
     * @param isEnabled Whether the soft keyboard is enabled.
     */
    public void setSoftKeyboardEnabled(boolean isEnabled) {
        delegate.setSoftKeyboardEnabled(isEnabled);
    }

    /**
     * Returns whether the soft keyboard is enabled.
     *
     * @return Whether the soft keyboard is enabled.
     */
    public boolean isSoftKeyboardEnabled() {
        return delegate.isSoftKeyboardEnabled();
    }

    /**
     * Set whether the soft keyboard should be disabled for this editor if a hardware keyboard is
     * connected to the device. Set to {@code true} by default.
     *
     * @param isDisabled Whether the soft keyboard should be enabled if hardware keyboard is connected.
     */
    public void setDisableSoftKbdIfHardKbdAvailable(boolean isDisabled) {
        delegate.setDisableSoftKbdIfHardKbdAvailable(isDisabled);
    }

    /**
     * Returns whether the soft keyboard should be enabled if hardware keyboard is connected.
     *
     * @return Whether the soft keyboard should be enabled if hardware keyboard is connected.
     */
    public boolean isDisableSoftKbdIfHardKbdAvailable() {
        return delegate.isDisableSoftKbdIfHardKbdAvailable();
    }

    /**
     * Send current selection position to input method
     */
    protected void updateSelection() {
        delegate.updateSelection();
    }

    /**
     * Update request result for monitoring request
     */
    protected void updateExtractedText() {
        delegate.updateExtractedText();
    }

    //-------------------------------------------------------------------------------
    //------------------------Internal Callbacks-------------------------------------
    //-------------------------------------------------------------------------------

    /**
     * Set request needed to update when editor updates selection
     */
    protected void setExtracting(@Nullable ExtractedTextRequest request) {
        delegate.setExtracting(request);
    }

    /**
     * Extract text in editor for input method
     */
    protected ExtractedText extractText(@NonNull ExtractedTextRequest request) {
        return delegate.extractText(request);
    }

    /**
     * Notify input method that text has been changed for external reason
     */
    public void notifyIMEExternalCursorChange() {
        delegate.notifyIMEExternalCursorChange();
    }

    /**
     * Restart the input connection.
     * Do not call this method randomly. Please refer to documentation first.
     *
     * @see InputConnection
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void restartInput() {
        if (inputConnection != null)
            inputConnection.reset();
        if (inputMethodManager != null)
            inputMethodManager.restartInput(this);
    }

    /**
     * Send cursor position in text and on screen to input method
     */
    public void updateCursor() {
        delegate.updateCursor();
    }

    /**
     * Release some resources held by editor.
     * This will stop completion threads and destroy using {@link Language} object.
     * Also it prevents future editor tasks (such as posted Runnable) to be executed.
     * <p>
     * You are expected to call this method when the editor instance is no longer used, especially when
     * your activity is to be destroyed. Invoking this method repeatedly will not generated errors.
     */
    public void release() {
        delegate.hideEditorWindows();
        if (!released) {
            delegate.dispatchEvent(new EditorReleaseEvent(delegate, this));
        } else {
            return;
        }
        released = true;
        if (delegate.getEditorLanguage() != null) {
            delegate.getEditorLanguage().getAnalyzeManager().destroy();
            var formatter = delegate.getEditorLanguage().getFormatter();
            formatter.setReceiver(null);
            formatter.destroy();
            delegate.getEditorLanguage().destroy();
            delegate.setEditorLanguage(new EmptyLanguage());
        }

        // avoid access to language related after releasing
        delegate.textStyles = null;
        delegate.setDiagnostics(null);
        delegate.styleDelegate.reset();

        final var text = this.delegate.getText();
        if (text != null) {
            text.removeContentListener(delegate);
        }
        delegate.getColorScheme().detachEditor(delegate);
    }

    /**
     * Check if the editor is released.
     * When an editor is released, you are unexpected to make any changes to it.
     */
    public boolean isReleased() {
        return released;
    }

    /**
     * Hide all built-in windows of the editor
     */
    public void hideEditorWindows() {
        delegate.hideEditorWindows();
    }

    /**
     * Called by ColorScheme to notify invalidate
     *
     * @param type Color type changed
     */
    public void onColorUpdated(int type) {
        delegate.onColorUpdated(type);
    }

    /**
     * Called by color scheme to init colors
     */
    public void onColorFullUpdate() {
        delegate.onColorFullUpdate();
    }

    /**
     * Get using {@link InputMethodManager}
     */
    protected InputMethodManager getInputMethodManager() {
        return inputMethodManager;
    }

    /**
     * Called by {@link EditorInputConnection}
     */
    protected void onCloseConnection() {
        delegate.onCloseConnection();
    }

    /**
     * This method is called once when the editor is created.
     */
    @NonNull
    protected EditorRenderer onCreateRenderer() {
        return delegate.onCreateRenderer();
    }

    /**
     * Called when the text is edited or {@link CodeEditor#setSelection} is called
     */
    protected void onSelectionChanged(int cause) {
        delegate.onSelectionChanged(cause);
    }

    /**
     * Release active edge effects on thumbs up
     */
    protected void releaseEdgeEffects() {
        delegate.releaseEdgeEffects();
    }

    //-------------------------------------------------------------------------------
    //-------------------------Override methods--------------------------------------
    //-------------------------------------------------------------------------------
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        delegate.getRenderer().draw(canvas);

        // Update magnifier
        if ((delegate.lastCursorState != delegate.getCursorBlink().visibility || !delegate.touchHandler.getScroller().isFinished()) && delegate.touchHandler.magnifier.isShowing()) {
            delegate.lastCursorState = delegate.getCursorBlink().visibility;
            postInLifecycle(delegate.touchHandler.magnifier::updateDisplay);
        }
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo() {
        var info = super.createAccessibilityNodeInfo();
        if (isEnabled()) {
            var maxTextLength = delegate.getProps().maxAccessibilityTextLength;
            if (maxTextLength > 0) {
                info.setEditable(delegate.isEditable());
                info.setTextSelection(delegate.getCursor().getLeft(), delegate.getCursor().getRight());
                info.setInputType(InputType.TYPE_CLASS_TEXT);
                info.setMultiLine(true);
                info.setText(TextUtils.trimToSize(delegate.getText(), maxTextLength).toString());
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);
            }

            info.setLongClickable(true);
            final int scrollRange = delegate.getScrollMaxY();
            if (scrollRange > 0) {
                info.setScrollable(true);
                var scrollY = delegate.getOffsetY();
                if (scrollY > 0) {
                    info.addAction(
                            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP);
                    }
                }
                if (scrollY < scrollRange) {
                    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN);
                    }
                }
            }
        }
        return info;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        var maxScrollY = delegate.getScrollMaxY();
        event.setScrollable(maxScrollY > 0);
        event.setMaxScrollX(delegate.getScrollMaxX());
        event.setMaxScrollY(maxScrollY);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
            case AccessibilityNodeInfo.ACTION_COPY -> {
                delegate.copyText();
                return true;
            }
            case AccessibilityNodeInfo.ACTION_CUT -> {
                delegate.cutText();
                return true;
            }
            case AccessibilityNodeInfo.ACTION_PASTE -> {
                delegate.pasteText();
                return true;
            }
            case AccessibilityNodeInfo.ACTION_SET_TEXT -> {
                delegate.setText(arguments.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE));
                return true;
            }
            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> {
                delegate.moveSelection(SelectionMovement.PAGE_DOWN);
                return true;
            }
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> {
                delegate.moveSelection(SelectionMovement.PAGE_UP);
                return true;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (action) {
                case android.R.id.accessibilityActionScrollDown -> {
                    delegate.moveSelection(SelectionMovement.PAGE_UP);
                    return true;
                }
                case android.R.id.accessibilityActionScrollUp -> {
                    delegate.moveSelection(SelectionMovement.PAGE_DOWN);
                    return true;
                }
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CodeEditor.class.getName();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                downX = x;
                if (delegate.isInterceptParentHorizontalScrollEnabled()) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
            case MotionEvent.ACTION_MOVE -> {
                int deltaX = x - downX;
                if (delegate.isInterceptParentHorizontalScrollEnabled() && !delegate.touchHandler.hasAnyHeldHandle()) {
                    if (deltaX > 0 && delegate.getScroller().getCurrX() == 0
                            || deltaX < 0 && delegate.getScroller().getCurrX() == delegate.getScrollMaxX()) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return isEnabled() && delegate.isEditable();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (!delegate.isEditable() || !isEnabled()) {
            return null;
        }
        if (delegate.checkSoftInputEnabled()) {
            outAttrs.inputType = delegate.getInputType() != 0 ? delegate.getInputType() : EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
        } else {
            outAttrs.inputType = InputType.TYPE_NULL;
        }
        outAttrs.initialSelStart = delegate.getCursor() != null ? delegate.getCursor().getLeft() : 0;
        outAttrs.initialSelEnd = delegate.getCursor() != null ? delegate.getCursor().getRight() : 0;
        outAttrs.initialCapsMode = inputConnection.getCursorCapsMode(0);

        // Prevent fullscreen when the screen height is too small
        // Especially in landscape mode
        if (!delegate.getProps().allowFullscreen) {
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        }

        delegate.dispatchEvent(new BuildEditorInfoEvent(delegate, outAttrs));
        inputConnection.reset();
        delegate.getText().resetBatchEdit();
        delegate.setExtracting(null);
        return inputConnection;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                if (delegate.isFormatting() || delegate.layoutBusy) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_WAIT);
                }
                if (delegate.touchHandler.hasAnyHeldHandle()) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRABBING);
                }
                if (getLeftHandleDescriptor().position.contains(event.getX(), event.getY())
                        || getRightHandleDescriptor().position.contains(event.getX(), event.getY())
                        || getInsertHandleDescriptor().position.contains(event.getX(), event.getY())) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRAB);
                }
                var res = RegionResolverKt.resolveTouchRegion(delegate, event, pointerIndex);
                var region = IntPair.getFirst(res);
                var inbound = IntPair.getSecond(res) == RegionResolverKt.IN_BOUND;
                if (region == RegionResolverKt.REGION_TEXT && inbound) {
                    if (delegate.touchHandler.mouseCanMoveText && !delegate.touchHandler.mouseClick) {
                        return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRABBING);
                    }
                    if (delegate.getRenderer().lastStuckLines != null) {
                        var stickyLineCount = delegate.getRenderer().lastStuckLines.size();
                        if (stickyLineCount > 0 && event.getY() < delegate.getRowBottom(stickyLineCount - 1)) {
                            return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND);
                        }
                    }
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TEXT);
                } else if (region == RegionResolverKt.REGION_LINE_NUMBER) {
                    switch (delegate.getProps().actionWhenLineNumberClicked) {
                        case DirectAccessProps.LN_ACTION_SELECT_LINE,
                             DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME -> {
                            return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND);
                        }
                    }
                }
                return super.onResolvePointerIcon(event, pointerIndex);
            }
        }
        return super.onResolvePointerIcon(event, pointerIndex);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) && delegate.getProps().mouseMode != DirectAccessProps.MOUSE_MODE_NEVER) {
            return delegate.touchHandler.onMouseEvent(event);
        }
        if (delegate.isFormatting()) {
            delegate.touchHandler.reset2();
            scaleDetector.onTouchEvent(event);
            return basicDetector.onTouchEvent(event);
        }
        boolean handlingBefore = delegate.touchHandler.handlingMotions();
        boolean res = delegate.touchHandler.onTouchEvent(event);
        boolean handling = delegate.touchHandler.handlingMotions();
        boolean res2 = false;
        boolean res3 = scaleDetector.onTouchEvent(event);
        if (!handling && !handlingBefore) {
            res2 = basicDetector.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            delegate.getVerticalEdgeEffect().onRelease();
            delegate.getHorizontalEdgeEffect().onRelease();
        }
        return (res3 || res2 || res);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return delegate.keyEventHandler.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return delegate.keyEventHandler.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return delegate.keyEventHandler.onKeyMultiple(keyCode, repeatCount, event);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean onSuperKeyDown(int keyCode, @NonNull KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean onSuperKeyUp(int keyCode, @NonNull KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean onSuperKeyMultiple(int keyCode, int repeatCount, @NonNull KeyEvent event) {
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            Log.w(LOG_TAG, "use wrap_content in editor may cause layout lags");
            @SuppressLint("RestrictedApi")
            long specs = ViewMeasureHelper.getDesiredSize(widthMeasureSpec, heightMeasureSpec, delegate.measureTextRegionOffset(),
                    delegate.getRowHeight(), delegate.isWordwrap(), delegate.getTabWidth(), delegate.getText(), delegate.getRenderer().paintGeneral);
            widthMeasureSpec = IntPair.getFirst(specs);
            heightMeasureSpec = IntPair.getSecond(specs);
            delegate.anyWrapContentSet = true;
        } else {
            delegate.anyWrapContentSet = false;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED -> {
                return true;
            }
            case DragEvent.ACTION_DRAG_LOCATION -> {
                var pos = delegate.getPointPositionOnScreen(event.getX(), event.getY());
                int line = IntPair.getFirst(pos), column = IntPair.getSecond(pos);
                delegate.touchHandler.draggingSelection = delegate.getText().getIndexer().getCharPosition(line, column);
                postInvalidate();
                delegate.touchHandler.scrollIfReachesEdge(null, event.getX(), event.getY());
                return true;
            }
            case DragEvent.ACTION_DRAG_EXITED -> {
                delegate.touchHandler.draggingSelection = null;
                postInvalidate();
                return true;
            }
            case DragEvent.ACTION_DROP -> {
                var targetPos = delegate.touchHandler.draggingSelection;
                if (targetPos == null) {
                    return false;
                }
                delegate.touchHandler.draggingSelection = null;
                delegate.setSelection(targetPos.line, targetPos.column);
                delegate.pasteText(ClipDataUtils.clipDataToString(event.getClipData()));
                requestFocus();
                postInvalidate();
                // Call super for notifying listeners
                super.onDragEvent(event);
                return true;
            }
        }
        return super.onDragEvent(event);
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        final var pos = delegate.touchHandler.getLastContextClickPosition();
        if (pos == null) {
            return;
        }
        var charPos = delegate.getPointPositionOnScreen(pos.x, pos.y);
        delegate.dispatchEvent(new CreateContextMenuEvent(delegate, menu, delegate.getText().getIndexer().getCharPosition(IntPair.getFirst(charPos), IntPair.getSecond(charPos))));
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        delegate.touchHandler.resetMouse();
        delegate.mouseHover = delegate.mouseButtonPressed = false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                delegate.mouseHover = true;
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                delegate.mouseHover = false;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_BUTTON_PRESS
                    || event.getActionMasked() == MotionEvent.ACTION_BUTTON_RELEASE) {
                delegate.mouseButtonPressed = event.getButtonState() != 0;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_MOVE:
                case MotionEvent.ACTION_HOVER_EXIT:
                    delegate.touchHandler.dispatchEditorMotionEvent(HoverEvent::new, null, event);
                    return true;
            }
        }
        if (event.getAction() == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER) && !delegate.keyEventHandler.getKeyMetaStates().isCtrlPressed()) {
            float v_scroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            float h_scroll = -event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            float distanceX = h_scroll * delegate.verticalScrollFactor * delegate.getProps().mouseWheelScrollFactor;
            float distanceY = v_scroll * delegate.verticalScrollFactor * delegate.getProps().mouseWheelScrollFactor;
            if (delegate.keyEventHandler.getKeyMetaStates().isAltPressed()) {
                float multiplier = delegate.getProps().fastScrollSensitivity;
                distanceX *= multiplier;
                distanceY *= multiplier;
            }
            if (delegate.keyEventHandler.getKeyMetaStates().isShiftPressed()) {
                float tmp = distanceX;
                distanceX = distanceY;
                distanceY = tmp;
            }
            delegate.touchHandler.onScroll(event, event, distanceX, distanceY);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        delegate.getRenderer().onSizeChanged(w, h);
        getVerticalEdgeEffect().setSize(w, h);
        getHorizontalEdgeEffect().setSize(h, w);
        getVerticalEdgeEffect().finish();
        getHorizontalEdgeEffect().finish();
        if (delegate.getLayout() == null || (delegate.isWordwrap() && w != oldWidth)) {
            delegate.createLayout();
        } else {
            delegate.touchHandler.scrollBy(delegate.getOffsetX() > delegate.getScrollMaxX() ? delegate.getScrollMaxX() - delegate.getOffsetX() : 0, delegate.getOffsetY() > delegate.getScrollMaxY() ? delegate.getScrollMaxY() - delegate.getOffsetY() : 0);
        }
        delegate.verticalAbsorb = false;
        delegate.horizontalAbsorb = false;
        if (oldHeight > h && delegate.getProps().adjustToSelectionOnResize) {
            delegate.ensureSelectionVisible();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        delegate.dispatchEvent(new EditorAttachStateChangeEvent(delegate, false));
        delegate.getCursorBlink().valid = false;
        removeCallbacks(delegate.getCursorBlink());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        delegate.dispatchEvent(new EditorAttachStateChangeEvent(delegate, true));
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
                                  @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            delegate.getCursorBlink().valid = delegate.getCursorBlink().period > 0;
            if (delegate.getCursorBlink().valid) {
                postInLifecycle(delegate.getCursorBlink());
            }
        } else {
            delegate.getCursorBlink().valid = false;
            delegate.getCursorBlink().visibility = false;
            delegate.touchHandler.hideInsertHandle();
            removeCallbacks(delegate.getCursorBlink());
        }
        delegate.dispatchEvent(new EditorFocusChangeEvent(delegate, gainFocus));
        invalidate();
    }

    @Override
    public void computeScroll() {
        var scroller = delegate.touchHandler.getScroller();
        if (scroller.computeScrollOffset()) {
            if (!scroller.isFinished() && (scroller.getStartX() != scroller.getFinalX() || scroller.getStartY() != scroller.getFinalY())) {
                delegate.scrollerFinalX = scroller.getFinalX();
                delegate.scrollerFinalY = scroller.getFinalY();
                delegate.horizontalAbsorb = Math.abs(scroller.getStartX() - scroller.getFinalX()) > delegate.getDpUnit() * 5;
                delegate.verticalAbsorb = Math.abs(scroller.getStartY() - scroller.getFinalY()) > delegate.getDpUnit() * 5;
            }
            if (scroller.getCurrX() <= 0 && delegate.scrollerFinalX <= 0 && delegate.getHorizontalEdgeEffect().isFinished() && delegate.horizontalAbsorb) {
                delegate.getHorizontalEdgeEffect().onAbsorb((int) scroller.getCurrVelocity());
                delegate.touchHandler.glowLeftOrRight = false;
            } else {
                var max = delegate.getScrollMaxX();
                if (scroller.getCurrX() >= max && delegate.scrollerFinalX >= max && delegate.getHorizontalEdgeEffect().isFinished() && delegate.horizontalAbsorb) {
                    delegate.getHorizontalEdgeEffect().onAbsorb((int) scroller.getCurrVelocity());
                    delegate.touchHandler.glowLeftOrRight = true;
                }
            }
            if (scroller.getCurrY() <= 0 && delegate.scrollerFinalY <= 0 && delegate.getVerticalEdgeEffect().isFinished() && delegate.verticalAbsorb) {
                delegate.getVerticalEdgeEffect().onAbsorb((int) scroller.getCurrVelocity());
                delegate.touchHandler.glowTopOrBottom = false;
            } else {
                var max = delegate.getScrollMaxY();
                if (scroller.getCurrY() >= max && delegate.scrollerFinalY >= max && delegate.getVerticalEdgeEffect().isFinished() && delegate.verticalAbsorb) {
                    delegate.getVerticalEdgeEffect().onAbsorb((int) scroller.getCurrVelocity());
                    delegate.touchHandler.glowTopOrBottom = true;
                }
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        return delegate.getScrollMaxY();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, Math.min(delegate.getScrollMaxY(), delegate.getOffsetY()));
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return delegate.getScrollMaxX();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, Math.min(delegate.getScrollMaxX(), delegate.getOffsetX()));
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return 0;
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return 0;
    }

    @Override
    public boolean removeCallbacks(Runnable action) {
        EditorHandler.INSTANCE.removeCallbacks(action);
        return super.removeCallbacks(action);
    }

    /**
     * Post the given action to message queue. Run the action if editor is not released.
     *
     * @param action The Runnable to be executed.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is exiting.
     * @see View#post(Runnable)
     */
    @Override
    public boolean postInLifecycle(@NonNull Runnable action) {
        return EditorHandler.INSTANCE.post(() -> {
            if (released) {
                return;
            }
            action.run();
        });
    }

    /**
     * Post the given action to message queue. Run the action if editor is not released.
     *
     * @param action      The Runnable to be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable will be executed.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is exiting.
     * @see View#postDelayed(Runnable, long)
     */
    @Override
    public boolean postDelayedInLifecycle(Runnable action, long delayMillis) {
        return EditorHandler.INSTANCE.postDelayed(() -> {
            if (released) {
                return;
            }
            action.run();
        }, delayMillis);
    }

    @Override
    public boolean isWrapContentWidth() {
        var params = getLayoutParams();
        return params != null && params.width == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public boolean isWrapContentHeight() {
        var params = getLayoutParams();
        return params != null && params.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startSearchActionMode() {
        class SearchActionMode implements ActionMode.Callback {

            @Override
            public boolean onCreateActionMode(ActionMode p1, Menu p2) {
                startedActionMode = CodeEditor.ACTION_MODE_SEARCH_TEXT;
                p2.add(0, 0, 0, I18nConfig.getResourceId(R.string.sora_editor_next)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                p2.add(0, 1, 0, I18nConfig.getResourceId(R.string.sora_editor_last)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                p2.add(0, 2, 0, I18nConfig.getResourceId(R.string.sora_editor_replace)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                p2.add(0, 3, 0, I18nConfig.getResourceId(R.string.sora_editor_replaceAll)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                SearchView sv = new SearchView(getContext());
                sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String text) {
                        getSearcher().gotoNext();
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String text) {
                        if (text == null || text.isEmpty()) {
                            getSearcher().stopSearch();
                            return false;
                        }
                        getSearcher().search(text, new EditorSearcher.SearchOptions(false, false));
                        return false;
                    }

                });
                p1.setCustomView(sv);
                sv.performClick();
                sv.setQueryHint(I18nConfig.getString(getContext(), R.string.sora_editor_text_to_search));
                sv.setIconifiedByDefault(false);
                sv.setIconified(false);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode p1, Menu p2) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode am, MenuItem p2) {
                if (!getSearcher().hasQuery()) {
                    return false;
                }
                switch (p2.getItemId()) {
                    case 1 -> getSearcher().gotoPrevious();
                    case 0 -> getSearcher().gotoNext();
                    case 2, 3 -> {
                        final boolean replaceAll = p2.getItemId() == 3;
                        final EditText et = new EditText(getContext());
                        et.setHint(I18nConfig.getResourceId(R.string.sora_editor_replacement));
                        new AlertDialog.Builder(getContext())
                                .setTitle(I18nConfig.getResourceId(replaceAll ? R.string.sora_editor_replaceAll : R.string.sora_editor_replace))
                                .setView(et)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(I18nConfig.getResourceId(R.string.sora_editor_replace), (dialog, which) -> {
                                    if (replaceAll) {
                                        getSearcher().replaceAll(et.getText().toString(), am::finish);
                                    } else {
                                        getSearcher().replaceCurrentMatch(et.getText().toString());
                                        am.finish();
                                    }
                                    dialog.dismiss();
                                })
                                .show();
                    }
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode p1) {
                startedActionMode = CodeEditor.ACTION_MODE_NONE;
                getSearcher().stopSearch();
            }

        }
        ActionMode.Callback callback = new SearchActionMode();
        startActionMode(callback);
    }

    @Override
    public void showKeyboard() {
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(this, 0);
        }
    }

    @Override
    public void updateCursorAnchorInfo(@NotNull CursorAnchorInfo info) {
        if (inputMethodManager != null) {
            inputMethodManager.updateCursorAnchorInfo(this, info);
        }
    }

    @Override
    public void hideKeyboard() {
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    @Override
    public void updateSelection(int selStart, int selEnd, int candidatesStart, int candidatesEnd) {
        if (inputMethodManager != null) {
            inputMethodManager.updateSelection(this, selStart, selEnd, candidatesStart, candidatesEnd);
        }
    }

    @Override
    public void updateExtractedText(int token, @NotNull ExtractedText text) {
        if (inputMethodManager != null) {
            inputMethodManager.updateExtractedText(this, token, text);
        }
    }

    @Override
    @NotNull
    public View getAttachedView() {
        return this;
    }
}
