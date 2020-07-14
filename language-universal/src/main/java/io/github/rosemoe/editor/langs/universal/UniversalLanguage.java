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
package io.github.rosemoe.editor.langs.universal;

import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.interfaces.EditorLanguage;
import io.github.rosemoe.editor.langs.IdentifierAutoComplete;
import io.github.rosemoe.editor.langs.internal.MyCharacter;
import io.github.rosemoe.editor.struct.BlockLine;
import io.github.rosemoe.editor.text.LineNumberCalculator;
import io.github.rosemoe.editor.text.TextAnalyzer;
import io.github.rosemoe.editor.widget.EditorColorScheme;

import java.util.Stack;

import static io.github.rosemoe.editor.langs.universal.UniversalTokens.EOF;

/**
 * Universal Language support
 * @author Rose
 */
public class UniversalLanguage implements EditorLanguage, CodeAnalyzer {

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
        LineNumberCalculator helper = new LineNumberCalculator(text);
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
                        colors.addIfNeeded(index, line, column, EditorColorScheme.KEYWORD);
                        break;
                    case IDENTIFIER:
                        identifiers.addIdentifier(text.substring(index, index + tokenizer.getTokenLength()));
                        colors.addIfNeeded(index, line, column, EditorColorScheme.TEXT_NORMAL);
                        break;
                    case LITERAL:
                        colors.addIfNeeded(index, line, column, EditorColorScheme.LITERAL);
                        break;
                    case LINE_COMMENT:
                    case LONG_COMMENT:
                        colors.addIfNeeded(index, line, column, EditorColorScheme.COMMENT);
                        break;
                    case OPERATOR:
                        colors.addIfNeeded(index, line, column, EditorColorScheme.OPERATOR);
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
                        colors.add(index, line, column, EditorColorScheme.ANNOTATION);
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
