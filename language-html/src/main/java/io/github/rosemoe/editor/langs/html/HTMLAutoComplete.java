package io.github.rosemoe.editor.langs.html;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.struct.CompletionItem;
import io.github.rosemoe.editor.text.TextAnalyzeResult;

/**
 * Provides auto complete items for HTML Language
 * This is basic support
 *
 * @author Akash Yadav
 */

public class HTMLAutoComplete implements AutoCompleteProvider {
    @Override
    public List<CompletionItem> getAutoCompleteItems(String prefix, boolean isInCodeBlock, TextAnalyzeResult colors, int line) {
        List<CompletionItem> items = new ArrayList<>();
        for (String key : HTMLLanguage.TAGS)
            if (key.toLowerCase().startsWith(prefix.toLowerCase()))
                items.add(new CompletionItem(key, "HTML Tag"));

        for (String key : HTMLLanguage.TAGS)
            if (key.toLowerCase().startsWith(prefix.toLowerCase()))
                items.add(new CompletionItem(key, "HTML Attribute"));
        return items;
    }
}
