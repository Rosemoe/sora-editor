package com.rose.editor.langs;

import com.rose.editor.android.ResultItem;
import com.rose.editor.common.TextColorProvider;
import com.rose.editor.common.TextColorProvider.AnalyzeThread.Delegate;
import com.rose.editor.interfaces.CodeAnalyzer;
import com.rose.editor.common.TextColorProvider.TextColors;
import com.rose.editor.interfaces.AutoCompleteProvider;
import com.rose.editor.interfaces.EditorLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * Empty language without any effect
 * @author Rose
 */
public class EmptyLanguage implements EditorLanguage
{

    @Override
    public CharSequence format(CharSequence text)
    {
        return text;
    }


    @Override
    public CodeAnalyzer createAnalyzer()
    {
        return new CodeAnalyzer(){

            @Override
            public void analyze(CharSequence content, TextColors colors, Delegate delegate)
            {
                colors.addNormalIfNull();
            }


        };
    }

    @Override
    public AutoCompleteProvider createAutoComplete()
    {
        return new AutoCompleteProvider(){

            @Override
            public List<ResultItem> getAutoCompleteItems(String prefix, boolean isInCodeBlock, TextColorProvider.TextColors colors, int line)
            {
                return new ArrayList<>();
            }


        };
    }

    @Override
    public boolean isAutoCompleteChar(char ch)
    {
        return false;
    }

    @Override
    public int getIndentAdvance(String content)
    {
        return 0;
    }

    @Override
    public boolean useTab()
    {
        return false;
    }

}

