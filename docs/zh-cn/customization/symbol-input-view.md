# 符号输入面板

我们提供了一个便捷的符号输入面板`SymbolInputView`供您使用。

## 使用方式

### 添加 SymbolInputView

``` xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <io.github.rosemoe.sora.widget.CodeEditor
        android:id="@+id/editor"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintBottom_toTopOf="@id/symbol_input_container"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <HorizontalScrollView
        android:id="@+id/symbol_input_container"
        android:layout_width="match_parent"
        android:layout_height="40dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent">

        <io.github.rosemoe.sora.widget.SymbolInputView
            android:id="@+id/symbol_input"
            android:layout_width="wrap_content"
            android:layout_height="40dp" />

    </HorizontalScrollView>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 将符号输入面板和编辑器绑定

``` java
CodeEditor codeEditor = findViewById(R.id.editor);
SymbolInputView symbolInputView = findViewById(R.id.symbol_input);
symbolInputView.bindEditor(codeEditor);
symbolInputView.addSymbols(
    new String[]{"->", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"},
    new String[]{"\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"}
);
```

``` kotlin
val codeEditor = findViewById<CodeEditor>(R.id.editor)
val symbolInputView = findViewById<SymbolInputView>(R.id.symbol_input)
symbolInputView.bindEditor(codeEditor)
symbolInputView.addSymbols(
    arrayOf("->", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"),
    arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/")
)
```