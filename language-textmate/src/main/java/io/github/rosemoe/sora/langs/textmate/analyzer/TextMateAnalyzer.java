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
import java.util.List;

import io.github.rosemoe.sora.lang.analysis.UIThreadIncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.textmate.core.grammar.IGrammar;
import io.github.rosemoe.sora.textmate.core.grammar.ITokenizeLineResult2;
import io.github.rosemoe.sora.textmate.core.grammar.StackElement;
import io.github.rosemoe.sora.textmate.core.internal.grammar.StackElementMetadata;
import io.github.rosemoe.sora.textmate.core.registry.Registry;
import io.github.rosemoe.sora.textmate.core.theme.FontStyle;
import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;
import io.github.rosemoe.sora.textmate.core.theme.Theme;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.LanguageConfigurator;
import io.github.rosemoe.sora.util.ArrayList;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class TextMateAnalyzer extends UIThreadIncrementalAnalyzeManager<StackElement, Span> {

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
    protected StackElement getInitialState() {
        return null;
    }

    @Override
    protected boolean stateEquals(StackElement state, StackElement another) {
        if (state == null && another == null) {
            return true;
        }
        if (state != null && another != null) {
            return state.equals(another);
        }
        return false;
    }

    @Override
    protected LineTokenizeResult<StackElement, Span> tokenizeLine(CharSequence lineC, StackElement state) {
        String line = lineC.toString();
        var tokens = new ArrayList<Span>();
        ITokenizeLineResult2 lineTokens = grammar.tokenizeLine2(line, state);
        int tokensLength = lineTokens.getTokens().length / 2;
        for (int i = 0; i < tokensLength; i++) {
            int startIndex = lineTokens.getTokens()[2 * i];
            if (i == 0 && startIndex != 0) {
                tokens.add(Span.obtain(0, EditorColorScheme.TEXT_NORMAL));
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

            tokens.add(span);
        }
        return new LineTokenizeResult<>(lineTokens.getRuleStack(), tokens);
    }

    @Override
    protected List<Span> generateSpansForLine(LineTokenizeResult<StackElement, Span> tokens) {
        var res = new ArrayList<Span>();
        for (var span : tokens.tokens) {
            res.add(span.copy());
        }
        return res;
    }

    public void updateTheme(IRawTheme theme) {
        registry.setTheme(theme);
        this.theme = Theme.createFromRawTheme(theme);
    }
}
