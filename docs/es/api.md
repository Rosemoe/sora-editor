# API

## Constructor

| PARAM        | TYPE           | DESCRIPTION |
|--------------|----------------|-------------|
| context      | `Context`      |             |
| attrs        | `AttributeSet` | optional    |
| defStyleAttr | `int`          | optional    |

## Events

## Methods

### getComponent

Get builtin component so that you can enable/disable them or do some other actions.

| PARAM | TYPE       | DESCRIPTION                         |
|-------|------------|-------------------------------------|
| clazz | `Class<T>` | \<T extends EditorBuiltinComponent> |

### replaceComponent

Replace the built-in component to the given one. The new component's enabled state will extend the
old one.

| PARAM       | TYPE       | DESCRIPTION                                               |
|-------------|------------|-----------------------------------------------------------|
| clazz       | `Class<T>` | Built-in class type. Such as `EditorAutoCompletion.class` |
| replacement | `T`        | The new component to apply                                |
| \<T>        |            | Type of built-in component                                |

### getKeyMetaStates

Get KeyMetaStates, which manages alt/shift state in editor

### measureTextRegionOffset

Get the width of line number and divider line

### getLeftHandleDescriptor

Get the rect of left selection handle painted on view

### getRightHandleDescriptor

Get the rect of right selection handle painted on view

### getOffset

Get the character's x offset on view

| PARAM  | TYPE  | DESCRIPTION                      |
|--------|-------|----------------------------------|
| line   | `int` | The line position of character   |
| column | `int` | The column position of character |

### getCharOffsetX

Get the character's x offset on view

| PARAM  | TYPE  | DESCRIPTION                      |
|--------|-------|----------------------------------|
| line   | `int` | The line position of character   |
| column | `int` | The column position of character |

### getCharOffsetY

Get the character's y offset on view

| PARAM  | TYPE  | DESCRIPTION                      |
|--------|-------|----------------------------------|
| line   | `int` | The line position of character   |
| column | `int` | The column position of character |

### getSnippetController

### getCompletionWndPositionMode

see `setCompletionWndPositionMode(int)`

### setCompletionWndPositionMode

Set how should we control the position&size of completion window

| PARAM | TYPE  | Optional                                                                                                    |
|-------|-------|-------------------------------------------------------------------------------------------------------------|
| mode  | `int` | `WINDOW_POS_MODE_AUTO` <br> `WINDOW_POS_MODE_FOLLOW_CURSOR_ALWAYS` <br> `WINDOW_POS_MODE_FULL_WIDTH_ALWAYS` |

### getProps

Get `DirectAccessProps` object of the editor. **You can update some features in editor with the
instance without disturb to call methods.**

### getFormatTip

see `setFormatTip(String)`

### setFormatTip

Set the tip text while formatting

| PARAM     | TYPE     | DESCRIPTION |
|-----------|----------|-------------|
| formatTip | `String` |             |

### setPinLineNumber

Set whether line number region will scroll together with code region

| PARAM         | TYPE      | DESCRIPTION |
|---------------|-----------|-------------|
| pinLineNumber | `boolean` |             |

### isLineNumberPinned

see `CodeEditor#setPinLineNumber(boolean)`

### isFirstLineNumberAlwaysVisible

see `CodeEditor#setFirstLineNumberAlwaysVisible(boolean)`

### setFirstLineNumberAlwaysVisible

Show first line number in screen in word wrap mode

| PARAM   | TYPE      | DESCRIPTION |
|---------|-----------|-------------|
| enabled | `boolean` |             |

### insertText

Inserts the given text in the editor.

This method allows you to insert texts externally to the content of editor.
The content of `text` is not checked to be exactly characters of symbols.

Note that this still works when the editor is not editable. But you should not
call it at that time due to possible problems, especially when {@link #getEditable()} returns
true but {@link #isEditable()} returns false

| PARAM           | TYPE     | DESCRIPTION                                                                                        |
|-----------------|----------|----------------------------------------------------------------------------------------------------|
| text            | `String` | Text to insert, usually a text of symbols                                                          |
| selectionOffset | `int`    | New selection position relative to the start of text to insert.Ranging from `0` to `text.length()` |

### setAutoCompletionItemAdapter

Set adapter for auto-completion window

Will take effect next time the window updates

| PARAM   | TYPE                      | DESCRIPTION             |
|---------|---------------------------|-------------------------|
| adapter | `EditorCompletionAdapter` | New adapter, maybe null |

### setCursorBlinkPeriod

Set cursor blinking period.If zero or negative period is passed, the cursor will always be shown.

| PARAM  | TYPE  | DESCRIPTION                        |
|--------|-------|------------------------------------|
| period | `int` | The period time of cursor blinking |

### getCursorBlink

### isLigatureEnabled

see `CodeEditor#setLigatureEnabled(boolean)`

### setLigatureEnabled

Enable/disable ligature of all types(except 'rlig').
Generally you should disable them unless enabling this will have no effect on text measuring.

Disabled by default. If you want to enable ligature of a specified type,
use`CodeEditor#setFontFeatureSettings(String)`

| PARAM   | TYPE      | DESCRIPTION |
|---------|-----------|-------------|
| enabled | `boolean` |             |

For enabling JetBrainsMono font's ligature, Use like this:

```
CodeEditor editor;
editor.setFontFeatureSettings(enabled?null:"'liga' 0,'hlig' 0,'dlig' 0,'clig' 0");
```

### setFontFeatureSettings

Set font feature settings for all paints used by editor

see `Paint#setFontFeatureSettings(String)`

| PARAM    | TYPE     | DESCRIPTION |
|----------|----------|-------------|
| features | `String` |             |

### setSelectionHandleStyle

Set the style of selection handler.

| PARAM    | TYPE     | see                                                                      |
|----------|----------|--------------------------------------------------------------------------|
| features | `String` | `SelectionHandleStyle` <br> `HandleStyleDrop` <br> `HandleStyleSideDrop` |

### getHandleStyle

### isHighlightCurrentBlock

Returns whether highlight current code block

### setHighlightCurrentBlock

Whether the editor should use a different color to draw
the current code block line and this code block's start line and end line's
background.

| PARAM                 | TYPE      | DESCRIPTION                    |
|-----------------------|-----------|--------------------------------|
| highlightCurrentBlock | `boolean` | Enabled / Disabled this module |

### isStickyTextSelection

Returns whether the cursor should stick to the text row while selecting the text

### setStickyTextSelection

Whether the cursor should stick to the text row while selecting the text.

| PARAM           | TYPE      | DESCRIPTION        |
|-----------------|-----------|--------------------|
| stickySelection | `boolean` | Enabled / Disabled |

### isHighlightCurrentLine

### setHighlightCurrentLine

Specify whether the editor should use a different color to draw the background of current line

| PARAM                | TYPE      | DESCRIPTION        |
|----------------------|-----------|--------------------|
| highlightCurrentLine | `boolean` | Enabled / Disabled |

### getEditorLanguage

Get the editor's language.

### setEditorLanguage

| PARAM | TYPE       | DESCRIPTION                   |
|-------|------------|-------------------------------|
| lang  | `Language` | New EditorLanguage for editor |

### canHandleKeyBinding

Internal callback to check if the editor is capable of handling the given keybinding `KeyEvent`

| PARAM        | TYPE      | DESCRIPTION                           |
|--------------|-----------|---------------------------------------|
| keyCode      | `int`     | The keycode for the keybinding event. |
| ctrlPressed  | `boolean` | Is 'Ctrl' key pressed?                |
| shiftPressed | `boolean` | Is 'Shift' key pressed?               |
| altPressed   | `boolean` | Is 'Alt' key pressed?                 |

### getBlockLineWidth

Get the width of code block line

### setBlockLineWidth

Set the width of code block line

| PARAM | TYPE    | DESCRIPTION      |
|-------|---------|------------------|
| dp    | `float` | Width in dp unit |

### isWordwrap

### setWordwrap

Set whether text in editor should be wrapped to fit its size, with anti-word-breaking enabled by
default

| PARAM    | TYPE      | DESCRIPTION        |
|----------|-----------|--------------------|
| wordwrap | `boolean` | Enabled / Disabled |

### isCursorAnimationEnabled

### setCursorAnimationEnabled

Set cursor animation enabled

| PARAM   | TYPE      | DESCRIPTION        |
|---------|-----------|--------------------|
| enabled | `boolean` | Enabled / Disabled |

### getCursorAnimator

Get cursor animation

### setCursorAnimator

Set cursor animation

| PARAM          | TYPE             | DESCRIPTION |
|----------------|------------------|-------------|
| cursorAnimator | `CursorAnimator` |             |

### setScrollBarEnabled

Whether display vertical scroll bar when scrolling

| PARAM   | TYPE      | DESCRIPTION        |
|---------|-----------|--------------------|
| enabled | `boolean` | Enabled / Disabled |

### setHorizontalScrollbarThumbDrawable

| PARAM    | TYPE       | DESCRIPTION |
|----------|------------|-------------|
| drawable | `Drawable` |             |

### getHorizontalScrollbarThumbDrawable

### setHorizontalScrollbarTrackDrawable

| PARAM    | TYPE       | DESCRIPTION |
|----------|------------|-------------|
| drawable | `Drawable` |             |

### getHorizontalScrollbarTrackDrawable

### setVerticalScrollbarThumbDrawable

| PARAM    | TYPE       | DESCRIPTION |
|----------|------------|-------------|
| drawable | `Drawable` |             |

### getVerticalScrollbarThumbDrawable

### setVerticalScrollbarTrackDrawable

| PARAM    | TYPE       | DESCRIPTION |
|----------|------------|-------------|
| drawable | `Drawable` |             |

### getVerticalScrollbarTrackDrawable

### isDisplayLnPanel

### setDisplayLnPanel

Whether display the line number panel beside vertical scroll bar when the scroll bar is touched by
user

| PARAM          | TYPE      | DESCRIPTION        |
|----------------|-----------|--------------------|
| displayLnPanel | `boolean` | Enabled / disabled |

### getLnPanelPositionMode

### Set display position mode the line number panel beside vertical scroll bar

Set display position mode the line number panel beside vertical scroll bar

| PARAM | TYPE  | DESCRIPTION                          | see                         |
|-------|-------|--------------------------------------|-----------------------------|
| mode  | `int` | Default LineInfoPanelPosition.FOLLOW | `LineInfoPanelPositionMode` |

### getLnPanelPosition

### setLnPanelPosition

Set display position the line number panel beside vertical scroll bar

!> Only TOP,CENTER and BOTTOM will be effective when position mode is follow.

| PARAM | TYPE  | DESCRIPTION                          | see                     |
|-------|-------|--------------------------------------|-------------------------|
| mode  | `int` | Default LineInfoPanelPosition.FOLLOW | `LineInfoPanelPosition` |

### getLineNumberTipTextProvider

### setLineNumberTipTextProvider

Set the tip text before line number for the line number panel

| PARAM    | TYPE                        | DESCRIPTION | 
|----------|-----------------------------|-------------|
| provider | `LineNumberTipTextProvider` |             |

### getInsertHandleDescriptor

Get the rect of insert cursor handle on view

### getTextSizePx

Get text size in pixel unit

### setTextSizePx

Set text size in pixel unit

| PARAM | TYPE    | DESCRIPTION             | 
|-------|---------|-------------------------|
| size  | `float` | Text size in pixel unit |

### getRenderer

### getLineNumberMetrics

### isHardwareAcceleratedDrawAllowed

### setHardwareAcceleratedDrawAllowed

Set whether allow the editor to use RenderNode to draw its text.

Enabling this can cause more memory usage, but the editor can display text much quicker.

However, only when hardware accelerate is enabled on this view can the switch make a difference.

| PARAM           | TYPE      | DESCRIPTION | 
|-----------------|-----------|-------------|
| acceleratedDraw | `boolean` |             |

### getEdgeEffectColor

Get the color of EdgeEffect

### setEdgeEffectColor

Set the color of EdgeEffect

### getCurrentCursorBlock

### getSpansForLine

Get spans on the given line

### measureLineNumber

Get the width of line number region (include line number margin)

### updateCompletionWindowPosition

### deleteText

Delete text before cursor or selected text (if there is)

### commitText

Commit text to the content from IME

| PARAM           | TYPE           | DESCRIPTION | 
|-----------------|----------------|-------------|
| text            | `CharSequence` |             |
| applyAutoIndent | `boolean`      | Optional    |

### getLineInfoTextSize

### setLineInfoTextSize

Set text size for line info panel

| PARAM | TYPE    | DESCRIPTION                                | 
|-------|---------|--------------------------------------------|
| size  | `float` | Text size for line information, unit is SP |

### getNonPrintablePaintingFlags

### setNonPrintablePaintingFlags

Sets non-printable painting flags.

Specify where they should be drawn and some other properties.

Flags can be `mixed`.

| PARAM | TYPE  | DESCRIPTION                                              | 
|-------|-------|----------------------------------------------------------|
| flags | `int` | [Flags](/zh-cn/constant?id=flag_draw_whitespace_leading) |

### ensureSelectionVisible

Make the selection visible

### ensurePositionVisible

| PARAM       | TYPE      | DESCRIPTION                            | Optional |
|-------------|-----------|----------------------------------------|----------|
| line        | `int`     | Line in text                           |          |
| column      | `int`     | Column in text                         |          |
| noAnimation | `boolean` | true if no animation should be applied | Yes      |

### hasClip

Whether there is clip

### getDpUnit

Get 1dp = ?px

### getScroller

Get scroller from EventHandler.You would better not use it for your own scrolling

### isOverMaxY

Checks whether the position is over max Y position

| PARAM       | TYPE    | DESCRIPTION        |
|-------------|---------|--------------------|
| posOnScreen | `float` | Y position on view |

### getPointPosition

Determine character position using positions in scroll coordinate

| PARAM   | TYPE    | DESCRIPTION                              |
|---------|---------|------------------------------------------|
| xOffset | `float` | Horizontal position in scroll coordinate |
| yOffset | `float` | Vertical position in scroll coordinate   |

### getPointPositionOnScreen

Determine character position using positions on view

| PARAM | TYPE    | DESCRIPTION |
|-------|---------|-------------|
| x     | `float` | X on view   |
| y     | `float` | Y on view   |