package com.rose.editor.interfaces;

import com.rose.editor.common.TextColorProvider;

public interface CodeAnalyzer {

    void analyze(CharSequence content, TextColorProvider.TextColors colors, TextColorProvider.AnalyzeThread.Delegate delegate);

}
