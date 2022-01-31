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
        Folding folding = configuration.getFolding();
        if (folding == null) return;
        FoldingRegions foldingRegions = null;
        try {
            foldingRegions = IndentRange.computeRanges(model, language.getTabSize(), folding.getOffSide(), folding, MAX_FOLDING_REGIONS_FOR_INDENT_LIMIT);
            for (int i = 0; i < foldingRegions.length(); i++) {
                FoldingRegion foldingRegion = foldingRegions.toRegion(i);
                int startLine = foldingRegion.getStartLineNumber();
                int endLine = foldingRegion.getEndLineNumber();
                if (startLine != endLine) {
                    CodeBlock codeBlock = new CodeBlock();
                    codeBlock.toBottomOfEndLine = true;
                    codeBlock.startLine = startLine;
                    codeBlock.endLine = endLine;
                    String line = model.getLineString(startLine);
                    // TODO: 2021/10/17 这2个column不知道怎么取值。。
                    codeBlock.startColumn = IndentRange.computeStartColumn(line, language.getTabSize());
//                    line= model.getLineString(endLine);
//                    int endColumn=IndentRange.computeStartColumn(line,language.getTabSize());
                    codeBlock.endColumn = codeBlock.startColumn;

                    blocks.add(codeBlock);
                }
            }

            Collections.sort(blocks, (o1, o2) -> o1.endLine - o2.endLine);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
