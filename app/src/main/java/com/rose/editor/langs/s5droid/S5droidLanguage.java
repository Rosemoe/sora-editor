package com.rose.editor.langs.s5droid;

import com.rose.editor.interfaces.CodeAnalyzer;
import com.rose.editor.interfaces.AutoCompleteProvider;
import com.rose.editor.interfaces.EditorLanguage;

/**
 * @author Rose
 */
public class S5droidLanguage implements EditorLanguage
{

    @Override
    public CharSequence format(CharSequence text)
    {
        //Actually the text is always the type of StringBuilder
        //subSequence()'s result can be safely cast to String
        S5dFormatter f = new S5dFormatter(text);
        try{
            f.setStyle1(3);
            f.format();
            return f.getResult();
        }
        catch(RuntimeException e) {
            return text;
        }
    }

    public S5droidAutoComplete completeP;
    public S5droidCodeAnalyzer colorP;

    @Override
    public CodeAnalyzer createAnalyzer()
    {
        return colorP = new S5droidCodeAnalyzer();
    }

    @Override
    public AutoCompleteProvider createAutoComplete()
    {
        return completeP = new S5droidAutoComplete();
    }

    @Override
    public boolean isAutoCompleteChar(char ch)
    {
        return Character.isJavaIdentifierPart(ch) || ch == '.' || ch == ':';
    }

    @Override
    public int getIndentAdvance(String content)
    {
        S5dTextTokenizer tk = new S5dTextTokenizer(content);
        Tokens token;
        int v = 0;
        while((token = tk.directNextToken()) != Tokens.EOF) {
            switch(token) {
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
    public boolean useTab()
    {
        return false;
    }

}

