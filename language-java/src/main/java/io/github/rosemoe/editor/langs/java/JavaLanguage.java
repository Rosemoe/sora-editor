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
package io.github.rosemoe.editor.langs.java;

import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.interfaces.EditorLanguage;
import io.github.rosemoe.editor.interfaces.NewlineHandler;
import io.github.rosemoe.editor.langs.IdentifierAutoComplete;
import io.github.rosemoe.editor.langs.internal.MyCharacter;
import io.github.rosemoe.editor.text.TextUtils;
import io.github.rosemoe.editor.widget.SymbolPairMatch;

/**
 * Java language is much complex.
 * This is a basic support
 *
 * @author Rose
 */
public class JavaLanguage implements EditorLanguage {

    @Override
    public CodeAnalyzer getAnalyzer() {
        return new JavaCodeAnalyzer();
    }

    @Override
    public AutoCompleteProvider getAutoCompleteProvider() {
        IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
        autoComplete.setKeywords(JavaTextTokenizer.sKeywords);
        return autoComplete;
    }

    @Override
    public boolean isAutoCompleteChar(char ch) {
        return MyCharacter.isJavaIdentifierPart(ch);
    }

    @Override
    public int getIndentAdvance(String content) {
        JavaTextTokenizer t = new JavaTextTokenizer(content);
        Tokens token;
        int advance = 0;
        while ((token = t.directNextToken()) != Tokens.EOF) {
            switch (token) {
                case LBRACE:
                    advance++;
                    break;
                /*case RBRACE:
                    advance--;
                    break;*/
            }
        }
        advance = Math.max(0, advance);
        return advance * 4;
    }

    @Override
    public boolean useTab() {
        return true;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    @Override
    public CharSequence format(CharSequence text) {
        return text;
    }

    private NewlineHandler[] newlineHandlers = new NewlineHandler[]{new BraceHandler()};

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return newlineHandlers;
    }

    class BraceHandler implements NewlineHandler {

        @Override
        public boolean matchesRequirement(String beforeText, String afterText) {
            return beforeText.endsWith("{") && afterText.startsWith("}");
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
