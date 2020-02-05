package com.rose.editor.interfaces;

import com.rose.editor.common.Content;
import com.rose.editor.common.TextColorProvider;

/**
 * Interface for analyzing highlight
 * @author Rose
 */
public interface CodeAnalyzer {

    /**
     * Analyze spans for the given input
     * @see TextColorProvider#analyze(Content)
     * @see TextColorProvider.AnalyzeThread.Delegate#shouldReAnalyze()
     * @param content The input text
     * @param colors Result dest
     * @param delegate Delegate between thread and analyzer
     */
    void analyze(CharSequence content, TextColorProvider.TextColors colors, TextColorProvider.AnalyzeThread.Delegate delegate);

}
