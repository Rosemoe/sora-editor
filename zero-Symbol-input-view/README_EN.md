# zero-Symbol-input-view Documentation (English)

## 1. Overview

`zero-Symbol-input-view` is a symbol-input enhancement module for code/text editing scenarios.
It is designed around Sora `CodeEditor` and provides:

- grouped symbol panel with paging
- gesture-driven expandable/collapsible symbol drawer
- configurable group indicator styles (Standard / Minimal Dot-Capsule / Hidden / Top Line / Block)
- symbol manager screen for CRUD/import/export/reorder/batch actions
- macro-based insertion (`$$`, `$S`, `$E`, `$T`)

---

## 2. Key Features

### 2.1 AdvancedSymbolInputView

- bind directly to `CodeEditor`
- drag gesture for drawer expansion/collapse
- remember expanded state + remember last symbol page
- optional uniform group height to reduce page-jump visual jitter
- switchable group indicator style
- short-press and long-press actions per symbol item

### 2.2 Data & Settings

- persisted via SharedPreferences
- safe fallback defaults injected on first run (`SymbolDefaults`)
- centralized UI settings read/write through `SymbolDataManager`

### 2.3 Symbol Manager

- add/delete/rename groups
- add/edit/copy/move/delete symbols
- batch operations
- import/export from clipboard and file
- Material-style dialogs and input controls

### 2.4 Macro Insertion

Supported macros:

- `$$`: literal `$`
- `$S`: selection start marker after insertion
- `$E`: selection end marker after insertion
- `$T`: currently selected text

Example: `{$S$T$E}`

---

## 3. Strengths

1. Faster editing workflow on mobile.
2. Highly configurable grouping/action model.
3. Better continuity with persisted UI state.
4. Extensible template insertion via macros.
5. Easy integration as a view + manager activity module.

---

## 4. Integration Guide

## 4.1 Add module dependency

`settings.gradle(.kts)`:

```kotlin
include(":zero-Symbol-input-view")
```

`app/build.gradle(.kts)`:

```kotlin
dependencies {
    implementation(project(":zero-Symbol-input-view"))
}
```

### 4.2 Add view in layout

```xml
<android.zero.studio.widget.editor.symbolinput.AdvancedSymbolInputView
    android:id="@+id/symbol_input_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
```

### 4.3 Bind editor and optional manager entry

```kotlin
val editor = findViewById<io.github.rosemoe.sora.widget.CodeEditor>(R.id.editor)
val symbolInputView = findViewById<android.zero.studio.widget.editor.symbolinput.AdvancedSymbolInputView>(R.id.symbol_input_view)

symbolInputView.bindEditor(editor)

symbolInputView.onOpenManagerListener = {
    startActivity(Intent(this, android.zero.studio.widget.editor.symbolinput.SymbolManagerActivity::class.java))
}
```

### 4.4 Lifecycle recommendation

```kotlin
override fun onResume() {
    super.onResume()
    symbolInputView.onHostResume()
}
```

Manual refresh when data/settings changed externally:

```kotlin
symbolInputView.refreshData()
```

---

## 5. IME Follow Property (`followSystemIme`)

`AdvancedSymbolInputView` still exposes `followSystemIme: Boolean` for legacy compatibility.

- Current status: available as a compatibility field.
- Behavior note: currently it is a no-op compatibility placeholder and does not actively drive layout behavior by itself.

Use it only if your host app depends on older API shape compatibility.

---

## 6. Recommended Best Practices

- Keep drawer collapsed by default for better initial editing focus.
- Enable remember-last-page for continuous symbol workflows.
- Use `$S/$E/$T` in frequent templates to reduce manual reselection.
- For many groups, use Minimal (dot-capsule) or Hidden indicator style.
- Export symbol configs regularly for team/device consistency.

---

## 7. FAQ

### Q1. Does Hidden indicator disable page switching?
No. It only hides visual indicators; paging and tab switching still work.

### Q2. Why are default groups present on first launch?
The module injects and caches fallback defaults to guarantee availability.

### Q3. Can macros be combined?
Yes. `$$`, `$S`, `$E`, `$T` can be mixed in one insertion template.
