这里是将该 markdown 文件翻译成中文的尝试:

# API

## 构造函数

| 参数           | 类型             | 描述  |
|--------------|----------------|-----|
| context      | `Context`      |     | 
| attrs        | `AttributeSet` | 可选的 |
| defStyleAttr | `int`          | 可选的 |

## 事件

## 方法

### getComponent

获取内置组件,以便您启用/禁用它们或执行其他操作。

| 参数    | 类型         | 描述                                  |
|-------|------------|-------------------------------------|
| clazz | `Class<T>` | \<T extends EditorBuiltinComponent> |

### replaceComponent

用给定的组件替换内置组件。新组件的启用状态将扩展旧的启用状态。

| 参数          | 类型         | 描述                                   |
|-------------|------------|--------------------------------------|
| clazz       | `Class<T>` | 内置类类型。 如`EditorAutoCompletion.class` |
| replacement | `T`        | 要应用的新组件                              |  
| \<T>        |            | 内置组件的类型                              |

### getKeyMetaStates

获取 KeyMetaStates,它管理编辑器中的 alt/shift 状态

### measureTextRegionOffset

获取行号和分割线的宽度

### getLeftHandleDescriptor

获取在视图上绘制的左侧选择手柄的矩形

### getRightHandleDescriptor

获取在视图上绘制的右侧选择手柄的矩形

### getOffset

获取字符在视图上的 x 偏移量

| 参数     | 类型    | 描述     |
|--------|-------|--------|
| line   | `int` | 字符的行位置 |
| column | `int` | 字符的列位置 |

### getCharOffsetX

获取字符在视图上的 x 偏移量

| 参数     | 类型    | 描述     |
|--------|-------|--------|
| line   | `int` | 字符的行位置 |
| column | `int` | 字符的列位置 |

### getCharOffsetY

获取字符在视图上的 y 偏移量

| 参数     | 类型    | 描述     |
|--------|-------|--------|
| line   | `int` | 字符的行位置 |
| column | `int` | 字符的列位置 |

### getSnippetController

### getCompletionWndPositionMode

参见 `setCompletionWndPositionMode(int)`

### setCompletionWndPositionMode

设置如何控制完成窗口的位置和大小

| 参数   | 类型    | 可选的值                                                                                                        |
|------|-------|-------------------------------------------------------------------------------------------------------------|
| mode | `int` | `WINDOW_POS_MODE_AUTO` <br> `WINDOW_POS_MODE_FOLLOW_CURSOR_ALWAYS` <br> `WINDOW_POS_MODE_FULL_WIDTH_ALWAYS` |

### getProps

获取编辑器的 `DirectAccessProps` 对象。**您可以使用该实例更新编辑器中的某些功能,而无需调用方法。**

### getFormatTip

参见 `setFormatTip(String)`

### setFormatTip

设置格式化时的提示文本

| 参数        | 类型       | 描述 |
|-----------|----------|----|
| formatTip | `String` |    |

### setPinLineNumber

设置行号区域是否会随代码区域一起滚动

| 参数            | 类型        | 描述 |
|---------------|-----------|----|
| pinLineNumber | `boolean` |    |

### isLineNumberPinned

参见 `CodeEditor#setPinLineNumber(boolean)`

### isFirstLineNumberAlwaysVisible

参见 `CodeEditor#setFirstLineNumberAlwaysVisible(boolean)`

### setFirstLineNumberAlwaysVisible

在换行模式下显示屏幕中的第一行行号

| 参数      | 类型        | 描述 |
|---------|-----------|----|
| enabled | `boolean` |    |

### insertText

在编辑器中插入给定的文本。

此方法允许您从编辑器内容之外插入文本。
`text` 的内容不会被检查为完全是符号的字符。

请注意,即使编辑器不可编辑,这仍然有效。 但由于可能的问题,您不应该在那时调用它,特别是当 {@link
#getEditable()} 返回true但{@link #isEditable()} 返回false时

| 参数              | 类型       | 描述                                           |
|-----------------|----------|----------------------------------------------|
| text            | `String` | 要插入的文本,通常是符号的文本                              |
| selectionOffset | `int`    | 相对于要插入的文本的开始位置的新选择位置。 范围从`0`到`text.length()` |

### setAutoCompletionItemAdapter

为自动完成窗口设置适配器

下次窗口更新时生效

| 参数      | 类型                        | 描述         |
|---------|---------------------------|------------|
| adapter | `EditorCompletionAdapter` | 新的适配器,可以为空 |

### setCursorBlinkPeriod

设置光标闪烁周期。如果传递零或负周期,则光标将始终显示。

| 参数     | 类型    | 描述        |
|--------|-------|-----------|
| period | `int` | 光标闪烁的周期时间 |

### getCursorBlink

### isLigatureEnabled

参见 `CodeEditor#setLigatureEnabled(boolean)`

### setLigatureEnabled

启用/禁用所有类型的连字(除了'rlig')。
通常,除非启用此功能不会影响文本测量,否则应禁用它们。

默认禁用。 如果要启用指定类型的连字,请使用 `CodeEditor#setFontFeatureSettings(String)`

| 参数      | 类型        | 描述 |
|---------|-----------|----|
| enabled | `boolean` |    |

要启用 JetBrainsMono 字体的连字,请这样使用:

```
CodeEditor editor;
editor.setFontFeatureSettings(enabled?null:"'liga' 0,'hlig' 0,'dlig' 0,'clig' 0");
```

### setFontFeatureSettings

为编辑器使用的所有绘画设置字体特征设置

参见 `Paint#setFontFeatureSettings(String)`

| 参数       | 类型       | 描述 |
|----------|----------|----|
| features | `String` |    |

### setSelectionHandleStyle

设置选择句柄的样式。

| 参数       | 类型       | 参见                                                                       |
|----------|----------|--------------------------------------------------------------------------|
| features | `String` | `SelectionHandleStyle` <br> `HandleStyleDrop` <br> `HandleStyleSideDrop` |

### getHandleStyle

### isHighlightCurrentBlock

返回是否高亮当前代码块

### setHighlightCurrentBlock

编辑器是否应使用不同的颜色来绘制当前代码块行以及此代码块的起始行和结束行的背景。

| 参数                    | 类型        | 描述       |
|-----------------------|-----------|----------|
| highlightCurrentBlock | `boolean` | 启用/禁用此模块 |

### isStickyTextSelection

返回在选择文本时光标是否粘贴到文本行

### setStickyTextSelection

在选择文本时光标是否应粘贴到文本行。

| 参数              | 类型        | 描述    |
|-----------------|-----------|-------|
| stickySelection | `boolean` | 启用/禁用 |

### isHighlightCurrentLine

### setHighlightCurrentLine

指定编辑器是否应使用不同的颜色来绘制当前行的背景

| 参数                   | 类型        | 描述    |
|----------------------|-----------|-------|
| highlightCurrentLine | `boolean` | 启用/禁用 |

### getEditorLanguage

获取编辑器的语言。

### setEditorLanguage

| 参数   | 类型         | 描述                   |
|------|------------|----------------------|
| lang | `Language` | 编辑器的新 EditorLanguage |

### canHandleKeyBinding

内部回调,用于检查编辑器是否能处理给定的 `KeyEvent` 键绑定

| 参数           | 类型        | 描述            |
|--------------|-----------|---------------|
| keyCode      | `int`     | 键绑定事件的键码。     |
| ctrlPressed  | `boolean` | ‘Ctrl’键是否按下?  |
| shiftPressed | `boolean` | ‘Shift’键是否按下? |
| altPressed   | `boolean` | ‘Alt’键是否按下?   |

### getBlockLineWidth

获取代码块线的宽度

### setBlockLineWidth

设置代码块线的宽度

| 参数 | 类型      | 描述       |
|----|---------|----------|
| dp | `float` | dp 单位的宽度 |

### isWordwrap

### setWordwrap

设置编辑器中的文本是否应折行以适合其大小,默认情况下启用防止单词中断

| 参数       | 类型        | 描述    |
|----------|-----------|-------|
| wordwrap | `boolean` | 启用/禁用 |

### isCursorAnimationEnabled

### setCursorAnimationEnabled

设置是否启用光标动画

| 参数      | 类型        | 描述    |
|---------|-----------|-------|
| enabled | `boolean` | 启用/禁用 |

### getCursorAnimator

获取光标动画

### setCursorAnimator

设置光标动画

| 参数             | 类型               | 描述 |
|----------------|------------------|----|
| cursorAnimator | `CursorAnimator` |    |

### setScrollBarEnabled

滚动时是否显示垂直滚动条

| 参数      | 类型        | 描述    |
|---------|-----------|-------|
| enabled | `boolean` | 启用/禁用 |

### setHorizontalScrollbarThumbDrawable

| 参数       | 类型         | 描述 |
|----------|------------|----|
| drawable | `Drawable` |    |

### getHorizontalScrollbarThumbDrawable

### setHorizontalScrollbarTrackDrawable

| 参数       | 类型         | 描述 |
|----------|------------|----|
| drawable | `Drawable` |    |

### getHorizontalScrollbarTrackDrawable

### setVerticalScrollbarThumbDrawable

| 参数       | 类型         | 描述 |
|----------|------------|----|
| drawable | `Drawable` |    |

### getVerticalScrollbarThumbDrawable

### setVerticalScrollbarTrackDrawable

| 参数       | 类型         | 描述 |
|----------|------------|----|
| drawable | `Drawable` |    |

### getVerticalScrollbarTrackDrawable

### isDisplayLnPanel

### setDisplayLnPanel

当滚动条被用户触碰时,是否在垂直滚动条旁显示行号面板

| 参数             | 类型        | 描述    |
|----------------|-----------|-------|
| displayLnPanel | `boolean` | 启用/禁用 |

### getLnPanelPositionMode

### Set display position mode the line number panel beside vertical scroll bar

设置垂直滚动条旁行号面板的显示位置模式

| 参数   | 类型    | 描述                              | 参见                          |
|------|-------|---------------------------------|-----------------------------|
| mode | `int` | 默认 LineInfoPanelPosition.FOLLOW | `LineInfoPanelPositionMode` |

### getLnPanelPosition

### setLnPanelPosition

设置垂直滚动条旁的行号面板的显示位置

!> 当位置模式为 follow 时,仅 TOP、CENTER 和 BOTTOM 会生效。

| 参数   | 类型    | 描述                              | 参见                      |  
|------|-------|---------------------------------|-------------------------|
| mode | `int` | 默认 LineInfoPanelPosition.FOLLOW | `LineInfoPanelPosition` |

### getLineNumberTipTextProvider

### setLineNumberTipTextProvider

为行号面板设置行号前的提示文本

| 参数       | 类型                          | 描述 |
|----------|-----------------------------|----| 
| provider | `LineNumberTipTextProvider` |    |

### getInsertHandleDescriptor

获取视图上插入光标手柄的矩形

### getTextSizePx

以像素为单位获取文本大小

### setTextSizePx

以像素为单位设置文本大小

| 参数   | 类型      | 描述          |
|------|---------|-------------|
| size | `float` | 以像素为单位的文本大小 |

### getRenderer

### getLineNumberMetrics

### isHardwareAcceleratedDrawAllowed

### setHardwareAcceleratedDrawAllowed

设置是否允许编辑器使用 RenderNode 来绘制文本。

启用此功能会导致更多内存使用,但编辑器可以更快地显示文本。

但是,仅当在此视图上启用硬件加速时,切换才会产生区别。

| 参数              | 类型        | 描述 |
|-----------------|-----------|----|
| acceleratedDraw | `boolean` |    |

### getEdgeEffectColor

获取 EdgeEffect 的颜色

### setEdgeEffectColor

设置 EdgeEffect 的颜色

### getCurrentCursorBlock

### getSpansForLine

获取给定行上的跨度

### measureLineNumber

获取行号区域的宽度(包括行号边距)

### updateCompletionWindowPosition

### deleteText

删除光标前的文本或选中文本(如果有)

### commitText

将文本从 IME 提交到内容

| 参数              | 类型             | 描述 |
|-----------------|----------------|----|
| text            | `CharSequence` |    |
| applyAutoIndent | `boolean`      | 可选 |

### getLineInfoTextSize

### setLineInfoTextSize

设置行信息面板的文本大小

| 参数   | 类型      | 描述              |
|------|---------|-----------------|
| size | `float` | 行信息的文本大小,单位为 SP |

### getNonPrintablePaintingFlags

### setNonPrintablePaintingFlags

设置非打印字符的绘画标志。

指定它们应该绘制在哪里以及一些其他属性。

标志可以`混合`。

| 参数    | 类型    | 描述                                                    |
|-------|-------|-------------------------------------------------------|
| flags | `int` | [标志](/zh-cn/constant?id=flag_draw_whitespace_leading) |

### ensureSelectionVisible

使选择可见

### ensurePositionVisible

| 参数          | 类型        | 描述             | 可选的 |
|-------------|-----------|----------------|-----|
| line        | `int`     | 文本中的行          |     |
| column      | `int`     | 文本中的列          |     |
| noAnimation | `boolean` | 如果不应用动画则为 true | 是   |

### hasClip

是否有裁剪

### getDpUnit

获取 1dp = ?px

### getScroller

从 EventHandler 获取滚动器。您最好不要使用它进行自己的滚动

### isOverMaxY

检查位置是否超过最大 Y 位置

| 参数          | 类型      | 描述         |
|-------------|---------|------------|
| posOnScreen | `float` | 在视图上的 Y 位置 |

### getPointPosition

使用滚动坐标中的位置确定字符位置

| 参数      | 类型      | 描述         |
|---------|---------|------------|
| xOffset | `float` | 滚动坐标中的水平位置 |
| yOffset | `float` | 滚动坐标中的垂直位置 |

### getPointPositionOnScreen

使用视图上的位置确定字符位置

| 参数 | 类型      | 描述      |
|----|---------|---------|
| x  | `float` | 在视图上的 x | 
| y  | `float` | 在视图上的 y |

### hasClip

是否有裁剪

### getDpUnit

获取 1dp = ?px

### getScroller

从 EventHandler 获取滚动器。您最好不要使用它进行自己的滚动

### isOverMaxY

检查位置是否超过最大 Y 位置

| 参数          | 类型      | 描述         |
|-------------|---------|------------|  
| posOnScreen | `float` | 在视图上的 Y 位置 |

### getPointPosition

使用滚动坐标中的位置确定字符位置

| 参数      | 类型      | 描述         |
|---------|---------|------------|
| xOffset | `float` | 滚动坐标中的水平位置 |
| yOffset | `float` | 滚动坐标中的垂直位置 |

### getPointPositionOnScreen

使用视图上的位置确定字符位置

| 参数 | 类型      | 描述      |
|----|---------|---------|
| x  | `float` | 在视图上的 x |
| y  | `float` | 在视图上的 y |

### hasClip

是否有裁剪

### getDpUnit

获取 1dp = ?px

### getScroller

从 EventHandler 获取滚动器。您最好不要使用它进行自己的滚动

### isOverMaxY

检查位置是否超过最大 Y 位置

| 参数          | 类型      | 描述         |  
|-------------|---------|------------|
| posOnScreen | `float` | 在视图上的 Y 位置 |