/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.langs.html;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.data.CompletionItem;
import io.github.rosemoe.sora.interfaces.AutoCompleteProvider;
import io.github.rosemoe.sora.text.TextAnalyzeResult;

/**
 * Provides auto complete items for HTML Language
 * This is basic support
 *
 * @author Akash Yadav
 */

public class HTMLAutoComplete implements AutoCompleteProvider {
    @Override
    public List<CompletionItem> getAutoCompleteItems(String prefix, TextAnalyzeResult analyzeResult, int line, int column) {
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
