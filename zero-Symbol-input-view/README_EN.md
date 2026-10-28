# Symbol input

`SymbolInputPanel` is the editor's Compose input UI. It uses `HorizontalPager`, `ScrollableTabRow` and `LazyVerticalGrid`. The former `AdvancedSymbolInputView`, Fragment pager and custom input ViewGroup have been removed.

```kotlin
SymbolInputPanel(
    editor = currentSoraEditor, // null disables input
    onOpenManager = { startActivity(Intent(this, SymbolManagerActivity::class.java)) },
)
```

The panel preserves stored groups, JSON keys, preferences, short/long press actions and macros through `SymbolDataManager` and `SymbolActionExecutor`. It observes preference updates, supports the five indicator styles, configurable columns/rows/font size, uniform page heights, and remembered page/expansion. Large groups scroll within a bounded height. Tap or vertically drag the arrow to expand/collapse.

`SymbolManagerActivity` remains the independent management screen for editing, sorting, importing/exporting groups and configuring actions. Its layouts remain in use. `showDragHandle` and `enableAdvancedActions` are manager options. Sora itself remains an external editing engine.

Compose dependencies and the Kotlin Compose plugin use the repository version catalog.
