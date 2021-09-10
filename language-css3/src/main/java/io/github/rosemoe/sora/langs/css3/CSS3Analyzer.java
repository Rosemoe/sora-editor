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
package io.github.rosemoe.sora.langs.css3;

import android.util.Log;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.StringReader;

import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;
import io.github.rosemoe.sora.widget.EditorColorScheme;

/**
 * Simple implementation of highlight CSS3.
 * The color matching may not be beautiful and reasonable.
 * Part of it depends on css3.g4.
 * You can continue to expand according to your needs.
 */
public class CSS3Analyzer implements CodeAnalyzer {
    @Override
    public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        try {
            CodePointCharStream stream = CharStreams.fromReader(new StringReader(content.toString()));
            CSS3Lexer lexer = new CSS3Lexer(stream);
            Token token;
            boolean first = true;
            int lastLine = 1;
            int line, column;
            while (delegate.shouldAnalyze()) {
                token = lexer.nextToken();
                if (token == null) break;
                if (token.getType() == CSS3Lexer.EOF) {
                    lastLine = token.getLine() - 1;
                    break;
                }
                line = token.getLine() - 1;
                column = token.getCharPositionInLine();
                lastLine = line;

                //Log.d("test",token.getText()+"  "+token.getType());
                switch (token.getType()) {
                    case CSS3Lexer.Space:
                        if (first) colors.addNormalIfNull();
                        break;
                    //运算符
                    case CSS3Lexer.Plus:
                    case CSS3Lexer.Minus:
                    case CSS3Lexer.Greater:
                    case CSS3Lexer.Tilde:
                    case CSS3Lexer.PseudoNot:
                    case CSS3Lexer.T__0:
                    case CSS3Lexer.T__2:
                    case CSS3Lexer.Comma:
                    case CSS3Lexer.T__5:
                    case CSS3Lexer.T__4:
                        colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        break;
                    //单位
                    case CSS3Lexer.Dimension:
                        colors.addIfNeeded(line, column, EditorColorScheme.ATTRIBUTE_VALUE);
                        break;
                    //@
                    case CSS3Lexer.T__14:
                        colors.addIfNeeded(line, column, EditorColorScheme.ANNOTATION);
                        break;
                    //函数
                    case CSS3Lexer.DxImageTransform:
                    case CSS3Lexer.Function_:
                    case CSS3Lexer.T__3:
                        colors.addIfNeeded(line, column, EditorColorScheme.FUNCTION_NAME);
                        break;
                    //数字
                    case CSS3Lexer.Percentage:
                    case CSS3Lexer.Number:
                        colors.addIfNeeded(line, column, EditorColorScheme.ANNOTATION);
                        break;
                    //名字
                    case CSS3Lexer.Ident:
                        colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        break;
                    //字符串
                    case CSS3Lexer.String_:
                        colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                        break;
                    //颜色
                    case CSS3Lexer.Hash:
                        colors.addIfNeeded(line, column, EditorColorScheme.IDENTIFIER_VAR);
                        break;
                    //注释
                    case CSS3Lexer.Comment:
                        colors.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                        break;
                    default:
                        colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                        break;
                }

                first = false;
            }
            colors.determine(lastLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
