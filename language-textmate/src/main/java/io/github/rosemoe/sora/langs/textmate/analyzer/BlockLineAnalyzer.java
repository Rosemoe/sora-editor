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
package io.github.rosemoe.sora.langs.textmate.analyzer;

import android.util.Log;

import java.util.Collections;
import java.util.List;

import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.folding.FoldingRegion;
import io.github.rosemoe.sora.langs.textmate.folding.FoldingRegions;
import io.github.rosemoe.sora.langs.textmate.folding.IndentRange;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.textmate.languageconfiguration.ILanguageConfiguration;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.Folding;

public class BlockLineAnalyzer {
    private static final int MAX_FOLDING_REGIONS_FOR_INDENT_LIMIT = 5000;
    private final ILanguageConfiguration configuration;

    public BlockLineAnalyzer(ILanguageConfiguration configuration) {
        this.configuration = configuration;
    }

    public void analyze(TextMateLanguage language, Content model, List<CodeBlock> blocks) {
        long start = System.nanoTime();
        var folding = configuration.getFolding();
        if (folding == null) return;
        try {
            var foldingRegions = IndentRange.computeRanges(model, language.getTabSize(), folding.getOffSide(), folding, MAX_FOLDING_REGIONS_FOR_INDENT_LIMIT);
            for (int i = 0; i < foldingRegions.length(); i++) {
                int startLine = foldingRegions.getStartLineNumber(i);
                int endLine = foldingRegions.getEndLineNumber(i);
                if (startLine != endLine) {
                    CodeBlock codeBlock = new CodeBlock();
                    codeBlock.toBottomOfEndLine = true;
                    codeBlock.startLine = startLine;
                    codeBlock.endLine = endLine;

                    // It's safe here to use raw data because the Content is only held by this thread
                    var length = model.getColumnCount(startLine);
                    var chars = model.getLine(startLine).getRawData();

                    codeBlock.startColumn = IndentRange.computeStartColumn(chars, length, language.getTabSize());
                    codeBlock.endColumn = codeBlock.startColumn;
                    blocks.add(codeBlock);
                }
            }
            Collections.sort(blocks, CodeBlock.COMPARATOR_END);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("tm-folding", "Time cost=" + (System.nanoTime() - start) / 10e6 + " ms");
    }

}
