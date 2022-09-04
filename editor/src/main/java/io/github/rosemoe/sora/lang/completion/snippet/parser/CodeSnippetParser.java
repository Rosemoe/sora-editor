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
package io.github.rosemoe.sora.lang.completion.snippet.parser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet;
import io.github.rosemoe.sora.lang.completion.snippet.PlaceholderDefinition;

public class CodeSnippetParser {

    private final String src;
    private final CodeSnippet.Builder builder;
    private final CodeSnippetTokenizer tokenizer;
    private Token token;

    private CodeSnippetParser(String snippet, List<PlaceholderDefinition> definitions) {
        src = snippet;
        builder = new CodeSnippet.Builder(definitions);
        tokenizer = new CodeSnippetTokenizer(snippet);
    }

    private void next() {
        token = tokenizer.nextToken();
    }

    private boolean accept(TokenType type) {
        if (token.type == type) {
            next();
            return true;
        }
        return false;
    }

    private String _accept(TokenType type) {
        if (token.type == type) {
            var text = src.substring(token.index, token.index + token.length);
            next();
            return text;
        }
        return null;
    }

    private boolean accept(TokenType... types) {
        for (var type : types) {
            if (token.type == type) {
                next();
                return true;
            }
        }
        return false;
    }

    private String _accept(TokenType... types) {
        if (types.length == 0) {
            var text = src.substring(token.index, token.index + token.length);
            next();
            return text;
        }
        for (var type : types) {
            if (token.type == type) {
                var text = src.substring(token.index, token.index + token.length);
                next();
                return text;
            }
        }
        return null;
    }

    private void backTo(Token token) {
        tokenizer.moveTo(token.index + token.length);
        this.token = token;
    }

    private void parse() {
        token = tokenizer.nextToken();
        while (parseInternal()) {
           //empty
        }
    }

    private boolean parseInternal() {
        return parseEscaped() ||
                parseTabStopOrVariableName() ||
                parseComplexVariable() ||
                parseComplexPlaceholder() ||
                parseOther();
    }

    private boolean parseEscaped() {
        if (accept(TokenType.Backslash)) {
            var escaped = _accept(TokenType.CurlyClose, TokenType.Dollar, TokenType.Backslash);
            if (escaped == null) {
                escaped = "\\";
            }
            builder.addPlainText(escaped);

            return true;
        }
        return false;
    }

    private boolean parseTabStopOrVariableName() {
        var backup = token;
        if (accept(TokenType.Dollar)) {
            String text;
            if ((text = _accept(TokenType.Int)) != null) {
                builder.addPlaceholder(Integer.parseInt(text));
            } else if ((text = _accept(TokenType.VariableName)) != null) {
                builder.addVariable(text, null);
            } else {
                backTo(backup);
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean parseComplexVariable() {
        var backup = token;
        String text;
        if (accept(TokenType.Dollar) && accept(TokenType.CurlyOpen) && (text = _accept(TokenType.VariableName)) != null) {
            var variableName = text;
            String defaultValue = null;
            if (accept(TokenType.Colon)) {
                // ${name:xxx}
                var sb = new StringBuilder();
                while (!accept(TokenType.CurlyClose)) {
                    if (accept(TokenType.Backslash)) {
                        if ((text = _accept(TokenType.Backslash, TokenType.Dollar, TokenType.CurlyClose)) != null) {
                            sb.append(text);
                        } else {
                            sb.append('\\');
                        }
                    } else if (token.type == TokenType.EOF) {
                        backTo(backup);
                        return false;
                    } else {
                        sb.append(src, token.index, token.index + token.length);
                        next();
                    }
                }
                builder.addVariable(variableName, sb.toString());
            } else if(accept(TokenType.CurlyClose)) {
                // ${name}
                builder.addVariable(variableName, null);
            } else {
                // missing token
                backTo(backup);
                return false;
            }
            return true;
        }
        backTo(backup);
        return false;
    }

    private boolean parseComplexPlaceholder() {
        var backup = token;
        String text;
        if (accept(TokenType.Dollar) && accept(TokenType.CurlyOpen) && (text = _accept(TokenType.Int)) != null) {
            var idText = text;
            String defaultValue = null;
            if (accept(TokenType.Colon)) {
                // ${1:xxx}
                var sb = new StringBuilder();
                while (!accept(TokenType.CurlyClose)) {
                    if (accept(TokenType.Backslash)) {
                        if ((text = _accept(TokenType.Backslash, TokenType.Dollar, TokenType.CurlyClose)) != null) {
                            sb.append(text);
                        } else {
                            sb.append('\\');
                        }
                    } else if (token.type == TokenType.EOF) {
                        backTo(backup);
                        return false;
                    } else {
                        sb.append(src, token.index, token.index + token.length);
                        next();
                    }
                }
                builder.addPlaceholder(Integer.parseInt(idText), sb.toString());
            } else if(accept(TokenType.Pipe)) {
                // ${1|one,two,three|}
                var choices = new ArrayList<String>();
                while (true) {
                    if (parseChoiceElement(choices)) {
                        if (accept(TokenType.Comma)) {
                            continue;
                        }
                        if (accept(TokenType.Pipe) && accept(TokenType.CurlyClose)) {
                            builder.addPlaceholder(Integer.parseInt(idText), choices);
                            return true;
                        }
                    }

                    backTo(backup);
                    return false;
                }
            } else if(accept(TokenType.CurlyClose)) {
                // ${1}
                builder.addPlaceholder(Integer.parseInt(idText));
            } else {
                // missing token
                backTo(backup);
                return false;
            }
            return true;
        }
        backTo(backup);
        return false;
    }

    private boolean parseChoiceElement(List<String> choices) {
        var backup = token;
        var sb = new StringBuilder();
        String text;
        while (token.type != TokenType.Comma && token.type != TokenType.Pipe) {
            if (accept(TokenType.Backslash)) {
                if ((text = _accept(TokenType.Pipe, TokenType.Comma, TokenType.Backslash)) != null) {
                    sb.append(text);
                } else {
                    sb.append('\\');
                }
            } else if (token.type != TokenType.EOF) {
                sb.append(_accept());
            } else {
                backTo(backup);
                return false;
            }
        }
        if (sb.length() == 0) {
            backTo(backup);
            return false;
        }
        choices.add(sb.toString());
        return true;
    }


    private boolean parseOther() {
        if (token.type == TokenType.EOF) {
            return false;
        }
        do {
            if (token.length > 0) {
                builder.addPlainText(src.substring(token.index, token.index + token.length));
            }
            next();
        } while (token.type != TokenType.Backslash && token.type != TokenType.Dollar && token.type != TokenType.EOF);
        return true;
    }

    public static CodeSnippet parse(@NonNull String snippet) {
        return parse(snippet, new ArrayList<>());
    }

    public static CodeSnippet parse(@NonNull String snippet, @NonNull List<PlaceholderDefinition> definitions) {
        var parser = new CodeSnippetParser(snippet, definitions);
        parser.parse();
        return parser.builder.build();
    }

}
