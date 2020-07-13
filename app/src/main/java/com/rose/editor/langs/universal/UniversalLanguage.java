/*
 *   Copyright 2020 Rose2073
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.rose.editor.langs.universal;

import com.rose.editor.android.ColorScheme;
import com.rose.editor.interfaces.AutoCompleteProvider;
import com.rose.editor.interfaces.CodeAnalyzer;
import com.rose.editor.interfaces.EditorLanguage;
import com.rose.editor.langs.IdentifierAutoComplete;
import com.rose.editor.langs.internal.MyCharacter;
import com.rose.editor.struct.BlockLine;
import com.rose.editor.text.TextAnalyzer;
import com.rose.editor.utils.LineNumberHelper;

import java.util.Stack;

import static com.rose.editor.langs.universal.UniversalTokens.EOF;

/**
 * Universal Language support
 * @author Rose
 */
public class UniversalLanguage implements EditorLanguage,CodeAnalyzer {

    private final LanguageDescription mLanguage;
    private final UniversalTokenizer tokenizer;
    private final UniversalTokenizer tokenizer2;

    public UniversalLanguage(LanguageDescription languageDescription) {
        mLanguage = languageDescription;
        tokenizer = new UniversalTokenizer(mLanguage);
        tokenizer2 = new UniversalTokenizer(mLanguage);
    }

    @Override
    public CodeAnalyzer getAnalyzer() {
        return this;
    }

    @Override
    public AutoCompleteProvider getAutoCompleteProvider() {
        IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
        autoComplete.setKeywords(mLanguage.getKeywords(),true);
        return autoComplete;
    }

    @Override
    public boolean isAutoCompleteChar(char ch) {
        return MyCharacter.isJavaIdentifierPart(ch);
    }

    @Override
    public int getIndentAdvance(String content) {
        int advance = 0;
        try {
            tokenizer2.setInput(content);
            UniversalTokens token;
            while((token = tokenizer2.nextToken()) != EOF) {
                if(token == UniversalTokens.OPERATOR) {
                    advance += mLanguage.getOperatorAdvance(tokenizer.getTokenString().toString());
                }
            }
        } catch (Exception e) {
            advance = 0;
        }
        return Math.max(0, advance);
    }

    @Override
    public boolean useTab() {
        return mLanguage.useTab();
    }

    @Override
    public CharSequence format(CharSequence text) {
        return text;
    }

    @Override
    public void analyze(CharSequence content, TextAnalyzer.TextColors colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        StringBuilder text = content instanceof StringBuilder ? (StringBuilder)content : new StringBuilder(content);
        tokenizer.setInput(text);
        LineNumberHelper helper = new LineNumberHelper(text);
        IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
        autoComplete.setKeywords(mLanguage.getKeywords(), false);
        IdentifierAutoComplete.Identifiers identifiers = new IdentifierAutoComplete.Identifiers();
        identifiers.begin();
        int maxSwitch = 0;
        int layer = 0;
        int currSwitch = 0;
        try {
            UniversalTokens token;
            Stack<BlockLine> stack = new Stack<>();
            while((token = tokenizer.nextToken()) != EOF) {
                int index = tokenizer.getOffset();
                int line = helper.getLine();
                int column = helper.getColumn();
                switch (token) {
                    case KEYWORD:
                        colors.addIfNeeded(index, line, column, ColorScheme.KEYWORD);
                        break;
                    case IDENTIFIER:
                        identifiers.addIdentifier(text.substring(index, index + tokenizer.getTokenLength()));
                        colors.addIfNeeded(index, line, column, ColorScheme.TEXT_NORMAL);
                        break;
                    case LITERAL:
                        colors.addIfNeeded(index, line, column, ColorScheme.LITERAL);
                        break;
                    case LINE_COMMENT:
                    case LONG_COMMENT:
                        colors.addIfNeeded(index, line, column, ColorScheme.COMMENT);
                        break;
                    case OPERATOR:
                        colors.addIfNeeded(index, line, column, ColorScheme.OPERATOR);
                        if(mLanguage.isSupportBlockLine()) {
                            String op = text.substring(index, index + tokenizer.getTokenLength());
                            if (mLanguage.isBlockStart(op)) {
                                BlockLine blockLine = colors.obtainNewBlock();
                                blockLine.startLine = line;
                                blockLine.startColumn = column;
                                stack.add(blockLine);
                                if(layer == 0) {
                                    currSwitch = 1;
                                } else {
                                    currSwitch++;
                                }
                                layer++;
                            } else if (mLanguage.isBlockEnd(op)) {
                                if (!stack.isEmpty()) {
                                    BlockLine blockLine = stack.pop();
                                    blockLine.endLine = line;
                                    blockLine.endColumn = column;
                                    colors.addBlockLine(blockLine);
                                    if(layer == 1) {
                                        if(currSwitch > maxSwitch) {
                                            maxSwitch = currSwitch;
                                        }
                                    }
                                    layer --;
                                }
                            }
                        }
                        break;
                    case WHITESPACE:
                    case NEWLINE:
                        break;
                    case UNKNOWN:
                        colors.add(index, line, column, ColorScheme.ANNOTATION);
                        break;
                }
                helper.update(tokenizer.getTokenLength());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        identifiers.finish();
        colors.mExtra = identifiers;
        tokenizer.setInput(null);
        if(currSwitch > maxSwitch) {
            maxSwitch = currSwitch;
        }
        colors.setSuppressSwitch(maxSwitch + 50);
    }

}
