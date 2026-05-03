/*
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
 */
package io.github.rosemoe.sora.widget;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.TransactionTooLargeException;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.EdgeEffect;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.collection.MutableIntSet;
import androidx.collection.MutableLongLongMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import io.github.rosemoe.sora.I18nConfig;
import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage;
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.EditorFormatEvent;
import io.github.rosemoe.sora.event.Event;
import io.github.rosemoe.sora.event.EventManager;
import io.github.rosemoe.sora.event.EventReceiver;
import io.github.rosemoe.sora.event.LayoutStateChangeEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.SubscriptionReceipt;
import io.github.rosemoe.sora.event.TextSizeChangeEvent;
import io.github.rosemoe.sora.event.Unsubscribe;
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRenderer;
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRendererProvider;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer;
import io.github.rosemoe.sora.lang.styling.inlayHint.IntSetUpdateRange;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.ContentListener;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.Indexer;
import io.github.rosemoe.sora.text.LineSeparator;
import io.github.rosemoe.sora.text.TextLayoutHelper;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.text.method.KeyMetaStates;
import io.github.rosemoe.sora.util.Chars;
import io.github.rosemoe.sora.util.ClipDataUtils;
import io.github.rosemoe.sora.util.Floats;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.KeyboardUtils;
import io.github.rosemoe.sora.util.LongArrayList;
import io.github.rosemoe.sora.util.Numbers;
import io.github.rosemoe.sora.util.TemporaryFloatBuffer;
import io.github.rosemoe.sora.util.ViewUtils;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent;
import io.github.rosemoe.sora.widget.component.EditorContextMenuCreator;
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.component.Magnifier;
import io.github.rosemoe.sora.widget.layout.Layout;
import io.github.rosemoe.sora.widget.layout.LineBreakLayout;
import io.github.rosemoe.sora.widget.layout.WordwrapLayout;
import io.github.rosemoe.sora.widget.rendering.RenderContext;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.snippet.SnippetController;
import io.github.rosemoe.sora.widget.style.CursorAnimator;
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle;
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition;
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode;
import io.github.rosemoe.sora.widget.style.LineNumberTipTextProvider;
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;
import io.github.rosemoe.sora.widget.style.builtin.DefaultLineNumberTip;
import io.github.rosemoe.sora.widget.style.builtin.HandleStyleDrop;
import io.github.rosemoe.sora.widget.style.builtin.HandleStyleSideDrop;
import kotlin.text.StringsKt;

public class CodeEditorDelegate implements InlayHintRendererProvider, ContentListener, Formatter.FormatResultReceiver {

    private SymbolPairMatch languageSymbolPairs;
    private int tabWidth;
    private int cursorPosition;
    private int inputType;
    private int nonPrintableOptions;
    private float dividerWidth;
    private float dividerMarginLeft;
    private float dividerMarginRight;
    private float insertSelectionWidth;
    private float blockLineWidth;
    private float textBorderWidth;
    private float lineInfoTextSize;
    private float lineSpacingMultiplier = 1f;
    private float lineSpacingAdd = 0f;
    private float lineNumberMarginLeft;
    private float verticalExtraSpaceFactor = 0.5f;
    private boolean scalable;
    private boolean editable;
    private boolean wordwrap;
    private boolean undoEnabled;
    private boolean displayLnPanel;
    private int lnPanelPosition;
    private int lnPanelPositionMode;
    private int rejectComposingCount;
    private boolean lineNumberEnabled;
    private boolean blockLineEnabled;
    private boolean forceHorizontalScrollable;
    private boolean highlightCurrentBlock;
    private boolean highlightCurrentLine;
    private boolean cursorAnimation;
    private boolean pinLineNumber;
    private boolean antiWordBreaking;
    private boolean wordwrapRtlDisplaySupport;
    private boolean firstLineNumberAlwaysVisible;
    private boolean ligatureEnabled;
    private boolean stickyTextSelection;
    private boolean highlightBracketPair;
    private boolean isInLongSelect;
    private boolean renderFunctionCharacters;
    private boolean isSoftKbdEnabled;
    private boolean isDisableSoftKbdOnHardKbd;
    private Cursor cursor;
    private Content text;
    private EditorColorScheme colorScheme;
    private LineNumberTipTextProvider lineNumberTipTextProvider;
    private String formatTip;
    private Language editorLanguage;
    private DiagnosticIndicatorStyle diagnosticStyle;
    private long lastMakeVisible = 0;
    private Paint.Align lineNumberAlign;
    private ExtractedTextRequest extractingTextRequest;
    private CursorAnimator cursorAnimator;
    private SelectionHandleStyle handleStyle;
    private Bundle extraArguments;
    private DiagnosticsContainer diagnostics;
    private InlayHintsContainer inlayHints;
    private boolean hardwareAccAllowed;
    private LineSeparator lineSeparator;
    private TextRange lastSelectedTextRange;

    private final ClipboardManager clipboardManager;
    private final List<Span> defaultSpans = new ArrayList<>(2);
    private final float dpUnit;
    private final Matrix matrix;
    private final CursorAnchorInfo.Builder anchorInfoBuilder;
    private final EditorSearcher editorSearcher;
    private final DirectAccessProps props;
    private final RenderContext renderContext;
    private final EditorRenderer renderer;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean mouseHover;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean mouseButtonPressed;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean anyWrapContentSet;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public float verticalScrollFactor;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean verticalAbsorb;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean horizontalAbsorb;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public float scrollerFinalX;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public float scrollerFinalY;

    boolean waitForNextChange;
    boolean lastAnchorIsSelLeft;
    volatile boolean layoutBusy;
    boolean verticalScrollBarEnabled;
    boolean horizontalScrollBarEnabled;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean lastCursorState;

    TextRange lastInsertion;
    EditorTextActionWindow textActionWindow;
    EditorDiagnosticTooltipWindow diagnosticTooltip;
    EditorContextMenuCreator contextMenuCreator;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public EditorStyleDelegate styleDelegate;
    CharPosition selectionAnchor;
    EventManager eventManager;
    EditorAutoCompletion completionWindow;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Styles textStyles;
    HighlightTextContainer highlightTextContainer;
    EditorInputConnection inputConnection;
    Layout layout;
    public CursorBlink cursorBlink;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public EditorTouchEventHandler touchHandler;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public EditorKeyEventHandler keyEventHandler;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @UnsupportedUserUsage
    public EditorOverscrollCallback overscrollCallback;

    private final SnippetController snippetController;
    private final Map<String, InlayHintRenderer> inlayHintRendererMap = new HashMap<>();

    private final SelectionHandleStyle.HandleDescriptor handleDescLeft;
    private final SelectionHandleStyle.HandleDescriptor handleDescRight;
    private final SelectionHandleStyle.HandleDescriptor handleDescInsert;

    private final EdgeEffect edgeEffectVertical;
    private final EdgeEffect edgeEffectHorizontal;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final CodeEditorHost host;

    public CodeEditorDelegate(CodeEditorHost host) {
        this.host = host;

        this.eventManager = new EventManager();
        this.renderer = onCreateRenderer();
        this.renderContext = new RenderContext(this);
        setRenderFunctionCharacters(true);

        this.styleDelegate = new EditorStyleDelegate(this);
        this.verticalScrollFactor = ViewUtils.getVerticalScrollFactor(getContext());
        this.clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        setLineSeparator(LineSeparator.LF);
        setLineNumberTipTextProvider(DefaultLineNumberTip.INSTANCE);

        this.props = new DirectAccessProps();
        this.dpUnit = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, Resources.getSystem().getDisplayMetrics()) / 10f;

        setDividerWidth(dpUnit);
        setTextBorderWidth(dpUnit);
        this.insertSelectionWidth = dpUnit * 1.5f;
        this.dividerMarginLeft = this.dividerMarginRight = dpUnit * 2;

        this.matrix = new Matrix();
        this.handleStyle = new HandleStyleSideDrop(getContext());
        this.editorSearcher = new EditorSearcher(this);
        this.anchorInfoBuilder = new CursorAnchorInfo.Builder();

        this.diagnosticStyle = DiagnosticIndicatorStyle.WAVY_LINE;
        handleDescInsert = new SelectionHandleStyle.HandleDescriptor();
        handleDescLeft = new SelectionHandleStyle.HandleDescriptor();
        handleDescRight = new SelectionHandleStyle.HandleDescriptor();

        var isComposeMode = DelegateKt.isComposeMode(this);
        edgeEffectVertical = isComposeMode ? null : new EdgeEffect(getContext());
        edgeEffectHorizontal = isComposeMode ? null : new EdgeEffect(getContext());

        setTextSize(CodeEditor.DEFAULT_TEXT_SIZE);
        setLineInfoTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, CodeEditor.DEFAULT_LINE_INFO_TEXT_SIZE, Resources.getSystem().getDisplayMetrics()));
        setColorScheme(EditorColorScheme.getDefault());
        colorScheme.attachEditor(this);

        setLineNumberAlign(Paint.Align.RIGHT);
        this.waitForNextChange = false;
        setBlockLineEnabled(true);
        setBlockLineWidth(1f);
        setUndoEnabled(true);
        this.cursorPosition = -1;
        setScalable(true);
        setHighlightBracketPair(true);

        setEditorLanguage(null);
        setTabWidth(4);
        setHighlightCurrentLine(true);
        setHighlightCurrentBlock(true);
        setDisplayLnPanel(true);
        setFirstLineNumberAlwaysVisible(true);
        setCursorAnimationEnabled(true);
        setEditable(true);
        setLineNumberEnabled(true);
        setHardwareAcceleratedDrawAllowed(true);
        setInterceptParentHorizontalScrollIfNeeded(false);
        setTypefaceText(Typeface.DEFAULT);
        setSoftKeyboardEnabled(true);
        setDisableSoftKbdIfHardKbdAvailable(true);
        this.snippetController = new SnippetController(this);

        touchHandler = new EditorTouchEventHandler(this, host);
        keyEventHandler = new EditorKeyEventHandler(this, host);

        completionWindow = new EditorAutoCompletion(this, host);
        textActionWindow = new EditorTextActionWindow(this, host);
        diagnosticTooltip = new EditorDiagnosticTooltipWindow(this, host);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void setInputConnection(EditorInputConnection connection) {
        this.inputConnection = connection;
    }

    public Context getContext() {
        return host.getContext();
    }

    public int getWidth() {
        return host.getWidth();
    }

    public int getHeight() {
        return host.getHeight();
    }

    public boolean postInLifecycle(Runnable action) {
        return host.postInLifecycle(action);
    }

    public boolean isHorizontalScrollBarEnabled() {
        return horizontalScrollBarEnabled;
    }

    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        this.horizontalScrollBarEnabled = horizontalScrollBarEnabled;
    }

    public boolean isVerticalScrollBarEnabled() {
        return verticalScrollBarEnabled;
    }

    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        this.verticalScrollBarEnabled = verticalScrollBarEnabled;
    }

    /**
     * Get the rect of insert cursor handle on view
     *
     * @return Rect of insert handle
     */
    public SelectionHandleStyle.HandleDescriptor getInsertHandleDescriptor() {
        return handleDescInsert;
    }

    /**
     * Get the rect of left selection handle painted on view
     *
     * @return Descriptor of left handle
     */
    public SelectionHandleStyle.HandleDescriptor getLeftHandleDescriptor() {
        return handleDescLeft;
    }

    /**
     * Get the rect of right selection handle painted on view
     *
     * @return Descriptor of right handle
     */
    public SelectionHandleStyle.HandleDescriptor getRightHandleDescriptor() {
        return handleDescRight;
    }

    /**
     * Get EdgeEffect for vertical direction
     *
     * @return EdgeEffect
     */
    public EdgeEffect getVerticalEdgeEffect() {
        return edgeEffectVertical;
    }

    /**
     * Get EdgeEffect for horizontal direction
     *
     * @return EdgeEffect
     */
    public EdgeEffect getHorizontalEdgeEffect() {
        return edgeEffectHorizontal;
    }

    /**
     * Checks whether this region has visible region on screen
     *
     * @param begin The start line of code block
     * @param end   The end line of code block
     * @param first The first visible line on screen
     * @param last  The last visible line on screen
     * @return Whether this block can be seen
     */
    public static boolean hasVisibleRegion(int begin, int end, int first, int last) {
        return (end > first && begin < last);
    }

    /**
     * Get builtin component so that you can enable/disable them or do some other actions.
     *
     * @see io.github.rosemoe.sora.widget.component
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T extends EditorBuiltinComponent> T getComponent(@NonNull Class<T> clazz) {
        if (clazz == EditorAutoCompletion.class) {
            return (T) completionWindow;
        } else if (clazz == Magnifier.class) {
            return (T) touchHandler.magnifier;
        } else if (clazz == EditorTextActionWindow.class) {
            return (T) textActionWindow;
        } else if (clazz == EditorDiagnosticTooltipWindow.class) {
            return (T) diagnosticTooltip;
        } else if (clazz == EditorContextMenuCreator.class) {
            return (T) contextMenuCreator;
        } else {
            throw new IllegalArgumentException("Unknown component type");
        }
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
        var old = getComponent(clazz);
        var isEnabled = old.isEnabled();
        old.setEnabled(false);
        if (clazz == EditorAutoCompletion.class) {
            completionWindow = (EditorAutoCompletion) replacement;
        } else if (clazz == Magnifier.class) {
            touchHandler.magnifier = (Magnifier) replacement;
        } else if (clazz == EditorTextActionWindow.class) {
            textActionWindow = (EditorTextActionWindow) replacement;
        } else if (clazz == EditorDiagnosticTooltipWindow.class) {
            diagnosticTooltip = (EditorDiagnosticTooltipWindow) replacement;
        } else if (clazz == EditorContextMenuCreator.class) {
            contextMenuCreator = (EditorContextMenuCreator) replacement;
        } else {
            throw new IllegalArgumentException("Unknown component type");
        }
        replacement.setEnabled(isEnabled);
    }

    public void registerInlayHintRenderers(InlayHintRenderer... renderers) {
        var needLayout = false;
        for (var renderer : renderers) {
            var oldValue = inlayHintRendererMap.put(renderer.getTypeName(), renderer);
            needLayout |= oldValue != renderer;
        }
        if (needLayout) {
            createLayout();
        }
    }

    public void registerInlayHintRenderer(@NonNull InlayHintRenderer renderer) {
        var oldValue = inlayHintRendererMap.put(renderer.getTypeName(), renderer);
        if (oldValue != renderer) {
            createLayout();
        }
    }

    public void removeInlayHintRenderer(@NonNull InlayHintRenderer renderer) {
        var oldValue = inlayHintRendererMap.get(renderer.getTypeName());
        if (oldValue == renderer) {
            inlayHintRendererMap.remove(renderer.getTypeName());
            createLayout();
        }
    }

    @NonNull
    public List<InlayHintRenderer> getInlayHintRenderers() {
        return new ArrayList<>(inlayHintRendererMap.values());
    }

    @Nullable
    public InlayHintRenderer getInlayHintRendererForType(@Nullable String type) {
        return inlayHintRendererMap.get(type);
    }

    /**
     * Get KeyMetaStates, which manages alt/shift state in editor
     */
    @NonNull
    public KeyMetaStates getKeyMetaStates() {
        return keyEventHandler.getKeyMetaStates();
    }

    /**
     * Cancel the next animation for {@link CodeEditor#ensurePositionVisible(int, int)}
     */
    public void cancelAnimation() {
        lastMakeVisible = System.currentTimeMillis();
    }

    /**
     * Get the width of line number and divider line
     *
     * @return The width
     */
    public float measureTextRegionOffset() {
        return isLineNumberEnabled() ?
                measureLineNumber() + dividerMarginLeft + dividerMarginRight + dividerWidth +
                        (renderer.hasSideHintIcons() ? getRowHeight() : 0) :
                dpUnit * 5;
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    public float getOffset(int line, int column) {
        return layout.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - getOffsetX();
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    public float getCharOffsetX(int line, int column) {
        return layout.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - getOffsetX();
    }

    /**
     * Get the character's y offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The y offset on view
     */
    public float getCharOffsetY(int line, int column) {
        return layout.getCharLayoutOffset(line, column)[0] - getOffsetY();
    }

    @NonNull
    public SnippetController getSnippetController() {
        return snippetController;
    }

    /**
     * Get {@code DirectAccessProps} object of the editor.
     * <p>
     * You can adjust some settings of the editor by modifying the fields in the object direcly.
     */
    @NonNull
    public DirectAccessProps getProps() {
        return props;
    }

    /**
     * @see #setFormatTip(String)
     */
    public String getFormatTip() {
        return formatTip;
    }

    /**
     * Set the tip text while formatting
     */
    public void setFormatTip(@NonNull String formatTip) {
        this.formatTip = Objects.requireNonNull(formatTip);
    }

    /**
     * Release active edge effects on thumbs up
     */
    public void releaseEdgeEffects() {
        if (DelegateKt.isViewMode(this)) {
            getHorizontalEdgeEffect().onRelease();
            getVerticalEdgeEffect().onRelease();
        }
    }

    /**
     * Set whether line number region will scroll together with code region
     *
     * @see CodeEditor#isLineNumberPinned()
     */
    public void setPinLineNumber(boolean pinLineNumber) {
        this.pinLineNumber = pinLineNumber;
        if (isLineNumberEnabled()) {
            host.invalidate();
        }
    }

    /**
     * @see CodeEditor#setPinLineNumber(boolean)
     */
    public boolean isLineNumberPinned() {
        return pinLineNumber;
    }

    /**
     * @see CodeEditor#setFirstLineNumberAlwaysVisible(boolean)
     */
    public boolean isFirstLineNumberAlwaysVisible() {
        return firstLineNumberAlwaysVisible;
    }

    /**
     * Show first line number in screen in word wrap mode
     *
     * @see CodeEditor#isFirstLineNumberAlwaysVisible()
     */
    public void setFirstLineNumberAlwaysVisible(boolean enabled) {
        firstLineNumberAlwaysVisible = enabled;
        if (isWordwrap()) {
            host.invalidate();
        }
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
        if (selectionOffset < 0 || selectionOffset > text.length()) {
            throw new IllegalArgumentException("selectionOffset is invalid");
        }
        var cur = getText().getCursor();
        if (cur.isSelected()) {
            deleteText();
            notifyIMEExternalCursorChange();
        }
        this.text.insert(cur.getRightLine(), cur.getRightColumn(), text);
        notifyIMEExternalCursorChange();
        if (selectionOffset != text.length()) {
            var pos = this.text.getIndexer().getCharPosition(cur.getRight() - (text.length() - selectionOffset));
            setSelection(pos.line, pos.column);
        }
    }

    CursorBlink getCursorBlink() {
        return cursorBlink;
    }

    /**
     * @see CodeEditor#setLigatureEnabled(boolean)
     */
    public boolean isLigatureEnabled() {
        return ligatureEnabled;
    }

    /**
     * Enable/disable ligature of all types(except 'rlig').
     */
    public void setLigatureEnabled(boolean enabled) {
        this.ligatureEnabled = enabled;
        setFontFeatureSettings(enabled ? null : "'liga' 0,'calt' 0,'hlig' 0,'dlig' 0,'clig' 0");
    }

    /**
     * Set font feature settings for all paints used by editor
     *
     * @see io.github.rosemoe.sora.graphics.Paint#setFontFeatureSettings(String)
     */
    public void setFontFeatureSettings(String features) {
        renderer.getPaint().setFontFeatureSettingsWrapped(features);
        renderer.getPaintOther().setFontFeatureSettings(features);
        renderer.getPaintGraph().setFontFeatureSettings(features);
        renderer.updateTimestamp();
        host.invalidate();
    }

    /**
     * Set the style of selection handler.
     *
     * @see SelectionHandleStyle
     * @see HandleStyleDrop
     * @see HandleStyleSideDrop
     */
    public void setSelectionHandleStyle(@NonNull SelectionHandleStyle style) {
        handleStyle = Objects.requireNonNull(style);
        host.invalidate();
    }

    @NonNull
    public SelectionHandleStyle getHandleStyle() {
        return handleStyle;
    }

    /**
     * Returns whether highlight current code block
     *
     * @return This module enabled / disabled
     * @see CodeEditor#setHighlightCurrentBlock(boolean)
     */
    public boolean isHighlightCurrentBlock() {
        return highlightCurrentBlock;
    }

    /**
     * Whether the editor should use a different color to draw
     * the current code block line and this code block's start line and end line's
     * background.
     *
     * @param highlightCurrentBlock Enabled / Disabled this module
     */
    public void setHighlightCurrentBlock(boolean highlightCurrentBlock) {
        this.highlightCurrentBlock = highlightCurrentBlock;
        if (!this.highlightCurrentBlock) {
            cursorPosition = -1;
        } else {
            cursorPosition = findCursorBlock();
        }
        host.invalidate();
    }

    /**
     * Returns whether the cursor should stick to the text row while selecting the text
     *
     * @see CodeEditor#setStickyTextSelection(boolean)
     */
    public boolean isStickyTextSelection() {
        return stickyTextSelection;
    }

    /**
     * Whether the cursor should stick to the text row while selecting the text.
     *
     * @param stickySelection value
     */
    public void setStickyTextSelection(boolean stickySelection) {
        this.stickyTextSelection = stickySelection;
    }

    /**
     * @see CodeEditor#setHighlightCurrentLine(boolean)
     */
    public boolean isHighlightCurrentLine() {
        return highlightCurrentLine;
    }

    /**
     * Specify whether the editor should use a different color to draw
     * the background of current line
     */
    public void setHighlightCurrentLine(boolean highlightCurrentLine) {
        this.highlightCurrentLine = highlightCurrentLine;
        host.invalidate();
    }

    /**
     * Get the editor's language.
     *
     * @return EditorLanguage
     */
    @NonNull
    public Language getEditorLanguage() {
        return editorLanguage;
    }

    /**
     * Set the editor's language.
     * A language is a tool for auto-completion,highlight and auto indent analysis.
     *
     * @param lang New EditorLanguage for editor
     */
    public void setEditorLanguage(@Nullable Language lang) {
        if (lang == null) {
            lang = new EmptyLanguage();
        }

        // Destroy old one
        var old = editorLanguage;
        if (old != null) {
            var formatter = old.getFormatter();
            formatter.setReceiver(this);
            formatter.destroy();
            old.getAnalyzeManager().setReceiver(null);
            old.getAnalyzeManager().destroy();
            old.destroy();
        }

        styleDelegate.reset();
        this.editorLanguage = lang;
        this.textStyles = null;

        if (this.diagnostics != null) {
            this.diagnostics.detachEditor();
        }

        this.diagnostics = null;

        // Setup new one
        var mgr = lang.getAnalyzeManager();
        mgr.setReceiver(styleDelegate);
        if (text != null) {
            mgr.reset(new ContentReference(text), extraArguments);
        }

        // Symbol pairs
        if (languageSymbolPairs != null) {
            languageSymbolPairs.setParent(null);
        }
        languageSymbolPairs = editorLanguage.getSymbolPairs();
        if (languageSymbolPairs == null) {
            Log.w("CodeEditor", "Language(" + editorLanguage.toString() + ") returned null for symbol pairs. It is a mistake.");
            languageSymbolPairs = new SymbolPairMatch();
        }
        languageSymbolPairs.setParent(props.overrideSymbolPairs);

        if (snippetController != null) {
            snippetController.stopSnippet();
        }
        renderContext.invalidateRenderNodes();
        host.invalidate();

        // reset inlay hints (partially re-layout required)
        if (this.inlayHints != null) {
            setInlayHints(null);
        }
        if (this.highlightTextContainer != null) {
            setHighlightTexts(null);
        }
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
    public boolean canHandleKeyBinding(int keyCode, boolean ctrlPressed, boolean shiftPressed, boolean altPressed) {
        final var isDpadKey = keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
        final var isHomeOrEnd = keyCode == KeyEvent.KEYCODE_MOVE_HOME || keyCode == KeyEvent.KEYCODE_MOVE_END;

        if (ctrlPressed) {
            if (shiftPressed) {
                // Ctrl+Shift+[xx] keys
                return isDpadKey || isHomeOrEnd || keyCode == KeyEvent.KEYCODE_J;
            }

            if (altPressed) {
                // Ctrl+Alt+[xx] keys
                return keyCode == KeyEvent.KEYCODE_ENTER;
            }

            // Ctrl+[xx] keys
            return isDpadKey || isHomeOrEnd
                    || keyCode == KeyEvent.KEYCODE_A || keyCode == KeyEvent.KEYCODE_C
                    || keyCode == KeyEvent.KEYCODE_X || keyCode == KeyEvent.KEYCODE_V
                    || keyCode == KeyEvent.KEYCODE_U || keyCode == KeyEvent.KEYCODE_R
                    || keyCode == KeyEvent.KEYCODE_D || keyCode == KeyEvent.KEYCODE_W
                    || keyCode == KeyEvent.KEYCODE_ENTER;
        }


        if (shiftPressed) {
            // Shift+[xx] keys
            return isDpadKey || isHomeOrEnd || keyCode == KeyEvent.KEYCODE_ENTER;
        }

        return false;
    }

    /**
     * Getter
     *
     * @return The width in dp unit
     * @see CodeEditor#setBlockLineWidth(float)
     */
    public float getBlockLineWidth() {
        return blockLineWidth;
    }

    /**
     * Set the width of code block line
     *
     * @param dp Width in dp unit
     */
    public void setBlockLineWidth(float dp) {
        blockLineWidth = dp;
        host.invalidate();
    }

    /**
     * @see #setWordwrap(boolean)
     * @see #setWordwrap(boolean, boolean)
     */
    public boolean isWordwrap() {
        return wordwrap;
    }

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if anti word breaking is enabled in wordwrap mode.
     */
    public boolean isAntiWordBreaking() {
        return antiWordBreaking;
    }

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if RTL-based text should display from right of the widget in wordwrap mode.
     */
    public boolean isWordwrapRtlDisplaySupport() {
        return wordwrapRtlDisplaySupport;
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
        setWordwrap(wordwrap, true);
    }

    /**
     * Set whether text in editor should be wrapped to fit its size
     *
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @see #isWordwrap()
     */
    public void setWordwrap(boolean wordwrap, boolean antiWordBreaking) {
        setWordwrap(wordwrap, antiWordBreaking, false);
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
        if (this.wordwrap != wordwrap || this.antiWordBreaking != antiWordBreaking || this.wordwrapRtlDisplaySupport != supportRtlRow) {
            this.wordwrap = wordwrap;
            this.antiWordBreaking = antiWordBreaking;
            this.wordwrapRtlDisplaySupport = supportRtlRow;
            requestLayoutIfNeeded();
            createLayout();
            if (!wordwrap) {
                renderContext.invalidateRenderNodes();
            }
            host.invalidate();
        }
    }

    /**
     * @see #setCursorAnimationEnabled(boolean)
     */
    public boolean isCursorAnimationEnabled() {
        return cursorAnimation;
    }

    /**
     * Set cursor animation enabled
     */
    public void setCursorAnimationEnabled(boolean enabled) {
        if (!enabled) {
            cursorAnimator.cancel();
        }
        cursorAnimation = enabled;
    }

    /**
     * @see #setCursorAnimator(CursorAnimator)
     */
    public CursorAnimator getCursorAnimator() {
        return cursorAnimator;
    }

    /**
     * Set cursor animation
     *
     * @see CursorAnimator
     * @see #getCursorAnimator()
     * @see #setCursorAnimationEnabled(boolean)  for disabling the animation
     */
    public void setCursorAnimator(@NonNull CursorAnimator cursorAnimator) {
        this.cursorAnimator = cursorAnimator;
    }

    /**
     * Whether display vertical scroll bar when scrolling
     *
     * @param enabled Enabled / disabled
     */
    public void setScrollBarEnabled(boolean enabled) {
        verticalScrollBarEnabled = horizontalScrollBarEnabled = enabled;
        host.invalidate();
    }

    /**
     * @return Enabled / disabled
     * @see CodeEditor#setDisplayLnPanel(boolean)
     */
    public boolean isDisplayLnPanel() {
        return displayLnPanel;
    }

    /**
     * Whether display the line number panel beside vertical scroll bar
     * when the scroll bar is touched by user
     *
     * @param displayLnPanel Enabled / disabled
     */
    public void setDisplayLnPanel(boolean displayLnPanel) {
        this.displayLnPanel = displayLnPanel;
        host.invalidate();
    }

    /**
     * @return LineInfoPanelPosition.FOLLOW or LineInfoPanelPosition.FIXED
     * @see CodeEditor#setLnPanelPosition(int)
     */
    public int getLnPanelPositionMode() {
        return lnPanelPositionMode;
    }

    /**
     * Set display position mode the line number panel beside vertical scroll bar
     *
     * @param mode Default LineInfoPanelPosition.FOLLOW
     * @see LineInfoPanelPositionMode
     */
    public void setLnPanelPositionMode(int mode) {
        this.lnPanelPositionMode = mode;
        host.invalidate();
    }

    /**
     * @return position
     * @see CodeEditor#setLnPanelPosition(int)
     */
    public int getLnPanelPosition() {
        return lnPanelPosition;
    }

    /**
     * Set display position the line number panel beside vertical scroll bar <br/>
     * Only TOP,CENTER and BOTTOM will be effective when position mode is follow.
     *
     * @param position default TOP|RIGHT
     * @see LineInfoPanelPosition
     */
    public void setLnPanelPosition(int position) {
        this.lnPanelPosition = position;
        host.invalidate();
    }

    /**
     * @see CodeEditor#setLineNumberTipTextProvider(LineNumberTipTextProvider)
     */
    public LineNumberTipTextProvider getLineNumberTipTextProvider() {
        return lineNumberTipTextProvider;
    }

    /**
     * Set the tip text before line number for the line number panel
     */
    public void setLineNumberTipTextProvider(LineNumberTipTextProvider provider) {
        Objects.requireNonNull(provider, "Provider can not be null");
        lineNumberTipTextProvider = provider;
        host.invalidate();
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
        return renderer.getPaint().getTextSize();
    }

    /**
     * Set text size in pixel unit
     *
     * @param size Text size in pixel unit
     */
    public void setTextSizePx(@Px float size) {
        setTextSizePxDirect(size);
        requestLayoutIfNeeded();
        createLayout();
        host.invalidate();
    }

    /**
     * Set text size directly without creating layout or invalidating view
     *
     * @param size Text size in pixel unit
     */
    public void setTextSizePxDirect(@Px float size) {
        float oldTextSize = getTextSizePx();
        renderer.setTextSizePxDirect(size);
        dispatchEvent(new TextSizeChangeEvent(this, oldTextSize, size));
    }

    public void requestLayoutIfNeeded() {
        if (anyWrapContentSet) {
            host.requestLayout();
        }
    }

    public void checkForRelayout() {
        if (anyWrapContentSet) {
            if (host.isWrapContentWidth()) {
                host.requestLayout();
            } else if (host.isWrapContentHeight()) {
                if (host.getHeight() != layout.getLayoutHeight()) {
                    host.requestLayout();
                }
            }
        }
    }

    public EditorRenderer getRenderer() {
        return renderer;
    }

    public RenderContext getRenderContext() {
        return renderContext;
    }

    public Paint.FontMetricsInt getLineNumberMetrics() {
        return renderer.getLineNumberMetrics();
    }

    /**
     * Whether non-printable is to be drawn
     */
    public boolean shouldInitializeNonPrintable() {
        return Numbers.clearBits(nonPrintableOptions, CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE | CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE |
                CodeEditor.FLAG_DRAW_LINE_SEPARATOR | CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0;
    }

    /**
     * @see #setHardwareAcceleratedDrawAllowed(boolean)
     */
    public boolean isHardwareAcceleratedDrawAllowed() {
        return hardwareAccAllowed;
    }

    /**
     * Set whether allow the editor to use RenderNode to draw its text.
     * Enabling this can cause more memory usage, but the editor can display text
     * much quicker.
     * However, only when hardware accelerate is enabled on this view can the switch
     * make a difference.
     */
    public void setHardwareAcceleratedDrawAllowed(boolean acceleratedDraw) {
        hardwareAccAllowed = acceleratedDraw;
        if (acceleratedDraw && !isWordwrap()) {
            renderContext.invalidateRenderNodes();
        }
    }

    /**
     * As the name is, we find where leading spaces end and trailing spaces start
     *
     * @param line The line to search
     */
    public long findLeadingAndTrailingWhitespacePos(ContentLine line) {
        int column = line.length();
        int leading = 0;
        int trailing = column;
        while (leading < column && isWhitespace(line.charAt(leading))) {
            leading++;
        }
        // Only when this action is needed
        if (leading != column && (nonPrintableOptions & (CodeEditor.FLAG_DRAW_WHITESPACE_INNER | CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING)) != 0) {
            while (trailing > 0 && isWhitespace(line.charAt(trailing - 1))) {
                trailing--;
            }
        }
        return IntPair.pack(leading, trailing);
    }

    /**
     * A quick method to predicate whitespace character
     */
    public boolean isWhitespace(char ch) {
        return ch == '\t' || ch == ' ';
    }

    /**
     * Get matched text regions on the given line
     *
     * @param line      Target line
     * @param positions Output start positions
     */
    public void computeMatchedPositions(int line, LongArrayList positions) {
        positions.clear();
        if (editorSearcher.currentPattern == null || editorSearcher.searchOptions == null) {
            return;
        }
        if (!editorSearcher.isResultValid()) {
            return;
        }
        var res = editorSearcher.lastResults;
        if (res == null) {
            return;
        }
        var lineLeft = text.getCharIndex(line, 0);
        var lineRight = lineLeft + text.getColumnCount(line);
        for (int i = Math.max(0, positions.lowerBoundByFirst(lineLeft) - 1); i < res.size(); i++) {
            var region = res.get(i);
            var start = IntPair.getFirst(region);
            var end = IntPair.getSecond(region);
            var highlightStart = Math.max(start, lineLeft);
            var highlightEnd = Math.min(end, lineRight);
            if (highlightStart < highlightEnd) {
                positions.add(IntPair.pack(highlightStart - lineLeft, highlightEnd - lineLeft));
            }
            if (start > lineRight) {
                break;
            }
        }
    }

    public void computeHighlightPositions(int line, MutableLongLongMap positions) {
        positions.clear();
        if (highlightTextContainer == null) {
            return;
        }
        var highlights = highlightTextContainer.getForLine(line);
        if (highlights.isEmpty()) {
            return;
        }
        int lineColumnCount = text.getColumnCount(line);
        boolean highlightBlankLine = false;
        for (var highlight : highlights) {
            if (line < highlight.getStartLine() || line > highlight.getEndLine()) {
                continue;
            }
            int startColumn = (line == highlight.getStartLine()) ? highlight.getStartColumn() : 0;
            int endColumn = (line == highlight.getEndLine()) ? highlight.getEndColumn() : lineColumnCount;
            if (startColumn < 0) {
                startColumn = 0;
            } else if (startColumn > lineColumnCount) {
                startColumn = lineColumnCount;
            }
            if (endColumn < 0) {
                endColumn = 0;
            } else if (endColumn > lineColumnCount) {
                endColumn = lineColumnCount;
            }
            if (lineColumnCount == 0) {
                continue;
            }
            if (startColumn < endColumn) {
                int backgroundColor = highlight.getColor().resolve(colorScheme);
                int borderColor = highlight.getBorderColor().resolve(colorScheme);
                positions.put(IntPair.pack(startColumn, endColumn), IntPair.pack(backgroundColor, borderColor));
            }
        }
    }

    /**
     * Get the layout of editor
     */
    @UnsupportedUserUsage
    public Layout getLayout() {
        return layout;
    }

    /**
     * Find the smallest code block that cursor is in
     *
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    public int findCursorBlock() {
        List<CodeBlock> blocks = textStyles == null ? null : textStyles.blocks;
        if (blocks == null || blocks.isEmpty()) {
            return -1;
        }
        return findCursorBlock(blocks);
    }

    /**
     * Find the cursor code block internal
     *
     * @param blocks Current code blocks
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    public int findCursorBlock(List<CodeBlock> blocks) {
        int line = cursor.getLeftLine();
        int min = binarySearchEndBlock(line, blocks);
        if (min == -1) {
            min = 0;
        }
        int max = blocks.size() - 1;
        int minDis = Integer.MAX_VALUE;
        int found = -1;
        int invalidCount = 0;
        int maxCount = Integer.MAX_VALUE;
        if (textStyles != null) {
            maxCount = textStyles.getSuppressSwitch();
        }
        for (int i = min; i <= max; i++) {
            CodeBlock block = blocks.get(i);
            if (block == null) {
                continue;
            }
            if (block.endLine >= line && block.startLine <= line) {
                int dis = block.endLine - block.startLine;
                if (dis < minDis) {
                    minDis = dis;
                    found = i;
                }
            } else if (minDis != Integer.MAX_VALUE) {
                invalidCount++;
                if (invalidCount >= maxCount) {
                    break;
                }
            }
        }
        return found;
    }

    /**
     * @see #findCursorBlock()
     */
    public int getCurrentCursorBlock() {
        return cursorPosition;
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
    public int binarySearchEndBlock(int firstVis, List<CodeBlock> blocks) {
        return CodeBlock.binarySearchEndBlock(firstVis, blocks);
    }

    /**
     * Get spans on the given line
     */
    @NonNull
    public List<Span> getSpansForLine(int line) {
        var spanMap = textStyles == null ? null : textStyles.spans;
        if (defaultSpans.isEmpty()) {
            defaultSpans.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL));
        }
        try {
            if (spanMap != null) {
                return spanMap.read().getSpansOnLine(line);
            } else {
                return defaultSpans;
            }
        } catch (Exception e) {
            return defaultSpans;
        }
    }

    /**
     * Get the width of line number region (include line number margin)
     *
     * @return width of line number region
     */
    public float measureLineNumber() {
        if (!isLineNumberEnabled()) {
            return 0f;
        }
        int count = 0;
        int lineCount = getLineCount();
        while (lineCount > 0) {
            count++;
            lineCount /= 10;
        }
        var len = CodeEditor.NUMBER_DIGITS.length();
        var buffer = TemporaryFloatBuffer.obtain(len);
        renderer.getPaintOther().getTextWidths(CodeEditor.NUMBER_DIGITS, buffer);
        TemporaryFloatBuffer.recycle(buffer);
        float single = 0f;
        for (int i = 0; i < len; i += 2) {
            single = Math.max(single, buffer[i]);
        }
        return single * count + lineNumberMarginLeft;
    }

    public void createLayout() {
        createLayout(true);
    }

    /**
     * Create layout for text
     */
    public void createLayout(boolean clearWordwrapCache) {
        if (layout != null) {
            if (layout instanceof LineBreakLayout && !wordwrap) {
                ((LineBreakLayout) layout).reuse(text);
                return;
            }
            if (layout instanceof WordwrapLayout && wordwrap) {
                var newLayout = new WordwrapLayout(this, text, antiWordBreaking, wordwrapRtlDisplaySupport, (WordwrapLayout) layout, clearWordwrapCache);
                layout.destroyLayout();
                layout = newLayout;
                return;
            }
            layout.destroyLayout();
        }
        if (wordwrap) {
            renderer.setCachedLineNumberWidth((int) measureLineNumber());
            layout = new WordwrapLayout(this, text, antiWordBreaking, wordwrapRtlDisplaySupport, null, false);
        } else {
            layout = new LineBreakLayout(this, text);
        }
        if (touchHandler != null) {
            touchHandler.scrollBy(0, 0);
        }
    }

    /**
     * Indents the selected lines. Does nothing if the text is not selected.
     */
    public void indentSelection() {
        indentLines(true);
    }

    /**
     * Indents the lines. Does nothing if the <code>onlyIfSelected</code> is <code>true</code> and
     * no text is selected.
     *
     * @param onlyIfSelected Set to <code>true</code> if lines must be indented only if the text is
     *                       selected.
     */
    public void indentLines(boolean onlyIfSelected) {

        final var cursor = getCursor();
        if (onlyIfSelected && !cursor.isSelected()) {
            return;
        }

        final var tabString = createTabString();
        final var text = getText();
        final var tabWidth = getTabWidth();

        text.beginBatchEdit();
        for (int i = cursor.getLeftLine(); i <= cursor.getRightLine(); i++) {
            final var line = text.getLine(i);
            final var result = TextUtils.countLeadingSpacesAndTabs(line);
            final var spaceCount = IntPair.getFirst(result);
            final var tabCount = IntPair.getSecond(result);
            final var spaces = spaceCount + (tabCount * tabWidth);
            final var endColumn = spaceCount + tabCount;

            final var requiredSpaces = tabWidth - (spaces % tabWidth);
            if (spaceCount > 0 && tabCount > 0) {
                // indentation contains spaces as well as tabs
                // replace the leading indentation with appropriate indentation (according to language.useTabs())
                // this should be done while incrementing the indentation
                final var finalSpaceCount = ((requiredSpaces == 0 ? tabWidth : requiredSpaces) + spaces) / tabWidth;
                text.replace(i, 0, i, endColumn, StringsKt.repeat(tabString, finalSpaceCount));
                continue;
            }

            if (requiredSpaces == tabWidth) {
                // line is evenly indented
                // increase the indentation by \t or tabWidthSpaces
                text.insert(i, endColumn, tabString);
            } else {
                // line is oddly indented
                // We know that a line can never be oddly indented when it is indented only with tabs
                // therefore, we insert spaces to align the line
                text.insert(i, endColumn, StringsKt.repeat(" ", requiredSpaces));
            }
        }
        text.endBatchEdit();
    }

    /**
     * Removes indentation from the start of the selected lines. If the text is not selected, or if
     * the start and end selection is on the same line, only the line at the cursor position is
     * unindented.
     */
    public void unindentSelection() {
        final var cursor = getCursor();
        final var text = getText();
        final var tabWidth = getTabWidth();
        final var tabString = createTabString();

        text.beginBatchEdit();
        for (int i = cursor.getLeftLine(); i <= cursor.getRightLine(); i++) {
            final var line = text.getLineString(i);
            final var result = TextUtils.countLeadingSpacesAndTabs(line);
            final var spaceCount = IntPair.getFirst(result);
            final var tabCount = IntPair.getSecond(result);
            final var spaces = spaceCount + (tabCount * tabWidth);
            if (spaces == 0) {
                // line is not indented
                continue;
            }

            final var endColumn = spaceCount + tabCount;

            final var extraSpaces = spaces % tabWidth;
            if (spaceCount > 0 && tabCount > 0) {
                // indentation contains spaces as well as tabs
                // replace the leading indentation with appropriate indentation (according to language.useTabs())
                // this should be done while decrementing the indentation
                final var finalSpaceCount = Math.abs(spaces - (extraSpaces == 0 ? tabWidth : extraSpaces)) / tabWidth;
                text.replace(i, 0, i, endColumn, StringsKt.repeat(tabString, finalSpaceCount));
                continue;
            }

            if (extraSpaces == 0) {
                // line is evenly indented
                // remove tabString.length() characters from the start

                // do not use tabString.length()
                text.delete(i, endColumn - (tabCount > 0 ? 1 : tabWidth), i, endColumn);
            } else {
                // line is oddly indented
                // We know that a line can never be oddly indented when it is indented only with tabs
                // therefore, we delete spaces to align the line
                text.delete(i, endColumn - extraSpaces, i, endColumn);
            }
        }
        text.endBatchEdit();
    }

    /**
     * Commit a tab to cursor
     */
    public void commitTab() {
        if (inputConnection != null && isEditable()) {
            if (inputConnection.composingText.isComposing()) {
                // This external change will damage the composing text (#784)
                restartInput();
            }
            inputConnection.commitTextInternal(createTabString(), true);
        }
    }

    /**
     * Indents the line if text is not selected and the cursor is at the start of the line. Inserts
     * an indentation string otherwise.
     */
    public void indentOrCommitTab() {
        final var cursor = getCursor();
        if (cursor.isSelected()) {
            indentSelection();
            return;
        }

        final var left = cursor.left();
        final var line = getText().getLine(left.line);

        final var count = TextUtils.countLeadingSpacesAndTabs(line);
        final var spaceCount = IntPair.getFirst(count);
        final var tabCount = IntPair.getSecond(count);

        if (left.column > spaceCount + tabCount) {
            // there is text before the cursor
            commitTab();
            return;
        }

        indentLines(false);
    }

    /**
     * Creates the string to insert when <code>KEYCODE_TAB</code> key event is received from the IME.
     *
     * @return The string to insert for tab character.
     */
    public String createTabString() {
        final var language = getEditorLanguage();
        return TextUtils.createIndent(getTabWidth(), getTabWidth(), language.useTab());
    }

    /**
     * Update the information of cursor
     * Such as the position of cursor on screen(For input method that can go to any position on screen like PC input method)
     *
     * @return The offset x of right cursor on view
     */
    public float updateCursorAnchor() {
        int l = cursor.getRightLine();
        int column = cursor.getRightColumn();
        boolean visible = true;
        float[] offsets = layout.getCharLayoutOffset(l, column);
        float x = measureTextRegionOffset() + offsets[1] - getOffsetX();
        float charBottom = offsets[0] - getOffsetY();
        float charTop = charBottom - getRowHeight();
        float charBaseline = getRowBaseline(Math.round(charTop / (float) getRowHeight()));
        if (x < 0) {
            visible = false;
            x = 0;
        } else if (x > host.getWidth()) {
            visible = false;
            x = host.getWidth();
        }
        var composingText = inputConnection.composingText;
        if (composingText.preSetComposing) {
            return x;
        }
        if (props.reportCursorAnchor) {
            CursorAnchorInfo.Builder builder = anchorInfoBuilder;
            builder.reset();
            matrix.set(host.getMatrix());
            int[] b = new int[2];
            host.getLocationOnScreen(b);
            matrix.postTranslate(b[0], b[1]);
            builder.setMatrix(matrix);
            builder.setSelectionRange(cursor.getLeft(), cursor.getRight());

            if (composingText.isComposing()) {
                builder.setComposingText(composingText.startIndex, text.substring(composingText.startIndex, composingText.endIndex));
            }
            builder.setInsertionMarkerLocation(x, charTop, charBaseline, charBottom, visible ? CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION : CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION);
            host.updateCursorAnchorInfo(builder.build());
        }
        return x;
    }

    /**
     * Delete text before cursor or selected text (if there is)
     */
    public void deleteText() {
        var cur = cursor;
        if (cur.isSelected()) {
            text.delete(cur.getLeftLine(), cur.getLeftColumn(), cur.getRightLine(), cur.getRightColumn());
        } else {
            int col = cur.getLeftColumn();
            int line = cur.getLeftLine();
            if (props.deleteEmptyLineFast || (props.deleteMultiSpaces != 1 && col > 0 && text.charAt(line, col - 1) == ' ')) {
                // Check whether selection is in leading spaces
                var text = this.text.getLine(cur.getLeftLine());
                var inLeading = true;
                for (int i = col - 1; i >= 0; i--) {
                    char ch = text.charAt(i);
                    if (ch != ' ' && ch != '\t') {
                        inLeading = false;
                        break;
                    }
                }

                if (inLeading) {
                    // Check empty line
                    var emptyLine = true;
                    var max = this.text.getColumnCount(line);
                    for (int i = col; i < max; i++) {
                        char ch = text.charAt(i);
                        if (ch != ' ' && ch != '\t') {
                            emptyLine = false;
                            break;
                        }
                    }
                    if (props.deleteEmptyLineFast && emptyLine) {
                        if (line == 0) {
                            // Just delete whitespaces before
                            this.text.delete(line, 0, line, col);
                        } else {
                            this.text.delete(line - 1, this.text.getColumnCount(line - 1), line, max);
                        }
                        return;
                    }

                    if (props.deleteMultiSpaces != 1 && col > 0 && this.text.charAt(line, col - 1) == ' ') {
                        this.text.delete(line, Math.max(0, col - (props.deleteMultiSpaces == -1 ? getTabWidth() : props.deleteMultiSpaces)), line, col);
                        return;
                    }
                }
            }
            // Do not put cursor inside combined characters
            int begin;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                begin = TextUtils.getOffsetForBackspaceKey(text.getLine(cur.getLeftLine()), col);
            } else {
                // Devices under API 23 does not use the strategy above
                begin = TextLayoutHelper.get().getCurPosLeft(col, text.getLine(cur.getLeftLine()));
            }
            int end = cur.getLeftColumn();
            if (begin > end) {
                int tmp = begin;
                begin = end;
                end = tmp;
            }
            if (begin == end) {
                if (cur.getLeftLine() > 0 && begin == 0) {
                    text.delete(cur.getLeftLine() - 1, text.getColumnCount(cur.getLeftLine() - 1), cur.getLeftLine(), 0);
                }
            } else {
                text.delete(cur.getLeftLine(), begin, cur.getLeftLine(), end);
            }
        }
    }

    /**
     * Commit text to the content from IME
     */
    public void commitText(@NonNull CharSequence text) {
        commitText(text, true);
    }

    /**
     * Commit text at current state from IME
     *
     * @param text            Text commit by InputConnection
     * @param applyAutoIndent Apply automatic indentation
     */
    public void commitText(@NonNull CharSequence text, boolean applyAutoIndent) {
        commitText(text, applyAutoIndent, true);
    }

    /**
     * Commit text with given options
     *
     * @param text                  Text commit by InputConnection
     * @param applyAutoIndent       Apply automatic indentation
     * @param applySymbolCompletion Apply symbol surroundings and completions
     */
    public void commitText(@NonNull CharSequence text, boolean applyAutoIndent, boolean applySymbolCompletion) {
        // replace text
        SymbolPairMatch.SymbolPair pair = null;
        if (applySymbolCompletion && getProps().symbolPairAutoCompletion && text.length() > 0) {
            var endCharFromText = text.charAt(text.length() - 1);

            char[] inputText = null;

            //size > 1
            if (text.length() > 1) {
                inputText = text.toString().toCharArray();
            }

            pair = languageSymbolPairs.matchBestPair(
                    this, cursor.left(),
                    inputText, endCharFromText
            );
        }

        var cur = cursor;
        var editorText = this.text;
        var quoteHandler = LanguageHelper.getQuickQuoteHandler(editorLanguage);

        if (pair != null && pair != SymbolPairMatch.SymbolPair.EMPTY_SYMBOL_PAIR) {

            // QuickQuoteHandler can easily implement the feature of AutoSurround
            // and is at a higher level (customizable),
            // so if the language implemented QuickQuoteHandler,
            // the AutoSurround feature is not considered needed because it can be implemented through QuickQuoteHandler
            if (pair.shouldDoAutoSurround(editorText) && quoteHandler == null) {

                editorText.beginBatchEdit();
                // insert left
                editorText.insert(cur.getLeftLine(), cur.getLeftColumn(), pair.open);
                // editorText.insert(editorCursor.getLeftLine(),editorCursor.getLeftColumn(),selectText);
                // insert right
                editorText.insert(cur.getRightLine(), cur.getRightColumn(), pair.close);
                editorText.endBatchEdit();

                // setSelection
                setSelectionRegion(cur.getLeftLine(), cur.getLeftColumn(),
                        cur.getRightLine(), cur.getRightColumn() - pair.close.length());

                return;
            } else if (cur.isSelected() && quoteHandler != null) {
                if (text.length() > 0 && text.length() == 1) {
                    var result = quoteHandler.onHandleTyping(text.toString(), this.text, getCursorRange(), getStyles());
                    if (result.isConsumed()) {
                        var range = result.getNewCursorRange();
                        if (range != null) {
                            setSelectionRegion(range.getStart().line, range.getStart().column, range.getEnd().line, range.getEnd().column);
                        }
                        return;
                    }
                }
            } else {
                editorText.beginBatchEdit();

                var insertPosition = editorText
                        .getIndexer()
                        .getCharPosition(pair.getInsertOffset());

                editorText.replace(insertPosition.line, insertPosition.column,
                        cur.getRightLine(), cur.getRightColumn(), pair.open);
                editorText.insert(insertPosition.line, insertPosition.column + pair.open.length(), pair.close);
                editorText.endBatchEdit();

                var cursorPosition = editorText
                        .getIndexer()
                        .getCharPosition(pair.getCursorOffset());

                setSelection(cursorPosition.line, cursorPosition.column);

                return;
            }
        }


        if (cur.isSelected()) {
            editorText.replace(cur.getLeftLine(), cur.getLeftColumn(), cur.getRightLine(), cur.getRightColumn(), text);
        } else {
            if (props.autoIndent && text.length() != 0 && applyAutoIndent) {
                char first = text.charAt(0);
                if (first == '\n' || first == '\r') {
                    String line = this.text.getLineString(cur.getLeftLine());
                    int p = 0, spaceCount = 0, tabCount = 0;
                    while (p < cur.getLeftColumn()) {
                        if (isWhitespace(line.charAt(p))) {
                            if (line.charAt(p) == '\t') {
                                ++tabCount;
                            } else {
                                ++spaceCount;
                            }
                            p++;
                        } else {
                            break;
                        }
                    }
                    int count = spaceCount + (tabCount * tabWidth);
                    try {
                        count += LanguageHelper.getIndentAdvance(
                                editorLanguage,
                                new ContentReference(this.text),
                                cur.getLeftLine(),
                                cur.getLeftColumn(),
                                spaceCount,
                                tabCount
                        );
                    } catch (Exception e) {
                        Log.w("CodeEditor", "Language object error", e);
                    }
                    var index = 1;
                    if (first == '\r' && text.length() >= 2 && text.charAt(1) == '\n') {
                        index = 2;
                    }
                    StringBuilder sb = new StringBuilder(text);
                    sb.insert(index, TextUtils.createIndent(count, tabWidth, editorLanguage.useTab()));
                    text = sb;
                }
            }
            editorText.insert(cur.getLeftLine(), cur.getLeftColumn(), text);
        }
    }

    /**
     * @see #setLineInfoTextSize(float)
     */
    public float getLineInfoTextSize() {
        return lineInfoTextSize;
    }

    /**
     * Set text size for line info panel
     *
     * @param size Text size for line information, <strong>unit is SP</strong>
     */
    public void setLineInfoTextSize(float size) {
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        lineInfoTextSize = size;
    }

    /**
     * @see #setNonPrintablePaintingFlags(int)
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_LEADING
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_INNER
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_TRAILING
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
     * @see CodeEditor#FLAG_DRAW_LINE_SEPARATOR
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_IN_SELECTION
     */
    public int getNonPrintablePaintingFlags() {
        return nonPrintableOptions;
    }

    /**
     * Sets non-printable painting flags.
     * Specify where they should be drawn and some other properties.
     * <p>
     * Flags can be mixed.
     *
     * @param flags Flags
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_LEADING
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_INNER
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_TRAILING
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
     * @see CodeEditor#FLAG_DRAW_LINE_SEPARATOR
     * @see CodeEditor#FLAG_DRAW_WHITESPACE_IN_SELECTION
     * @see CodeEditor#FLAG_DRAW_SOFT_WRAP
     */
    public void setNonPrintablePaintingFlags(int flags) {
        int oldFlags = nonPrintableOptions;
        this.nonPrintableOptions = flags;
        if ((oldFlags & CodeEditor.FLAG_DRAW_SOFT_WRAP) != (flags & CodeEditor.FLAG_DRAW_SOFT_WRAP)) {
            createLayout();
        }
        host.invalidate();
    }

    public boolean hasComposingText() {
        return inputConnection.composingText.isComposing();
    }

    /**
     * Make the selection visible
     */
    public void ensureSelectionVisible() {
        ensurePositionVisible(getCursor().getRightLine(), getCursor().getRightColumn());
    }

    /**
     * Make the given character position visible
     *
     * @param line   Line in text
     * @param column Column in text
     */
    public void ensurePositionVisible(int line, int column) {
        ensurePositionVisible(line, column, false);
    }

    /**
     * Make the given character position visible
     *
     * @param line        Line in text
     * @param column      Column in text
     * @param noAnimation true if no animation should be applied
     */
    public void ensurePositionVisible(int line, int column, boolean noAnimation) {
        var scroller = getScroller();
        float[] layoutOffset = layout.getCharLayoutOffset(line, column);
        // x offset is the left of character
        float xOffset = layoutOffset[1] + measureTextRegionOffset();
        // y offset is the bottom of row
        float yOffset = layoutOffset[0];

        float currFinalY = scroller.isFinished() ? getOffsetY() : scroller.getFinalY();
        float currFinalX = scroller.isFinished() ? getOffsetX() : scroller.getFinalX();
        float targetY = currFinalY;
        float targetX = currFinalX;

        int topLines = props.stickyScroll ? props.stickyScrollMaxLines : 2;
        if (yOffset - getRowHeight() * topLines < currFinalY) {
            // top may be invisible
            targetY = yOffset - getRowHeight() * topLines;
        }
        if (yOffset > host.getHeight() + currFinalY) {
            // bottom invisible
            targetY = yOffset - host.getHeight() + getRowHeight() * 1f;
        }
        float charWidth = column == 0 ? 0 : getTextPaint().measureText("a");
        if (xOffset < currFinalX + (pinLineNumber ? measureTextRegionOffset() : 0)) {
            float backupX = targetX;
            var scrollSlopX = host.getWidth() / 2;
            targetX = xOffset + (pinLineNumber ? -measureTextRegionOffset() : 0) - charWidth;
            if (Math.abs(targetX - backupX) < scrollSlopX) {
                targetX = Math.max(1, backupX - scrollSlopX);
            }
        }
        if (xOffset + charWidth > currFinalX + host.getWidth()) {
            targetX = xOffset + charWidth * 0.8f - host.getWidth();
        }

        targetX = Math.max(0, Math.min(getScrollMaxX(), targetX));
        targetY = Math.max(0, Math.min(getScrollMaxY(), targetY));

        if (Floats.withinDelta(targetX, getOffsetX(), 1f) && Floats.withinDelta(targetY, getOffsetY(), 1f)) {
            host.invalidate();
            return;
        }

        boolean animation = System.currentTimeMillis() - lastMakeVisible >= 100;
        lastMakeVisible = System.currentTimeMillis();

        if (animation && !noAnimation) {
            scroller.forceFinished(true);
            scroller.startScroll(getOffsetX(), getOffsetY(), (int) (targetX - getOffsetX()), (int) (targetY - getOffsetY()));
            if (props.awareScrollbarWhenAdjust && Math.abs(getOffsetY() - targetY) > dpUnit * 100) {
                touchHandler.notifyScrolled();
            }
        } else {
            scroller.startScroll(getOffsetX(), getOffsetY(), (int) (targetX - getOffsetX()), (int) (targetY - getOffsetY()), 0);
            scroller.abortAnimation();
        }

        dispatchEvent(new ScrollEvent(this, getOffsetX(),
                getOffsetY(), (int) targetX, (int) targetY, ScrollEvent.CAUSE_MAKE_POSITION_VISIBLE));

        host.invalidate();
    }

    /**
     * Whether there is clip
     *
     * @return whether clip in clip board
     */
    public boolean hasClip() {
        return clipboardManager.hasPrimaryClip();
    }

    /**
     * Get 1dp = ?px
     *
     * @return 1dp in pixel
     */
    public float getDpUnit() {
        return dpUnit;
    }

    /**
     * Get scroller from EventHandler
     * You would better not use it for your own scrolling
     *
     * @return The scroller
     */
    public EditorScroller getScroller() {
        return touchHandler.getScroller();
    }

    /**
     * Checks whether the position is over max Y position
     *
     * @param posOnScreen Y position on view
     * @return Whether over max Y
     */
    public boolean isOverMaxY(float posOnScreen) {
        return posOnScreen + getOffsetY() > layout.getLayoutHeight();
    }

    /**
     * Check if the point on editor view, is inside text region on that row
     */
    public boolean isScreenPointOnText(float x, float y) {
        var pos = getPointPositionOnScreen(x, y);
        var rowIdx = layout.getRowIndexForPosition(text.getCharIndex(IntPair.getFirst(pos), IntPair.getSecond(pos)));
        var layoutMax = renderer.getRowWidth(rowIdx);
        var textRegionX = measureTextRegionOffset();
        var rowRegionRightX = textRegionX + layoutMax;
        var offset = getOffsetX() + x;
        return offset >= textRegionX && offset <= rowRegionRightX;
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
        return layout.getCharPositionForLayoutOffset(xOffset - measureTextRegionOffset(), yOffset);
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
        y = Math.max(0, y);
        var stuckLines = renderer.lastStuckLines;
        if (stuckLines != null) {
            // position Y maybe negative
            var index = (int) Math.max(0, (y / getRowHeight()));
            if (y < stuckLines.size() * getRowHeight() && index < stuckLines.size()) {
                return getPointPosition(x, layout.getCharLayoutOffset(stuckLines.get(index).startLine, 0)[0] - getRowHeight() / 2f);
            }
        }
        return getPointPosition(x + getOffsetX(), y + getOffsetY());
    }

    public Layout.VisualLocation getPointVisualPosition(float xOffset, float yOffset) {
        return layout.getVisualPositionForLayoutOffset(xOffset - measureTextRegionOffset(), yOffset);
    }

    public Layout.VisualLocation getPointVisualPositionOnScreen(float x, float y) {
        y = Math.max(0, y);
        var stuckLines = renderer.lastStuckLines;
        if (stuckLines != null) {
            // position Y maybe negative
            var index = (int) Math.max(0, (y / getRowHeight()));
            if (y < stuckLines.size() * getRowHeight() && index < stuckLines.size()) {
                return getPointVisualPosition(x, layout.getCharLayoutOffset(stuckLines.get(index).startLine, 0)[0] - getRowHeight() / 2f);
            }
        }
        return getPointVisualPosition(x + getOffsetX(), y + getOffsetY());
    }

    /**
     * Get max scroll y
     *
     * @return max scroll y
     */
    public int getScrollMaxY() {
        var isWrapHeight = host.isWrapContentHeight();
        var height = host.getHeight();
        return Math.max(0, layout.getLayoutHeight() - (int) (isWrapHeight ? height : height * (1 - verticalExtraSpaceFactor)));
    }

    /**
     * Get max scroll x
     *
     * @return max scroll x
     */
    public int getScrollMaxX() {
        return (int) Math.max(0, layout.getLayoutWidth() + measureTextRegionOffset() - host.getWidth() / 2f);
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
        if (extraSpaceFactor < 0 || extraSpaceFactor > 1.0f) {
            throw new IllegalArgumentException("the factor should be in range [0.0, 1.0]");
        }
        this.verticalExtraSpaceFactor = extraSpaceFactor;
        // ensure offset is in scroll range
        touchHandler.scrollBy(0, 0);
    }

    /**
     * Get the factor used to compute extra space of vertical viewport.
     *
     * @see #setVerticalExtraSpaceFactor(float)
     */
    public float getVerticalExtraSpaceFactor() {
        return verticalExtraSpaceFactor;
    }

    /**
     * Get EditorSearcher
     *
     * @return EditorSearcher
     */
    public EditorSearcher getSearcher() {
        return editorSearcher;
    }

    /**
     * Set selection around the given position
     * It will try to set selection as near as possible (Exactly the position if that position exists)
     */
    public void setSelectionAround(int line, int column) {
        if (line < getLineCount()) {
            int columnCount = text.getColumnCount(line);
            if (column > columnCount) {
                column = columnCount;
            }
            setSelection(line, column);
        } else {
            setSelection(getLineCount() - 1, text.getColumnCount(getLineCount() - 1));
        }
    }

    /**
     * Format text Async
     *
     * @return Whether the format task is scheduled
     */
    public synchronized boolean formatCodeAsync() {
        if (isFormatting()) {
            return false;
        }
        var formatter = editorLanguage.getFormatter();
        formatter.setReceiver(null);
        var formatContent = text.copyText(false);
        formatContent.setUndoEnabled(false);
        formatter.format(formatContent, getCursorRange());
        host.postInvalidate();
        return true;
    }

    /**
     * Format text in the given region.
     * <p>
     * Note: Make sure the given positions are valid (line, column and index). Typically, you should
     * obtain a position by an object of {@link Indexer}
     *
     * @param start Start position created by Indexer
     * @param end   End position created by Indexer
     * @return Whether the format task is scheduled
     */
    public synchronized boolean formatCodeAsync(CharPosition start, CharPosition end) {
        if (start.index > end.index) {
            throw new IllegalArgumentException("start > end");
        }
        if (isFormatting()) {
            return false;
        }
        var formatter = editorLanguage.getFormatter();
        formatter.setReceiver(null);
        var formatContent = text.copyText(false);
        formatContent.setUndoEnabled(false);
        formatter.formatRegion(formatContent, new TextRange(start, end), getCursorRange());
        host.postInvalidate();
        return true;
    }

    /**
     * Get the cursor range of editor
     */
    public TextRange getCursorRange() {
        return cursor.getRange();
    }

    /**
     * If any text is selected
     */
    public boolean isTextSelected() {
        return cursor.isSelected();
    }

    /**
     * Get tab width
     *
     * @return tab width
     */
    public int getTabWidth() {
        return tabWidth;
    }

    /**
     * Set tab width
     *
     * @param width tab width compared to space
     */
    public void setTabWidth(int width) {
        if (width < 1) {
            throw new IllegalArgumentException("width can not be under 1");
        }
        tabWidth = width;
        renderContext.invalidateRenderNodes();
        renderer.updateTimestamp();
        requestLayoutIfNeeded();
        host.invalidate();
    }

    /**
     * Set max and min text size that can be used by user zooming.
     * <p>
     * Unit is px.
     */
    public void setScaleTextSizes(float minSize, float maxSize) {
        if (minSize > maxSize) {
            throw new IllegalArgumentException("min size can not be bigger than max size");
        }
        if (minSize < 2f) {
            throw new IllegalArgumentException("min size must be at least 2px");
        }
        touchHandler.scaleMinSize = minSize;
        touchHandler.scaleMaxSize = maxSize;
    }

    /**
     * @see CodeEditor#setInterceptParentHorizontalScrollIfNeeded(boolean)
     */
    public boolean isInterceptParentHorizontalScrollEnabled() {
        return forceHorizontalScrollable;
    }

    /**
     * When the parent is a scrollable view group,
     * request it not to allow horizontal scrolling to be intercepted.
     * Until the code cannot scroll horizontally
     *
     * @param forceHorizontalScrollable Whether force horizontal scrolling
     */
    public void setInterceptParentHorizontalScrollIfNeeded(boolean forceHorizontalScrollable) {
        this.forceHorizontalScrollable = forceHorizontalScrollable;
        if (!forceHorizontalScrollable) {
            host.requestDisallowInterceptTouchEvent(false);
        }
    }

    /**
     * @see #setHighlightBracketPair(boolean)
     */
    public boolean isHighlightBracketPair() {
        return highlightBracketPair;
    }

    /**
     * Whether to highlight brackets pairs
     */
    public void setHighlightBracketPair(boolean highlightBracketPair) {
        this.highlightBracketPair = highlightBracketPair;
        if (!highlightBracketPair) {
            styleDelegate.clearFoundBracketPair();
        } else {
            styleDelegate.postUpdateBracketPair();
        }
        host.invalidate();
    }

    /**
     * Set line separator when new lines are created in editor (only texts from IME. texts from clipboard
     * or other strategies are not encountered). Must not be{@link LineSeparator#NONE}
     *
     * @see #getLineSeparator()
     * @see LineSeparator
     */
    public void setLineSeparator(@NonNull LineSeparator lineSeparator) {
        if (Objects.requireNonNull(lineSeparator) == LineSeparator.NONE) {
            throw new IllegalArgumentException();
        }
        this.lineSeparator = lineSeparator;
    }

    /**
     * @see #setLineSeparator(LineSeparator)
     */
    public LineSeparator getLineSeparator() {
        return lineSeparator;
    }

    /**
     * @see CodeEditor#setInputType(int)
     */
    public int getInputType() {
        return inputType;
    }

    /**
     * Specify input type for the editor
     * <p>
     * Zero for default input type
     *
     * @see EditorInfo#inputType
     */
    public void setInputType(int inputType) {
        this.inputType = inputType;
        restartInput();
    }

    /**
     * Undo last action
     */
    public void undo() {
        var range = text.undo();
        if (range != null) {
            try {
                setSelectionRegion(range.getStart().line, range.getStart().column, range.getEnd().line, range.getEnd().column, true, SelectionChangeEvent.CAUSE_TEXT_MODIFICATION);
            } catch (IndexOutOfBoundsException e) {
                // Suppressed, typically because an invalid position is memorized.
            }
        }
        notifyIMEExternalCursorChange();
    }

    /**
     * Redo last action
     */
    public void redo() {
        text.redo();
        notifyIMEExternalCursorChange();
    }

    /**
     * Checks whether we can undo
     *
     * @return true if we can undo
     */
    public boolean canUndo() {
        return text.canUndo();
    }

    /**
     * Checks whether we can redo
     *
     * @return true if we can redo
     */
    public boolean canRedo() {
        return text.canRedo();
    }

    /**
     * @return Enabled/Disabled
     * @see CodeEditor#setUndoEnabled(boolean)
     */
    public boolean isUndoEnabled() {
        return undoEnabled;
    }

    /**
     * Enable / disabled undo manager
     *
     * @param enabled Enable/Disable
     */
    public void setUndoEnabled(boolean enabled) {
        undoEnabled = enabled;
        if (text != null) {
            text.setUndoEnabled(enabled);
        }
    }

    public DiagnosticIndicatorStyle getDiagnosticIndicatorStyle() {
        return diagnosticStyle;
    }

    public void setDiagnosticIndicatorStyle(@NonNull DiagnosticIndicatorStyle
                                                    diagnosticIndicatorStyle) {
        this.diagnosticStyle = diagnosticIndicatorStyle;
        host.invalidate();
    }

    /**
     * Start search action mode
     */
    public void beginSearchMode() {
        host.startSearchActionMode();
    }

    /**
     * Get {@link EditorTouchEventHandler} of the editor
     */
    public EditorTouchEventHandler getEventHandler() {
        return touchHandler;
    }

    /**
     * @return Margin left of divider line
     * @see CodeEditor#setDividerMargin(float, float)
     */
    @Px
    public float getDividerMarginLeft() {
        return dividerMarginLeft;
    }

    /**
     * @return Margin right of divider line
     * @see CodeEditor#setDividerMargin(float, float)
     */
    @Px
    public float getDividerMarginRight() {
        return dividerMarginRight;
    }

    /**
     * Set divider line's left and right margin
     *
     * @param marginLeft  Margin left for divider line
     * @param marginRight Margin right for divider line
     */
    public void setDividerMargin(@Px float marginLeft, @Px float marginRight) {
        if (marginLeft < 0 || marginRight < 0) {
            throw new IllegalArgumentException("margin can not be under zero");
        }
        dividerMarginLeft = marginLeft;
        dividerMarginRight = marginRight;
        requestLayoutIfNeeded();
        host.invalidate();
    }

    /**
     * Set divider line's left and right margin
     *
     * @param margin Margin left and right for divider line
     */
    public void setDividerMargin(@Px float margin) {
        setDividerMargin(margin, margin);
    }

    /**
     * Set line number margin left
     */
    public void setLineNumberMarginLeft(@Px float lineNumberMarginLeft) {
        this.lineNumberMarginLeft = lineNumberMarginLeft;
        requestLayoutIfNeeded();
        host.invalidate();
    }

    /**
     * @see #setLineNumberMarginLeft(float)
     */
    @Px
    public float getLineNumberMarginLeft() {
        return lineNumberMarginLeft;
    }

    /**
     * @return Width of divider line
     * @see CodeEditor#setDividerWidth(float)
     */
    @Px
    public float getDividerWidth() {
        return dividerWidth;
    }

    /**
     * Set divider line's width
     *
     * @param dividerWidth Width of divider line
     */
    public void setDividerWidth(@Px float dividerWidth) {
        if (dividerWidth < 0) {
            throw new IllegalArgumentException("width can not be under zero");
        }
        this.dividerWidth = dividerWidth;
        requestLayoutIfNeeded();
        host.invalidate();
    }

    /**
     * @return Typeface of line number
     * @see CodeEditor#setTypefaceLineNumber(Typeface)
     */
    public Typeface getTypefaceLineNumber() {
        return renderer.getPaintOther().getTypeface();
    }

    /**
     * Set line number's typeface
     *
     * @param typefaceLineNumber New typeface
     */
    public void setTypefaceLineNumber(Typeface typefaceLineNumber) {
        renderer.setTypefaceLineNumber(typefaceLineNumber);
        requestLayoutIfNeeded();
    }

    /**
     * @return Typeface of text
     * @see CodeEditor#setTypefaceText(Typeface)
     */
    public Typeface getTypefaceText() {
        return renderer.getPaint().getTypeface();
    }

    /**
     * Set text's typeface
     *
     * @param typefaceText New typeface
     */
    public void setTypefaceText(Typeface typefaceText) {
        renderer.setTypefaceText(typefaceText);
        requestLayoutIfNeeded();
    }

    /**
     * @see #setTextScaleX(float)
     */
    public float getTextScaleX() {
        return renderer.getPaint().getTextScaleX();
    }

    /**
     * Set text scale x of Paint
     *
     * @see io.github.rosemoe.sora.graphics.Paint#setTextScaleX(float)
     * @see #getTextScaleX()
     */
    public void setTextScaleX(float textScaleX) {
        renderer.setTextScaleX(textScaleX);
    }

    /**
     * @see #setTextLetterSpacing(float)
     */
    public float getTextLetterSpacing() {
        return renderer.getPaint().getLetterSpacing();
    }

    /**
     * Set letter spacing of Paint
     *
     * @see io.github.rosemoe.sora.graphics.Paint#setLetterSpacing(float)
     * @see #getTextLetterSpacing()
     */
    public void setTextLetterSpacing(float textLetterSpacing) {
        renderer.setLetterSpacing(textLetterSpacing);
        requestLayoutIfNeeded();
    }

    /**
     * @return Line number align
     * @see CodeEditor#setLineNumberAlign(Paint.Align)
     */
    public Paint.Align getLineNumberAlign() {
        return lineNumberAlign;
    }

    /**
     * Set line number align
     *
     * @param align Align for line number
     */
    public void setLineNumberAlign(Paint.Align align) {
        if (align == null) {
            align = Paint.Align.LEFT;
        }
        lineNumberAlign = align;
        host.invalidate();
    }

    /**
     * Width for insert cursor
     *
     * @param width Cursor width
     */
    public void setCursorWidth(@Px float width) {
        if (width < 0) {
            throw new IllegalArgumentException("width can not be under zero");
        }
        insertSelectionWidth = width;
        host.invalidate();
    }

    @Px
    public float getInsertSelectionWidth() {
        return insertSelectionWidth;
    }

    /**
     * Border width for text border
     */
    public void setTextBorderWidth(@Px float width) {
        if (width < 0) {
            throw new IllegalArgumentException("width can not be under zero");
        }
        textBorderWidth = width;
        host.invalidate();
    }

    /**
     * @see #setTextBorderWidth(float)
     */
    @Px
    public float getTextBorderWidth() {
        return textBorderWidth;
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
        return cursor;
    }

    /**
     * Get line count
     *
     * @return line count
     */
    public int getLineCount() {
        return text.getLineCount();
    }

    /**
     * Get first visible line on screen
     *
     * @return first visible line
     */
    public int getFirstVisibleLine() {
        try {
            return layout.getLineNumberForRow(getFirstVisibleRow());
        } catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }

    /**
     * Get first visible row on screen
     *
     * @return first visible row
     */
    public int getFirstVisibleRow() {
        return Math.max(0, getOffsetY() / getRowHeight());
    }

    /**
     * Get last visible row on screen.
     *
     * @return last visible row
     */
    public int getLastVisibleRow() {
        return Math.max(0, Math.min(layout.getRowCount() - 1, (getOffsetY() + host.getHeight()) / getRowHeight()));
    }

    /**
     * Get last visible line on screen
     *
     * @return last visible line
     */
    public int getLastVisibleLine() {
        try {
            return layout.getLineNumberForRow(getLastVisibleRow());
        } catch (IndexOutOfBoundsException e) {
            return getLineCount() - 1;
        }
    }

    /**
     * Checks whether this row is visible on screen
     *
     * @param row Row to check
     * @return Whether visible
     */
    public boolean isRowVisible(int row) {
        return (getFirstVisibleRow() <= row && row <= getLastVisibleRow());
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
        lineSpacingAdd = add;
        lineSpacingMultiplier = mult;
    }

    /**
     * Gets the line spacing extra space
     *
     * @return the extra space that is added to the height of each lines of this TextView.
     * @see #setLineSpacing(float, float)
     * @see #getLineSpacingMultiplier()
     */
    public float getLineSpacingExtra() {
        return lineSpacingAdd;
    }

    /**
     * @param lineSpacingExtra The value in pixels that should be added to each line other than the last line.
     *                         *            This will be applied after the multiplier
     */
    public void setLineSpacingExtra(float lineSpacingExtra) {
        lineSpacingAdd = lineSpacingExtra;
        host.invalidate();
    }

    /**
     * @return the value by which each line's height is multiplied to get its actual height.
     * @see #setLineSpacingMultiplier(float)
     */
    public float getLineSpacingMultiplier() {
        return lineSpacingMultiplier;
    }

    /**
     * @param lineSpacingMultiplier The value by which each line height other than the last line will be multiplied
     *                              *             by. Default 1.0f
     */
    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        this.lineSpacingMultiplier = lineSpacingMultiplier;
        host.invalidate();
    }

    /**
     * Get actual line spacing in pixels.
     */
    public int getLineSpacingPixels() {
        var metrics = renderer.metricsText;
        return ((int) ((metrics.descent - metrics.ascent) * (lineSpacingMultiplier - 1f) + lineSpacingAdd)) / 2 * 2;
    }

    /**
     * Get baseline directly
     *
     * @param row Row
     * @return baseline y offset
     */
    public int getRowBaseline(int row) {
        var lineSpacing = getLineSpacingPixels();
        var metrics = renderer.metricsText;
        return Math.max(1, metrics.descent - metrics.ascent + lineSpacing) * (row + 1) - metrics.descent - lineSpacing / 2;
    }

    /**
     * Get row height
     *
     * @return height of single row
     */
    public int getRowHeight() {
        var metrics = renderer.metricsText;
        // Do not let the row height be zero...
        return Math.max(1, metrics.descent - metrics.ascent + getLineSpacingPixels());
    }

    /**
     * Get row top y offset
     *
     * @param row Row
     * @return top y offset
     */
    public int getRowTop(int row) {
        return getRowHeight() * row;
    }

    /**
     * Get row bottom y offset
     *
     * @param row Row
     * @return Bottom y offset
     */
    public int getRowBottom(int row) {
        return getRowHeight() * (row + 1);
    }

    /**
     * Get the top of text in target row
     */
    public int getRowTopOfText(int row) {
        return getRowTop(row) + getLineSpacingPixels() / 2;
    }

    /**
     * Get the bottom of text in target row
     */
    public int getRowBottomOfText(int row) {
        return getRowBottom(row) - getLineSpacingPixels() / 2;
    }

    /**
     * Get the height of text in row
     */
    public int getRowHeightOfText() {
        var metrics = renderer.metricsText;
        return metrics.descent - metrics.ascent;
    }

    /**
     * Get scroll x
     *
     * @return scroll x
     */
    public int getOffsetX() {
        return touchHandler.getScroller().getCurrX();
    }

    /**
     * Get scroll y
     *
     * @return scroll y
     */
    public int getOffsetY() {
        return touchHandler.getScroller().getCurrY();
    }

    /**
     * Indicate whether the layout is working
     */
    @UnsupportedUserUsage
    public void setLayoutBusy(boolean busy) {
        if (layoutBusy && !busy) {
            if (wordwrap && touchHandler.positionNotApplied) {
                touchHandler.positionNotApplied = false;
                int line = IntPair.getFirst(touchHandler.memoryPosition);
                int column = IntPair.getSecond(touchHandler.memoryPosition);
                // Compute new scroll position
                var row = ((WordwrapLayout) layout).findRow(line, column);
                var afterScrollY = row * getRowHeight() - touchHandler.focusY;
                var scroller = touchHandler.getScroller();
                dispatchEvent(new ScrollEvent(this, scroller.getCurrX(),
                        scroller.getCurrY(), 0, (int) afterScrollY, ScrollEvent.CAUSE_SCALE_TEXT));
                scroller.startScroll(0, (int) afterScrollY, 0, 0, 0);
                scroller.abortAnimation();
            }
            // IMPORTANT restart input after clearing the busy flag
            // otherwise, the connection may fallback to inactive mode
            this.layoutBusy = false;
            restartInput();
            host.postInvalidate();
            dispatchEvent(new LayoutStateChangeEvent(this, false));
            return;
        }
        if (layoutBusy == busy) {
            return;
        }
        this.layoutBusy = busy;
        dispatchEvent(new LayoutStateChangeEvent(this, busy));
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
        return editable && !layoutBusy && !isFormatting();
    }

    /**
     * @see #setEditable(boolean)
     */
    public boolean getEditable() {
        return editable;
    }

    /**
     * Set whether text can be edited
     *
     * @param editable Editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        if (!editable) {
            hideSoftInput();
            snippetController.stopSnippet();
        }
    }

    /**
     * @return Whether allow scaling
     * @see CodeEditor#setScalable(boolean)
     */
    public boolean isScalable() {
        return scalable;
    }

    /**
     * Allow scale text size by thumb
     *
     * @param scale Whether allow
     */
    public void setScalable(boolean scale) {
        scalable = scale;
    }

    public boolean isBlockLineEnabled() {
        return blockLineEnabled;
    }

    public void setBlockLineEnabled(boolean enabled) {
        blockLineEnabled = enabled;
        host.invalidate();
    }

    /**
     * Begin a rejection on composing texts
     */
    public void beginComposingTextRejection() {
        rejectComposingCount++;
    }

    /**
     * If the editor accepts composing text now, according to composing text rejection count
     */
    public boolean acceptsComposingText() {
        return rejectComposingCount == 0;
    }

    /**
     * End a rejection on composing texts
     */
    public void endComposingTextRejection() {
        rejectComposingCount--;
        if (rejectComposingCount < 0) {
            rejectComposingCount = 0;
        }
    }

    /**
     * Check if there is a mouse inside editor, hovering
     */
    public boolean hasMouseHovering() {
        return mouseHover;
    }

    /**
     * Check if there is a mouse inside editor with any button pressed
     */
    public boolean hasMousePressed() {
        return mouseButtonPressed;
    }

    /**
     * Check if editor is in mouse mode.
     *
     * @see DirectAccessProps#mouseMode
     */
    public boolean isInMouseMode() {
        switch (props.mouseMode) {
            case DirectAccessProps.MOUSE_MODE_ALWAYS -> {
                return true;
            }
            case DirectAccessProps.MOUSE_MODE_NEVER -> {
                return false;
            }
        }
        // MOUSE_MODE_AUTO
        return hasMouseHovering() || hasMousePressed();
    }

    /**
     * Get the target cursor to move when shift is pressed
     */
    public CharPosition getSelectingTarget() {
        if (cursor.left().equals(selectionAnchor)) {
            return cursor.right();
        } else {
            return cursor.left();
        }
    }

    /**
     * Make sure the moving selection is visible
     */
    public void ensureSelectingTargetVisible() {
        if (cursor.left().equals(selectionAnchor)) {
            // Ensure right selection visible
            ensureSelectionVisible();
        } else {
            ensurePositionVisible(cursor.getLeftLine(), cursor.getLeftColumn());
        }
    }

    public void ensureSelectionAnchorAvailable() {
        if (selectionAnchor == null || !text.isValidPosition(selectionAnchor)) {
            selectionAnchor = cursor.right();
        }
    }

    /**
     * Move or extend selection, according to {@code extend} param.
     *
     * @param extend True if you want to extend selection.
     */
    public void moveOrExtendSelection(@NonNull SelectionMovement movement, boolean extend) {
        if (extend) {
            extendSelection(movement);
        } else {
            moveSelection(movement);
        }
    }

    /**
     * Extend the selection, based on the selection anchor (select text)
     */
    public void extendSelection(@NonNull SelectionMovement movement) {
        ensureSelectionAnchorAvailable();
        CharPosition sel = movement.getPositionAfterMovement(this, getSelectingTarget());
        setSelectionRegion(selectionAnchor.line, selectionAnchor.column, sel.line, sel.column, false, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE);
        if (movement == SelectionMovement.PAGE_UP) {
            touchHandler.scrollBy(0f, -host.getHeight(), true);
        } else if (movement == SelectionMovement.PAGE_DOWN) {
            touchHandler.scrollBy(0f, host.getHeight(), true);
        }
        ensureSelectingTargetVisible();
    }

    /**
     * Move the selection. Selected text will be de-selected.
     */
    public void moveSelection(@NonNull SelectionMovement movement) {
        if (cursor.isSelected()) {
            if (movement == SelectionMovement.LEFT) {
                setSelection(cursor.getLeftLine(), cursor.getLeftColumn(), SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE);
                return;
            }
            if (movement == SelectionMovement.RIGHT) {
                setSelection(cursor.getRightLine(), cursor.getRightColumn(), SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE);
                return;
            }
        }
        CharPosition pos;
        switch (movement.getBasePosition()) {
            case LEFT_SELECTION -> pos = cursor.left();
            case RIGHT_SELECTION -> pos = cursor.right();
            default -> {
                ensureSelectionAnchorAvailable();
                pos = selectionAnchor;
            }
        }
        CharPosition sel = movement.getPositionAfterMovement(this, pos);
        if (movement == SelectionMovement.PAGE_UP) {
            touchHandler.scrollBy(0f, -host.getHeight(), true);
        } else if (movement == SelectionMovement.PAGE_DOWN) {
            touchHandler.scrollBy(0f, host.getHeight(), true);
        }
        setSelection(sel.line, sel.column, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE);
    }

    /**
     * Move selection to given position
     *
     * @param line   The line to move
     * @param column The column to move
     */
    public void setSelection(int line, int column) {
        setSelection(line, column, SelectionChangeEvent.CAUSE_UNKNOWN);
    }

    /**
     * Move selection to given position
     *
     * @param line   The line to move
     * @param column The column to move
     */
    public void setSelection(int line, int column, int cause) {
        setSelection(line, column, true, cause);
    }

    /**
     * Move selection to given position
     *
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    public void setSelection(int line, int column, boolean makeItVisible) {
        setSelection(line, column, makeItVisible, SelectionChangeEvent.CAUSE_UNKNOWN);
    }

    /**
     * Move selection to given position
     *
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    public void setSelection(int line, int column, boolean makeItVisible, int cause) {
        cursorAnimator.markStartPos();
        if (column > 0 && Character.isHighSurrogate(text.charAt(line, column - 1))) {
            column++;
            if (column > text.getColumnCount(line)) {
                column--;
            }
        }
        cursor.set(line, column);
        if (highlightCurrentBlock) {
            cursorPosition = findCursorBlock();
        }
        updateCursor();
        updateSelection();
        if (editable && !touchHandler.hasAnyHeldHandle() && acceptsComposingText()) {
            cursorAnimator.markEndPos();
            cursorAnimator.start();
        }

        // Update cursor anchor
        selectionAnchor = cursor.right();

        renderContext.invalidateRenderNodes();
        if (makeItVisible) {
            ensurePositionVisible(line, column);
        } else {
            host.invalidate();
        }
        onSelectionChanged(cause);
    }

    /**
     * Select all text
     */
    public void selectAll() {
        setSelectionRegion(0, 0, getLineCount() - 1, getText().getColumnCount(getLineCount() - 1));
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
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, cause);
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
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, SelectionChangeEvent.CAUSE_UNKNOWN);
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
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, makeRightVisible, SelectionChangeEvent.CAUSE_UNKNOWN);
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
        host.requestFocus();
        int start = getText().getCharIndex(lineLeft, columnLeft);
        int end = getText().getCharIndex(lineRight, columnRight);
        if (start == end) {
            setSelection(lineLeft, columnLeft, makeRightVisible, cause);
            return;
        }
        if (start > end) {
            setSelectionRegion(lineRight, columnRight, lineLeft, columnLeft, makeRightVisible, cause);
            Log.w("CodeEditor", "setSelectionRegion() error: start > end:start = " + start + " end = " + end + " lineLeft = " + lineLeft + " columnLeft = " + columnLeft + " lineRight = " + lineRight + " columnRight = " + columnRight);
            return;
        }
        cursorAnimator.cancel();
        boolean lastState = cursor.isSelected();
        if (columnLeft > 0) {
            int column = columnLeft - 1;
            char ch = text.charAt(lineLeft, column);
            if (Character.isHighSurrogate(ch)) {
                columnLeft++;
                if (columnLeft > text.getColumnCount(lineLeft)) {
                    columnLeft--;
                }
            }
        }
        if (columnRight > 0) {
            int column = columnRight - 1;
            char ch = text.charAt(lineRight, column);
            if (Character.isHighSurrogate(ch)) {
                columnRight++;
                if (columnRight > text.getColumnCount(lineRight)) {
                    columnRight--;
                }
            }
        }
        cursor.setLeft(lineLeft, columnLeft);
        cursor.setRight(lineRight, columnRight);
        updateCursor();
        updateSelection();
        renderContext.invalidateRenderNodes();

        // Update selection anchor
        if (!cursor.left().equals(selectionAnchor) && !cursor.right().equals(selectionAnchor)) {
            selectionAnchor = cursor.right();
        }

        if (makeRightVisible) {
            if (cause == SelectionChangeEvent.CAUSE_SEARCH) {
                ensurePositionVisible(lineLeft, columnLeft);
                lastMakeVisible = 0;
                ensurePositionVisible(lineRight, columnRight);
            } else {
                ensurePositionVisible(lineRight, columnRight);
            }
        } else {
            host.invalidate();
        }
        onSelectionChanged(cause);
    }

    /**
     * Get system clipboard manager used by editor
     */
    public ClipboardManager getClipboardManager() {
        return clipboardManager;
    }

    /**
     * Paste text from clip board
     */
    public void pasteText() {
        try {
            ClipData clip;
            if (!clipboardManager.hasPrimaryClip() || (clip = clipboardManager.getPrimaryClip()) == null) {
                return;
            }
            pasteText(ClipDataUtils.clipDataToString(clip));
        } catch (Exception e) {
            Log.w(CodeEditor.LOG_TAG, "Error pasting text to editor", e);
            Toast.makeText(host.getContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Paste external text into editor
     */
    public void pasteText(@Nullable CharSequence text) {
        if (text != null && inputConnection != null) {
            inputConnection.commitText(text, 1);
            if (props.formatPastedText) {
                formatCodeAsync(lastInsertion.getStart(), lastInsertion.getEnd());
            }
            notifyIMEExternalCursorChange();
        }
    }

    /**
     * Copy text to clipboard.
     */
    public void copyText() {
        copyText(true);
    }

    /**
     * Copy text to clipboard.
     *
     * @param shouldCopyLine State whether the editor should select whole line if
     *                       cursor is not in selection mode.
     */
    public void copyText(boolean shouldCopyLine) {
        if (cursor.isSelected()) {
            copyTextToClipboard(getText(), cursor.getLeft(), cursor.getRight());
        } else if (shouldCopyLine) {
            copyLine();
        } else {
            var text = getLineSeparator().getContent();
            copyTextToClipboard(text, 0, text.length());
        }
    }

    /**
     * Copy the given text region to clipboard, and follow editor's IPC properties.
     */
    public void copyTextToClipboard(@NonNull CharSequence text, int start, int end) {
        if (end < start) {
            return;
        }
        if (end - start > props.clipboardTextLengthLimit) {
            Toast.makeText(host.getContext(), I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            var clip = (text instanceof Content) ? ((Content) text).substring(start, end) : text.subSequence(start, end).toString();
            clipboardManager.setPrimaryClip(ClipData.newPlainText(clip, clip));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof TransactionTooLargeException) {
                Toast.makeText(host.getContext(), I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large), Toast.LENGTH_SHORT).show();
            } else {
                Log.w(CodeEditor.LOG_TAG, e);
                Toast.makeText(host.getContext(), e.getClass().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Copies the current line to clipboard.
     */
    public void copyLine() {
        final var cursor = getCursor();
        if (cursor.isSelected()) {
            copyText();
            return;
        }

        final var line = cursor.left().line;
        setSelectionRegion(line, 0, line, getText().getColumnCount(line));
        copyText(false);
    }

    /**
     * Copy text to clipboard and delete them
     */
    public void cutText() {
        if (cursor.isSelected()) {
            copyText();
            deleteText();
            notifyIMEExternalCursorChange();
        } else {
            cutLine();
        }
    }

    /**
     * Copy the current line to clipboard and delete it.
     */
    public void cutLine() {
        final var cursor = getCursor();
        if (cursor.isSelected()) {
            cutText();
            return;
        }

        final var left = cursor.left();
        final var line = left.line;
        final var column = getText().getColumnCount(left.line);

        if (line + 1 == getLineCount()) {
            int columnCount = getText().getColumnCount(line);
            if (columnCount == 0) {
                // copy line separator
                copyText(false);
                return;
            }
            setSelectionRegion(line, 0, line, getText().getColumnCount(line));
        } else {
            setSelectionRegion(line, 0, line + 1, 0);
        }

        cutText();
        if (props.placeSelOnPreviousLineAfterCut) {
            moveSelection(SelectionMovement.LEFT);
        }
    }

    /**
     * Duplicates the current line.
     * Does not selects the duplicated line.
     */
    public void duplicateLine() {
        final var cursor = getCursor();
        if (cursor.isSelected()) {
            duplicateSelection();
            return;
        }

        final var left = cursor.left();
        setSelectionRegion(left.line, 0, left.line, getText().getColumnCount(left.line), true);
        duplicateSelection("\n", false);
    }

    /**
     * Copies the current selection and pastes it at the right selection handle,
     * then selects the duplicated content.
     */
    public void duplicateSelection() {
        duplicateSelection(true);
    }

    /**
     * Copies the current selection and pastes it at the right selection handle.
     *
     * @param selectDuplicate Whether to select the duplicated content.
     */
    public void duplicateSelection(boolean selectDuplicate) {
        duplicateSelection("", selectDuplicate);
    }

    /**
     * Copies the current selection, add the <code>prefix</code> to it
     * and pastes it at the right selection handle.
     *
     * @param prefix          The prefix for the selected content.
     * @param selectDuplicate Whether to select the duplicated content.
     */
    public void duplicateSelection(String prefix, boolean selectDuplicate) {
        final var cursor = getCursor();
        if (!cursor.isSelected()) {
            return;
        }

        final var left = cursor.left();
        final var right = cursor.right().fromThis();
        final var sub = getText().subContent(left.line, left.column, right.line, right.column);

        setSelection(right.line, right.column);
        commitText(prefix + sub, false);

        if (selectDuplicate) {
            final var r = cursor.right();
            setSelectionRegion(right.line, right.column, r.line, r.column);
        }
    }

    /**
     * Selects the word at the left selection handle.
     */
    public void selectCurrentWord() {
        final var left = getCursor().left();
        selectWord(left.line, left.column);
    }

    /**
     * Selects the word at the given character position.
     *
     * @param line   The line.
     * @param column The column.
     */
    public void selectWord(int line, int column) {
        final var range = getWordRange(line, column);
        final var start = range.getStart();
        final var end = range.getEnd();
        setSelectionRegion(start.line, start.column, end.line, end.column, SelectionChangeEvent.CAUSE_LONG_PRESS);
    }

    /**
     * @see #getWordRange(int, int, boolean)
     */
    public TextRange getWordRange(final int line, final int column) {
        return getWordRange(line, column, props.useICULibToSelectWords);
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
        return Chars.getWordRange(getText(), line, column, useIcu);
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
        return text;
    }

    /**
     * Set the text to be displayed.
     * With no extra arguments.
     *
     * @param text the new text you want to display
     */
    public void setText(@Nullable CharSequence text) {
        setText(text, true, null);
    }

    /**
     * Get extra argument set by {@link CodeEditor#setText(CharSequence, Bundle)}
     */
    @NonNull
    public Bundle getExtraArguments() {
        return extraArguments;
    }

    /**
     * Sets the text to be displayed.
     *
     * @param text           the new text you want to display
     * @param extraArguments Extra arguments for the document. This {@link Bundle} object is passed
     *                       to all languages and plugins in editor.
     */
    public void setText(@Nullable CharSequence text, @Nullable Bundle extraArguments) {
        setText(text, true, extraArguments);
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
        if (text == null) {
            text = "";
        }

        if (this.text != null) {
            this.text.removeContentListener(null);
            this.text.resetBatchEdit();
        }
        this.extraArguments = extraArguments == null ? new Bundle() : extraArguments;
        lastInsertion = null;
        if (reuseContentObject && text instanceof Content) {
            this.text = (Content) text;
            this.text.resetBatchEdit();
            renderer.updateTimestamp();
        } else {
            this.text = new Content(text);
        }
        styleDelegate.reset();
        textStyles = null;
        cursor = this.text.getCursor();
        selectionAnchor = cursor.right();
        touchHandler.reset();
        this.text.addContentListener(this);
        this.text.setUndoEnabled(undoEnabled);
        this.text.setBidiEnabled(true);
        renderContext.reset(this.text.getLineCount());
        renderer.onEditorFullTextUpdate();

        if (editorLanguage != null) {
            editorLanguage.getAnalyzeManager().reset(new ContentReference(this.text), this.extraArguments);
            editorLanguage.getFormatter().cancel();
        }
        inlayHints = null;

        dispatchEvent(new ContentChangeEvent(this, ContentChangeEvent.ACTION_SET_NEW_TEXT, new CharPosition(), this.text.getIndexer().getCharPosition(getLineCount() - 1, this.text.getColumnCount(getLineCount() - 1)), this.text, false));
        createLayout();
        restartInput();
        host.requestLayout();
        renderContext.invalidateRenderNodes();
        host.invalidate();
    }

    /**
     * Set the editor's text size in sp unit. This value must be greater than 0
     *
     * @param textSize the editor's text size in <strong>Sp</strong> units.
     */
    public void setTextSize(float textSize) {
        Context context = host.getContext();
        Resources res;

        if (context == null) {
            res = Resources.getSystem();
        } else {
            res = context.getResources();
        }

        setTextSizePx(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, res.getDisplayMetrics()));
    }

    /**
     * Render ASCII Function characters
     *
     * @see #isRenderFunctionCharacters()
     */
    public void setRenderFunctionCharacters(boolean renderFunctionCharacters) {
        if (this.renderFunctionCharacters != renderFunctionCharacters) {
            this.renderFunctionCharacters = renderFunctionCharacters;
            renderer.onTextStyleUpdate();
            requestLayoutIfNeeded();
            createLayout();
            host.invalidate();
        }
    }

    /**
     * @see #setRenderFunctionCharacters(boolean)
     */
    public boolean isRenderFunctionCharacters() {
        return renderFunctionCharacters;
    }

    /**
     * Subscribe event of the given type.
     *
     * @see EventManager#subscribeEvent(Class, EventReceiver)
     */
    public <T extends
            Event> SubscriptionReceipt<T> subscribeEvent(Class<T> eventType, EventReceiver<T> receiver) {
        return eventManager.subscribeEvent(eventType, receiver);
    }

    /**
     * Subscribe event of the given type, without {@link Unsubscribe}.
     *
     * @see EventManager#subscribeEvent(Class, EventReceiver)
     */
    public <T extends
            Event> SubscriptionReceipt<T> subscribeAlways(Class<T> eventType, EventManager.NoUnsubscribeReceiver<T> receiver) {
        return eventManager.subscribeAlways(eventType, receiver);
    }

    /**
     * Dispatch the given event
     *
     * @see EventManager#dispatchEvent(Event)
     */
    public <T extends Event> int dispatchEvent(T event) {
        return eventManager.dispatchEvent(event);
    }

    /**
     * Create a new {@link EventManager} instance that can be used to subscribe events in editor,
     * as a child instance of editor.
     *
     * @return Child EventManager instance
     */
    @NonNull
    public EventManager createSubEventManager() {
        return new EventManager(eventManager);
    }

    /**
     * Check whether the editor is currently performing a format operation
     *
     * @return whether the editor is currently formatting
     */
    public boolean isFormatting() {
        return editorLanguage.getFormatter().isRunning();
    }

    /**
     * Check whether line numbers are shown
     *
     * @return The state of line number displaying
     */
    public boolean isLineNumberEnabled() {
        return lineNumberEnabled;
    }

    /**
     * Set whether we should display line numbers
     *
     * @param lineNumberEnabled The state of line number displaying
     */
    public void setLineNumberEnabled(boolean lineNumberEnabled) {
        if (lineNumberEnabled != this.lineNumberEnabled && isWordwrap()) {
            createLayout();
        }
        this.lineNumberEnabled = lineNumberEnabled;
        host.invalidate();
    }

    /**
     * Get the paint of the editor
     * You should not change text size and other attributes that are related to text measuring by the object
     *
     * @return The paint which is used by the editor now
     */
    @NonNull
    public io.github.rosemoe.sora.graphics.Paint getTextPaint() {
        return renderer.getPaint();
    }

    public io.github.rosemoe.sora.graphics.Paint getOtherPaint() {
        return renderer.getPaintOther();
    }

    public io.github.rosemoe.sora.graphics.Paint getGraphPaint() {
        return renderer.getPaintGraph();
    }

    /**
     * Get the ColorScheme object of this editor
     * You can config colors of some regions, texts and highlight text
     *
     * @return ColorScheme object using
     */
    @NonNull
    public EditorColorScheme getColorScheme() {
        return colorScheme;
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
        if (colorScheme != null) {
            colorScheme.detachEditor(this);
        }
        colorScheme = colors;
        // Automatically invoke scheme updating related methods
        colors.attachEditor(this);
        host.invalidate();
    }

    /**
     * Move selection to line start with scrolling
     *
     * @param line Line index to jump
     */
    public void jumpToLine(int line) {
        setSelection(line, 0);
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
        if (!isEditable()) {
            return;
        }
        if (cursor.isSelected()) {
            setSelection(cursor.getLeftLine(), cursor.getLeftColumn());
        }
        isInLongSelect = true;
        host.invalidate();
    }

    /**
     * Checks whether long select mode is started
     */
    public boolean isInLongSelect() {
        return isInLongSelect;
    }

    /**
     * Marks long select mode is end.
     * This does nothing but set the flag to false.
     */
    public void endLongSelect() {
        isInLongSelect = false;
    }

    /**
     * Rerun analysis forcibly
     */
    public void rerunAnalysis() {
        if (editorLanguage != null) {
            editorLanguage.getAnalyzeManager().rerun();
        }
    }

    /**
     * Get analyze result.
     * <strong>Do not make changes to it or read concurrently</strong>
     */
    @Nullable
    public Styles getStyles() {
        return textStyles;
    }

    @UiThread
    public void setStyles(@Nullable Styles styles) {
        textStyles = styles;
        if (highlightCurrentBlock) {
            cursorPosition = findCursorBlock();
        }
        renderContext.invalidateRenderNodes();
        renderer.updateTimestamp();
        host.invalidate();
    }

    @UiThread
    public void updateStyles(@NonNull Styles styles, @Nullable StyleUpdateRange range) {
        if (textStyles != styles || range == null) {
            setStyles(styles);
            return;
        }
        if (highlightCurrentBlock) {
            cursorPosition = findCursorBlock();
        }
        renderContext.updateForRange(range);
        renderer.updateTimestamp();
        host.invalidate();
    }

    @Nullable
    public DiagnosticsContainer getDiagnostics() {
        return diagnostics;
    }

    @UiThread
    public void setDiagnostics(@Nullable DiagnosticsContainer diagnostics) {
        if (this.diagnostics != null) {
            this.diagnostics.detachEditor();
        }

        this.diagnostics = diagnostics;

        if (this.diagnostics != null) {
            this.diagnostics.attachEditor(this);
        }
        host.invalidate();
    }

    public void setInlayHints(@Nullable InlayHintsContainer inlayHints) {
        var affectedLines = new MutableIntSet();
        var oldInlayHints = this.inlayHints;
        if (oldInlayHints != null) {
            affectedLines.addAll(oldInlayHints.getLineNumbers());
        }
        if (inlayHints != null) {
            affectedLines.addAll(inlayHints.getLineNumbers());
        }
        this.inlayHints = inlayHints;
        var range = new IntSetUpdateRange(affectedLines);
        if (!layoutBusy) {
            layout.invalidateLines(range);
        } else {
            createLayout();
        }
        renderContext.invalidateRenderNodes();
    }

    @Nullable
    public InlayHintsContainer getInlayHints() {
        return inlayHints;
    }

    @UiThread
    public void setHighlightTexts(@Nullable HighlightTextContainer highlightTexts) {
        var affectedLines = new MutableIntSet();
        var oldHighlights = this.highlightTextContainer;
        if (oldHighlights != null) {
            var lines = oldHighlights.getLineNumbers();
            for (int line : lines) {
                affectedLines.add(line);
            }
        }
        this.highlightTextContainer = highlightTexts;
        if (highlightTexts != null) {
            var lines = highlightTexts.getLineNumbers();
            for (int line : lines) {
                affectedLines.add(line);
            }
        }
        if (affectedLines._size == 0) {
            return;
        }
        if (layout == null || renderContext == null) {
            host.invalidate();
            return;
        }
        var range = new IntSetUpdateRange(affectedLines);
        if (!layoutBusy) {
            layout.invalidateLines(range);
        } else {
            createLayout();
        }
        renderContext.invalidateRenderNodes();
        host.invalidate();
    }

    @Nullable
    public HighlightTextContainer getHighlightTexts() {
        return highlightTextContainer;
    }

    /**
     * Hide auto complete window if shown
     */
    public void hideAutoCompleteWindow() {
        if (completionWindow != null) {
            completionWindow.hide();
        }
    }

    /**
     * Get cursor code block index
     *
     * @return index of cursor's code block
     */
    public int getBlockIndex() {
        return cursorPosition;
    }

    /**
     * Display soft input method for self
     */
    public void showSoftInput() {
        if (isEditable() && host.isEnabled()) {
            // Note that we always try to get focused at this time.
            if (host.isInTouchMode() && !host.isFocused()) {
                host.requestFocusFromTouch();
            }
            if (!host.isFocused()) {
                host.requestFocus();
            }

            if (checkSoftInputEnabled())
                host.showKeyboard();
        }
        host.invalidate();
    }

    /**
     * Hide soft input
     */
    public void hideSoftInput() {
        host.hideKeyboard();
    }

    /**
     * Check whether the soft keyboard is enabled for this editor. Unlike {@link #isSoftKeyboardEnabled()},
     * this method also checks whether a hardware keyboard is connected.
     *
     * @return Whether the editor should show soft keyboard.
     * @see #isSoftKeyboardEnabled()
     * @see #isDisableSoftKbdIfHardKbdAvailable()
     */
    public boolean checkSoftInputEnabled() {
        if (isDisableSoftKbdIfHardKbdAvailable()
                && KeyboardUtils.INSTANCE.isHardKeyboardConnected(host.getContext())) {
            return false;
        }
        return isSoftKeyboardEnabled();
    }

    /**
     * Set whether the soft keyboard is enabled for this editor. Set to {@code true} by default.
     *
     * @param isEnabled Whether the soft keyboard is enabled.
     */
    public void setSoftKeyboardEnabled(boolean isEnabled) {
        if (isSoftKeyboardEnabled() == isEnabled) {
            // no need to do anything
            return;
        }

        this.isSoftKbdEnabled = isEnabled;
        hideSoftInput();
        restartInput();
    }

    /**
     * Returns whether the soft keyboard is enabled.
     *
     * @return Whether the soft keyboard is enabled.
     */
    public boolean isSoftKeyboardEnabled() {
        return this.isSoftKbdEnabled;
    }

    /**
     * Set whether the soft keyboard should be disabled for this editor if a hardware keyboard is
     * connected to the device. Set to {@code true} by default.
     *
     * @param isDisabled Whether the soft keyboard should be enabled if hardware keyboard is connected.
     */
    public void setDisableSoftKbdIfHardKbdAvailable(boolean isDisabled) {
        if (isDisableSoftKbdIfHardKbdAvailable() == isDisabled) {
            // no need to do anything
            return;
        }

        this.isDisableSoftKbdOnHardKbd = isDisabled;
        hideSoftInput();
        restartInput();
    }

    /**
     * Returns whether the soft keyboard should be enabled if hardware keyboard is connected.
     *
     * @return Whether the soft keyboard should be enabled if hardware keyboard is connected.
     */
    public boolean isDisableSoftKbdIfHardKbdAvailable() {
        return isDisableSoftKbdOnHardKbd;
    }

    /**
     * Send current selection position to input method
     */
    public void updateSelection() {
        if (props.disallowSuggestions) {
            var index = new Random().nextInt();
            host.updateSelection(index, index, -1, -1);
            return;
        }
        if (inputConnection.composingText.preSetComposing) {
            return;
        }
        int candidatesStart = -1, candidatesEnd = -1;
        if (inputConnection.composingText.isComposing()) {
            try {
                candidatesStart = inputConnection.composingText.startIndex;
                candidatesEnd = inputConnection.composingText.endIndex;
            } catch (IndexOutOfBoundsException e) {
                // Ignored
            }
        }
        host.updateSelection(cursor.getLeft(), cursor.getRight(), candidatesStart, candidatesEnd);
    }

    /**
     * Update request result for monitoring request
     */
    public void updateExtractedText() {
        if (extractingTextRequest != null) {
            var text = extractText(extractingTextRequest);
            host.updateExtractedText(extractingTextRequest.token, text);
        }
    }

    /**
     * Set request needed to update when editor updates selection
     */
    public void setExtracting(@Nullable ExtractedTextRequest request) {
        if (getProps().disallowSuggestions) {
            extractingTextRequest = null;
            return;
        }
        extractingTextRequest = request;
    }

    /**
     * Extract text in editor for input method
     */
    public ExtractedText extractText(@NonNull ExtractedTextRequest request) {
        if (getProps().disallowSuggestions || getProps().disableTextExtracting) {
            return null;
        }
        Cursor cur = getCursor();
        ExtractedText text = new ExtractedText();
        int selBegin = cur.getLeft();
        int selEnd = cur.getRight();
        int startOffset = 0;
        if (request.hintMaxChars == 0) {
            request.hintMaxChars = props.maxIPCTextLength;
        }
        if (startOffset + request.hintMaxChars < selBegin) {
            startOffset = selBegin - request.hintMaxChars / 2;
            startOffset = Math.min(startOffset, selBegin); // Ensure not negative
        }
        text.text = inputConnection.getTextRegion(startOffset, startOffset + request.hintMaxChars, request.flags);
        text.startOffset = startOffset;
        text.selectionStart = selBegin - startOffset;
        text.selectionEnd = selEnd - startOffset;
        if (getKeyMetaStates().isSelecting()) {
            text.flags |= ExtractedText.FLAG_SELECTING;
        }
        return text;
    }

    /**
     * Notify input method that text has been changed for external reason
     */
    public void notifyIMEExternalCursorChange() {
        updateExtractedText();
        updateSelection();
        updateCursorAnchor();
        // Restart if composing
        if (inputConnection.composingText.isComposing()) {
            restartInput();
        }
    }

    /**
     * Restart the input connection.
     * Do not call this method randomly. Please refer to documentation first.
     *
     * @see InputConnection
     */
    public void restartInput() {
        host.restartInput();
    }

    /**
     * Send cursor position in text and on screen to input method
     */
    public void updateCursor() {
        updateCursorAnchor();
        updateExtractedText();
        if (text.getNestedBatchEdit() <= 1 && !inputConnection.composingText.isComposing()) {
            updateSelection();
        }
    }

    /**
     * Hide all built-in windows of the editor
     */
    public void hideEditorWindows() {
        completionWindow.cancelCompletion();
        completionWindow.hide();
        textActionWindow.dismiss();
        touchHandler.magnifier.dismiss();
        diagnosticTooltip.dismiss();
    }

    /**
     * Called by ColorScheme to notify invalidate
     *
     * @param type Color type changed
     */
    public void onColorUpdated(int type) {
        dispatchEvent(new ColorSchemeUpdateEvent(this));
        renderContext.invalidateRenderNodes();
        host.invalidate();
    }

    /**
     * Called by color scheme to init colors
     */
    public void onColorFullUpdate() {
        dispatchEvent(new ColorSchemeUpdateEvent(this));
        renderContext.invalidateRenderNodes();
        host.invalidate();
    }

    /**
     * Called by {@link EditorInputConnection}
     */
    public void onCloseConnection() {
        setExtracting(null);
        host.invalidate();
    }

    /**
     * This method is called once when the editor is created.
     */
    @NonNull
    public EditorRenderer onCreateRenderer() {
        return new EditorRenderer(this);
    }

    /**
     * Called when the text is edited or {@link CodeEditor#setSelection} is called
     */
    public void onSelectionChanged(int cause) {
        CharPosition oldLeft = null;
        CharPosition oldRight = null;
        final TextRange lastTextRange = this.lastSelectedTextRange;
        if (lastTextRange != null) {
            oldLeft = lastTextRange.getStart();
            oldRight = lastTextRange.getEnd();
        }
        dispatchEvent(new SelectionChangeEvent(this, oldLeft, oldRight, cause));
        this.lastSelectedTextRange = getCursorRange();
    }

    @Override
    public void beforeReplace(@NonNull Content content) {
        waitForNextChange = true;
        layout.beforeReplace(content);
    }

    @Override
    public void afterInsert(@NonNull Content content, int startLine, int startColumn, int endLine, int endColumn, @NonNull CharSequence insertedContent) {
        renderContext.updateForInsertion(startLine, endLine);
        renderer.updateTimestamp();
        styleDelegate.onTextChange();
        var start = text.getIndexer().getCharPosition(startLine, startColumn);
        var end = text.getIndexer().getCharPosition(endLine, endColumn);

        // Update spans
        try {
            if (textStyles != null) {
                textStyles.adjustOnInsert(start, end);
            }
            if (diagnostics != null) {
                diagnostics.shiftOnInsert(start.index, end.index);
            }
            if (inlayHints != null) {
                inlayHints.updateOnInsertion(startLine, startColumn, endLine, endColumn);
            }
            if (highlightTextContainer != null) {
                highlightTextContainer.updateOnInsertion(startLine, startColumn, endLine, endColumn);
            }
        } catch (Exception e) {
            Log.w(CodeEditor.LOG_TAG, "Update failure", e);
        }

        layout.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent);
        renderer.buildMeasureCacheForLines(startLine, endLine);
        checkForRelayout();

        editorLanguage.getAnalyzeManager().insert(start, end, insertedContent);
        touchHandler.hideInsertHandle();
        if (isEditable() && !cursor.isSelected() && !inputConnection.composingText.isComposing() && acceptsComposingText()) {
            cursorAnimator.markEndPos();
            cursorAnimator.start();
        }
        selectionAnchor = lastAnchorIsSelLeft ? cursor.left() : cursor.right();
        dispatchEvent(new ContentChangeEvent(this, ContentChangeEvent.ACTION_INSERT, start, end, insertedContent, text.isUndoManagerWorking()));
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION);
        lastInsertion = new TextRange(start.fromThis(), end.fromThis());
        waitForNextChange = false;
        ensureSelectionVisible();

        // Notify input method
        updateCursor();
    }

    @Override
    public void afterDelete(@NonNull Content content, int startLine, int startColumn, int endLine, int endColumn, @NonNull CharSequence deletedContent) {
        renderContext.updateForDeletion(startLine, endLine);
        renderer.updateTimestamp();
        styleDelegate.onTextChange();
        var start = text.getIndexer().getCharPosition(startLine, startColumn);
        var end = start.fromThis();
        end.column = endColumn;
        end.line = endLine;
        end.index = start.index + deletedContent.length();

        try {
            if (textStyles != null) {
                textStyles.adjustOnDelete(start, end);
            }
            if (diagnostics != null) {
                diagnostics.shiftOnDelete(start.index, end.index);
            }
            if (inlayHints != null) {
                inlayHints.updateOnDeletion(startLine, startColumn, endLine, endColumn);
            }
            if (highlightTextContainer != null) {
                highlightTextContainer.updateOnDeletion(startLine, startColumn, endLine, endColumn);
            }
        } catch (Exception e) {
            Log.w(CodeEditor.LOG_TAG, "Update failure", e);
        }

        layout.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent);
        renderer.buildMeasureCacheForLines(startLine, startLine + 1);
        checkForRelayout();

        if (isEditable() && !cursor.isSelected() && !waitForNextChange && !inputConnection.composingText.isComposing() && acceptsComposingText()) {
            cursorAnimator.markEndPos();
            cursorAnimator.start();
        }
        editorLanguage.getAnalyzeManager().delete(start, end, deletedContent);
        selectionAnchor = lastAnchorIsSelLeft ? cursor.left() : cursor.right();
        dispatchEvent(new ContentChangeEvent(this, ContentChangeEvent.ACTION_DELETE, start, end, deletedContent, text.isUndoManagerWorking()));
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION);

        if (!waitForNextChange) {
            updateCursor();
            ensureSelectionVisible();
            touchHandler.hideInsertHandle();
        }
    }

    @Override
    public void beforeModification(@NonNull Content content) {
        if (props.checkModificationThread && host.isAttachedToWindow()) {
            var handler = host.getHandler();
            if (handler != null) {
                if (handler.getLooper().getThread() != Thread.currentThread()) {
                    throw new RuntimeException("text is changed in wrong thread");
                }
            }
        }
        cursorAnimator.markStartPos();
        lastAnchorIsSelLeft = cursor.left().equals(selectionAnchor);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onFormatSucceed(@NonNull CharSequence applyContent, @Nullable TextRange cursorRange) {
        postInLifecycle(() -> {
            int line = cursor.getLeftLine();
            int column = cursor.getLeftColumn();
            int x = getOffsetX();
            int y = getOffsetY();
            var string = (applyContent instanceof Content) ? ((Content) applyContent).toStringBuilder() : applyContent;
            text.beginBatchEdit();
            text.delete(0, 0, text.getLineCount() - 1,
                    text.getColumnCount(text.getLineCount() - 1));
            text.insert(0, 0, string);
            text.endBatchEdit();
            inputConnection.markInvalid();
            if (cursorRange == null) {
                setSelectionAround(line, column);
            } else {
                try {
                    var start = cursorRange.getStart();
                    var end = cursorRange.getEnd();
                    setSelectionRegion(start.line, start.column, end.line, end.column);
                } catch (IndexOutOfBoundsException e) {
                    Log.w(CodeEditor.LOG_TAG, e);
                }
            }
            getScroller().forceFinished(true);
            getScroller().startScroll(x, y, 0, 0, 0);
            getScroller().abortAnimation();
            // Ensure the scroll offset is valid
            touchHandler.scrollBy(0, 0);
            inputConnection.reset();
            restartInput();
            dispatchEvent(new EditorFormatEvent(this, true));
        });
    }

    @Override
    public void onFormatFail(@Nullable Throwable throwable) {
        postInLifecycle(() -> {
            Toast.makeText(getContext(), "Format:" + throwable, Toast.LENGTH_SHORT).show();
            dispatchEvent(new EditorFormatEvent(this, false));
        });
    }
}
