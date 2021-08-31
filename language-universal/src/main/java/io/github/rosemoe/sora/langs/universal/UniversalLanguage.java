/*
 *    CodeEditor - the awesome code editor for Android
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
package io.github.rosemoe.sora.langs.universal;

import io.github.rosemoe.sora.interfaces.AutoCompleteProvider;
import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.interfaces.EditorLanguage;
import io.github.rosemoe.sora.interfaces.NewlineHandler;
import io.github.rosemoe.sora.langs.IdentifierAutoComplete;
import io.github.rosemoe.sora.langs.internal.MyCharacter;
import io.github.rosemoe.sora.data.BlockLine;
import io.github.rosemoe.sora.text.LineNumberCalculator;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;
import io.github.rosemoe.sora.widget.EditorColorScheme;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

import java.util.Stack;

import static io.github.rosemoe.sora.langs.universal.UniversalTokens.EOF;

/**
 * Universal Language support
 *
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
        autoComplete.setKeywords(mLanguage.getKeywords());
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
            while ((token = tokenizer2.nextToken()) != EOF) {
                if (token == UniversalTokens.OPERATOR) {
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
    public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        StringBuilder text = content instanceof StringBuilder ? (StringBuilder) content : new StringBuilder(content);
        tokenizer.setInput(text);
        LineNumberCalculator helper = new LineNumberCalculator(text);
        IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
        autoComplete.setKeywords(mLanguage.getKeywords());
        IdentifierAutoComplete.Identifiers identifiers = new IdentifierAutoComplete.Identifiers();
        identifiers.begin();
        int maxSwitch = 0;
        int layer = 0;
        int currSwitch = 0;
        try {
            UniversalTokens token;
            Stack<BlockLine> stack = new Stack<>();
            while ((token = tokenizer.nextToken()) != EOF) {
                int index = tokenizer.getOffset();
                int line = helper.getLine();
                int column = helper.getColumn();
                switch (token) {
                    case KEYWORD:
                        colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        break;
                    case IDENTIFIER:
                        identifiers.addIdentifier(text.substring(index, index + tokenizer.getTokenLength()));
                        colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                        break;
                    case LITERAL:
                        colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                        break;
                    case LINE_COMMENT:
                    case LONG_COMMENT:
                        colors.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                        break;
                    case OPERATOR:
                        colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        if (mLanguage.isSupportBlockLine()) {
                            String op = text.substring(index, index + tokenizer.getTokenLength());
                            if (mLanguage.isBlockStart(op)) {
                                BlockLine blockLine = colors.obtainNewBlock();
                                blockLine.startLine = line;
                                blockLine.startColumn = column;
                                stack.add(blockLine);
                                if (layer == 0) {
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
                                    if (layer == 1) {
                                        if (currSwitch > maxSwitch) {
                                            maxSwitch = currSwitch;
                                        }
                                    }
                                    layer--;
                                }
                            }
                        }
                        break;
                    case WHITESPACE:
                    case NEWLINE:
                        colors.addNormalIfNull();
                        break;
                    case UNKNOWN:
                        colors.addIfNeeded(line, column, EditorColorScheme.ANNOTATION);
                        break;
                }
                helper.update(tokenizer.getTokenLength());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        colors.determine(helper.getLine());
        identifiers.finish();
        colors.setExtra(identifiers);
        tokenizer.setInput(null);
        if (currSwitch > maxSwitch) {
            maxSwitch = currSwitch;
        }
        colors.setSuppressSwitch(maxSwitch + 50);
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }

}
