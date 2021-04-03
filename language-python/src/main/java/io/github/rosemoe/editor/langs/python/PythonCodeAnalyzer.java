/*
 *   Copyright 2020-2021 Rosemoe
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
package io.github.rosemoe.editor.langs.python;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.StringReader;

import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.text.TextAnalyzeResult;
import io.github.rosemoe.editor.text.TextAnalyzer;
import io.github.rosemoe.editor.widget.EditorColorScheme;

public class PythonCodeAnalyzer implements CodeAnalyzer {
    @Override
    public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        try {
            CodePointCharStream stream = CharStreams.fromReader(new StringReader(content.toString()));
            PythonLexer lexer = new PythonLexer(stream);
            Token token = null, previous = null;
            boolean first = true;

            int lastLine = 1;
            int line = 0, column = 0;

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
                            colors.addNormalIfNull();
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
                        colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        break;
                    case PythonLexer.COMMENT:
                        colors.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                        break;
                    case PythonLexer.STRING:
                        colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                        break;
                    case PythonLexer.DECIMAL_INTEGER:
                        colors.addIfNeeded(line, column, EditorColorScheme.LINE_NUMBER);
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
                        colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        break;
                    case PythonLexer.NAME: {
                        if (previous == null) {
                            colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                            break;
                        } else if (previous.getType() == PythonLexer.DEF) {
                            colors.addIfNeeded(line, column, EditorColorScheme.FUNCTION_NAME);
                            break;
                        } else if (previous.getType() == PythonLexer.CLASS) {
                            colors.addIfNeeded(line, column, EditorColorScheme.IDENTIFIER_NAME);
                            break;
                        }
                    }
                    default:
                        colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                        break;
                }
                first = false;
                int currentTokenType = token.getType();
                if (token != null && currentTokenType != PythonLexer.WS && currentTokenType != PythonLexer.NEWLINE) {
                    previous = token;
                }
            }

            colors.determine(lastLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
