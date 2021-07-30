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
package io.github.rosemoe.editor.langs;

import io.github.rosemoe.editor.text.TextAnalyzeResult;
import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.struct.CompletionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Identifier auto-completion
 * You can use it to provide identifiers
 * <strong>Note:</strong> To use this, you must use {@link Identifiers} as {@link TextAnalyzeResult#mExtra}
 */
public class IdentifierAutoComplete implements AutoCompleteProvider {

    private String[] mKeywords;
    private boolean mKeywordsAreLowCase;

    public IdentifierAutoComplete() {

    }

    public IdentifierAutoComplete(String[] keywords) {
        setKeywords(keywords);
    }

    public void setKeywords(String[] keywords) {
        mKeywords = keywords;
        mKeywordsAreLowCase = true;
    }

    public String[] getKeywords() {
        return mKeywords;
    }

    public static class Identifiers {

        private final List<String> identifiers = new ArrayList<>();
        private HashMap<String, Object> cache;
        private final static Object SIGN = new Object();

        public void addIdentifier(String identifier) {
            if (cache == null) {
                throw new IllegalStateException("begin() has not been called");
            }
            if (cache.put(identifier, SIGN) == SIGN) {
                return;
            }
            identifiers.add(identifier);
        }

        public void begin() {
            cache = new HashMap<>();
        }

        public void finish() {
            cache.clear();
            cache = null;
        }

        public List<String> getIdentifiers() {
            return identifiers;
        }

    }

    @Override
    public List<CompletionItem> getAutoCompleteItems(String prefix, boolean isInCodeBlock, TextAnalyzeResult colors, int line) {
        List<CompletionItem> keywords = new ArrayList<>();
        final String[] keywordArray = mKeywords;
        final boolean lowCase = mKeywordsAreLowCase;
        String match = prefix.toLowerCase();
        if (keywordArray != null) {
            if (lowCase) {
                for (String kw : keywordArray) {
                    if (kw.startsWith(match)) {
                        keywords.add(new CompletionItem(kw, "Keyword"));
                    }
                }
            } else {
                for (String kw : keywordArray) {
                    if (kw.toLowerCase().startsWith(match)) {
                        keywords.add(new CompletionItem(kw, "Keyword"));
                    }
                }
            }
        }
        Collections.sort(keywords, CompletionItem.COMPARATOR_BY_NAME);
        Object extra = colors.mExtra;
        Identifiers userIdentifiers = (extra instanceof Identifiers) ? (Identifiers) extra : null;
        if (userIdentifiers != null) {
            List<CompletionItem> words = new ArrayList<>();
            for (String word : userIdentifiers.getIdentifiers()) {
                if (word.toLowerCase().startsWith(match)) {
                    words.add(new CompletionItem(word, "Identifier"));
                }
            }
            Collections.sort(words, CompletionItem.COMPARATOR_BY_NAME);
            keywords.addAll(words);
        }
        return keywords;
    }


}
