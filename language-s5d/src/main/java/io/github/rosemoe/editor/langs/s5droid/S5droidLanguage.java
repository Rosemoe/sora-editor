/*
 *   Copyright 2020-2021 Rosemoe
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.github.rosemoe.editor.langs.s5droid;

import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.interfaces.EditorLanguage;
import io.github.rosemoe.editor.interfaces.NewlineHandler;
import io.github.rosemoe.editor.widget.SymbolPairMatch;

/**
 * @author Rose
 */
public class S5droidLanguage implements EditorLanguage {

    @Override
    public CharSequence format(CharSequence text) {
        //Actually the text is always the type of StringBuilder
        //subSequence()'s result can be safely cast to String
        S5dFormatter f = new S5dFormatter(text);
        try {
            f.setStyle1(3);
            f.format();
            return f.getResult();
        } catch (RuntimeException e) {
            return text;
        }
    }

    public S5droidAutoComplete completeP;
    public S5droidCodeAnalyzer colorP;

    @Override
    public CodeAnalyzer getAnalyzer() {
        return colorP = new S5droidCodeAnalyzer();
    }

    @Override
    public AutoCompleteProvider getAutoCompleteProvider() {
        return completeP = new S5droidAutoComplete();
    }

    @Override
    public boolean isAutoCompleteChar(char ch) {
        return Character.isJavaIdentifierPart(ch) || ch == '.' || ch == ':';
    }

    @Override
    public int getIndentAdvance(String content) {
        S5dTextTokenizer tk = new S5dTextTokenizer(content);
        Tokens token;
        int v = 0;
        while ((token = tk.directNextToken()) != Tokens.EOF) {
            switch (token) {
                case FORLOOP:
                case WHILELOOP:
                case IF:
                case SWITCH:
                case CASE:
                case EVENT:
                case METHOD:
                case LOOP:
                case ELSE:
                case ELSEIF:
                case CATCH:
                    v++;
                    break;
                case END:
                    v--;
                    break;
            }
        }
        return v * 3;
    }

    @Override
    public boolean useTab() {
        return false;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }

}

