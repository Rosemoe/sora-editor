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
package io.github.rosemoe.sora.langs.python;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.StringReader;

import io.github.rosemoe.sora.data.Span;
import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;
import io.github.rosemoe.sora.widget.EditorColorScheme;

public class PythonCodeAnalyzer implements CodeAnalyzer {
    @Override
    public void analyze(CharSequence content, TextAnalyzeResult result, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        try {
            CodePointCharStream stream = CharStreams.fromReader(new StringReader(content.toString()));
            PythonLexer lexer = new PythonLexer(stream);
            Token token, previous = null;
            boolean first = true;

            int lastLine = 1;
            int line, column;

            while (delegate.shouldAnalyze()) {
                token = lexer.nextToken();
                if (token == null) break;
                if (token.getType() == PythonLexer.EOF) {
                    lastLine = token.getLine() - 1;
                    break;
                }
                line = token.getLine() - 1;
                column = token.getCharPositionInLine();
                lastLine = line;

                switch (token.getType()) {
                    case PythonLexer.WS:
                    case PythonLexer.NEWLINE:
                        if (first) {
                            result.addNormalIfNull();
                        }
                        break;
                    case PythonLexer.DEF:
                    case PythonLexer.RETURN:
                    case PythonLexer.RAISE:
                    case PythonLexer.FROM:
                    case PythonLexer.IMPORT:
                    case PythonLexer.NONLOCAL:
                    case PythonLexer.AS:
                    case PythonLexer.GLOBAL:
                    case PythonLexer.ASSERT:
                    case PythonLexer.IF:
                    case PythonLexer.ELIF:
                    case PythonLexer.ELSE:
                    case PythonLexer.WHILE:
                    case PythonLexer.FOR:
                    case PythonLexer.IN:
                    case PythonLexer.TRY:
                    case PythonLexer.NONE:
                    case PythonLexer.FINALLY:
                    case PythonLexer.WITH:
                    case PythonLexer.EXCEPT:
                    case PythonLexer.LAMBDA:
                    case PythonLexer.OR:
                    case PythonLexer.AND:
                    case PythonLexer.NOT:
                    case PythonLexer.IS:
                    case PythonLexer.CLASS:
                    case PythonLexer.YIELD:
                    case PythonLexer.DEL:
                    case PythonLexer.PASS:
                    case PythonLexer.CONTINUE:
                    case PythonLexer.BREAK:
                    case PythonLexer.ASYNC:
                    case PythonLexer.AWAIT:
                        result.addIfNeeded(line, column, EditorColorScheme.KEYWORD, Span.STYLE_BOLD);
                        break;
                    case PythonLexer.COMMENT:
                        result.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                        break;
                    case PythonLexer.STRING:
                        result.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                        break;
                    case PythonLexer.DECIMAL_INTEGER:
                        result.addIfNeeded(line, column, EditorColorScheme.LINE_NUMBER);
                        break;
                    case PythonLexer.OPEN_BRACE:
                    case PythonLexer.CLOSE_BRACE:
                    case PythonLexer.OPEN_BRACKET:
                    case PythonLexer.CLOSE_BRACKET:
                    case PythonLexer.OPEN_PAREN:
                    case PythonLexer.CLOSE_PAREN:
                    case PythonLexer.DOT:
                    case PythonLexer.ELLIPSIS:
                    case PythonLexer.STAR:
                    case PythonLexer.COMMA:
                    case PythonLexer.COLON:
                    case PythonLexer.SEMI_COLON:
                    case PythonLexer.POWER:
                    case PythonLexer.ASSIGN:
                    case PythonLexer.OR_OP:
                    case PythonLexer.XOR:
                    case PythonLexer.AND_OP:
                    case PythonLexer.LEFT_SHIFT:
                    case PythonLexer.RIGHT_SHIFT:
                    case PythonLexer.ADD:
                    case PythonLexer.MINUS:
                    case PythonLexer.DIV:
                    case PythonLexer.MOD:
                    case PythonLexer.IDIV:
                    case PythonLexer.NOT_OP:
                    case PythonLexer.LESS_THAN:
                    case PythonLexer.GREATER_THAN:
                    case PythonLexer.EQUALS:
                    case PythonLexer.GT_EQ:
                    case PythonLexer.LT_EQ:
                    case PythonLexer.NOT_EQ_1:
                    case PythonLexer.NOT_EQ_2:
                    case PythonLexer.AT:
                    case PythonLexer.ARROW:
                    case PythonLexer.ADD_ASSIGN:
                    case PythonLexer.SUB_ASSIGN:
                    case PythonLexer.MULT_ASSIGN:
                    case PythonLexer.AT_ASSIGN:
                    case PythonLexer.DIV_ASSIGN:
                    case PythonLexer.MOD_ASSIGN:
                    case PythonLexer.AND_ASSIGN:
                    case PythonLexer.OR_ASSIGN:
                    case PythonLexer.XOR_ASSIGN:
                    case PythonLexer.LEFT_SHIFT_ASSIGN:
                    case PythonLexer.RIGHT_SHIFT_ASSIGN:
                    case PythonLexer.POWER_ASSIGN:
                    case PythonLexer.IDIV_ASSIGN:
                        result.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        break;
                    case PythonLexer.NAME: {
                        if (previous == null) {
                            result.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                            break;
                        } else if (previous.getType() == PythonLexer.DEF) {
                            result.addIfNeeded(line, column, EditorColorScheme.FUNCTION_NAME);
                            break;
                        } else if (previous.getType() == PythonLexer.CLASS) {
                            result.addIfNeeded(line, column, EditorColorScheme.IDENTIFIER_NAME);
                            break;
                        }
                    }
                    default:
                        result.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                        break;
                }
                first = false;
                int currentTokenType = token.getType();
                if (currentTokenType != PythonLexer.WS && currentTokenType != PythonLexer.NEWLINE) {
                    previous = token;
                }
            }

            result.determine(lastLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
