# zero-Symbol-input-view

`zero-Symbol-input-view` is an Android symbol-input module for code and text editing apps.
It provides a production-ready symbol drawer + management system built around **Sora CodeEditor**,
with support for grouped paging, gesture-controlled drawer states, configurable indicators,
and import/export of symbol configuration JSON.

![preview](zero-Symbol-input-view/ResizedImage_2026-04-27_12-59-55_6518.png)

---
[简体中文](README_CN.md)
---
## 1) Purpose

This module is designed for scenarios where users frequently input symbols, templates,
and editing actions (e.g., cursor movement, line actions, clipboard actions) while writing code.

Compared with a plain soft keyboard symbol row, this module offers:

- multi-group symbol organization
- tap/long-press dual behavior per symbol
- stable drawer interaction (collapse/expand + drag)
- visual group indicators (tab/line/dot/block/hidden)
- full symbol data lifecycle management

---

## 2) Core Capabilities

### 2.1 Runtime Input Drawer (`AdvancedSymbolInputView`)

- Grouped symbol pages with horizontal switching
- Adjustable collapsed/expanded layout model
- Gesture-based expand/collapse behavior
- Per-symbol click and long-click action dispatch
- Remembered expanded state and remembered last page (optional)
- Multiple indicator styles:
  - Standard tab indicator
  - Minimal compact dots (bottom)
  - Hidden indicator
  - Top line indicator
  - Block indicator (bottom)

### 2.2 Symbol Management (`SymbolManagerActivity`)

- Group CRUD (add/edit/delete/reorder)
- Symbol CRUD (add/edit/copy/move/delete)
- Batch operations
- Import/export via clipboard and file
- Settings panel for rows, per-row count, indicator style, behavior toggles

### 2.3 Action Execution Model

Each symbol can map to:

- short-press action
- optional long-press action
- action payload text (insert content / custom text)

Action execution is delegated through `SymbolActionExecutor` to the bound editor instance.

### 2.4 Same Capabilities as MT Manager

- Supports direct import of JSON files from the MT Manager symbol input toolbar

- Similar to importing, JSON files exported from zero-Symbol-input-view can also be directly used in the MT Manager symbol input toolbar

- Mostly the same design as MT Manager.

---

## 3) Architecture & Design

### 3.1 High-level Architecture

- **View layer**
  - `AdvancedSymbolInputView`
  - `GroupIndicatorBar`
  - compact bottom indicator view
  - `SymbolPageGridView`
- **Data/model layer**
  - `SymbolGroup`, `SymbolItem`, `SymbolUiSettings`
- **Persistence layer**
  - `SymbolDataManager` (SharedPreferences + JSON)
- **Behavior layer**
  - `SymbolActionExecutor` (editor action routing)

### 3.2 Design Principles

1. **Data-driven UI**: group/symbol rendering is generated from persisted JSON data.
2. **Interaction consistency**: drawer height uses unified row-height formulas.
3. **Extensibility**: indicator styles and symbol actions are decoupled from core paging.
4. **Compatibility**: keeps legacy integration points (e.g., `followSystemIme`) while using the new internal drawer model.

### 3.3 Indicator Design

Indicator rendering is style-based:

- tab-like styles render in the top indicator bar
- compact styles render at the drawer bottom
- hidden style suppresses indicator UI

This avoids forcing a single indicator paradigm for all UX requirements.

---

## 4) Integration

### 4.1 Gradle

`settings.gradle.kts`

```kotlin
include(":zero-Symbol-input-view")
```

`build.gradle.kts`

```kotlin
dependencies {
    implementation(project(":zero-Symbol-input-view"))
}
```

### 4.2 Layout

```xml
<android.zero.studio.widget.editor.symbolinput.AdvancedSymbolInputView
    android:id="@+id/symbol_input_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### 4.3 Activity Usage

```kotlin
val symbolInputView = findViewById<android.zero.studio.widget.editor.symbolinput.AdvancedSymbolInputView>(R.id.symbol_input_view)
val editor = findViewById<io.github.rosemoe.sora.widget.CodeEditor>(R.id.editor)

symbolInputView.bindEditor(editor)
symbolInputView.onOpenManagerListener = {
    startActivity(Intent(this, android.zero.studio.widget.editor.symbolinput.SymbolManagerActivity::class.java))
}

override fun onResume() {
    super.onResume()
    symbolInputView.onHostResume()
}

// reload after settings/data changes
symbolInputView.refreshData()
```

---

## 5) Data Interoperability

The module supports JSON import/export and is compatible with symbol-config workflows
commonly used by MT-style editor toolbars.

Recommended practice:

- version and back up symbol JSON files
- keep shared team presets under VCS
- validate imported data before production rollout

---

## 6) Project Structure (Key Files)

```text
zero-Symbol-input-view/
└── src/main/
    ├── kotlin/android/zero/studio/widget/editor/symbolinput/
    │   ├── AdvancedSymbolInputView.kt
    │   ├── SymbolManagerActivity.kt
    │   ├── SymbolData.kt
    │   ├── SymbolDataManager.kt
    │   └── SymbolActionExecutor.kt
    └── res/
```

---

## 7) Compatibility Notes

- `AdvancedSymbolInputView.followSystemIme` is retained for legacy integration compatibility.
- If you migrate from an older bottom-sheet based implementation, call `refreshData()` after migration.

