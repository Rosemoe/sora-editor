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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.StringReader;

import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.langs.EmptyLanguage;


public class XMLLanguage extends EmptyLanguage {
    private final XMLAnalyzer analyzer = new XMLAnalyzer();
    private int tabSize = 4;

    @Override
    public CodeAnalyzer getAnalyzer() {
        return analyzer;
    }


    @Override
    public int getIndentAdvance(String content) {
        try {
            XMLLexer lexer = new XMLLexer(CharStreams.fromReader(new StringReader(content)));
            Token token ;
            int advance = 0;
            while (((token = lexer.nextToken()) != null && token.getType() != token.EOF)) {
                switch (token.getType()) {
                    case XMLLexer.OPEN:
                        advance++;
                        break;
                    case XMLLexer.SLASH:
                    case XMLLexer.SLASH_CLOSE:
                        advance--;
                        break;
                }
            }
            advance = Math.max(0, advance);
            return advance * getTabSize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean useTab() {
        return true;
    }

    public int getTabSize() {
        return useTab() ? tabSize : 1;
    }

    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    public boolean isSyntaxCheckEnable() {
        return analyzer.isSyntaxCheckEnable();
    }

    public void setSyntaxCheckEnable(boolean syntaxCheckEnable) {
        analyzer.setSyntaxCheckEnable(syntaxCheckEnable);
    }
}
