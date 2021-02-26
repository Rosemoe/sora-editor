package io.github.rosemoe.editor.langs.html;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.struct.ResultItem;
import io.github.rosemoe.editor.text.TextAnalyzeResult;

/**
 * Provides auto complete items for HTML Language
 * This is basic support
 *
 * @author Akash Yadav
 */

public class HTMLAutoComplete implements AutoCompleteProvider {
    @Override
    public List<ResultItem> getAutoCompleteItems(String prefix, boolean isInCodeBlock, TextAnalyzeResult colors, int line) {
        List<ResultItem> items = new ArrayList<>();
        for (String key : HTMLLanguage.TAGS)
            if (key.toLowerCase().startsWith(prefix.toLowerCase()))
                items.add(new ResultItem(key, "HTML Tag", ResultItem.TYPE_KEYWORD));

        for (String key : HTMLLanguage.TAGS)
            if (key.toLowerCase().startsWith(prefix.toLowerCase()))
                items.add(new ResultItem(key, "HTML Atrribute", ResultItem.TYPE_KEYWORD));
        return items;
    }
}
