# Changelog

## **[0.21.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.21.0) (2023-01-08)**

> This release includes bug fixes and little improvement as well as minor API changes.

### Bugs fixed

- incorrect horizontal scroll range when `renderFunctionCharacter` is on
- `TSQuery` is not verified before making access to it
- IME that relies on `InputConnection#getSurroundingText` gets invalid position description when `maxIPCTextLength` is
  exceeded
- invalid composing text range can be set through `EditorInputConnection#setComposingRegion`
- text becomes dirty when using old Gboard to delete characters fast
- animated row background is on wrong layer
- empty text is matched repeatly in regex, leading to OOM
- `StringIndex OutOfBoundsException` in `TextMateNewlineHandler` (by **[@dingyi222666](https://github.com/dingyi222666)**)
- `CodeEditor#release` does not detach `EditorColorScheme`
- NPE when sending message in `AsyncIncrementalAnalyzeManager`
- leaking thread by `TextMateLanguage#updateLanguage`
- `ReplaceAction` can be added without its `DeleteAction`
- nested undo/redo is possible
- `JavaTextTokenizer` does not take down current token [#349](https://github.com/Rosemoe/sora-editor/issues/349)
- `AsyncIncrementalAnalyzeManager` does not update the spans of last line being affected by
  insertion [#350](https://github.com/Rosemoe/sora-editor/issues/350)
- `Styles#eraseAllLineStyles` raises npe when no line style is there

### Improvments

- better search experience, including support for whole word search, speed improvement of regex search, cyclic jumping
  and better scrolling strategy [#321](https://github.com/Rosemoe/sora-editor/issues/321)
- performance improvement of tree-sitter predicates
- avoid edge effect in `CodeEditor#ensurePositionVisible`
- boost the speed of querying search results by binary search
- show built-in text actions window on insert handle release
- add touch slop before magnifier is triggered
- add `I18nConfig` for application provided replacement string resources
- add `DirectAccessProps#clipboardTextLengthLimit` and add tip when text is unable to be copied due to limit
  or `TransactionTooLargeException`
- more exact position of built-in text actions window
- editor uses global default color scheme on startup
- enhanced function of `DirectAccessProps#disallowSuggestions` (tested on newest Gboard and Sogou Input for MIUI)
- adapt view parameters for long screenshot in MIUI system
- add two new accessibility actions since Android M

### Updated API

- new function `Content#substring`
- new event `PublishSearchResultEvent`, which is called when search result is available in main thread or searching is
  stopped
- new `I18nConfig` class for replacing string resources
- new function `add CodeEditor#isAntiWordBreaking`
- **[BREAKING]** new API in `EditorColorScheme` for global default theme. All newly-created editors use the global
  default theme. Your modifications to global default theme will reflect in those editors.
- **[BREAKING]** `CodeEditor#getScroller` now has return type `EditorScroller`. Get the original `OverScroller`
  by `EditorScroller#getImplScroller`
- **[BREAKING]** `AsyncIncrementalAnalyzeManager` and `TsAnalyzeManager` will receive thread interruption on exit

## **[0.20.4](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.4) (2022-12-30)**

### Bugs Fixed

- member variable highlighting in tree-sitter
- editor crashes in rendering highlighted delimeters when spans are corrupted
- `EditorDiagnosticTootipWindow` can not be got from editor components
- `EventManager` can not be disabled

### New Features

- `CodeEditor#createSubEventManager()`

## **[0.20.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.2) (2022-12-27)**

### Fixed Bugs

- indent is unexpectedly doubled in keybinding `Ctrl+D`
- default completion layout can not be clicked after animation unless you scroll the list
- invalid indices are used in editing `TSTree` in main thread
- `IndexOutOfBoundsException` when appending text if tree-sitter is used
- unable to run predicates after text editions in tree-sitter

### New features

- line info panel style is now a bubble [#283](https://github.com/Rosemoe/sora-editor/issues/283)
- `Content.getDocumentVersion()` is added for checking document modifications
- `DiagnosticDetail` for describing diagnostics and builtin `EditorDiagnosticTooltipWindow` [#314](https://github.com/Rosemoe/sora-editor/issues/314)
- experimental `Quickfix` item
- support members scope in tree-sitter (see `LocalsCaptureSpec` for more information) and improve speed in finding
  definitions
- custom text provider for line info panel `LineNumberTipTextProvider`

## **[0.20.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.1) (2022-12-21)**

- Fixed a critical bug of tree-sitter code blocks in previous release of 0.20.0

## **[0.20.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.0) (2022-12-21)**

!> Use `0.20.1` instead

### Bugs fixed

- memory leaks in `LanguageServerWrapper`
- `EditorSearcher.gotoPrevious()` always jumps to the first item when regex is enabled
- transparent line in selection handle drawable ([#270](https://github.com/Rosemoe/sora-editor/issues/270), fixed by [@tegajoel](https://github.com/tegajoel) [#312](https://github.com/Rosemoe/sora-editor/pull/312))
- whole document becomes black-colored when user is scaling in some languages
- `SimpleAnalyzeManager` ignores line separators
- `OnigRegExp` returns corrupted cache in multi-threaded access ([#315](https://github.com/Rosemoe/sora-editor/issues/315) by [@xyzxqs](https://github.com/xyzxqs))
- `TextReference.toString()` does not return string of backed sequence
- text context region can be unexpectedly bigger the bounds of text [#318](https://github.com/Rosemoe/sora-editor/issues/318)
- textmate newline handler fixs ([#319](https://github.com/Rosemoe/sora-editor/issues/319) by [@dingyi222666](https://github.com/dingyi222666))
- `corePoolSize` exceeds `maximumPoolSize` on 1-core devices [#320](https://github.com/Rosemoe/sora-editor/issues/320)

### New Features

- add support for `wrap_content` in view measuring (not recommended, be cautious)
- better interaction with vertical scrollbar
- `IdentifierAutoComplete` now does not add item that is the same as prefix
- reselect text after long press again (optional)
- show ASCII function characters in editor style (optional, enabled by default)
- **tree-sitter support, including highlighting, code blocks and brackets for editor, as well as tree-sitter predicate support**

### Special Thanks

- **@itsaky for [android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter)**

## **[0.19.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.19.0) (2022-12-15)**

### Bugs fixed

- can't add language with scope not "source" ([#288](https://github.com/Rosemoe/sora-editor/issues/288) by [@dingyi222666](https://github.com/dingyi222666))
- nullable data bundle in ImePrivateCommandEvent is marked nonnull  [#293](https://github.com/Rosemoe/sora-editor/issues/293)
- index out of bounds when rendering in preset composing state [#294](https://github.com/Rosemoe/sora-editor/issues/294)
- magnifier shows when window size is illegal  [#298](https://github.com/Rosemoe/sora-editor/issues/298)
- partly broken logic of Content#deleteInternal after introducing line separator types [#299](https://github.com/Rosemoe/sora-editor/issues/299)
- completion window size is not adjusted when size is too small
- layout is not recreated after scaling when wordwrap is enabled and gutter is disabled
- occasional invalid height for completion window
- occasional NPE when searching with regex (AndroidIDE#588)
- unexpected symbol pair matching for '<' '>' in textmate Java
- unexpected symbol pair match when pasting text (AndroidIDE#593)
- selection span can be out of range when shifting cursor in some situations(AndroidIDE#579)
- posted Runnable can be executed out of editor lifecycle [#305](https://github.com/Rosemoe/sora-editor/issues/305)
- Spans.Reader is not locked during text line rendering, giving chances to hold dirty spanOffset if span content is
  modified in another thread [#290](https://github.com/Rosemoe/sora-editor/issues/290)
- editor layout state is not check when layout initialization is finished and is trying to update editor state [#307](https://github.com/Rosemoe/sora-editor/issues/307)
- View#post does not execute posted actions on startup, causing pending layoutBusy state [#308](https://github.com/Rosemoe/sora-editor/issues/308)

### New features

- updated auto completion ui and sorting method (by **[@dingyi222666 ](https://github.com/dingyi222666)**)
- lua language server in sample app (by **[@dingyi222666](https://github.com/dingyi222666)**)
- expose some internal classes of `EditorRenderer` for better extension
- very alpha release of `tree-sitter` [RIP]

## **[0.18.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.18.1) (2022-11-05)**

> A major update of editor. This release note covers the updates of 0.18.0 and 0.18.1

### Improvements

- more built-in key bindings (by **[@itsaky](https://github.com/itsaky)**)
- update lsp4j to 0.17.0
- anti-word-breaking option for wordwrap (for English). enabled by default
- animation for fading scrollbars
- registry feature in textmate, which provides multi-language highlighting in one file (by **[@dingyi222666](https://github.com/dingyi222666)**)
- hardwrap marker
- input connection events `BuildEditorInfoEvent` and `ImePrivateCommandEvent`
- symbol pair completion for textmate (by **[@dingyi222666](https://github.com/dingyi222666)**)
- updated symbol pair api (by **[@dingyi222666](https://github.com/dingyi222666)**)
- add bom module (by **[@keta1](https://github.com/keta1)**)
- support indentationRules in textmate (by **[@dingyi222666](https://github.com/dingyi222666)**)

### Fixed Bugs

- [VITAL] wrong state passed to incremental analysis process when line state-affecting text is inserted
- selection anchor not updated when moving lines (by **[@itsaky](https://github.com/itsaky)**)
- possible crash on low API device [#246](https://github.com/Rosemoe/sora-editor/issues/246)
- measure cache is not verified before being used
- selection animation is unexpectedly canceled when composing text is present
- selection scale animation causes visual lag when setting new selection position
- emoji character is unexpectedly cut in wordwrap mode
- potential recycled editor in LspEditor#setWrapperLanguage
- IndexOutOfBoundsException generated during rendering with complex text directions
- invalid selection position for RTL text under wordwrap mode
- unable to type after scaling text under wordwrap mode
- `MatchHelper#startsWith` not working correctly
- editor saves redundant undo operation when user is using newest Gboard and some other IMEs
- `CodeEditor#commitTab` ignores language settings
- `EditorKeyEvent` can not be intercepted

### Migration

- Due to new API of textmate, some methods are deprecated. They are still compatiable until 0.20.0 or so. See [#282](https://github.com/Rosemoe/sora-editor/issues/282) for detailed message
- `NewlineHandler` should get textBefore and textAfter by manually slicing given text with the text position
- refer to [new symbol pair interface](https://github.com/Rosemoe/sora-editor/blob/b343f0ae71ab3f8fe06576fcd9a813eb78554935/editor/src/main/java/io/github/rosemoe/sora/widget/SymbolPairMatch.java)

## **[0.17.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.17.2) (2022-10-08)**

### Bugs Fixed

- fix unexpected cursor animation when text is not editable
- fix text selecting issue on low API devices [#238](https://github.com/Rosemoe/sora-editor/issues/238)
- fix unsupported symbol pair is still being used in textmate
- notify the input method when layout is initialized
- fix potential nullptr when `CodeEdditor#updateStyle` is called
- fix compatibility issues in textmate ([#252](https://github.com/Rosemoe/sora-editor/pull/252) by [@MuntashirAkon](https://github.com/MuntashirAkon))
- fix symbol input wrongly adding text to editor when editor is not editable [#253](https://github.com/Rosemoe/sora-editor/issues/253)
- fix unnrenderer trailing diagnostics when text is short
- fix horizontal scrollbar becoming too short to touch when text includes any single long line ([#259](https://github.com/Rosemoe/sora-editor/pull/259) by [@summerain0](https://github.com/summerain0))
- fix selected text color on high API devices
- fix wrong display of code block lines in textmate when using tabs [#248](https://github.com/Rosemoe/sora-editor/issues/248)
- fix ContentReference.RefReader ignoring line separator type [#260](https://github.com/Rosemoe/sora-editor/issues/260)
- fix completion window not getting hidden when user switches between tabstops
- fix conflict of edge effect and reversed scroll on Android 11 and below
- fix `NewlineHandler` not triggered when completion window is shown
- fix wrongly inserted line separator in completion window

### Improvements

- add `StyleUpdateRange` for performance when highlight is updated
- (BREAKING) separate some less frequently used field to  `AdvancedSpan` from `Span`
- support `highlightedDelimetersForeground` color property in textmate ([#247](https://github.com/Rosemoe/sora-editor/pull/247) by [@PranavPurwar](https://github.com/PranavPurwar))
- add optional enchanced functionality of HOME and END
- reorder the dispatch of content edit event and completion request
- add support for custom scrollbar styles ([#255](https://github.com/Rosemoe/sora-editor/pull/255) by [@MuntashirAkon](https://github.com/@MuntashirAkon))
- line info panel position can be changed to other places ([#305](https://github.com/Rosemoe/sora-editor/pull/259) by [@summerain0](https://github.com/summerain0))
- improve connection speed of language server ([#262](https://github.com/Rosemoe/sora-editor/pull/262) by [@dingyi222666](https://github.com/dingyi222666))
- add `verticalExtraSpaceFactor` for extra space size in vertical viewport

### Dependency

- remove unused xerces dependency ([#243](https://github.com/Rosemoe/sora-editor/pull/243) by [@PranavPurwar](https://github.com/PranavPurwar))
- update kotlin to 1.7.20
- update snakeyaml to 1.33

## **[0.17.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.17.1) (2022-09-04)**

### Bugs Fixed

- fix nullptr when destroying lsp server without completion implementation
- fix trailing FORMAT sting at snippet end causing index out of bounds
- fix a bug in `JavaTextTokenizer` that causes the completion stop

### Improvements

- editor code snippets now support complex variables and placeholders (with transform or choices) as well as usage
  of `\u` and interpolated shell code
- do not log `CompletionCancelledException` in `CompletionThread`
- add `SnippetEvent` for code snippet events
- export more API in `SnippetController`
- add `QuickQuoteHandler` (separated from `SymbolPairMatch`)
- add code snippet support in editor-lsp backed by editor snippet (by *
  *[@dingyi222666 ](https://github.com/dingyi222666 )**)
- add `StylesUtils#checkNoCompletion`
- enhanced `NewlineHandler` interface

## **[0.17.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.17.0) (2022-09-03)**

### Fixed bugs

- a potential bug in composing text
- invalid emoji text display when using textmate
- an exception when using lsp (by **[@dingyi222666](https://github.com/dingyi222666)**)
- stack overflow in `copyLine()`  (by **[@itsaky](https://github.com/itsaky)**)
- potential nullptr when user selects completion item
- invalid display of code block lines after text style update
- bug in selecting text when ICU is unavailable [#238](https://github.com/Rosemoe/sora-editor/issues/238)
- occasional index out of bounds in drawText

### New features and Improvements

- Optional bold highlighted delimiters (by **[@ikws4](https://github.com/ikws4)**)
- Better auto completion in lsp (by **[@dingyi222666](https://github.com/dingyi222666)**)
- Upstream update of tm4e (by **[@dingyi222666](https://github.com/dingyi222666)**)
- Line number color for current line
- Very basic support of code snippets (simple tabstops, variables and placeholder. choice and rules are unavailable now)

### Breaking changes

> tm4e upstream code is changed significantly. You are expected to refer to our sample app for migrating old code.

## **[0.16.4](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.4) (2022-08-14)**

### Updates

- Fix potential bug when format thread continues after setText() is called
- Merge composing text edits with composing text submission in undo stack
- Smoother layout update when text size changed after user scaling
- Optional animation in CodeEditor#ensurePositionVisible
- Cut unnecessary memory usage of Paint
- Fix bugs in BlockIntList, which affects horizontal scroll range when wordwrap is disabled
- Support standalone divider margins for left and right
- Update style of default text action window
- Gson dependency update
- Internal field naming updates

### Note

> Users who rely on using internal fields should check their reflection targets.

## **[0.16.3](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.3) (2022-08-11)**

### Updates

- add shortcut method for setting keywords in textmate auto completer
- do not show identifier if it is duplicated with a keyword in `IdentifierAutoCompleter`
- fix bug in rendering text background when wordwrap is enabled

## **[0.16.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.2) (2022-08-11)**

### Updates

- fix potential bug in ContentCreator when reading CRLF text
- fix bug in Content#copyText
- fix occasional concurrent modification error when invoking `ContentBidi` and `CachedIndexer`
- performance enhancement in text copying
- some internal code style uniformation
- add some potential combined character clusters

## **[0.16.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.1) (2022-08-10)**

### Updates

- Specify line separators for new lines when users edit text in editor
- Fix Content#subSequence bugs in CRLF text
- Fix composing text bug in a specific situations when `trackComposingText` is enabled
- Do not highlight delimiters when text is selected
- Remove debug logs
- Performance enhancement in ContentLine#subSequence

## **[0.16.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.0) (2022-08-10)**

### Performance Enhancements

- This release is mainly focused on performance and display correctness. We optimized the speed of highlighting delimiters and the time cost when text is editted. We also add a new mode called "Basic Display Mode", which provides basic display of text and fast measuring speed. However, some features such as RTL and ligatures are disabled when this is enabled.

### New Features

- correct LTR and RTL mixed display
- customizible round text background factor
- side icon for lines and its click event are available now
- mixed CR,LF,CRLF in `Content` (Note that CodeEditor#setLineSeparator is not complete yet. editor still uses LF for
  newlines when you edit text in editor)

### Bugs fixed

- fix the width measurement of RTL texts
- fix bugs in `TextRegionIterator`

## **[0.15.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.15.1) (2022-07-23)**

> This is a major update of sora-editor

### New features

- `editor-lsp` for LSP users, provided by @dingyi222666
- new selection position can be provided by formatters
- code completion in language-textmate (optional)
- specify whether a scheme is dark and some new color ids for completion
  window [#215](https://github.com/Rosemoe/sora-editor/issues/215)
- specify line background color from language analyzer and these backgrounds are automatically shifted when text is
  editted
- highlight matching delimiters (including underline, bold and background) (optional)

### Improvements

- quicker speed when finding index in text
- use Android 12 introduced new `EdgeEffect`
- reusing layout objects and async loading
- editor is still partially interactive when formatting
- draw only visible region of diagnostics
- optimize shifting logic of diagnostics
- optimize the speed of deleting texts in editor, especially when deleting text with a lot of lines
- better display of symbols in wordwrap mode, especially in Chinese and Japanese
- do not recreate layout when text size is not actually changed when the user finishes scaling
- better user experience when editor is not editable
- more switches in `DirectAccessProps`

### Fixed bugs

- fix potential NPE during destruction of `AsyncIncrementalAnalyzeManager`
- fix concurrent issue of `InsertTextHelper` **critical**
- fix sometimes wrongly drawn newline markers on high API devices

### Notice

- From **next version** of sora-editor, the min SDK version will rise to Android API 24 due to better maintainence.

## **[0.14.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.14.0) (2022-06-29)**

### Bug fix

- Fix unnotified text change for IME when undo/redo [#210](https://github.com/Rosemoe/sora-editor/issues/210)
- Fix bad scroll range [#212](https://github.com/Rosemoe/sora-editor/issues/212)
- Bounds check in Content
- Reset batch edit when `Content` object is detached
- Reset all styles when text or language changes

### Improvements

- `Indexer` and `Content` share a lock
- `Content` has 8x faster speed when inserting
- `Content` now recoginzes newline correctly: CR, LF, CRLF are all considered '\n'

### New Features

- Brackets matching and highlighting in language-java and language-textmate [#194](https://github.com/Rosemoe/sora-editor/issues/194)

## **[0.13.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.13.1) (2022-06-26)**

- Fix some issues in composing text
- Fix incorrect sticky selection

## **[0.13.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.13.0) (2022-06-25)**

### Improvements

- Fix unexpectedly created 512 bytes array when drawing
- Fix text width caching
- Optimize memory usage of language-java, language-textmate
- Optimize speed of analyzing code blocks in language-textmate
- Cache theme colors in textmate
- Track composing text when external text changes occur [#186](https://github.com/Rosemoe/sora-editor/issues/186) [#204](https://github.com/Rosemoe/sora-editor/issues/204)
- Add new APIs in `Layout`
- Fix some deprecations and better RTL support [@PranavPurwar](https://github.com/PranavPurwar)
- Add line spacing APIs
- Add option for round text background

### Chores

- Update dependencies of textmate & testing instrumentations

### Breaking changes

- `UIThreadIncrementalAnalyzeManager` is removed
- `IdentifierAutoComplete` is updated so that it can be used incrementally
- `IncrementalAnalyzeManager` now have integer for line index passed in `tokenizeLine`

## **[0.12.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.12.0) (2022-06-21)**

### Bug fix

- Fix parent view of Magnifier
- Fix missing `invalidate()` call in `setFontFeatureSettings()`
- Remove unnecessary string resource items
- Adapt Mircosoft Swift Key
- Fix potential invalid state of SHIFT and ALT in `EditorKeyEvent`
- Fix invalid error indicator line display (invalid phi for the wavy lines)
- Fix 1 char invisible at line ending sometimes when wordwrap is enabled

### New & Improvements

- Custom scale factor in `Magnifier`
- Add time limit for merging undo actions (can be modified by `UndoManager#setMergeTimeLimit(long)`)
- Better magnifier image quality (@massivemadness )
- Better magnifer position when sticky cursor is enabled [@massivemadness](https://github.com/massivemadness)
- `KeyBindingEvent` and some new built-in keybindings [@itsaky](https://github.com/itsaky)
- Improved cursor animation (`ScaleCursorAnimation`)
- Diagnostics APIs
- More diagnostic indicator styles (`DiagnosticIndicatorStyle`)
- Approriate default size for diagnostic indicator lines

### Breaking changes

- `Span#problemFlags`, `MappedSpans#markProblemRegion` and `MappedSpans.Builder#markProblemRegion` are removed. Instead, you are expected to replace them with Diagnostic APIs
- In order to catch up with the updates from tm4e in time, some packages in language-textmate are moved to its original package in tm4e project.

### Migration Guide

> Mainly, your work will be miragting your problem marking logic to the new diagnostic API.
Now, the diagnostics are sent by calling `StyleReceiver#setDiagnostics(DiagnosticContainer)`. You are expected to add
your `DiagnosticRegion` objects to the `DiagnosticContainer`. The container will maintain the positions of those added
regions. And also, `DiagnosticRegion` is described by the start index and end index of the diagnostic item, but not by (
line, column) pairs. So you need to compute the index by shadowed Content.
Note that it is - -not - - recommended to add new regions to a container that is already being used by editor though the
class is thread-safe.
See package `io.github.rosemoe.sora.lang.diagnostic`.

> package `io.github.rosemoe.langs.textmate.core` and `io.github.rosemoe.langs.textmate.languageconfiguration` are moved
to `org.eclipse.tm4e.core` and `org.eclipse.tm4e.languageconfiguration`

### More information

- Now editor will show diagnostics with zero length. The editor will show the indicator with a width of the character 'a'.

### Note

- Maven artifact language-textmate 0.12.0 is broken. Use 0.12.0-1 instead.

## **[0.11.4](https://github.com/Rosemoe/sora-editor/releases/tag/0.11.4) (2022-06-21)**

### Bug fix

- Fix invalid position of editor windows
- Fix unexpected cursor update notification to IME when composing text

## **[0.11.3](https://github.com/Rosemoe/sora-editor/releases/tag/0.11.3) (2022-05-22)**

### Fix

- Fix comosing text not removed
- Fix position of editor windows

### Improvements

- Longer duration for some cursor animators
- Magnifier scale factor is set to be Android default

### Note

- Version 0.11.2's maven repo is broken. Use `0.11.3` instead.

## **[0.11.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.11.2) (2022-05-21)**

### New Features

- Multiple cursor animation available (by **[@massivemadness](https://github.com/massivemadness)**)
- Optional sticky selection while selecting text (by **[@massivemadness](https://github.com/massivemadness)**)
- Perform haptic feedback on long press
- Option to create scaled image within editor itself in `Magnifier` **[1]**

### Bug fix

- Fix unexpected cursor animation when composing text changes
- Fix NPE when cursor animation is disabled
- Fix potential unchanged empty text in `SimpleAnalyzeManager`
- Incorrect color for Java's import statements in textmate language (by **[@PranavPurwar](https://github.com/PranavPurwar)**)
- Fix potential NPE while moving selection handles
- Replace `showAtLocation()` with `showAsDropdown()` to avoid window type violations when displaying editor windows

> [1] This should be enabled if the editor is not added to the activity window itself. Otherwise, wrong image will be
created on Android O or above.

## **[0.11.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.11.1) (2022-05-01)**

### Improvements

- Reduce waiting time to for span map lock while drawing
- Reduce acquisitions of locks while drawing
- Invalidate only corrupted cache in hardware-accelerated renderer
- Reuse `Paint` objects while rendering
- Use `Collections#swap` to swap value in List

### New

- Add letter spacing and text scale settings

## **[0.11.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.11.0) (2022-04-30)**

### Bug fix

- Fix `deleteEmptyLineFast` and `deleteMultiSpaces` when using Gboard [#170](https://github.com/Rosemoe/sora-editor/issues/170)
- Fix crash while performing 'Replace all' (by **[@itsaky](https://github.com/itsaky)**)
- Unset receiver field in analyzers when released
- Fix crash/ANR when deleting chars in wordwrap mode [#168](https://github.com/Rosemoe/sora-editor/issues/168)
- Fix memory leak in sample app

### Improvements

- Optimize auto-scrolling when editing at the end of text
- Add method to format code partially
- Migrate code to new Android Gradle Plugin DSL

### Tip

> After upgrading to the new version, you may need to re-compile your project (clean & build) if you get an `AbstractMethodError` because a method with default implementation is added to `Language` class.

## **[0.10.11](https://github.com/Rosemoe/sora-editor/releases/tag/0.10.11) (2022-04-02)**

### Bug fix

- Fix non-threadsafe access to spans in IncrementalAnalyzeManager
- Fix measure cache based position computing

### Improvement

- Add some functions in editor-kt (by **[@dingyi222666](https://github.com/dingyi222666)**)

## **[0.10.10](https://github.com/Rosemoe/sora-editor/releases/tag/0.10.10) (2022-03-26)**

### Improvement

- Improve rendering performance for long lines
- Add option to allow render cache to be save for long lines. enabled by default

## **[0.10.9](https://github.com/Rosemoe/sora-editor/releases/tag/0.10.9) (2022-03-19)**

### Bug fix

- Fix exception occurred when deleting texts
- Fix current code block line color

### Feature

- Add side block line for current block in wordwrap mode

## **[0.10.8](https://github.com/Rosemoe/sora-editor/releases/tag/0.10.8) (2022-03-14)**

- Fix custom adapter not applied to completion window

## **[0.10.7](https://github.com/Rosemoe/sora-editor/releases/tag/0.10.7) (2022-03-11)**

### Bug fix

- Fix wrong text object used in AsyncIncremntalAnalyzeManager#initialize
- Fix occasional failed java highlighting due to concurrently accessed tokenizer
- Fix occasional failed textmate highlighting due to concurrently accessed grammar
- Dismiss editor windows in `release()` to avoid window leak
- Fix overflowed problem indicator region

### Improvement

- Adjust defualt width of problem indicator