/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.OverScroller;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.UiThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.Event;
import io.github.rosemoe.sora.event.EventManager;
import io.github.rosemoe.sora.event.EventReceiver;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.SubscriptionReceipt;
import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.ContentListener;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.ICUUtils;
import io.github.rosemoe.sora.text.LineRemoveListener;
import io.github.rosemoe.sora.text.LineSeparator;
import io.github.rosemoe.sora.text.TextLayoutHelper;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.text.method.KeyMetaStates;
import io.github.rosemoe.sora.util.Floats;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.Logger;
import io.github.rosemoe.sora.util.LongArrayList;
import io.github.rosemoe.sora.util.Numbers;
import io.github.rosemoe.sora.util.TemporaryFloatBuffer;
import io.github.rosemoe.sora.util.ThemeUtils;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent;
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.component.Magnifier;
import io.github.rosemoe.sora.widget.layout.Layout;
import io.github.rosemoe.sora.widget.layout.LineBreakLayout;
import io.github.rosemoe.sora.widget.layout.WordwrapLayout;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.style.CursorAnimator;
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle;
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;
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
@SuppressWarnings("unused")
public class CodeEditor extends View implements ContentListener, Formatter.FormatResultReceiver, LineRemoveListener {

    /**
     * The default size when creating the editor object. Unit is sp.
     */
    public static final int DEFAULT_TEXT_SIZE = 18;
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
     * Adjust the completion window's position scheme according to the device's screen size.
     */
    public static final int WINDOW_POS_MODE_AUTO = 0;
    /**
     * Completion window always follow the cursor
     */
    public static final int WINDOW_POS_MODE_FOLLOW_CURSOR_ALWAYS = 1;
    /**
     * Completion window always stay at the bottom of view and occupies the
     * horizontal viewport
     */
    public static final int WINDOW_POS_MODE_FULL_WIDTH_ALWAYS = 2;
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
    private final static String NUMBER_DIGITS = "0 1 2 3 4 5 6 7 8 9";
    private static final String LOG_TAG = "CodeEditor";
    private final static String COPYRIGHT = "sora-editor\nCopyright (C) Rosemoe roses2020@qq.com\nThis project is distributed under the LGPL v2.1 license";
    final EditorKeyEventHandler mKeyEventHandler = new EditorKeyEventHandler(this);
    protected SymbolPairMatch mLanguageSymbolPairs;
    protected EditorTextActionWindow mTextActionWindow;
    protected List<Span> defSpans = new ArrayList<>(2);
    protected EditorStyleDelegate mStyleDelegate;
    int mStartedActionMode;
    CharPosition mSelectionAnchor;
    EditorInputConnection mConnection;
    EventManager mEventManager;
    Layout mLayout;
    private int mTabWidth;
    private int mCursorPosition;
    private int mDownX = 0;
    private int mInputType;
    private int mNonPrintableOptions;
    private int mCompletionPosMode;
    private long mAvailableFloatArrayRegion;
    private float mDpUnit;
    private float mDividerWidth;
    private float mDividerMargin;
    private float mInsertSelWidth;
    private float mBlockLineWidth;
    private float mVerticalScrollFactor;
    private float mLineInfoTextSize;
    private float mLineSpacingMultiplier = 1f;
    private float mLineSpacingAdd = 0f;
    private boolean mWait;
    private boolean mScalable;
    private boolean mEditable;
    private boolean mWordwrap;
    private boolean mUndoEnabled;
    private boolean mLayoutBusy;
    private boolean mDisplayLnPanel;
    private boolean mLineNumberEnabled;
    private boolean mBlockLineEnabled;
    private boolean mForceHorizontalScrollable;
    private boolean mHighlightCurrentBlock;
    private boolean mHighlightCurrentLine;
    private boolean mVerticalScrollBarEnabled;
    private boolean mHorizontalScrollBarEnabled;
    private boolean mCursorAnimation;
    private boolean mPinLineNumber;
    private boolean mFirstLineNumberAlwaysVisible;
    private boolean mLigatureEnabled;
    private boolean mLastCursorState;
    private boolean mStickyTextSelection;
    private boolean mHighlightBracketPair;
    private SelectionHandleStyle.HandleDescriptor mLeftHandle;
    private SelectionHandleStyle.HandleDescriptor mRightHandle;
    private SelectionHandleStyle.HandleDescriptor mInsertHandle;
    private ClipboardManager mClipboardManager;
    private InputMethodManager mInputMethodManager;
    private Cursor mCursor;
    private Content mText;
    private Matrix mMatrix;
    private EditorColorScheme mColors;
    private String mLnTip;
    private String mFormatTip;
    private Language mLanguage;
    private DiagnosticIndicatorStyle mDiagnosticStyle = DiagnosticIndicatorStyle.WAVY_LINE;
    private long mLastMakeVisible = 0;
    private EditorAutoCompletion mCompletionWindow;
    private EditorTouchEventHandler mEventHandler;
    private Paint.Align mLineNumberAlign;
    private GestureDetector mBasicDetector;
    private ScaleGestureDetector mScaleDetector;
    private CursorAnchorInfo.Builder mAnchorInfoBuilder;
    private EdgeEffect mVerticalGlow;
    private EdgeEffect mHorizontalGlow;
    private ExtractedTextRequest mExtracting;
    private EditorSearcher mSearcher;
    private CursorAnimator mCursorAnimator;
    private Paint.FontMetricsInt mLineNumberMetrics;
    private Paint.FontMetricsInt mGraphMetrics;
    private SelectionHandleStyle mHandleStyle;
    private CursorBlink mCursorBlink;
    private DirectAccessProps mProps;
    private Bundle mExtraArguments;
    private Styles mStyles;
    private DiagnosticsContainer mDiagnostics;
    private EditorRenderer mRenderer;
    private boolean mHardwareAccAllowed;
    private float scrollerFinalX;
    private float scrollerFinalY;
    private boolean verticalAbsorb;
    private boolean horizontalAbsorb;
    private LineSeparator lineSeparator;

    public CodeEditor(Context context) {
        this(context, null);
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(attrs, defStyleAttr, defStyleRes);
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
    static boolean hasVisibleRegion(int begin, int end, int first, int last) {
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
            return (T) mCompletionWindow;
        } else if (clazz == Magnifier.class) {
            return (T) mEventHandler.mMagnifier;
        } else if (clazz == EditorTextActionWindow.class) {
            return (T) mTextActionWindow;
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
            mCompletionWindow = (EditorAutoCompletion) replacement;
        } else if (clazz == Magnifier.class) {
            mEventHandler.mMagnifier = (Magnifier) replacement;
        } else if (clazz == EditorTextActionWindow.class) {
            mTextActionWindow = (EditorTextActionWindow) replacement;
        } else {
            throw new IllegalArgumentException("Unknown component type");
        }
        replacement.setEnabled(isEnabled);
    }

    /**
     * Get KeyMetaStates, which manages alt/shift state in editor
     */
    @NonNull
    public KeyMetaStates getKeyMetaStates() {
        return mKeyEventHandler.getKeyMetaStates();
    }

    /**
     * Cancel the next animation for {@link CodeEditor#ensurePositionVisible(int, int)}
     */
    protected void cancelAnimation() {
        mLastMakeVisible = System.currentTimeMillis();
    }

    /**
     * Get the width of line number and divider line
     *
     * @return The width
     */
    public float measureTextRegionOffset() {
        return isLineNumberEnabled() ?
                measureLineNumber() + mDividerMargin * 2 + mDividerWidth +
                        (mRenderer.hasSideHintIcons() ? getRowHeight() : 0) :
                mDpUnit * 5;
    }

    /**
     * Get the rect of left selection handle painted on view
     *
     * @return Descriptor of left handle
     */
    public SelectionHandleStyle.HandleDescriptor getLeftHandleDescriptor() {
        return mLeftHandle;
    }

    /**
     * Get the rect of right selection handle painted on view
     *
     * @return Descriptor of right handle
     */
    public SelectionHandleStyle.HandleDescriptor getRightHandleDescriptor() {
        return mRightHandle;
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    public float getOffset(int line, int column) {
        return mLayout.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - getOffsetX();
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    public float getCharOffsetX(int line, int column) {
        return mLayout.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - getOffsetX();
    }

    /**
     * Get the character's y offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The y offset on view
     */
    public float getCharOffsetY(int line, int column) {
        return mLayout.getCharLayoutOffset(line, column)[0] - getOffsetY();
    }

    /**
     * Prepare editor
     * <p>
     * Initialize variants
     */
    private void initialize(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        Log.v(LOG_TAG, COPYRIGHT);

        mEventManager = new EventManager();
        mRenderer = new EditorRenderer(this);
        mStyleDelegate = new EditorStyleDelegate(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var configuration = ViewConfiguration.get(getContext());
            mVerticalScrollFactor = configuration.getScaledVerticalScrollFactor();
        } else {
            try {
                try (var a = getContext().obtainStyledAttributes(new int[]{android.R.attr.listPreferredItemHeight})) {
                    mVerticalScrollFactor = a.getFloat(0, 32);
                    a.recycle();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to get scroll factor", e);
                mVerticalScrollFactor = 32;
            }
        }
        lineSeparator = LineSeparator.LF;
        mLnTip = getContext().getString(R.string.editor_line_number_tip_prefix);
        mFormatTip = getContext().getString(R.string.editor_formatting);
        mProps = new DirectAccessProps();
        mDpUnit = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, Resources.getSystem().getDisplayMetrics()) / 10F;
        mDividerWidth = mDpUnit;
        mInsertSelWidth = mDpUnit;
        mDividerMargin = mDpUnit * 2;

        mMatrix = new Matrix();
        mHandleStyle = new HandleStyleSideDrop(getContext());
        mSearcher = new EditorSearcher(this);
        mCursorAnimator = new MoveCursorAnimator(this);
        setCursorBlinkPeriod(DEFAULT_CURSOR_BLINK_PERIOD);
        mAnchorInfoBuilder = new CursorAnchorInfo.Builder();

        mStartedActionMode = ACTION_MODE_NONE;
        setTextSize(DEFAULT_TEXT_SIZE);
        setLineInfoTextSize(mRenderer.getPaint().getTextSize());
        mColors = new EditorColorScheme(this);
        mEventHandler = new EditorTouchEventHandler(this);
        mBasicDetector = new GestureDetector(getContext(), mEventHandler);
        mBasicDetector.setOnDoubleTapListener(mEventHandler);
        mScaleDetector = new ScaleGestureDetector(getContext(), mEventHandler);
        mInsertHandle = new SelectionHandleStyle.HandleDescriptor();
        mLeftHandle = new SelectionHandleStyle.HandleDescriptor();
        mRightHandle = new SelectionHandleStyle.HandleDescriptor();
        mLineNumberAlign = Paint.Align.RIGHT;
        mWait = false;
        mBlockLineEnabled = true;
        mBlockLineWidth = 1f;
        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        setUndoEnabled(true);
        mCursorPosition = -1;
        setScalable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        mHighlightBracketPair = true;
        mConnection = new EditorInputConnection(this);
        mCompletionWindow = new EditorAutoCompletion(this);
        mVerticalGlow = new EdgeEffect(getContext());
        mHorizontalGlow = new EdgeEffect(getContext());
        mTextActionWindow = new EditorTextActionWindow(this);
        setEditorLanguage(null);
        setText(null);
        setTabWidth(4);
        setHighlightCurrentLine(true);
        setVerticalScrollBarEnabled(true);
        setHighlightCurrentBlock(true);
        setDisplayLnPanel(true);
        setHorizontalScrollBarEnabled(true);
        setFirstLineNumberAlwaysVisible(true);
        setCursorAnimationEnabled(true);
        setEditable(true);
        setLineNumberEnabled(true);
        setHardwareAcceleratedDrawAllowed(true);
        setInterceptParentHorizontalScrollIfNeeded(false);
        setTypefaceText(Typeface.DEFAULT);
        setCompletionWndPositionMode(WINDOW_POS_MODE_AUTO);

        // Issue #41 View being highlighted when focused on Android 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false);
        }
        if (getContext() instanceof ContextThemeWrapper) {
            setEdgeEffectColor(ThemeUtils.getColorPrimary((ContextThemeWrapper) getContext()));
        }

        // Config scale detector
        mScaleDetector.setQuickScaleEnabled(false);
    }

    /**
     * @see #setCompletionWndPositionMode(int)
     */
    public int getCompletionWndPositionMode() {
        return mCompletionPosMode;
    }

    /**
     * Set how should we control the position&size of completion window
     *
     * @see #WINDOW_POS_MODE_AUTO
     * @see #WINDOW_POS_MODE_FOLLOW_CURSOR_ALWAYS
     * @see #WINDOW_POS_MODE_FULL_WIDTH_ALWAYS
     */
    public void setCompletionWndPositionMode(int mode) {
        mCompletionPosMode = mode;
        updateCompletionWindowPosition();
    }

    /**
     * Get {@code DirectAccessProps} object of the editor.
     * <p>
     * You can update some features in editor with the instance without disturb to call methods.
     */
    public DirectAccessProps getProps() {
        return mProps;
    }

    /**
     * @see #setFormatTip(String)
     */
    public String getFormatTip() {
        return mFormatTip;
    }

    /**
     * Set the tip text while formatting
     */
    public void setFormatTip(@NonNull String formatTip) {
        this.mFormatTip = Objects.requireNonNull(formatTip);
    }

    /**
     * Set whether line number region will scroll together with code region
     *
     * @see CodeEditor#isLineNumberPinned()
     */
    public void setPinLineNumber(boolean pinLineNumber) {
        mPinLineNumber = pinLineNumber;
        if (isLineNumberEnabled()) {
            invalidate();
        }
    }

    /**
     * @see CodeEditor#setPinLineNumber(boolean)
     */
    public boolean isLineNumberPinned() {
        return mPinLineNumber;
    }

    /**
     * @see CodeEditor#setFirstLineNumberAlwaysVisible(boolean)
     */
    public boolean isFirstLineNumberAlwaysVisible() {
        return mFirstLineNumberAlwaysVisible;
    }

    /**
     * Show first line number in screen in word wrap mode
     *
     * @see CodeEditor#isFirstLineNumberAlwaysVisible()
     */
    public void setFirstLineNumberAlwaysVisible(boolean enabled) {
        mFirstLineNumberAlwaysVisible = enabled;
        if (isWordwrap()) {
            invalidate();
        }
    }

    /**
     * Inserts the given text in the editor.
     * <p>
     * This method allows you to insert texts externally to the content of editor.
     * The content of {@param text} is not checked to be exactly characters of symbols.
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
        mText.insert(cur.getRightLine(), cur.getRightColumn(), text);
        notifyIMEExternalCursorChange();
        if (selectionOffset != text.length()) {
            var pos = mText.getIndexer().getCharPosition(cur.getRight() - (text.length() - selectionOffset));
            setSelection(pos.line, pos.column);
        }
    }

    /**
     * Set adapter for auto-completion window
     * Will take effect next time the window updates
     *
     * @param adapter New adapter, maybe null
     */
    public void setAutoCompletionItemAdapter(@Nullable EditorCompletionAdapter adapter) {
        mCompletionWindow.setAdapter(adapter);
    }

    /**
     * Set cursor blinking period
     * If zero or negative period is passed, the cursor will always be shown.
     *
     * @param period The period time of cursor blinking
     */
    public void setCursorBlinkPeriod(int period) {
        if (mCursorBlink == null) {
            mCursorBlink = new CursorBlink(this, period);
        } else {
            int before = mCursorBlink.period;
            mCursorBlink.setPeriod(period);
            if (before <= 0 && mCursorBlink.valid && isAttachedToWindow()) {
                post(mCursorBlink);
            }
        }
    }

    protected CursorBlink getCursorBlink() {
        return mCursorBlink;
    }

    /**
     * @see CodeEditor#setLigatureEnabled(boolean)
     */
    public boolean isLigatureEnabled() {
        return mLigatureEnabled;
    }

    /**
     * Enable/disable ligature of all types(except 'rlig').
     * Generally you should disable them unless enabling this will have no effect on text measuring.
     * <p>
     * Disabled by default. If you want to enable ligature of a specified type, use
     * {@link CodeEditor#setFontFeatureSettings(String)}
     * <p>
     * For enabling JetBrainsMono font's ligature, Use like this:
     * <pre class="pretty-print">
     * CodeEditor editor = ...;
     * editor.setFontFeatureSettings(enabled ? null : "'liga' 0,'hlig' 0,'dlig' 0,'clig' 0");
     * </pre>
     */
    public void setLigatureEnabled(boolean enabled) {
        this.mLigatureEnabled = enabled;
        setFontFeatureSettings(enabled ? null : "'liga' 0,'calt' 0,'hlig' 0,'dlig' 0,'clig' 0");
    }

    /**
     * Set font feature settings for all paints used by editor
     *
     * @see Paint#setFontFeatureSettings(String)
     */
    public void setFontFeatureSettings(String features) {
        mRenderer.getPaint().setFontFeatureSettingsWrapped(features);
        mRenderer.getPaintOther().setFontFeatureSettings(features);
        mRenderer.getPaintGraph().setFontFeatureSettings(features);
        mRenderer.updateTimestamp();
        invalidate();
    }

    /**
     * Set the style of selection handler.
     *
     * @see SelectionHandleStyle
     * @see io.github.rosemoe.sora.widget.style.builtin.HandleStyleDrop
     * @see HandleStyleSideDrop
     */
    public void setSelectionHandleStyle(@NonNull SelectionHandleStyle style) {
        mHandleStyle = Objects.requireNonNull(style);
        invalidate();
    }

    @NonNull
    public SelectionHandleStyle getHandleStyle() {
        return mHandleStyle;
    }

    /**
     * Returns whether highlight current code block
     *
     * @return This module enabled / disabled
     * @see CodeEditor#setHighlightCurrentBlock(boolean)
     */
    public boolean isHighlightCurrentBlock() {
        return mHighlightCurrentBlock;
    }

    /**
     * Whether the editor should use a different color to draw
     * the current code block line and this code block's start line and end line's
     * background.
     *
     * @param highlightCurrentBlock Enabled / Disabled this module
     */
    public void setHighlightCurrentBlock(boolean highlightCurrentBlock) {
        this.mHighlightCurrentBlock = highlightCurrentBlock;
        if (!mHighlightCurrentBlock) {
            mCursorPosition = -1;
        } else {
            mCursorPosition = findCursorBlock();
        }
        invalidate();
    }

    /**
     * Returns whether the cursor should stick to the text row while selecting the text
     *
     * @see CodeEditor#setStickyTextSelection(boolean)
     */
    public boolean isStickyTextSelection() {
        return mStickyTextSelection;
    }

    /**
     * Whether the cursor should stick to the text row while selecting the text.
     *
     * @param stickySelection value
     */
    public void setStickyTextSelection(boolean stickySelection) {
        this.mStickyTextSelection = stickySelection;
    }

    /**
     * @see CodeEditor#setHighlightCurrentLine(boolean)
     */
    public boolean isHighlightCurrentLine() {
        return mHighlightCurrentLine;
    }

    /**
     * Specify whether the editor should use a different color to draw
     * the background of current line
     */
    public void setHighlightCurrentLine(boolean highlightCurrentLine) {
        mHighlightCurrentLine = highlightCurrentLine;
        invalidate();
    }

    /**
     * Get the editor's language.
     *
     * @return EditorLanguage
     */
    @NonNull
    public Language getEditorLanguage() {
        return mLanguage;
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
        var old = mLanguage;
        if (old != null) {
            old.getFormatter().setReceiver(null);
            old.getFormatter().destroy();
            old.getAnalyzeManager().setReceiver(null);
            old.getAnalyzeManager().destroy();
            old.destroy();
        }

        mStyleDelegate.reset();
        this.mLanguage = lang;
        this.mStyles = null;
        this.mDiagnostics = null;

        if (mCompletionWindow != null) {
            mCompletionWindow.hide();
        }
        // Setup new one
        var mgr = lang.getAnalyzeManager();
        mgr.setReceiver(mStyleDelegate);
        if (mText != null) {
            mgr.reset(new ContentReference(mText), mExtraArguments);
        }

        // Symbol pairs
        if (mLanguageSymbolPairs != null) {
            mLanguageSymbolPairs.setParent(null);
        }
        mLanguageSymbolPairs = mLanguage.getSymbolPairs();
        if (mLanguageSymbolPairs == null) {
            Log.w(LOG_TAG, "Language(" + mLanguage.toString() + ") returned null for symbol pairs. It is a mistake.");
            mLanguageSymbolPairs = new SymbolPairMatch();
        }
        mLanguageSymbolPairs.setParent(mProps.overrideSymbolPairs);

        mRenderer.invalidateRenderNodes();
        invalidate();
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
        if (ctrlPressed && !shiftPressed && altPressed) {
            return keyCode == KeyEvent.KEYCODE_A || keyCode == KeyEvent.KEYCODE_C
                    || keyCode == KeyEvent.KEYCODE_X || keyCode == KeyEvent.KEYCODE_V
                    || keyCode == KeyEvent.KEYCODE_U || keyCode == KeyEvent.KEYCODE_R
                    || keyCode == KeyEvent.KEYCODE_D || keyCode == KeyEvent.KEYCODE_W;
        }

        if (shiftPressed && !altPressed) {
            if (ctrlPressed) {
                // Ctrl + Shift + J
                return keyCode == KeyEvent.KEYCODE_J;
            } else {
                // Shift + Enter
                return keyCode == KeyEvent.KEYCODE_ENTER;
            }
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
        return mBlockLineWidth;
    }

    /**
     * Set the width of code block line
     *
     * @param dp Width in dp unit
     */
    public void setBlockLineWidth(float dp) {
        mBlockLineWidth = dp;
        invalidate();
    }

    /**
     * @see #setWordwrap(boolean)
     */
    public boolean isWordwrap() {
        return mWordwrap;
    }

    /**
     * Set whether text in editor should be wrapped to fit its size
     *
     * @param wordwrap Whether to wrap words
     */
    public void setWordwrap(boolean wordwrap) {
        if (mWordwrap != wordwrap) {
            mWordwrap = wordwrap;
            createLayout();
            if (!wordwrap) {
                mRenderer.invalidateRenderNodes();
            }
            invalidate();
        }
    }

    /**
     * @see #setCursorAnimationEnabled(boolean)
     */
    public boolean isCursorAnimationEnabled() {
        return mCursorAnimation;
    }

    /**
     * Set cursor animation enabled
     */
    public void setCursorAnimationEnabled(boolean enabled) {
        if (!enabled) {
            mCursorAnimator.cancel();
        }
        mCursorAnimation = enabled;
    }

    /**
     * @see #setCursorAnimator(CursorAnimator)
     */
    public CursorAnimator getCursorAnimator() {
        return mCursorAnimator;
    }

    /**
     * Set cursor animation
     *
     * @see CursorAnimator
     * @see #getCursorAnimator()
     * @see #setCursorAnimationEnabled(boolean)  for disabling the animation
     */
    public void setCursorAnimator(@NonNull CursorAnimator cursorAnimator) {
        mCursorAnimator = cursorAnimator;
    }

    /**
     * Whether display vertical scroll bar when scrolling
     *
     * @param enabled Enabled / disabled
     */
    public void setScrollBarEnabled(boolean enabled) {
        mVerticalScrollBarEnabled = mHorizontalScrollBarEnabled = enabled;
        invalidate();
    }

    /**
     * @return Enabled / disabled
     * @see CodeEditor#setDisplayLnPanel(boolean)
     */
    public boolean isDisplayLnPanel() {
        return mDisplayLnPanel;
    }

    /**
     * Whether display the line number panel beside vertical scroll bar
     * when the scroll bar is touched by user
     *
     * @param displayLnPanel Enabled / disabled
     */
    public void setDisplayLnPanel(boolean displayLnPanel) {
        this.mDisplayLnPanel = displayLnPanel;
        invalidate();
    }

    /**
     * @return The prefix
     * @see CodeEditor#setLnTip(String)
     */
    public String getLnTip() {
        return mLnTip;
    }

    /**
     * Set the tip text before line number for the line number panel
     *
     * @param prefix The prefix for text
     */
    public void setLnTip(String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        mLnTip = prefix;
        invalidate();
    }

    @Override
    public boolean isHorizontalScrollBarEnabled() {
        return mHorizontalScrollBarEnabled;
    }

    @Override
    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        mHorizontalScrollBarEnabled = horizontalScrollBarEnabled;
    }

    @Override
    public boolean isVerticalScrollBarEnabled() {
        return mVerticalScrollBarEnabled;
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        mVerticalScrollBarEnabled = verticalScrollBarEnabled;
    }

    /**
     * Get the rect of insert cursor handle on view
     *
     * @return Rect of insert handle
     */
    public SelectionHandleStyle.HandleDescriptor getInsertHandleDescriptor() {
        return mInsertHandle;
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
        return mRenderer.getPaint().getTextSize();
    }

    /**
     * Set text size in pixel unit
     *
     * @param size Text size in pixel unit
     */
    public void setTextSizePx(@Px float size) {
        setTextSizePxDirect(size);
        createLayout();
        invalidate();
    }

    /**
     * Set text size directly without creating layout or invalidating view
     *
     * @param size Text size in pixel unit
     */
    protected void setTextSizePxDirect(float size) {
        mRenderer.setTextSizePxDirect(size);
    }

    public EditorRenderer getRenderer() {
        return mRenderer;
    }

    public Paint.FontMetricsInt getLineNumberMetrics() {
        return mRenderer.getLineNumberMetrics();
    }

    /**
     * Update displayed lines after drawing
     */
    protected void rememberDisplayedLines() {
        mAvailableFloatArrayRegion = IntPair.pack(getFirstVisibleLine(), getLastVisibleLine());
    }

    /**
     * Obtain a float array from previously displayed lines, or either create a new one
     * if no float array matches the requirement.
     */
    protected float[] obtainFloatArray(int desiredSize, boolean usePainter) {
        var start = IntPair.getFirst(mAvailableFloatArrayRegion);
        var end = IntPair.getSecond(mAvailableFloatArrayRegion);
        var firstVis = getFirstVisibleLine();
        var lastVis = getLastVisibleLine();
        start = Math.max(0, start - 5);
        end = Math.min(end + 5, getLineCount());
        for (int i = start; i < end; i++) {
            // Find line that is not displaying currently
            if (i < firstVis || i > lastVis) {
                var line = usePainter ? mRenderer.getLine(i) : mText.getLine(i);
                if (line.widthCache != null && line.widthCache.length >= desiredSize) {
                    line.timestamp = 0;
                    var res = line.widthCache;
                    line.widthCache = null;
                    return res;
                }
            }
            // Skip the region because we can't obtain arrays from here
            if (i >= firstVis && i <= lastVis) {
                i = lastVis;
            }
        }
        return new float[desiredSize];
    }

    /**
     * Whether non-printable is to be drawn
     */
    protected boolean shouldInitializeNonPrintable() {
        return Numbers.clearBit(Numbers.clearBit(mNonPrintableOptions, FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE), FLAG_DRAW_TAB_SAME_AS_SPACE) != 0;
    }

    /**
     * @see #setHardwareAcceleratedDrawAllowed(boolean)
     */
    public boolean isHardwareAcceleratedDrawAllowed() {
        return mHardwareAccAllowed;
    }

    /**
     * Set whether allow the editor to use RenderNode to draw its text.
     * Enabling this can cause more memory usage, but the editor can display text
     * much quicker.
     * However, only when hardware accelerate is enabled on this view can the switch
     * make a difference.
     */
    public void setHardwareAcceleratedDrawAllowed(boolean acceleratedDraw) {
        mHardwareAccAllowed = acceleratedDraw;
        if (acceleratedDraw && !isWordwrap()) {
            mRenderer.invalidateRenderNodes();
        }
    }

    /**
     * As the name is, we find where leading spaces end and trailing spaces start
     *
     * @param line The line to search
     */
    protected long findLeadingAndTrailingWhitespacePos(ContentLine line) {
        var buffer = line.value;
        int column = line.length();
        int leading = 0;
        int trailing = column;
        while (leading < column && isWhitespace(buffer[leading])) {
            leading++;
        }
        // Only them this action is needed
        if (leading != column && (mNonPrintableOptions & (FLAG_DRAW_WHITESPACE_INNER | FLAG_DRAW_WHITESPACE_TRAILING)) != 0) {
            while (trailing > 0 && isWhitespace(buffer[trailing - 1])) {
                trailing--;
            }
        }
        return IntPair.pack(leading, trailing);
    }

    /**
     * A quick method to predicate whitespace character
     */
    private boolean isWhitespace(char ch) {
        return ch == '\t' || ch == ' ';
    }

    /**
     * Get matched text regions on the given line
     *
     * @param line      Target line
     * @param positions Outputs start positions
     */
    protected void computeMatchedPositions(int line, LongArrayList positions) {
        positions.clear();
        if (mSearcher.mPattern == null || mSearcher.mOptions == null) {
            return;
        }
        if (mSearcher.mOptions.useRegex) {
            if (!mSearcher.isResultValid()) {
                return;
            }
            var res = mSearcher.mLastResults;
            var lineLeft = mText.getCharIndex(line, 0);
            var lineRight = lineLeft + mText.getColumnCount(line);
            for (int i = 0; i < res.size(); i++) {
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
            return;
        }
        ContentLine seq = mText.getLine(line);
        int index = 0;
        var len = mSearcher.mPattern.length();
        while (index != -1) {
            index = TextUtils.indexOf(seq, mSearcher.mPattern, mSearcher.mOptions.ignoreCase, index);
            if (index != -1) {
                positions.add(IntPair.pack(index, index + len));
                index += len;
            }
        }
    }

    /**
     * Get the color of EdgeEffect
     *
     * @return The color of EdgeEffect.
     */
    public int getEdgeEffectColor() {
        return mVerticalGlow.getColor();
    }

    /**
     * Set the color of EdgeEffect
     *
     * @param color The color of EdgeEffect
     */
    public void setEdgeEffectColor(int color) {
        mVerticalGlow.setColor(color);
        mHorizontalGlow.setColor(color);
    }

    /**
     * Get the layout of editor
     */
    @UnsupportedUserUsage
    public Layout getLayout() {
        return mLayout;
    }

    /**
     * Get EdgeEffect for vertical direction
     *
     * @return EdgeEffect
     */
    protected EdgeEffect getVerticalEdgeEffect() {
        return mVerticalGlow;
    }

    /**
     * Get EdgeEffect for horizontal direction
     *
     * @return EdgeEffect
     */
    protected EdgeEffect getHorizontalEdgeEffect() {
        return mHorizontalGlow;
    }

    /**
     * Find the smallest code block that cursor is in
     *
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    private int findCursorBlock() {
        List<CodeBlock> blocks = mStyles == null ? null : mStyles.blocks;
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
    private int findCursorBlock(List<CodeBlock> blocks) {
        int line = mCursor.getLeftLine();
        int min = binarySearchEndBlock(line, blocks);
        int max = blocks.size() - 1;
        int minDis = Integer.MAX_VALUE;
        int found = -1;
        int invalidCount = 0;
        int maxCount = Integer.MAX_VALUE;
        if (mStyles != null) {
            maxCount = mStyles.getSuppressSwitch();
        }
        for (int i = min; i <= max; i++) {
            CodeBlock block = blocks.get(i);
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
        return mCursorPosition;
    }

    /**
     * Find the first code block that maybe seen on screen
     * Because the code blocks is sorted by its end line position
     * we can use binary search to quicken this process in order to decrease
     * the time we use on finding
     *
     * @param firstVis The first visible line
     * @param blocks   Current code blocks
     * @return The block we found. It is always a valid index(Unless there is no block)
     */
    int binarySearchEndBlock(int firstVis, List<CodeBlock> blocks) {
        //end > firstVis
        int left = 0, right = blocks.size() - 1, mid, row;
        int max = right;
        while (left <= right) {
            mid = (left + right) / 2;
            if (mid < 0) return 0;
            if (mid > max) return max;
            row = blocks.get(mid).endLine;
            if (row > firstVis) {
                right = mid - 1;
            } else if (row < firstVis) {
                left = mid + 1;
            } else {
                left = mid;
                break;
            }
        }
        return Math.max(0, Math.min(left, max));
    }

    /**
     * Get spans on the given line
     */
    @NonNull
    public List<Span> getSpansForLine(int line) {
        var spanMap = mStyles == null ? null : mStyles.spans;
        if (defSpans.size() == 0) {
            defSpans.add(Span.obtain(0, EditorColorScheme.TEXT_NORMAL));
        }
        try {
            if (spanMap != null) {
                return spanMap.read().getSpansOnLine(line);
            } else {
                return defSpans;
            }
        } catch (Exception e) {
            return defSpans;
        }
    }

    /**
     * Get the width of line number region
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
        var len = NUMBER_DIGITS.length();
        var buffer = TemporaryFloatBuffer.obtain(len);
        mRenderer.getPaintOther().getTextWidths(NUMBER_DIGITS, buffer);
        TemporaryFloatBuffer.recycle(buffer);
        float single = 0f;
        for (int i = 0; i < len; i += 2) {
            single = Math.max(single, buffer[i]);
        }
        return single * count;
    }

    /**
     * Create layout for text
     */
    protected void createLayout() {
        if (mLayout != null) {
            if (mLayout instanceof LineBreakLayout && !mWordwrap) {
                ((LineBreakLayout) mLayout).reuse(mText);
                return;
            }
            if (mLayout instanceof WordwrapLayout && mWordwrap) {
                var newLayout = new WordwrapLayout(this, mText, ((WordwrapLayout) mLayout).getRowTable());
                mLayout.destroyLayout();
                mLayout = newLayout;
                return;
            }
            mLayout.destroyLayout();
        }
        if (mWordwrap) {
            mRenderer.setCachedLineNumberWidth((int) measureLineNumber());
            mLayout = new WordwrapLayout(this, mText, null);
        } else {
            mLayout = new LineBreakLayout(this, mText);
        }
        if (mEventHandler != null) {
            mEventHandler.scrollBy(0, 0);
        }
    }

    /**
     * Commit a tab to cursor
     */
    void commitTab() {
        if (mConnection != null && isEditable()) {
            mConnection.commitTextInternal("\t", true);
        }
    }

    protected void updateCompletionWindowPosition() {
        updateCompletionWindowPosition(true);
    }

    /**
     * Apply new position of auto-completion window
     */
    protected void updateCompletionWindowPosition(boolean shift) {
        float panelX = updateCursorAnchor() + mDpUnit * 20;
        float[] rightLayoutOffset = mLayout.getCharLayoutOffset(mCursor.getRightLine(), mCursor.getRightColumn());
        float panelY = rightLayoutOffset[0] - getOffsetY() + getRowHeight() / 2f;
        float restY = getHeight() - panelY;
        if (restY > mDpUnit * 200) {
            restY = mDpUnit * 200;
        } else if (restY < mDpUnit * 100 && shift) {
            float offset = 0;
            while (restY < mDpUnit * 100) {
                restY += getRowHeight();
                panelY -= getRowHeight();
                offset += getRowHeight();
            }
            getScroller().startScroll(getOffsetX(), getOffsetY(), 0, (int) offset, 0);
        }
        int width;
        if ((getWidth() < 500 * mDpUnit && mCompletionPosMode == WINDOW_POS_MODE_AUTO) || mCompletionPosMode == WINDOW_POS_MODE_FULL_WIDTH_ALWAYS) {
            // center mode
            width = getWidth() * 7 / 8;
            panelX = getWidth() / 8f / 2f;
        } else {
            // follow cursor mode
            width = (int) Math.min(300 * mDpUnit, getWidth() / 2f);
        }
        int height = mCompletionWindow.getHeight();
        if (!mCompletionWindow.isShowing()) {
            height = (int) restY;
        }
        mCompletionWindow.setMaxHeight((int) restY);
        mCompletionWindow.setSize(width, height);
        mCompletionWindow.setLocation((int) panelX + getOffsetX(), (int) panelY + getOffsetY());
    }

    /**
     * Update the information of cursor
     * Such as the position of cursor on screen(For input method that can go to any position on screen like PC input method)
     *
     * @return The offset x of right cursor on view
     */
    protected float updateCursorAnchor() {
        CursorAnchorInfo.Builder builder = mAnchorInfoBuilder;
        builder.reset();
        mMatrix.set(getMatrix());
        int[] b = new int[2];
        getLocationOnScreen(b);
        mMatrix.postTranslate(b[0], b[1]);
        builder.setMatrix(mMatrix);
        builder.setSelectionRange(mCursor.getLeft(), mCursor.getRight());
        int l = mCursor.getRightLine();
        int column = mCursor.getRightColumn();
        boolean visible = true;
        float x = measureTextRegionOffset();
        x = x + mLayout.getCharLayoutOffset(l, column)[1];
        x = x - getOffsetX();
        if (x < 0) {
            visible = false;
            x = 0;
        }
        builder.setInsertionMarkerLocation(x, getRowTop(l) - getOffsetY(), getRowBaseline(l) - getOffsetY(), getRowBottom(l) - getOffsetY(), visible ? CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION : CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION);
        mInputMethodManager.updateCursorAnchorInfo(this, builder.build());
        return x;
    }

    /**
     * Delete text before cursor or selected text (if there is)
     */
    public void deleteText() {
        var cur = mCursor;
        if (cur.isSelected()) {
            mText.delete(cur.getLeftLine(), cur.getLeftColumn(), cur.getRightLine(), cur.getRightColumn());
        } else {
            int col = cur.getLeftColumn();
            int line = cur.getLeftLine();
            if (mProps.deleteEmptyLineFast || (mProps.deleteMultiSpaces != 1 && col > 0 && mText.charAt(line, col - 1) == ' ')) {
                // Check whether selection is in leading spaces
                var text = mText.getLine(cur.getLeftLine()).value;
                var inLeading = true;
                for (int i = col - 1; i >= 0; i--) {
                    char ch = text[i];
                    if (ch != ' ' && ch != '\t') {
                        inLeading = false;
                        break;
                    }
                }

                if (inLeading) {
                    // Check empty line
                    var emptyLine = true;
                    var max = mText.getColumnCount(line);
                    for (int i = col; i < max; i++) {
                        char ch = text[i];
                        if (ch != ' ' && ch != '\t') {
                            emptyLine = false;
                            break;
                        }
                    }
                    if (mProps.deleteEmptyLineFast && emptyLine) {
                        if (line == 0) {
                            // Just delete whitespaces before
                            mText.delete(line, 0, line, col);
                        } else {
                            mText.delete(line - 1, mText.getColumnCount(line - 1), line, max);
                        }
                        return;
                    }

                    if (mProps.deleteMultiSpaces != 1 && col > 0 && mText.charAt(line, col - 1) == ' ') {
                        mText.delete(line, Math.max(0, col - (mProps.deleteMultiSpaces == -1 ? getTabWidth() : mProps.deleteMultiSpaces)), line, col);
                        return;
                    }
                }
            }
            // Do not put cursor inside combined characters
            int begin = TextLayoutHelper.get().getCurPosLeft(col, mText.getLine(cur.getLeftLine()));
            int end = cur.getLeftColumn();
            if (begin > end) {
                int tmp = begin;
                begin = end;
                end = tmp;
            }
            if (begin == end) {
                if (cur.getLeftLine() > 0) {
                    mText.delete(cur.getLeftLine() - 1, mText.getColumnCount(cur.getLeftLine() - 1), cur.getLeftLine(), 0);
                }
            } else {
                mText.delete(cur.getLeftLine(), begin, cur.getLeftLine(), end);
            }
        }
    }

    /**
     * Commit text to the content from IME
     */
    public void commitText(CharSequence text) {
        commitText(text, true);
    }

    /**
     * Commit text at current state from IME
     *
     * @param text Text commit by InputConnection
     */
    public void commitText(CharSequence text, boolean applyAutoIndent) {
        var cur = mCursor;
        if (cur.isSelected()) {
            mText.replace(cur.getLeftLine(), cur.getLeftColumn(), cur.getRightLine(), cur.getRightColumn(), text);
        } else {
            if (mProps.autoIndent && text.length() != 0 && applyAutoIndent) {
                char first = text.charAt(0);
                if (first == '\n') {
                    String line = mText.getLineString(cur.getLeftLine());
                    int p = 0, count = 0;
                    while (p < cur.getLeftColumn()) {
                        if (isWhitespace(line.charAt(p))) {
                            if (line.charAt(p) == '\t') {
                                count += mTabWidth;
                            } else {
                                count++;
                            }
                            p++;
                        } else {
                            break;
                        }
                    }
                    try {
                        count += mLanguage.getIndentAdvance(new ContentReference(mText), cur.getLeftLine(), cur.getLeftColumn());
                    } catch (Exception e) {
                        Log.w(LOG_TAG, "Language object error", e);
                    }
                    StringBuilder sb = new StringBuilder(text);
                    sb.insert(1, TextUtils.createIndent(count, mTabWidth, mLanguage.useTab()));
                    text = sb;
                }
            }
            mText.insert(cur.getLeftLine(), cur.getLeftColumn(), text);
        }
    }

    /**
     * @see #setLineInfoTextSize(float)
     */
    public float getLineInfoTextSize() {
        return mLineInfoTextSize;
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
        mLineInfoTextSize = size;
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
        return mNonPrintableOptions;
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
     */
    public void setNonPrintablePaintingFlags(int flags) {
        this.mNonPrintableOptions = flags;
        invalidate();
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
     * @param line   Line of char
     * @param column Column of char
     */
    public void ensurePositionVisible(int line, int column) {
        float[] layoutOffset = mLayout.getCharLayoutOffset(line, column);
        // x offset is the left of character
        float xOffset = layoutOffset[1] + measureTextRegionOffset();
        // y offset is the bottom of row
        float yOffset = layoutOffset[0];

        float targetY = getOffsetY();
        float targetX = getOffsetX();

        if (yOffset - getRowHeight() < getOffsetY()) {
            //top invisible
            targetY = yOffset - getRowHeight() * 1.1f;
        }
        if (yOffset > getHeight() + getOffsetY()) {
            //bottom invisible
            targetY = yOffset - getHeight() + getRowHeight() * 0.1f;
        }
        float charWidth = column == 0 ? 0 : mRenderer.measureText(mText.getLine(line), line, column - 1, 1);
        if (xOffset < getOffsetX() + (mPinLineNumber ? measureTextRegionOffset() : 0)) {
            targetX = xOffset + (mPinLineNumber ? -measureTextRegionOffset() : 0) - charWidth * 0.2f;
        }
        if (xOffset + charWidth > getOffsetX() + getWidth()) {
            targetX = xOffset + charWidth * 0.8f - getWidth();
        }

        targetX = Math.max(0, Math.min(getScrollMaxX(), targetX));
        targetY = Math.max(0, Math.min(getScrollMaxY(), targetY));

        if (Floats.withinDelta(targetX, getOffsetX(), 1f) && Floats.withinDelta(targetY, getOffsetY(), 1f)) {
            invalidate();
            return;
        }

        boolean animation = System.currentTimeMillis() - mLastMakeVisible >= 100;
        mLastMakeVisible = System.currentTimeMillis();

        if (animation) {
            getScroller().forceFinished(true);
            getScroller().startScroll(getOffsetX(), getOffsetY(), (int) (targetX - getOffsetX()), (int) (targetY - getOffsetY()));
            if (mProps.awareScrollbarWhenAdjust && Math.abs(getOffsetY() - targetY) > mDpUnit * 100) {
                mEventHandler.notifyScrolled();
            }
        } else {
            getScroller().startScroll(getOffsetX(), getOffsetY(), (int) (targetX - getOffsetX()), (int) (targetY - getOffsetY()), 0);
        }

        dispatchEvent(new ScrollEvent(this, getOffsetX(),
                getOffsetY(), (int) targetX, (int) targetY, ScrollEvent.CAUSE_MAKE_POSITION_VISIBLE));

        invalidate();
    }

    /**
     * Whether there is clip
     *
     * @return whether clip in clip board
     */
    public boolean hasClip() {
        return mClipboardManager.hasPrimaryClip();
    }

    /**
     * Get 1dp = ?px
     *
     * @return 1dp in pixel
     */
    public float getDpUnit() {
        return mDpUnit;
    }

    /**
     * Get scroller from EventHandler
     * You would better not use it for your own scrolling
     *
     * @return The scroller
     */
    public OverScroller getScroller() {
        return mEventHandler.getScroller();
    }

    /**
     * Checks whether the position is over max Y position
     *
     * @param posOnScreen Y position on view
     * @return Whether over max Y
     */
    public boolean isOverMaxY(float posOnScreen) {
        return posOnScreen + getOffsetY() > mLayout.getLayoutHeight();
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
        return mLayout.getCharPositionForLayoutOffset(xOffset - measureTextRegionOffset(), yOffset);
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
        return getPointPosition(x + getOffsetX(), y + getOffsetY());
    }

    /**
     * Get max scroll y
     *
     * @return max scroll y
     */
    public int getScrollMaxY() {
        return Math.max(0, mLayout.getLayoutHeight() - getHeight() / 2);
    }

    /**
     * Get max scroll x
     *
     * @return max scroll x
     */
    public int getScrollMaxX() {
        return (int) Math.max(0, mLayout.getLayoutWidth() + measureTextRegionOffset() - getWidth() / 2f);
    }

    /**
     * Get EditorSearcher
     *
     * @return EditorSearcher
     */
    public EditorSearcher getSearcher() {
        return mSearcher;
    }

    /**
     * Set whether the editor use basic display mode to render and measure texts.<br/>
     * When basic display mode is enabled, the following changes will take:<br/>
     * 1. Ligatures are divided into single characters.<br/>
     * 2. Text direction is always LTR (left-to-right).<br/>
     * 3. Some emojis with variation selector or fitzpatrick can not be shown correctly with specified attributes.<br/>
     * 4. ZWJ and ZWNJ takes no effect.<br/>
     * Benefits:<br/>
     * Better performance when the text is very big, especially when you are displaying a text with long lines.
     * @see #isBasicDisplayMode()
     */
    public void setBasicDisplayMode(boolean enabled) {
        mText.setBidiEnabled(!enabled);
        mRenderer.invalidateRenderNodes();
        mRenderer.basicDisplayMode = enabled;
        mRenderer.updateTimestamp();
        invalidate();
    }

    /**
     * @see #setBasicDisplayMode(boolean)
     */
    public boolean isBasicDisplayMode() {
        return mRenderer.basicDisplayMode;
    }

    /**
     * Set selection around the given position
     * It will try to set selection as near as possible (Exactly the position if that position exists)
     */
    protected void setSelectionAround(int line, int column) {
        if (line < getLineCount()) {
            int columnCount = mText.getColumnCount(line);
            if (column > columnCount) {
                column = columnCount;
            }
            setSelection(line, column);
        } else {
            setSelection(getLineCount() - 1, mText.getColumnCount(getLineCount() - 1));
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
        mLanguage.getFormatter().setReceiver(this);
        var formatContent = mText.copyText(false);
        formatContent.setUndoEnabled(false);
        mLanguage.getFormatter().format(formatContent, createCursorRange());
        postInvalidate();
        return true;
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
        if (start.index > end.index) {
            throw new IllegalArgumentException("start > end");
        }
        if (isFormatting()) {
            return false;
        }
        mLanguage.getFormatter().setReceiver(this);
        var formatContent = mText.copyText(false);
        formatContent.setUndoEnabled(false);
        mLanguage.getFormatter().formatRegion(formatContent, new TextRange(start, end), createCursorRange());
        postInvalidate();
        return true;
    }

    public TextRange createCursorRange() {
        return new TextRange(mCursor.left(), mCursor.right());
    }

    /**
     * Get tab width
     *
     * @return tab width
     */
    public int getTabWidth() {
        return mTabWidth;
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
        mTabWidth = width;
        mRenderer.invalidateRenderNodes();
        mRenderer.updateTimestamp();
        invalidate();
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
        mEventHandler.scaleMinSize = minSize;
        mEventHandler.scaleMaxSize = maxSize;
    }

    /**
     * @see CodeEditor#setInterceptParentHorizontalScrollIfNeeded(boolean)
     */
    public boolean isInterceptParentHorizontalScrollEnabled() {
        return mForceHorizontalScrollable;
    }

    /**
     * When the parent is a scrollable view group,
     * request it not to allow horizontal scrolling to be intercepted.
     * Until the code cannot scroll horizontally
     *
     * @param forceHorizontalScrollable Whether force horizontal scrolling
     */
    public void setInterceptParentHorizontalScrollIfNeeded(boolean forceHorizontalScrollable) {
        this.mForceHorizontalScrollable = forceHorizontalScrollable;
        if (!forceHorizontalScrollable) {
            ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(false);
            }
        }
    }

    /**
     * @see #setHighlightBracketPair(boolean)
     */
    public boolean isHighlightBracketPair() {
        return mHighlightBracketPair;
    }

    /**
     * Whether to highlight brackets pairs
     */
    public void setHighlightBracketPair(boolean highlightBracketPair) {
        this.mHighlightBracketPair = highlightBracketPair;
        if (!highlightBracketPair) {
            mStyleDelegate.clearFoundBracketPair();
        } else {
            mStyleDelegate.postUpdateBracketPair();
        }
        invalidate();
    }

    /**
     * Set line separator when new lines are created in editor (only texts from IME. texts from clipboard
     * or other strategies are not encountered). Must not be{@link LineSeparator#NONE}
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
        return mInputType;
    }

    /**
     * Specify input type for the editor
     * <p>
     * Zero for default input type
     *
     * @see EditorInfo#inputType
     */
    public void setInputType(int inputType) {
        mInputType = inputType;
        restartInput();
    }

    /**
     * Undo last action
     */
    public void undo() {
        mText.undo();
        notifyIMEExternalCursorChange();
        mCompletionWindow.hide();
    }

    /**
     * Redo last action
     */
    public void redo() {
        mText.redo();
        notifyIMEExternalCursorChange();
        mCompletionWindow.hide();
    }

    /**
     * Checks whether we can undo
     *
     * @return true if we can undo
     */
    public boolean canUndo() {
        return mText.canUndo();
    }

    /**
     * Checks whether we can redo
     *
     * @return true if we can redo
     */
    public boolean canRedo() {
        return mText.canRedo();
    }

    /**
     * @return Enabled/Disabled
     * @see CodeEditor#setUndoEnabled(boolean)
     */
    public boolean isUndoEnabled() {
        return mUndoEnabled;
    }

    /**
     * Enable / disabled undo manager
     *
     * @param enabled Enable/Disable
     */
    public void setUndoEnabled(boolean enabled) {
        mUndoEnabled = enabled;
        if (mText != null) {
            mText.setUndoEnabled(enabled);
        }
    }

    public DiagnosticIndicatorStyle getDiagnosticIndicatorStyle() {
        return mDiagnosticStyle;
    }

    public void setDiagnosticIndicatorStyle(@NonNull DiagnosticIndicatorStyle diagnosticIndicatorStyle) {
        this.mDiagnosticStyle = diagnosticIndicatorStyle;
        invalidate();
    }

    /**
     * Start search action mode
     */
    public void beginSearchMode() {
        class SearchActionMode implements ActionMode.Callback {

            @Override
            public boolean onCreateActionMode(ActionMode p1, Menu p2) {
                mStartedActionMode = ACTION_MODE_SEARCH_TEXT;
                p2.add(0, 0, 0, R.string.next).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                p2.add(0, 1, 0, R.string.last).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                p2.add(0, 2, 0, R.string.replace).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                p2.add(0, 3, 0, R.string.replaceAll).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                SearchView sv = new SearchView(getContext());
                sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String text) {
                        getSearcher().gotoNext();
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String text) {
                        if (text == null || text.length() == 0) {
                            getSearcher().stopSearch();
                            return false;
                        }
                        getSearcher().search(text, new EditorSearcher.SearchOptions(false, false));
                        return false;
                    }

                });
                p1.setCustomView(sv);
                sv.performClick();
                sv.setQueryHint(getContext().getString(R.string.text_to_search));
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
                    case 1:
                        getSearcher().gotoPrevious();
                        break;
                    case 0:
                        getSearcher().gotoNext();
                        break;
                    case 2:
                    case 3:
                        final boolean replaceAll = p2.getItemId() == 3;
                        final EditText et = new EditText(getContext());
                        et.setHint(R.string.replacement);
                        new AlertDialog.Builder(getContext())
                                .setTitle(replaceAll ? R.string.replaceAll : R.string.replace)
                                .setView(et)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.replace, (dialog, which) -> {
                                    if (replaceAll) {
                                        getSearcher().replaceAll(et.getText().toString(), am::finish);
                                    } else {
                                        getSearcher().replaceThis(et.getText().toString());
                                        am.finish();
                                    }
                                    dialog.dismiss();
                                })
                                .show();
                        break;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode p1) {
                mStartedActionMode = ACTION_MODE_NONE;
                getSearcher().stopSearch();
            }

        }
        ActionMode.Callback callback = new SearchActionMode();
        startActionMode(callback);
    }

    /**
     * Get {@link EditorTouchEventHandler} of the editor
     */
    public EditorTouchEventHandler getEventHandler() {
        return mEventHandler;
    }

    /**
     * @return Margin of divider line
     * @see CodeEditor#setDividerMargin(float)
     */
    public float getDividerMargin() {
        return mDividerMargin;
    }

    /**
     * Set divider line's left and right margin
     *
     * @param dividerMargin Margin for divider line
     */
    public void setDividerMargin(float dividerMargin) {
        if (dividerMargin < 0) {
            throw new IllegalArgumentException("margin can not be under zero");
        }
        this.mDividerMargin = dividerMargin;
        invalidate();
    }

    /**
     * @return Width of divider line
     * @see CodeEditor#setDividerWidth(float)
     */
    public float getDividerWidth() {
        return mDividerWidth;
    }

    /**
     * Set divider line's width
     *
     * @param dividerWidth Width of divider line
     */
    public void setDividerWidth(float dividerWidth) {
        if (dividerWidth < 0) {
            throw new IllegalArgumentException("width can not be under zero");
        }
        this.mDividerWidth = dividerWidth;
        invalidate();
    }

    /**
     * @return Typeface of line number
     * @see CodeEditor#setTypefaceLineNumber(Typeface)
     */
    public Typeface getTypefaceLineNumber() {
        return mRenderer.getPaintOther().getTypeface();
    }

    /**
     * Set line number's typeface
     *
     * @param typefaceLineNumber New typeface
     */
    public void setTypefaceLineNumber(Typeface typefaceLineNumber) {
        mRenderer.setTypefaceLineNumber(typefaceLineNumber);
    }

    /**
     * @return Typeface of text
     * @see CodeEditor#setTypefaceText(Typeface)
     */
    public Typeface getTypefaceText() {
        return mRenderer.getPaint().getTypeface();
    }

    /**
     * Set text's typeface
     *
     * @param typefaceText New typeface
     */
    public void setTypefaceText(Typeface typefaceText) {
        mRenderer.setTypefaceText(typefaceText);
    }

    /**
     * @see #setTextScaleX(float)
     */
    public float getTextScaleX() {
        return mRenderer.getPaint().getTextScaleX();
    }

    /**
     * Set text scale x of Paint
     *
     * @see Paint#setTextScaleX(float)
     * @see #getTextScaleX()
     */
    public void setTextScaleX(float textScaleX) {
        mRenderer.setTextScaleX(textScaleX);
    }

    /**
     * @see #setTextLetterSpacing(float)
     */
    public float getTextLetterSpacing() {
        return mRenderer.getPaint().getLetterSpacing();
    }

    /**
     * Set letter spacing of Paint
     *
     * @see Paint#setLetterSpacing(float)
     * @see #getTextLetterSpacing()
     */
    public void setTextLetterSpacing(float textLetterSpacing) {
        mRenderer.setLetterSpacing(textLetterSpacing);
    }

    /**
     * @return Line number align
     * @see CodeEditor#setLineNumberAlign(Paint.Align)
     */
    public Paint.Align getLineNumberAlign() {
        return mLineNumberAlign;
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
        mLineNumberAlign = align;
        invalidate();
    }

    /**
     * Width for insert cursor
     *
     * @param width Cursor width
     */
    public void setCursorWidth(float width) {
        if (width < 0) {
            throw new IllegalArgumentException("width can not be under zero");
        }
        mInsertSelWidth = width;
        invalidate();
    }

    public float getInsertSelectionWidth() {
        return mInsertSelWidth;
    }

    /**
     * Get Cursor
     * Internal method!
     *
     * @return Cursor of text
     */
    public Cursor getCursor() {
        return mCursor;
    }

    /**
     * Get line count
     *
     * @return line count
     */
    public int getLineCount() {
        return mText.getLineCount();
    }

    /**
     * Get first visible line on screen
     *
     * @return first visible line
     */
    public int getFirstVisibleLine() {
        try {
            return mLayout.getLineNumberForRow(getFirstVisibleRow());
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
        return Math.max(0, Math.min(mLayout.getRowCount() - 1, (getOffsetY() + getHeight()) / getRowHeight()));
    }

    /**
     * Get last visible line on screen
     *
     * @return last visible line
     */
    public int getLastVisibleLine() {
        try {
            return mLayout.getLineNumberForRow(getLastVisibleRow());
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
        mLineSpacingAdd = add;
        mLineSpacingMultiplier = mult;
    }

    /**
     * Gets the line spacing extra space
     *
     * @return the extra space that is added to the height of each lines of this TextView.
     * @see #setLineSpacing(float, float)
     * @see #getLineSpacingMultiplier()
     */
    public float getLineSpacingExtra() {
        return mLineSpacingAdd;
    }

    /**
     * @param lineSpacingExtra The value in pixels that should be added to each line other than the last line.
     *                         *            This will be applied after the multiplier
     */
    public void setLineSpacingExtra(float lineSpacingExtra) {
        mLineSpacingAdd = lineSpacingExtra;
        invalidate();
    }

    /**
     * @return the value by which each line's height is multiplied to get its actual height.
     * @see #setLineSpacingMultiplier(float)
     */
    public float getLineSpacingMultiplier() {
        return mLineSpacingMultiplier;
    }

    /**
     * @param lineSpacingMultiplier The value by which each line height other than the last line will be multiplied
     *                              *             by. Default 1.0f
     */
    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        this.mLineSpacingMultiplier = lineSpacingMultiplier;
        invalidate();
    }

    /**
     * Get actual line spacing in pixels.
     */
    public int getLineSpacingPixels() {
        var metrics = mRenderer.mTextMetrics;
        return ((int) ((metrics.descent - metrics.ascent) * (mLineSpacingMultiplier - 1f) + mLineSpacingAdd)) / 2 * 2;
    }

    /**
     * Get baseline directly
     *
     * @param row Row
     * @return baseline y offset
     */
    public int getRowBaseline(int row) {
        return getRowHeight() * (row + 1) - mRenderer.mTextMetrics.descent - getLineSpacingPixels() / 2;
    }

    /**
     * Get row height
     *
     * @return height of single row
     */
    public int getRowHeight() {
        var metrics = mRenderer.mTextMetrics;
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
        var metrics = mRenderer.mTextMetrics;
        return metrics.descent - metrics.ascent;
    }

    /**
     * Get scroll x
     *
     * @return scroll x
     */
    public int getOffsetX() {
        return mEventHandler.getScroller().getCurrX();
    }

    /**
     * Get scroll y
     *
     * @return scroll y
     */
    public int getOffsetY() {
        return mEventHandler.getScroller().getCurrY();
    }

    /**
     * Indicate whether the layout is working
     */
    @UnsupportedUserUsage
    public void setLayoutBusy(boolean busy) {
        this.mLayoutBusy = busy;
    }

    /**
     * @return Whether the editor is editable, actually.
     * @see CodeEditor#setEditable(boolean)
     * @see CodeEditor#setLayoutBusy(boolean)
     * @see #isFormatting()
     */
    public boolean isEditable() {
        return mEditable && !mLayoutBusy && !isFormatting();
    }

    /**
     * @see #setEditable(boolean)
     */
    public boolean getEditable() {
        return mEditable;
    }

    /**
     * Set whether text can be edited
     *
     * @param editable Editable
     */
    public void setEditable(boolean editable) {
        mEditable = editable;
        if (!editable) {
            hideSoftInput();
        }
    }

    /**
     * @return Whether allow scaling
     * @see CodeEditor#setScalable(boolean)
     */
    public boolean isScalable() {
        return mScalable;
    }

    /**
     * Allow scale text size by thumb
     *
     * @param scale Whether allow
     */
    public void setScalable(boolean scale) {
        mScalable = scale;
    }

    public boolean isBlockLineEnabled() {
        return mBlockLineEnabled;
    }

    public void setBlockLineEnabled(boolean enabled) {
        mBlockLineEnabled = enabled;
        invalidate();
    }

    /**
     * Get the target cursor to move when shift is pressed
     */
    private CharPosition getSelectingTarget() {
        if (mCursor.left().equals(mSelectionAnchor)) {
            return mCursor.right();
        } else {
            return mCursor.left();
        }
    }

    /**
     * Make sure the moving selection is visible
     */
    private void ensureSelectingTargetVisible() {
        if (mCursor.left().equals(mSelectionAnchor)) {
            // Ensure right selection visible
            ensureSelectionVisible();
        } else {
            ensurePositionVisible(mCursor.getLeftLine(), mCursor.getLeftColumn());
        }
    }

    /**
     * Move the selection down
     * If the auto complete panel is shown,move the selection in panel to next
     */
    public void moveSelectionDown() {
        if (mSelectionAnchor == null) {
            if (mCompletionWindow.isShowing()) {
                mCompletionWindow.moveDown();
                return;
            }
            long pos = mLayout.getDownPosition(mCursor.getLeftLine(), mCursor.getLeftColumn());
            setSelection(IntPair.getFirst(pos), IntPair.getSecond(pos));
        } else {
            mCompletionWindow.hide();
            long pos = mLayout.getDownPosition(getSelectingTarget().getLine(), getSelectingTarget().getColumn());
            setSelectionRegion(mSelectionAnchor.line, mSelectionAnchor.column, IntPair.getFirst(pos), IntPair.getSecond(pos), false);
            ensureSelectingTargetVisible();
        }
    }

    /**
     * Move the selection up
     * If Auto complete panel is shown,move the selection in panel to last
     */
    public void moveSelectionUp() {
        if (mSelectionAnchor == null) {
            if (mCompletionWindow.isShowing()) {
                mCompletionWindow.moveUp();
                return;
            }
            long pos = mLayout.getUpPosition(mCursor.getLeftLine(), mCursor.getLeftColumn());
            setSelection(IntPair.getFirst(pos), IntPair.getSecond(pos));
        } else {
            mCompletionWindow.hide();
            long pos = mLayout.getUpPosition(getSelectingTarget().getLine(), getSelectingTarget().getColumn());
            setSelectionRegion(mSelectionAnchor.line, mSelectionAnchor.column, IntPair.getFirst(pos), IntPair.getSecond(pos), false);
            ensureSelectingTargetVisible();
        }
    }

    /**
     * Move the selection left
     */
    public void moveSelectionLeft() {
        if (mSelectionAnchor == null) {
            Cursor c = getCursor();
            int line = c.getLeftLine();
            int column = c.getLeftColumn();
            long pos = mCursor.getLeftOf(IntPair.pack(line, column));
            int lineAfter = IntPair.getFirst(pos);
            int columnAfter = IntPair.getSecond(pos);
            setSelection(lineAfter, columnAfter);
            if (line == lineAfter) {
                if (mCompletionWindow.isShowing()) {
                    if (columnAfter == 0) {
                        mCompletionWindow.hide();
                    } else {
                        mCompletionWindow.requireCompletion();
                    }
                }
            }
        } else {
            mCompletionWindow.hide();
            long pos = mCursor.getLeftOf(getSelectingTarget().toIntPair());
            setSelectionRegion(mSelectionAnchor.line, mSelectionAnchor.column, IntPair.getFirst(pos), IntPair.getSecond(pos), false);
            ensureSelectingTargetVisible();
        }
    }

    /**
     * Move the selection right
     */
    public void moveSelectionRight() {
        if (mSelectionAnchor == null) {
            Cursor c = getCursor();
            int line = c.getLeftLine();
            int column = c.getLeftColumn();
            int c_column = getText().getColumnCount(line);
            long pos = mCursor.getRightOf(IntPair.pack(line, column));
            int lineAfter = IntPair.getFirst(pos);
            int columnAfter = IntPair.getSecond(pos);
            setSelection(lineAfter, columnAfter);
            if (line == lineAfter && mCompletionWindow.isShowing()) {
                mCompletionWindow.requireCompletion();
            }
        } else {
            mCompletionWindow.hide();
            long pos = mCursor.getRightOf(getSelectingTarget().toIntPair());
            setSelectionRegion(mSelectionAnchor.line, mSelectionAnchor.column, IntPair.getFirst(pos), IntPair.getSecond(pos), false);
            ensureSelectingTargetVisible();
        }
    }

    /**
     * Move selection to end of line
     */
    public void moveSelectionEnd() {
        if (mSelectionAnchor == null) {
            int line = mCursor.getLeftLine();
            setSelection(line, getText().getColumnCount(line));
        } else {
            int line = getSelectingTarget().line;
            setSelectionRegion(mSelectionAnchor.line, mSelectionAnchor.column, line, getText().getColumnCount(line), false);
            ensureSelectingTargetVisible();
        }
    }

    /**
     * Move selection to start of line
     */
    public void moveSelectionHome() {
        if (mSelectionAnchor == null) {
            setSelection(mCursor.getLeftLine(), 0);
        } else {
            setSelectionRegion(mSelectionAnchor.line, mSelectionAnchor.column, getSelectingTarget().line, 0, false);
            ensureSelectingTargetVisible();
        }
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
        mRenderer.invalidateInCursor();
        mCursorAnimator.markStartPos();
        if (column > 0 && Character.isHighSurrogate(mText.charAt(line, column - 1))) {
            column++;
            if (column > mText.getColumnCount(line)) {
                column--;
            }
        }
        mCursor.set(line, column);
        if (mHighlightCurrentBlock) {
            mCursorPosition = findCursorBlock();
        }
        updateCursor();
        updateSelection();
        mRenderer.invalidateInCursor();
        if (!mEventHandler.hasAnyHeldHandle() && !mConnection.composingText.isComposing() && !mCompletionWindow.shouldRejectComposing()) {
            mCursorAnimator.markEndPos();
            mCursorAnimator.start();
        }
        if (makeItVisible) {
            ensurePositionVisible(line, column);
        } else {
            invalidate();
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
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight, int columnRight, int cause) {
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
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight, int columnRight) {
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
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight, int columnRight, boolean makeRightVisible) {
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
    public void setSelectionRegion(int lineLeft, int columnLeft, int lineRight, int columnRight, boolean makeRightVisible, int cause) {
        mRenderer.invalidateInCursor();
        int start = getText().getCharIndex(lineLeft, columnLeft);
        int end = getText().getCharIndex(lineRight, columnRight);
        if (start == end) {
            setSelection(lineLeft, columnLeft);
            return;
        }
        if (start > end) {
            setSelectionRegion(lineRight, columnRight, lineLeft, columnLeft, makeRightVisible, cause);
            Log.w(LOG_TAG, "setSelectionRegion() error: start > end:start = " + start + " end = " + end + " lineLeft = " + lineLeft + " columnLeft = " + columnLeft + " lineRight = " + lineRight + " columnRight = " + columnRight);
            return;
        }
        mCursorAnimator.cancel();
        boolean lastState = mCursor.isSelected();
        if (columnLeft > 0) {
            int column = columnLeft - 1;
            char ch = mText.charAt(lineLeft, column);
            if (Character.isHighSurrogate(ch)) {
                columnLeft++;
                if (columnLeft > mText.getColumnCount(lineLeft)) {
                    columnLeft--;
                }
            }
        }
        if (columnRight > 0) {
            int column = columnRight - 1;
            char ch = mText.charAt(lineRight, column);
            if (Character.isHighSurrogate(ch)) {
                columnRight++;
                if (columnRight > mText.getColumnCount(lineRight)) {
                    columnRight--;
                }
            }
        }
        mCursor.setLeft(lineLeft, columnLeft);
        mCursor.setRight(lineRight, columnRight);
        mRenderer.invalidateInCursor();
        updateCursor();
        updateSelection();
        mCompletionWindow.hide();
        if (makeRightVisible) {
            ensurePositionVisible(lineRight, columnRight);
        } else {
            invalidate();
        }
        onSelectionChanged(cause);
    }

    /**
     * Move to next page
     */
    public void movePageDown() {
        mEventHandler.scrollBy(0, getHeight(), true);
        mCompletionWindow.hide();
    }

    /**
     * Move to previous page
     */
    public void movePageUp() {
        mEventHandler.scrollBy(0, -getHeight(), true);
        mCompletionWindow.hide();
    }

    /**
     * Paste text from clip board
     */
    public void pasteText() {
        try {
            if (!mClipboardManager.hasPrimaryClip() || mClipboardManager.getPrimaryClip() == null) {
                return;
            }
            ClipData.Item data = mClipboardManager.getPrimaryClip().getItemAt(0);
            CharSequence text = data.getText();
            if (text != null && mConnection != null) {
                mConnection.commitText(text, 0);
            }
            notifyIMEExternalCursorChange();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
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
        try {
            if (mCursor.isSelected()) {
                String clip = getText().subContent(mCursor.getLeftLine(),
                        mCursor.getLeftColumn(),
                        mCursor.getRightLine(),
                        mCursor.getRightColumn()).toString();
                mClipboardManager.setPrimaryClip(ClipData.newPlainText(clip, clip));
            } else if (shouldCopyLine) {
                copyLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Copies the current line to clipboard.
     */
    private void copyLine() {
        final var cursor = getCursor();
        if (cursor.isSelected()) {
            copyText();
            return;
        }

        final var line = cursor.left().line;
        setSelectionRegion(line, 0, line, getText().getColumnCount(line));
        copyText();
    }

    /**
     * Copy text to clipboard and delete them
     */
    public void cutText() {
        if (mCursor.isSelected()) {
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
            setSelectionRegion(line, 0, line, getText().getColumnCount(line));
        } else {
            setSelectionRegion(line, 0, line + 1, 0);
        }

        cutText();
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
        commitText(prefix + sub);

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
        // Find word edges
        int startLine = line, endLine = line;
        var lineObj = getText().getLine(line);
        long edges = ICUUtils.getWordEdges(lineObj, column, mProps.useICULibToSelectWords);
        int startColumn = IntPair.getFirst(edges);
        int endColumn = IntPair.getSecond(edges);
        if (startColumn == endColumn) {
            if (startColumn > 0) {
                startColumn--;
            } else if (endColumn < lineObj.length()) {
                endColumn++;
            } else {
                if (line > 0) {
                    int lastColumn = getText().getColumnCount(line - 1);
                    startLine = line - 1;
                    startColumn = lastColumn;
                } else if (line < getLineCount() - 1) {
                    endLine = line + 1;
                    endColumn = 0;
                }
            }
        }
        requestFocusFromTouch();
        setSelectionRegion(startLine, startColumn, endLine, endColumn, SelectionChangeEvent.CAUSE_LONG_PRESS);
    }

    /**
     * @return Text displaying, the result is read-only. You should not make changes to this object as it is used internally
     * @see CodeEditor#setText(CharSequence)
     * @see CodeEditor#setText(CharSequence, Bundle)
     */
    @NonNull
    public Content getText() {
        return mText;
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
        return mExtraArguments;
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
    public void setText(@Nullable CharSequence text, boolean reuseContentObject, @Nullable Bundle extraArguments) {
        if (text == null) {
            text = "";
        }

        if (mText != null) {
            mText.removeContentListener(this);
            mText.setLineListener(null);
            mText.resetBatchEdit();
        }
        mExtraArguments = extraArguments == null ? new Bundle() : extraArguments;
        if (reuseContentObject && text instanceof Content) {
            mText = (Content) text;
            mText.resetBatchEdit();
            mRenderer.updateTimestamp();
        } else {
            mText = new Content(text);
        }
        mText.setBidiEnabled(!mRenderer.basicDisplayMode);
        mStyleDelegate.reset();
        mStyles = null;
        mCursor = mText.getCursor();
        mEventHandler.reset();
        mText.addContentListener(this);
        mText.setUndoEnabled(mUndoEnabled);
        mText.setLineListener(this);
        mRenderer.notifyFullTextUpdate();

        if (mLanguage != null) {
            mLanguage.getAnalyzeManager().reset(new ContentReference(mText), mExtraArguments);
        }

        dispatchEvent(new ContentChangeEvent(this, ContentChangeEvent.ACTION_SET_NEW_TEXT, new CharPosition(), mText.getIndexer().getCharPosition(getLineCount() - 1, mText.getColumnCount(getLineCount() - 1)), mText));
        if (mInputMethodManager != null) {
            mInputMethodManager.restartInput(this);
        }
        createLayout();
        requestLayout();
        mRenderer.invalidateRenderNodes();
        invalidate();
    }

    /**
     * Set the editor's text size in sp unit. This value must be > 0
     *
     * @param textSize the editor's text size in <strong>Sp</strong> units.
     */
    public void setTextSize(float textSize) {
        Context context = getContext();
        Resources res;

        if (context == null) {
            res = Resources.getSystem();
        } else {
            res = context.getResources();
        }

        setTextSizePx(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, res.getDisplayMetrics()));
    }

    /**
     * Subscribe event of the given type.
     *
     * @see EventManager#subscribeEvent(Class, EventReceiver)
     */
    public <T extends Event> SubscriptionReceipt<T> subscribeEvent(Class<T> eventType, EventReceiver<T> receiver) {
        return mEventManager.subscribeEvent(eventType, receiver);
    }

    /**
     * Dispatch the given event
     *
     * @see EventManager#dispatchEvent(Event)
     */
    public <T extends Event> int dispatchEvent(T event) {
        return mEventManager.dispatchEvent(event);
    }

    /**
     * Check whether the editor is currently performing a format operation
     *
     * @return whether the editor is currently formatting
     */
    public boolean isFormatting() {
        return mLanguage.getFormatter().isRunning();
    }

    /**
     * Check whether line numbers are shown
     *
     * @return The state of line number displaying
     */
    public boolean isLineNumberEnabled() {
        return mLineNumberEnabled;
    }

    /**
     * Set whether we should display line numbers
     *
     * @param lineNumberEnabled The state of line number displaying
     */
    public void setLineNumberEnabled(boolean lineNumberEnabled) {
        if (lineNumberEnabled != mLineNumberEnabled && isWordwrap()) {
            createLayout();
        }
        mLineNumberEnabled = lineNumberEnabled;
        invalidate();
    }

    /**
     * Get the paint of the editor
     * You should not change text size and other attributes that are related to text measuring by the object
     *
     * @return The paint which is used by the editor now
     */
    @NonNull
    public Paint getTextPaint() {
        return mRenderer.getPaint();
    }

    public Paint getOtherPaint() {
        return mRenderer.getPaintOther();
    }

    public Paint getGraphPaint() {
        return mRenderer.getPaintGraph();
    }

    /**
     * Get the ColorScheme object of this editor
     * You can config colors of some regions, texts and highlight text
     *
     * @return ColorScheme object using
     */
    @NonNull
    public EditorColorScheme getColorScheme() {
        return mColors;
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
        if (mColors != null) {
            mColors.detachEditor(this);
        }
        mColors = colors;
        // Automatically invoke scheme updating related methods
        colors.attachEditor(this);
        invalidate();
    }

    /**
     * Move selection to line start with scrolling
     *
     * @param line Line to jump
     */
    public void jumpToLine(int line) {
        setSelection(line, 0);
    }


    //-------------------------------------------------------------------------------
    //-------------------------IME Interaction---------------------------------------
    //-------------------------------------------------------------------------------

    /**
     * Rerun analysis forcibly
     */
    public void rerunAnalysis() {
        if (mLanguage != null) {
            mLanguage.getAnalyzeManager().rerun();
        }
    }

    /**
     * Get analyze result.
     * <strong>Do not make changes to it or read concurrently</strong>
     */
    @Nullable
    public Styles getStyles() {
        return mStyles;
    }

    @UiThread
    public void setStyles(@Nullable Styles styles) {
        mStyles = styles;
        if (mHighlightCurrentBlock) {
            mCursorPosition = findCursorBlock();
        }
        mRenderer.invalidateRenderNodes();
        mRenderer.updateTimestamp();
        invalidate();
    }

    @Nullable
    public DiagnosticsContainer getDiagnostics() {
        return mDiagnostics;
    }

    @UiThread
    public void setDiagnostics(@Nullable DiagnosticsContainer diagnostics) {
        mDiagnostics = diagnostics;
        invalidate();
    }

    /**
     * Hide auto complete window if shown
     */
    public void hideAutoCompleteWindow() {
        if (mCompletionWindow != null) {
            mCompletionWindow.hide();
        }
    }

    /**
     * Get cursor code block index
     *
     * @return index of cursor's code block
     */
    public int getBlockIndex() {
        return mCursorPosition;
    }

    /**
     * Display soft input method for self
     */
    public void showSoftInput() {
        if (isEditable() && isEnabled()) {
            if (isInTouchMode()) {
                requestFocusFromTouch();
            }
            if (!hasFocus()) {
                requestFocus();
            }
            mInputMethodManager.showSoftInput(this, 0);
        }
        invalidate();
    }

    /**
     * Hide soft input
     */
    public void hideSoftInput() {
        mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    /**
     * Send current selection position to input method
     */
    protected void updateSelection() {
        int candidatesStart = -1, candidatesEnd = -1;
        if (mConnection.composingText.isComposing()) {
            try {
                candidatesStart = mConnection.composingText.startIndex;
                candidatesEnd = mConnection.composingText.endIndex;
            } catch (IndexOutOfBoundsException e) {
                //Ignored
            }
        }
        mInputMethodManager.updateSelection(this, mCursor.getLeft(), mCursor.getRight(), candidatesStart, candidatesEnd);
    }

    /**
     * Update request result for monitoring request
     */
    protected void updateExtractedText() {
        if (mExtracting != null) {
            var text = extractText(mExtracting);
            mInputMethodManager.updateExtractedText(this, mExtracting.token, text);
        }
    }

    //-------------------------------------------------------------------------------
    //------------------------Internal Callbacks-------------------------------------
    //-------------------------------------------------------------------------------

    /**
     * Set request needed to update when editor updates selection
     */
    protected void setExtracting(@Nullable ExtractedTextRequest request) {
        if (getProps().disallowSuggestions) {
            mExtracting = null;
            return;
        }
        mExtracting = request;
    }

    /**
     * Extract text in editor for input method
     */
    protected ExtractedText extractText(@NonNull ExtractedTextRequest request) {
        if (getProps().disallowSuggestions) {
            return null;
        }
        Cursor cur = getCursor();
        ExtractedText text = new ExtractedText();
        int selBegin = cur.getLeft();
        int selEnd = cur.getRight();
        int startOffset = 0;
        if (request.hintMaxChars == 0) {
            request.hintMaxChars = mProps.maxIPCTextLength;
        }
        if (request.hintMaxChars < mProps.maxIPCTextLength) {
            if (startOffset + request.hintMaxChars < selBegin) {
                startOffset = selBegin - request.hintMaxChars / 2;
            }
        }
        text.text = mConnection.getTextRegion(startOffset, startOffset + request.hintMaxChars, request.flags);
        text.startOffset = startOffset;
        text.selectionStart = selBegin - startOffset;
        text.selectionEnd = selEnd - startOffset;
        if (selBegin != selEnd) {
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
        if (mConnection.composingText.isComposing()) {
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
        if (mConnection != null)
            mConnection.invalid();
        if (mInputMethodManager != null)
            mInputMethodManager.restartInput(this);
    }

    /**
     * Send cursor position in text and on screen to input method
     */
    public void updateCursor() {
        updateCursorAnchor();
        updateExtractedText();
        if (!mText.isInBatchEdit() && !mConnection.composingText.isComposing()) {
            updateSelection();
        }
    }

    /**
     * Release any resources held by editor.
     * This will stop completion threads and destroy using {@link Language} object.
     * <p>
     * Recommend to call if the activity is to destroy.
     */
    public void release() {
        hideEditorWindows();
        if (mLanguage != null) {
            mLanguage.getAnalyzeManager().destroy();
            mLanguage.getFormatter().setReceiver(null);
            mLanguage.getFormatter().destroy();
            mLanguage.destroy();
            mLanguage = new EmptyLanguage();
        }
    }

    /**
     * Hide all built-in windows of the editor
     */
    public void hideEditorWindows() {
        mCompletionWindow.cancelCompletion();
        mTextActionWindow.dismiss();
        mEventHandler.mMagnifier.dismiss();
    }

    /**
     * Called by ColorScheme to notify invalidate
     *
     * @param type Color type changed
     */
    public void onColorUpdated(int type) {
        if (type == EditorColorScheme.COMPLETION_WND_BACKGROUND || type == EditorColorScheme.COMPLETION_WND_CORNER
                || type == EditorColorScheme.COMPLETION_WND_ITEM_CURRENT || type == EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY
                || type == EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY) {
            if (mCompletionWindow != null)
                mCompletionWindow.applyColorScheme();
            return;
        }
        mRenderer.invalidateRenderNodes();
        invalidate();
    }

    /**
     * Called by color scheme to init colors
     */
    public void onColorFullUpdate() {
        if (mCompletionWindow != null)
            mCompletionWindow.applyColorScheme();
        mRenderer.invalidateRenderNodes();
        invalidate();
    }

    /**
     * Get using InputMethodManager
     */
    protected InputMethodManager getInputMethodManager() {
        return mInputMethodManager;
    }

    /**
     * Called by CodeEditorInputConnection
     */
    protected void onCloseConnection() {
        setExtracting(null);
        invalidate();
    }

    /**
     * Called when the text is edited or {@link CodeEditor#setSelection} is called
     */
    protected void onSelectionChanged(int cause) {
        dispatchEvent(new SelectionChangeEvent(this, cause));

    }

    protected void releaseEdgeEffects() {
        mHorizontalGlow.onRelease();
        mVerticalGlow.onRelease();
    }

    //-------------------------------------------------------------------------------
    //-------------------------Override methods--------------------------------------
    //-------------------------------------------------------------------------------
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mRenderer.draw(canvas);

        // Update magnifier
        if ((mLastCursorState != mCursorBlink.visibility || !mEventHandler.getScroller().isFinished()) && mEventHandler.mMagnifier.isShowing()) {
            mLastCursorState = mCursorBlink.visibility;
            post(mEventHandler.mMagnifier::updateDisplay);
        }
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo() {
        AccessibilityNodeInfo node = super.createAccessibilityNodeInfo();
        node.setEditable(isEditable());
        node.setTextSelection(mCursor.getLeft(), mCursor.getRight());
        node.setScrollable(true);
        node.setInputType(InputType.TYPE_CLASS_TEXT);
        node.setMultiLine(true);
        node.setText(getText().toStringBuilder());
        node.setLongClickable(true);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        return node;
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
            case AccessibilityNodeInfo.ACTION_COPY:
                copyText();
                return true;
            case AccessibilityNodeInfo.ACTION_CUT:
                cutText();
                return true;
            case AccessibilityNodeInfo.ACTION_PASTE:
                pasteText();
                return true;
            case AccessibilityNodeInfo.ACTION_SET_TEXT:
                setText(arguments.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE));
                return true;
            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
                movePageDown();
                return true;
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
                movePageUp();
                return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                if (mForceHorizontalScrollable) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = x - mDownX;
                if (mForceHorizontalScrollable) {
                    if (deltaX > 0 && getScroller().getCurrX() == 0
                            || deltaX < 0 && getScroller().getCurrX() == getScrollMaxX()) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return isEnabled() && isEditable();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (!isEditable() || !isEnabled()) {
            return null;
        }
        outAttrs.inputType = mInputType != 0 ? mInputType : EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.initialSelStart = getCursor() != null ? getCursor().getLeft() : 0;
        outAttrs.initialSelEnd = getCursor() != null ? getCursor().getRight() : 0;
        outAttrs.initialCapsMode = mConnection.getCursorCapsMode(0);

        // Prevent fullscreen when the screen height is too small
        // Especially in landscape mode
        if (!mProps.allowFullscreen) {
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        }

        mConnection.reset();
        mText.resetBatchEdit();
        setExtracting(null);
        return mConnection;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (isFormatting()) {
            mEventHandler.reset2();
            mScaleDetector.onTouchEvent(event);
            return mBasicDetector.onTouchEvent(event);
        }
        boolean handlingBefore = mEventHandler.handlingMotions();
        boolean res = mEventHandler.onTouchEvent(event);
        boolean handling = mEventHandler.handlingMotions();
        boolean res2 = false;
        boolean res3 = mScaleDetector.onTouchEvent(event);
        if (!handling && !handlingBefore) {
            res2 = mBasicDetector.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mVerticalGlow.onRelease();
            mHorizontalGlow.onRelease();
        }
        return (res3 || res2 || res);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mKeyEventHandler.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mKeyEventHandler.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return mKeyEventHandler.onKeyMultiple(keyCode, repeatCount, event);
    }

    boolean onSuperKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    boolean onSuperKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    boolean onSuperKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean warn = false;
        //Fill the horizontal layout if WRAP_CONTENT mode
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST || MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY);
            warn = true;
        }
        //Fill the vertical layout if WRAP_CONTENT mode
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST || MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY);
            warn = true;
        }
        if (warn) {
            Log.w(LOG_TAG, "onMeasure():CodeEditor does not support wrap_content mode when measuring.It will just fill the whole space.");
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
            float v_scroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            float h_scroll = -event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            mEventHandler.onScroll(event, event, h_scroll * mVerticalScrollFactor, v_scroll * mVerticalScrollFactor);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        mRenderer.onSizeChanged(w, h);
        getVerticalEdgeEffect().setSize(w, h);
        getHorizontalEdgeEffect().setSize(h, w);
        getVerticalEdgeEffect().finish();
        getHorizontalEdgeEffect().finish();
        if (mLayout == null || (isWordwrap() && w != oldWidth)) {
            createLayout();
        } else {
            mEventHandler.scrollBy(getOffsetX() > getScrollMaxX() ? getScrollMaxX() - getOffsetX() : 0, getOffsetY() > getScrollMaxY() ? getScrollMaxY() - getOffsetY() : 0);
        }
        verticalAbsorb = false;
        horizontalAbsorb = false;
        if (oldHeight > h && mProps.adjustToSelectionOnResize) {
            ensureSelectionVisible();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCursorBlink.valid = false;
        removeCallbacks(mCursorBlink);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            mCursorBlink.valid = mCursorBlink.period > 0;
            if (mCursorBlink.valid) {
                post(mCursorBlink);
            }
        } else {
            mCursorBlink.valid = false;
            mCursorBlink.visibility = false;
            mCompletionWindow.hide();
            mTextActionWindow.dismiss();
            mEventHandler.hideInsertHandle();
            removeCallbacks(mCursorBlink);
        }
        invalidate();
    }

    @Override
    public void computeScroll() {
        var scroller = mEventHandler.getScroller();
        if (scroller.computeScrollOffset()) {
            if (!scroller.isFinished() && (scroller.getStartX() != scroller.getFinalX() || scroller.getStartY() != scroller.getFinalY())) {
                scrollerFinalX = scroller.getFinalX();
                scrollerFinalY = scroller.getFinalY();
                horizontalAbsorb = Math.abs(scroller.getStartX() - scroller.getFinalX()) > getDpUnit() * 5;
                verticalAbsorb = Math.abs(scroller.getStartY() - scroller.getFinalY()) > getDpUnit() * 5;
            }
            if (scroller.getCurrX() <= 0 && scrollerFinalX <= 0 && mHorizontalGlow.isFinished() && horizontalAbsorb) {
                mHorizontalGlow.onAbsorb((int) scroller.getCurrVelocity());
                mEventHandler.glowLeftOrRight = false;
            } else {
                var max = getScrollMaxX();
                if (scroller.getCurrX() >= max && scrollerFinalX >= max && mHorizontalGlow.isFinished() && horizontalAbsorb) {
                    mHorizontalGlow.onAbsorb((int) scroller.getCurrVelocity());
                    mEventHandler.glowLeftOrRight = true;
                }
            }
            if (scroller.getCurrY() <= 0 && scrollerFinalY <= 0 && mVerticalGlow.isFinished() && verticalAbsorb) {
                mVerticalGlow.onAbsorb((int) scroller.getCurrVelocity());
                mEventHandler.glowTopOrBottom = false;
            } else {
                var max = getScrollMaxY();
                if (scroller.getCurrY() >= max && scrollerFinalY >= max && mVerticalGlow.isFinished() && verticalAbsorb) {
                    mVerticalGlow.onAbsorb((int) scroller.getCurrVelocity());
                    mEventHandler.glowTopOrBottom = true;
                }
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void beforeReplace(Content content) {
        mWait = true;
        mLayout.beforeReplace(content);
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence insertedContent) {
        mRenderer.updateTimestamp();
        mStyleDelegate.onTextChange();
        var start = mText.getIndexer().getCharPosition(startLine, startColumn);
        var end = mText.getIndexer().getCharPosition(endLine, endColumn);
        mRenderer.buildMeasureCacheForLines(startLine, endLine);

        // Update spans
        try {
            if (mStyles != null) {
                mStyles.adjustOnInsert(start, end);
            }
            if (mDiagnostics != null) {
                mDiagnostics.shiftOnInsert(start.index, end.index);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Update failure", e);
        }

        mLayout.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent);

        // Notify input method
        updateCursor();
        mWait = false;

        // Auto completion
        if (mCompletionWindow.isEnabled() && !mText.isUndoManagerWorking()) {
            if ((!mConnection.composingText.isComposing() || mProps.autoCompletionOnComposing) && endColumn != 0 && startLine == endLine) {
                mCompletionWindow.requireCompletion();
            } else {
                mCompletionWindow.hide();
            }
            updateCompletionWindowPosition(mCompletionWindow.isShowing());
        } else {
            mCompletionWindow.hide();
        }

        //Log.d(LOG_TAG, "Ins: " + startLine + " " + startColumn + ", " + endLine + " " + endColumn + ", content = " + insertedContent);
        updateCursorAnchor();
        mRenderer.invalidateRenderNodes();
        ensureSelectionVisible();

        mLanguage.getAnalyzeManager().insert(start, end, insertedContent);
        mEventHandler.hideInsertHandle();
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION);
        if (!mCursor.isSelected() && !mConnection.composingText.isComposing() && !mCompletionWindow.shouldRejectComposing()) {
            mCursorAnimator.markEndPos();
            mCursorAnimator.start();
        }
        dispatchEvent(new ContentChangeEvent(this, ContentChangeEvent.ACTION_INSERT, start, end, insertedContent));
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent) {
        mRenderer.updateTimestamp();
        mStyleDelegate.onTextChange();
        var start = mText.getIndexer().getCharPosition(startLine, startColumn);
        var end = start.fromThis();
        end.column = endColumn;
        end.line = endLine;
        end.index = start.index + deletedContent.length();
        mRenderer.buildMeasureCacheForLines(startLine, startLine + 1);

        try {
            if (mStyles != null) {
                mStyles.adjustOnDelete(start, end);
            }
            if (mDiagnostics != null) {
                mDiagnostics.shiftOnDelete(start.index, end.index);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Update failure", e);
        }

        mLayout.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent);

        updateCursor();

        if (mCompletionWindow.isEnabled() && !mText.isUndoManagerWorking()) {
            if (!mConnection.composingText.isComposing() && mCompletionWindow.isShowing()) {
                if (startLine != endLine || startColumn != endColumn - 1) {
                    mCompletionWindow.hide();
                } else {
                    mCompletionWindow.requireCompletion();
                    updateCompletionWindowPosition();
                }
            }
        } else {
            mCompletionWindow.hide();
        }

        //Log.d(LOG_TAG, "Del: " + startLine + " " + startColumn + ", " + endLine + " " + endColumn + ", content = " + deletedContent);
        mRenderer.invalidateRenderNodes();
        if (!mWait) {
            updateCursorAnchor();
            ensureSelectionVisible();
            mEventHandler.hideInsertHandle();
        }
        if (!mCursor.isSelected() && !mWait && !mConnection.composingText.isComposing() && !mCompletionWindow.shouldRejectComposing()) {
            mCursorAnimator.markEndPos();
            mCursorAnimator.start();
        }
        mLanguage.getAnalyzeManager().delete(start, end, deletedContent);
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION);
        dispatchEvent(new ContentChangeEvent(this, ContentChangeEvent.ACTION_DELETE, start, end, deletedContent));
    }

    @Override
    public void beforeModification(Content content) {
        mCursorAnimator.markStartPos();
    }

    @Override
    public void onFormatSucceed(@NonNull CharSequence applyContent, @Nullable TextRange cursorRange) {
        post(() -> {
            int line = mCursor.getLeftLine();
            int column = mCursor.getLeftColumn();
            int x = getOffsetX();
            int y = getOffsetY();
            var string = (applyContent instanceof Content) ? ((Content) applyContent).toStringBuilder() : applyContent;
            mText.beginBatchEdit();
            mText.delete(0, 0, mText.getLineCount() - 1,
                    mText.getColumnCount(mText.getLineCount() - 1));
            mText.insert(0, 0, string);
            mText.endBatchEdit();
            mCompletionWindow.hide();
            mConnection.invalid();
            if (cursorRange == null) {
                setSelectionAround(line, column);
            } else {
                try {
                    var start = cursorRange.getStart();
                    var end = cursorRange.getEnd();
                    setSelectionRegion(start.line, start.column, end.line, end.column);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
            getScroller().forceFinished(true);
            getScroller().startScroll(x, y, 0, 0, 0);
            getScroller().abortAnimation();
            // Ensure the scroll offset is valid
            mEventHandler.scrollBy(0, 0);
            mConnection.reset();
        });
    }

    @Override
    public void onFormatFail(final Throwable throwable) {
        post(() -> Toast.makeText(getContext(), "Format:" + throwable, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRemove(Content content, ContentLine line) {
        mLayout.onRemove(content, line);
    }

}