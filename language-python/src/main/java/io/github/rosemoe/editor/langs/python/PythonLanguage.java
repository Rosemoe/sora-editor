/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.langs.python;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.interfaces.EditorLanguage;
import io.github.rosemoe.editor.interfaces.NewlineHandler;
import io.github.rosemoe.editor.langs.IdentifierAutoComplete;
import io.github.rosemoe.editor.langs.internal.MyCharacter;
import io.github.rosemoe.editor.struct.CompletionItem;
import io.github.rosemoe.editor.text.TextUtils;
import io.github.rosemoe.editor.widget.SymbolPairMatch;

public class PythonLanguage implements EditorLanguage {
    private final static String[] keywords = {
            "and", "as", "assert", "break", "class", "continue", "def",
            "del", "elif", "else", "except", "exec", "finally", "for",
            "from", "global", "if", "import", "in", "is", "lambda",
            "not", "or", "pass", "print", "raise", "return", "try",
            "while", "with", "yield"
    };

    @Override
    public CodeAnalyzer getAnalyzer() {
        return new PythonCodeAnalyzer();
    }

    @Override
    public AutoCompleteProvider getAutoCompleteProvider() {
        return new IdentifierAutoComplete(keywords);
    }

    @Override
    public boolean isAutoCompleteChar(char ch) {
        return MyCharacter.isJavaIdentifierPart(ch);
    }

    @Override
    public int getIndentAdvance(String content) {
        Token token = null;
        int advance = 0;
        boolean openBlock = false;
        try {
            PythonLexer lexer = new PythonLexer(CharStreams.fromReader(new StringReader(content)));
            while (((token = lexer.nextToken()) != null && token.getType() != token.EOF)) {
                switch (token.getType()) {
                    case PythonLexer.CLASS:
                    case PythonLexer.DEF:
                    case PythonLexer.IF:
                    case PythonLexer.ELIF:
                    case PythonLexer.FOR:
                    case PythonLexer.WHILE:
                    case PythonLexer.TRY:
                    case PythonLexer.EXCEPT:
                        openBlock = !openBlock;
                        break;
                    case PythonLexer.COLON:
                        advance++;
                        break;
                    case PythonLexer.CONTINUE:
                    case PythonLexer.BREAK:
                        openBlock = !openBlock;
                        advance--;
                        break;
                }
            }
            advance = Math.max(0, advance);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return openBlock ? advance * 4 : 0;
    }

    @Override
    public boolean useTab() {
        return true;
    }

    @Override
    public CharSequence format(CharSequence text) {
        return text;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return null;
    }

    private NewlineHandler[] newlineHandlers = new NewlineHandler[]{new ColonHandler()};

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return newlineHandlers;
    }

    class ColonHandler implements NewlineHandler {

        @Override
        public boolean matchesRequirement(String beforeText, String afterText) {
            return beforeText.endsWith(":");
        }

        @Override
        public HandleResult handleNewline(String beforeText, String afterText, int tabSize) {
            int count = TextUtils.countLeadingSpaceCount(beforeText, tabSize);
            int advanceBefore = getIndentAdvance(beforeText);
            int advanceAfter = getIndentAdvance(afterText);
            String text;
            StringBuilder sb = new StringBuilder("\n")
                    .append(TextUtils.createIndent(count + advanceBefore, tabSize, useTab()))
                    .append('\n')
                    .append(text = TextUtils.createIndent(count + advanceAfter, tabSize, useTab()));
            int shiftLeft = text.length() + 1;
            return new HandleResult(sb, shiftLeft);
        }
    }
}
