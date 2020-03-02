package com.rose.editor.langs.java;

import com.rose.editor.interfaces.AutoCompleteProvider;
import com.rose.editor.interfaces.CodeAnalyzer;
import com.rose.editor.interfaces.EditorLanguage;
import com.rose.editor.langs.IdentifierAutoComplete;
import com.rose.editor.langs.internal.MyCharacter;

/**
 * Java language is much complex.
 * This is a basic support
 * @author Rose
 */
public class JavaLanguage implements EditorLanguage {

    @Override
    public CodeAnalyzer createAnalyzer() {
        return new JavaCodeAnalyzer();
    }

    @Override
    public AutoCompleteProvider createAutoComplete() {
        IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
        autoComplete.setKeywords(JavaTextTokenizer.sKeywords,true);
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
                case RBRACE:
                    advance--;
                    break;
            }
        }
        advance = Math.max(0,advance);
        return advance * 4;
    }

    @Override
    public boolean useTab() {
        return true;
    }

    @Override
    public CharSequence format(CharSequence text) {
        return text;
    }
}
