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
package io.github.rosemoe.sora.langs.xml.analyzer;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;

import io.github.rosemoe.sora.data.BlockLine;
import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.langs.xml.XMLLexer;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;
import io.github.rosemoe.sora.widget.EditorColorScheme;


/**
 * For highlight and code block line.
 * <p>
 * Note:Android Studio xml highlight style.
 */
public class HighLightAnalyzer implements CodeAnalyzer {
    private int lastLine;

    public int getLastLine() {
        return lastLine;
    }

    @Override
    public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        try {
            CodePointCharStream stream = CharStreams.fromReader(new StringReader(content.toString()));
            XMLLexer lexer = new XMLLexer(stream);
            Token token , preToken = null, prePreToken = null;
            boolean first = true;
            lastLine = 1;
            int line , column ;
            Stack<BlockLine> stack = new Stack<>();
            while (delegate.shouldAnalyze()) {
                token = lexer.nextToken();
                if (token == null) break;
                if (token.getType() == XMLLexer.EOF) {
                    lastLine = token.getLine() - 1;
                    break;
                }
                line = token.getLine() - 1;
                column = token.getCharPositionInLine();
                lastLine = line;

                switch (token.getType()) {
                    case XMLLexer.SEA_WS:
                    case XMLLexer.S:
                        if (first) colors.addNormalIfNull();
                        break;
                    //<?xml
                    case XMLLexer.XMLDeclOpen:
                        //<?
                        colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        //xml
                        colors.addIfNeeded(line, column + 2, EditorColorScheme.TEXT_NORMAL);
                        break;
                    case XMLLexer.EQUALS:
                        //colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        //break;
                    case XMLLexer.STRING:
                        colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                        break;
                    // />
                    case XMLLexer.SLASH_CLOSE:
                        colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        //set block line end position
                        if (!stack.isEmpty()) {
                            BlockLine block = stack.pop();
                            block.endLine = line;
                            block.endColumn = column;
                            if (block.startLine != block.endLine) {
                                if (preToken.getLine() == token.getLine())
                                    block.toBottomOfEndLine = true;
                                colors.addBlockLine(block);
                            }
                        }
                        break;
                    // /
                    case XMLLexer.SLASH:
                        colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        //When we get "/", check the previous token.
                        //If we get '<',set block line end position
                        if (preToken != null && preToken.getType() == XMLLexer.OPEN) {
                            if (!stack.isEmpty()) {
                                BlockLine block = stack.pop();
                                block.endLine = preToken.getLine() - 1;
                                block.endColumn = preToken.getCharPositionInLine();
                                if (block.startLine != block.endLine) {
                                    if (prePreToken.getLine() == preToken.getLine())
                                        block.toBottomOfEndLine = true;
                                    colors.addBlockLine(block);
                                }
                            }
                        }
                        break;
                    case XMLLexer.Name:
                        String text = token.getText();
                        // for name in </name>
                        if (preToken != null && preToken.getType() == XMLLexer.SLASH) {
                            colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        }
                        //for name in <name...
                        //code block start
                        else if (preToken != null && preToken.getType() == XMLLexer.OPEN) {
                            colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                            BlockLine block = new BlockLine();
                            block.startLine = preToken.getLine() - 1;
                            block.startColumn = preToken.getCharPositionInLine();//-1 for '<'
                            stack.push(block);
                        }
                        //android studio style
                        else if (text.startsWith("xmlns:")) {
                            colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                            if (text.length() > "xmlns:".length()) {
                                colors.addIfNeeded(line, column + "xmlns:".length(), EditorColorScheme.IDENTIFIER_VAR);
                            }
                        } else {
                            //for 'a:b:c' style,we high light all before the last ':' as namespace
                            //(namespace:namespace:...):(the last one)
                            //Color scheme name may be strange.
                            if (text.contains(":")) {
                                int index = text.lastIndexOf(':');
                                colors.addIfNeeded(line, column, EditorColorScheme.IDENTIFIER_VAR);
                                if (index != text.length() - 1)
                                    colors.addIfNeeded(line, column + index + 1, EditorColorScheme.TEXT_NORMAL);
                            } else {
                                colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                            }
                        }
                        break;
                    case XMLLexer.COMMENT:
                        colors.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                        break;
                    case XMLLexer.OPEN:
                    case XMLLexer.CLOSE:
                        colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                        break;
                    default:
                        colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                        break;
                }

                if (preToken != null) {
                    prePreToken = preToken;
                }
                if (token.getType() != XMLLexer.SEA_WS && token.getType() != XMLLexer.S) {
                    preToken = token;
                }

                first = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
