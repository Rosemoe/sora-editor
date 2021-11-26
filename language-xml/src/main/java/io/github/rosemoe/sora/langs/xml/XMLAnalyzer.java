/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
 *    Copyright (C) 2020-2021  Rosemoe
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
package io.github.rosemoe.sora.langs.xml;

import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.langs.xml.analyzer.BasicSyntaxPullAnalyzer;
import io.github.rosemoe.sora.langs.xml.analyzer.HighLightAnalyzer;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;

public class XMLAnalyzer implements CodeAnalyzer {
    private final HighLightAnalyzer highLightAnalyzer = new HighLightAnalyzer();
    //private final BasicSyntaxSaxAnalyzer basicSyntaxAnalyzer = new BasicSyntaxSaxAnalyzer();
    private final BasicSyntaxPullAnalyzer basicSyntaxAnalyzer = new BasicSyntaxPullAnalyzer();
    private boolean syntaxCheckEnable;

    public boolean isSyntaxCheckEnable() {
        return syntaxCheckEnable;
    }

    public void setSyntaxCheckEnable(boolean syntaxCheckEnable) {
        this.syntaxCheckEnable = syntaxCheckEnable;
    }

    @Override
    public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        if (content.toString().isEmpty()) {
            return;
        }
        //high light analyze first to get lastLine.
        highLightAnalyzer.analyze(content, colors, delegate);

        if (syntaxCheckEnable)
            basicSyntaxAnalyzer.analyze(content, colors, delegate);

        colors.determine(highLightAnalyzer.getLastLine());
    }

}
