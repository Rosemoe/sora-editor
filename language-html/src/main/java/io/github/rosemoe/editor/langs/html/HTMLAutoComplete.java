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
