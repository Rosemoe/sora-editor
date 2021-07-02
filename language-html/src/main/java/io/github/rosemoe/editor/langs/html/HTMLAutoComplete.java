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
        for (String tag : HTMLLanguage.TAGS)
            if (tag.toLowerCase().startsWith(prefix.toLowerCase()))
                items.add(tagAsCompletion(tag, "HTML Tag"));

        for (String attr : HTMLLanguage.ATTRIBUTES)
            if (attr.toLowerCase().startsWith(prefix.toLowerCase()))
                items.add(attrAsCompletion(attr, "HTML Attribute"));
        return items;
    }

	private CompletionItem attrAsCompletion(String attr, String desc) {
		final CompletionItem item = new CompletionItem(attr, attr.concat("=\"\""), desc);
		item.cursorOffset(item.commit.length() - 1);
		return item;
	}

	private CompletionItem tagAsCompletion(String tag, String desc) {
		final String open = "<".concat(tag).concat(">");
		final String close = "</".concat(tag).concat(">");
		final CompletionItem item = new CompletionItem(tag, desc);
		item.commit = open.concat(close);
		item.cursorOffset(item.commit.length() - close.length());
		return item;
	}
}
