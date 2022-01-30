/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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
package io.github.rosemoe.sora.langs.textmate.analyzer;

import android.graphics.Color;

import java.io.InputStream;
import java.io.Reader;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;
import io.github.rosemoe.sora.text.TextStyle;
import io.github.rosemoe.sora.textmate.core.grammar.IGrammar;
import io.github.rosemoe.sora.textmate.core.grammar.ITokenizeLineResult2;
import io.github.rosemoe.sora.textmate.core.grammar.StackElement;
import io.github.rosemoe.sora.textmate.core.internal.grammar.StackElementMetadata;
import io.github.rosemoe.sora.textmate.core.registry.Registry;
import io.github.rosemoe.sora.textmate.core.theme.FontStyle;
import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;
import io.github.rosemoe.sora.textmate.core.theme.Theme;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.LanguageConfigurator;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class TextMateAnalyzer implements CodeAnalyzer {

    private final Registry registry = new Registry();
    private final IGrammar grammar;
    private Theme theme;
    private BlockLineAnalyzer blockLineAnalyzer;
    private final TextMateLanguage language;

    public TextMateAnalyzer(TextMateLanguage language, String grammarName, InputStream grammarIns, Reader languageConfiguration, IRawTheme theme) throws Exception {
        registry.setTheme(theme);
        this.language = language;
        this.theme = Theme.createFromRawTheme(theme);
        this.grammar = registry.loadGrammarFromPathSync(grammarName, grammarIns);
        if (languageConfiguration != null) {
            LanguageConfigurator languageConfigurator = new LanguageConfigurator(languageConfiguration);
            blockLineAnalyzer = new BlockLineAnalyzer(languageConfigurator.getLanguageConfiguration());
        }
    }

    @Override
    public void analyze(CharSequence content, TextAnalyzeResult result, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        Content model = new Content(content);

        try {
            boolean first = true;
            StackElement ruleStack = null;
            for (int lineCount = 0; lineCount < model.getLineCount() && delegate.shouldAnalyze(); lineCount++) {
                String line = model.getLineString(lineCount) + "\n";
                if (first) {
                    result.addNormalIfNull();
                    first = false;
                }
                ITokenizeLineResult2 lineTokens = grammar.tokenizeLine2(line, ruleStack);
                int tokensLength = lineTokens.getTokens().length / 2;
                for (int i = 0; i < tokensLength; i++) {
                    int startIndex = lineTokens.getTokens()[2 * i];
                    int nextStartIndex = i + 1 < tokensLength ? lineTokens.getTokens()[2 * i + 2] : line.length();
                    String tokenText = line.substring(startIndex, nextStartIndex);
                    if (tokenText.trim().isEmpty()) {
                        continue;
                    }
                    int metadata = lineTokens.getTokens()[2 * i + 1];
                    int foreground = StackElementMetadata.getForeground(metadata);
                    int fontStyle = StackElementMetadata.getFontStyle(metadata);
                    Span span = Span.obtain(startIndex, TextStyle.makeStyle(foreground + 255, 0, (fontStyle & FontStyle.Bold) != 0, (fontStyle & FontStyle.Italic) != 0, false));

                    if ((fontStyle & FontStyle.Underline) != 0) {
                        String color = theme.getColor(foreground);
                        if (color != null) {
                            span.underlineColor = Color.parseColor(color);
                        }
                    }

                    result.add(lineCount, span);
                    //Erase the extra style at the end of the span
                    result.addIfNeeded(lineCount, nextStartIndex, EditorColorScheme.TEXT_NORMAL);
                }
                ruleStack = lineTokens.getRuleStack();
            }

            if (blockLineAnalyzer != null) {
                blockLineAnalyzer.analyze(language, model, result);
            }

            result.determine(model.getLineCount() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTheme(IRawTheme theme) {
        registry.setTheme(theme);
        this.theme = Theme.createFromRawTheme(theme);
    }
}
