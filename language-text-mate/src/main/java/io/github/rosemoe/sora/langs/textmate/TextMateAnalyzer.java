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
package io.github.rosemoe.sora.langs.textmate;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

import io.github.rosemoe.sora.data.Span;
import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.interfaces.ExternalRenderer;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;
import io.github.rosemoe.sora.textmate.core.grammar.IGrammar;
import io.github.rosemoe.sora.textmate.core.grammar.IToken;
import io.github.rosemoe.sora.textmate.core.grammar.ITokenizeLineResult;
import io.github.rosemoe.sora.textmate.core.grammar.StackElement;
import io.github.rosemoe.sora.textmate.core.registry.Registry;
import io.github.rosemoe.sora.textmate.core.theme.FontStyle;
import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;
import io.github.rosemoe.sora.textmate.core.theme.Theme;
import io.github.rosemoe.sora.textmate.core.theme.ThemeTrieElementRule;
import io.github.rosemoe.sora.widget.EditorColorScheme;

public class TextMateAnalyzer implements CodeAnalyzer {

    private Registry registry = new Registry();
    private IGrammar grammar;
    private Theme theme;

    public TextMateAnalyzer(String grammarName, InputStream grammarIns, IRawTheme theme) throws Exception {
        registry.setTheme(theme);
        this.theme = Theme.createFromRawTheme(theme);
        this.grammar = registry.loadGrammarFromPathSync(grammarName, grammarIns);

    }

    @Override
    public void analyze(CharSequence content, TextAnalyzeResult result, TextAnalyzer.AnalyzeThread.Delegate delegate) {

        try {
            boolean first = true;
            BufferedReader bufferedReader = new BufferedReader(new StringReader(content.toString()));
            String line;
            int lineCount = 0;
            StackElement ruleStack = null;
            while ((line = bufferedReader.readLine()) != null && delegate.shouldAnalyze()) {

                if (first) {
                    result.addNormalIfNull();
                    first = false;
                }
                ITokenizeLineResult lineTokens = grammar.tokenizeLine(line, ruleStack);
                ruleStack = lineTokens.getRuleStack();
                for (int i = 0; i < lineTokens.getTokens().length; i++) {
                    IToken token = lineTokens.getTokens()[i];
                    List<ThemeTrieElementRule> list = theme.match(token.getScopes().get(token.getScopes().size() - 1));
                    ThemeTrieElementRule rule=list.get(0);

                    Span span=Span.obtain(token.getStartIndex(), rule.foreground + 255);

                    if(rule.fontStyle!=FontStyle.NotSet){
                        if((rule.fontStyle&FontStyle.Underline)== FontStyle.Underline){
                            String color=theme.getColor(rule.foreground);
                            if(color!=null){
                                span.underlineColor=Color.parseColor(color);
                            }

                        }
                        //Paint object should not be modified. This is just for test.
                        span.renderer=new ExternalRenderer() {
                            @Override
                            public boolean requirePreDraw() {
                                return true;
                            }

                            @Override
                            public boolean requirePostDraw() {
                                return true;
                            }

                            final boolean bold=(rule.fontStyle&FontStyle.Bold)== FontStyle.Bold;
                            final boolean italic=(rule.fontStyle&FontStyle.Italic)== FontStyle.Italic;
                            @Override
                            public void draw(Canvas canvas, Paint paint, EditorColorScheme colorScheme, boolean preOrPost) {
                                if(bold){
                                    paint.setFakeBoldText(preOrPost);
                                }

                                if(italic){
                                    paint.setTextSkewX(preOrPost?-0.2f:0);
                                }
                            }
                        };
                    }




                    result.add(lineCount,span);
                }

                lineCount++;
            }

            result.determine(lineCount);
            bufferedReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateTheme(IRawTheme theme) {
        registry.setTheme(theme);
        this.theme = Theme.createFromRawTheme(theme);
        ;
    }
}
