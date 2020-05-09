/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.rose.editor.langs;

import com.rose.editor.simpleclass.ResultItem;
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

