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
package io.github.rosemoe.sora.langs.java;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Stack;

import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.util.ArrayList;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class JavaIncrementalAnalyzeManager extends AsyncIncrementalAnalyzeManager<State, Long> {

    private final static int STATE_NORMAL = 0;
    private final static int STATE_INCOMPLETE_COMMENT = 1;

    private final ThreadLocal<JavaTextTokenizer> tokenizerProvider = new ThreadLocal<>();
    protected IdentifierAutoComplete.SyncIdentifiers identifiers = new IdentifierAutoComplete.SyncIdentifiers();

    private synchronized JavaTextTokenizer obtainTokenizer() {
        var res = tokenizerProvider.get();
        if (res == null) {
            res = new JavaTextTokenizer("");
            tokenizerProvider.set(res);
        }
        return res;
    }

    @Override
    public List<CodeBlock> computeBlocks(Content text, AsyncIncrementalAnalyzeManager<State, Long>.CodeBlockAnalyzeDelegate delegate) {
        var stack = new Stack<CodeBlock>();
        var blocks = new ArrayList<CodeBlock>();
        var maxSwitch = 0;
        var currSwitch = 0;
        for (int i = 0; i < text.getLineCount() && delegate.isNotCancelled(); i++) {
            var state = getState(i);
            boolean checkForIdentifiers = state.state.state == STATE_NORMAL || (state.state.state == STATE_INCOMPLETE_COMMENT && state.tokens.size() > 1);
            if (state.state.hasBraces || checkForIdentifiers) {
                // Iterate tokens
                for (int i1 = 0; i1 < state.tokens.size(); i1++) {
                    var tokenRecord = state.tokens.get(i1);
                    var token = IntPair.getFirst(tokenRecord);
                    if (token == ORDINAL_LBRACE) {
                        var offset = IntPair.getSecond(tokenRecord);
                        if (stack.isEmpty()) {
                            if (currSwitch > maxSwitch) {
                                maxSwitch = currSwitch;
                            }
                            currSwitch = 0;
                        }
                        currSwitch++;
                        CodeBlock block = new CodeBlock();
                        block.startLine = i;
                        block.startColumn = offset;
                        stack.push(block);
                    } else if (token == ORDINAL_RBRACE) {
                        var offset = IntPair.getSecond(tokenRecord);
                        if (!stack.isEmpty()) {
                            CodeBlock block = stack.pop();
                            block.endLine = i;
                            block.endColumn = offset;
                            if (block.startLine != block.endLine) {
                                blocks.add(block);
                            }
                        }
                    }
                }
            }
        }
        return blocks;
    }

    @Override
    @NonNull
    public State getInitialState() {
        return new State();
    }

    @Override
    public boolean stateEquals(@NonNull State state, @NonNull State another) {
        return state.equals(another);
    }

    @Override
    public void onAddState(State state) {
        if (state.identifiers != null) {
            for (String identifier : state.identifiers) {
                identifiers.identifierIncrease(identifier);
            }
        }
    }

    @Override
    public void onAbandonState(State state) {
        if (state.identifiers != null) {
            for (String identifier : state.identifiers) {
                identifiers.identifierDecrease(identifier);
            }
        }
    }

    @Override
    public LineTokenizeResult<State, Long> tokenizeLine(CharSequence line, State state, int lineIndex) {
        var tokens = new ArrayList<Long>();
        int newState = 0;
        var stateObj = new State();
        if (state.state == STATE_NORMAL) {
            newState = tokenizeNormal(line, 0, tokens, stateObj);
        } else if (state.state == STATE_INCOMPLETE_COMMENT) {
            var res = tryFillIncompleteComment(line, tokens);
            newState = IntPair.getFirst(res);
            if (newState == STATE_NORMAL) {
                newState = tokenizeNormal(line, IntPair.getSecond(res), tokens, stateObj);
            } else {
                newState = STATE_INCOMPLETE_COMMENT;
            }
        }
        if (tokens.isEmpty()) {
            tokens.add(token(Tokens.UNKNOWN, 0));
        }
        stateObj.state = newState;
        return new LineTokenizeResult<>(stateObj, tokens);
    }

    /**
     * @return state and offset
     */
    private long tryFillIncompleteComment(CharSequence line, List<Long> tokens) {
        char pre = '\0', cur = '\0';
        int offset = 0;
        while ((pre != '*' || cur != '/') && offset < line.length()) {
            pre = cur;
            cur = line.charAt(offset);
            offset++;
        }
        if (pre == '*' && cur == '/') {
            tokens.add(token(Tokens.LONG_COMMENT_COMPLETE, 0));
            return IntPair.pack(STATE_NORMAL, offset);
        }
        tokens.add(token(Tokens.LONG_COMMENT_INCOMPLETE, 0));
        return IntPair.pack(STATE_INCOMPLETE_COMMENT, offset);
    }

    private int tokenizeNormal(CharSequence text, int offset, List<Long> tokens, State st) {
        var tokenizer = obtainTokenizer();
        tokenizer.reset(text);
        tokenizer.offset = offset;
        Tokens token;
        int state = STATE_NORMAL;
        while ((token = tokenizer.nextToken()) != Tokens.EOF) {
            tokens.add(token(token, tokenizer.offset));
            if (token == Tokens.LBRACE || token == Tokens.RBRACE) {
                st.hasBraces = true;
            }
            if (token == Tokens.IDENTIFIER) {
                st.addIdentifier(tokenizer.getTokenText());
            }
            if (token == Tokens.LONG_COMMENT_INCOMPLETE) {
                state = STATE_INCOMPLETE_COMMENT;
                break;
            }
        }
        return state;
    }

    private static long token(Tokens type, int column) {
        return IntPair.pack(type.ordinal(), column);
    }

    @Override
    public List<Span> generateSpansForLine(LineTokenizeResult<State, Long> lineResult) {
        var spans = new ArrayList<Span>();
        var tokens = lineResult.tokens;
        Tokens previous = Tokens.UNKNOWN;
        boolean classNamePrevious = false;
        for (int i = 0; i < tokens.size(); i++) {
            var tokenRecord = tokens.get(i);
            var token = ordinalToToken(IntPair.getFirst(tokenRecord));
            int offset = IntPair.getSecond(tokenRecord);
            switch (token) {
                case WHITESPACE:
                case NEWLINE:
                    spans.add(Span.obtain(offset, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)));
                    break;
                case CHARACTER_LITERAL:
                case FLOATING_POINT_LITERAL:
                case INTEGER_LITERAL:
                case STRING:
                    classNamePrevious = false;
                    spans.add(Span.obtain(offset, TextStyle.makeStyle(EditorColorScheme.LITERAL, true)));
                    break;
                case INT:
                case LONG:
                case BOOLEAN:
                case BYTE:
                case CHAR:
                case FLOAT:
                case DOUBLE:
                case SHORT:
                case VOID:
                case VAR:
                    classNamePrevious = true;
                    spans.add(Span.obtain(offset, TextStyle.makeStyle(EditorColorScheme.KEYWORD, 0, true, false, false)));
                    break;
                case ABSTRACT:
                case ASSERT:
                case CLASS:
                case DO:
                case FINAL:
                case FOR:
                case IF:
                case NEW:
                case PUBLIC:
                case PRIVATE:
                case PROTECTED:
                case PACKAGE:
                case RETURN:
                case STATIC:
                case SUPER:
                case SWITCH:
                case ELSE:
                case VOLATILE:
                case SYNCHRONIZED:
                case STRICTFP:
                case GOTO:
                case CONTINUE:
                case BREAK:
                case TRANSIENT:
                case TRY:
                case CATCH:
                case FINALLY:
                case WHILE:
                case CASE:
                case DEFAULT:
                case CONST:
                case ENUM:
                case EXTENDS:
                case IMPLEMENTS:
                case IMPORT:
                case INSTANCEOF:
                case INTERFACE:
                case NATIVE:
                case THIS:
                case THROW:
                case THROWS:
                case TRUE:
                case FALSE:
                case NULL:
                case SEALED:
                case PERMITS:
                    classNamePrevious = false;
                    spans.add(Span.obtain(offset, TextStyle.makeStyle(EditorColorScheme.KEYWORD, 0, true, false, false)));
                    break;
                case LINE_COMMENT:
                case LONG_COMMENT_COMPLETE:
                case LONG_COMMENT_INCOMPLETE:
                    spans.add(Span.obtain(offset, TextStyle.makeStyle(EditorColorScheme.COMMENT, true)));
                    break;
                case IDENTIFIER: {
                    int type = EditorColorScheme.IDENTIFIER_NAME;
                    if (classNamePrevious) {
                        type = EditorColorScheme.IDENTIFIER_VAR;
                        classNamePrevious = false;
                    } else {
                        if (previous == Tokens.AT) {
                            type = EditorColorScheme.ANNOTATION;
                        } else {
                            // Peek next token
                            int j = i + 1;
                            var next = Tokens.UNKNOWN;
                            label:
                            while (j < tokens.size()) {
                                next = ordinalToToken(IntPair.getFirst(tokens.get(j)));
                                switch (next) {
                                    case WHITESPACE:
                                    case NEWLINE:
                                    case LONG_COMMENT_INCOMPLETE:
                                    case LONG_COMMENT_COMPLETE:
                                    case LINE_COMMENT:
                                        break;
                                    default:
                                        break label;
                                }
                                j++;
                            }
                            if (next == Tokens.LPAREN) {
                                type = EditorColorScheme.FUNCTION_NAME;
                            } else {
                                classNamePrevious = true;
                            }
                        }
                    }
                    spans.add(Span.obtain(offset, TextStyle.makeStyle(type)));
                    break;
                }
                default:
                    if (token == Tokens.LBRACK || (token == Tokens.RBRACK && previous == Tokens.LBRACK)) {
                        spans.add(Span.obtain(offset, EditorColorScheme.OPERATOR));
                        break;
                    }
                    classNamePrevious = false;
                    spans.add(Span.obtain(offset, EditorColorScheme.OPERATOR));
            }
            switch (token) {
                case LINE_COMMENT:
                case LONG_COMMENT_COMPLETE:
                case LONG_COMMENT_INCOMPLETE:
                case WHITESPACE:
                case NEWLINE:
                    break;
                default:
                    previous = token;
            }
        }
        return spans;
    }

    private static Tokens ordinalToToken(int ordinal) {
        if (mapping == null) {
            var tokens = Tokens.values();
            mapping = new Tokens[tokens.length];
            for (var token : tokens) {
                mapping[token.ordinal()] = token;
            }
        }
        return mapping[ordinal];
    }

    private static final int ORDINAL_LBRACE = Tokens.LBRACE.ordinal();
    private static final int ORDINAL_RBRACE = Tokens.RBRACE.ordinal();
    private static final int ORDINAL_IDT = Tokens.IDENTIFIER.ordinal();
    private static Tokens[] mapping;

}
