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
package io.github.rosemoe.sora.langs.css3;

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
    public void analyze(CharSequence content, TextAnalyzeResult result, TextAnalyzer.AnalyzeThread.Delegate delegate) {
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
                        if (first) result.addNormalIfNull();
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
                        result.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        break;
                    //单位
                    case CSS3Lexer.Dimension:
                        result.addIfNeeded(line, column, EditorColorScheme.ATTRIBUTE_VALUE);
                        break;
                    //@
                    case CSS3Lexer.T__14:
                        result.addIfNeeded(line, column, EditorColorScheme.ANNOTATION);
                        break;
                    //函数
                    case CSS3Lexer.DxImageTransform:
                    case CSS3Lexer.Function_:
                    case CSS3Lexer.T__3:
                        result.addIfNeeded(line, column, EditorColorScheme.FUNCTION_NAME);
                        break;
                    //数字
                    case CSS3Lexer.Percentage:
                    case CSS3Lexer.Number:
                        result.addIfNeeded(line, column, EditorColorScheme.ANNOTATION);
                        break;
                    //名字
                    case CSS3Lexer.Ident:
                        result.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        break;
                    //字符串
                    case CSS3Lexer.String_:
                        result.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                        break;
                    //颜色
                    case CSS3Lexer.Hash:
                        result.addIfNeeded(line, column, EditorColorScheme.IDENTIFIER_VAR);
                        break;
                    //注释
                    case CSS3Lexer.Comment:
                        result.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                        break;
                    default:
                        result.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                        break;
                }

                first = false;
            }
            result.determine(lastLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
