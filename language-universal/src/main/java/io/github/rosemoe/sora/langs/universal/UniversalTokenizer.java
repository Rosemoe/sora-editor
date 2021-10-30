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
package io.github.rosemoe.sora.langs.universal;

import io.github.rosemoe.sora.langs.internal.MyCharacter;
import io.github.rosemoe.sora.langs.internal.TrieTree;

/**
 * A class to tokenize for a limited style-able language
 *
 * @author Rose
 */
public class UniversalTokenizer {

    private final LanguageDescription mLanguage;
    private final TrieTree<Object> mKeywords;
    private CharSequence input;
    private int bufferLen;
    private int offset;
    private int length;
    private UniversalTokens currToken;
    private boolean skipWS;
    private boolean skipComment;
    private final char[] operatorBuffer = new char[64];

    public UniversalTokenizer(LanguageDescription languageDescription) {
        mLanguage = languageDescription;
        mKeywords = new TrieTree<>();
        for (String keyword : mLanguage.getKeywords()) {
            mKeywords.put(keyword, true);
        }
        setSkipComment(false);
        setSkipWhitespace(false);
    }

    public void setInput(CharSequence input) {
        this.input = input;
        bufferLen = input != null ? input.length() : 0;
        offset = length = 0;
        currToken = UniversalTokens.UNKNOWN;
    }

    private char charAt(int i) {
        return input.charAt(i);
    }

    private char charAt() {
        return input.charAt(offset + length);
    }

    public void setSkipWhitespace(boolean skip) {
        this.skipWS = skip;
    }

    public void setSkipComment(boolean skip) {
        this.skipComment = skip;
    }

    public UniversalTokens nextToken() {
        UniversalTokens token;
        do {
            token = nextTokenDirect();
        } while ((skipWS && token == UniversalTokens.WHITESPACE) || (skipComment && (token == UniversalTokens.LINE_COMMENT || token == UniversalTokens.LONG_COMMENT)));
        currToken = token;
        return token;
    }

    public int getOffset() {
        return offset;
    }

    public CharSequence getTokenString() {
        return input.subSequence(offset, offset + length);
    }

    public int getTokenLength() {
        return length;
    }

    public UniversalTokens getToken() {
        return currToken;
    }

    public UniversalTokens nextTokenDirect() {
        offset += length;
        if (offset >= bufferLen) {
            length = 0;
            return UniversalTokens.EOF;
        }
        length = 0;
        char ch = charAt();
        length = 1;
        if (ch == '\n') {
            return UniversalTokens.NEWLINE;
        } else if (ch == '\r') {
            scanNewline();
            return UniversalTokens.NEWLINE;
        } else if (isWhitespace(ch)) {
            char chLocal;
            while (offset + length < bufferLen && isWhitespace(chLocal = charAt(offset + length))) {
                if (chLocal == '\r' || chLocal == '\n') {
                    break;
                }
                length++;
            }
            return UniversalTokens.WHITESPACE;
        } else {
            if (offset + length < bufferLen) {
                char nextChar = charAt();
                if (mLanguage.isLineCommentStart(ch, nextChar)) {
                    while (offset + length < bufferLen && charAt() != '\n') {
                        length++;
                    }
                    return UniversalTokens.LINE_COMMENT;
                } else if (mLanguage.isLongCommentStart(ch, nextChar)) {
                    length++;
                    char pre2 = '\0', pre1 = '\0';
                    while (!mLanguage.isLongCommentEnd(pre2, pre1) && offset + length < bufferLen) {
                        pre2 = pre1;
                        pre1 = charAt();
                        length++;
                    }
                    return UniversalTokens.LONG_COMMENT;
                }
            }
            if (isIdentifierStart(ch)) {
                return scanIdentifier(ch);
            }
            if (isPrimeDigit(ch)) {
                scanNumber();
                return UniversalTokens.LITERAL;
            }
            if (ch == '\'') {
                scanCharLiteral();
                return UniversalTokens.LITERAL;
            }
            if (ch == '\"') {
                scanStringLiteral();
                return UniversalTokens.LITERAL;
            }
            operatorBuffer[0] = ch;
            if (mLanguage.isOperator(operatorBuffer, 1)) {
                boolean result = true;
                while (offset + length < bufferLen && (result = mLanguage.isOperator(operatorBuffer, length))) {
                    operatorBuffer[length] = charAt();
                    length++;
                }
                if (!result && length > 1) {
                    length--;
                }
                return UniversalTokens.OPERATOR;
            }
        }
        return UniversalTokens.UNKNOWN;
    }

    protected void scanTrans() {
        if (offset + length == bufferLen) {
            return;
        }
        char ch = charAt();
        if (ch == '\\' || ch == 't' || ch == 'f' || ch == 'n' || ch == 'r' || ch == '0' || ch == '\"' || ch == '\''
                || ch == 'b') {
            length++;
        } else if (ch == 'u') {
            length++;
            for (int i = 0; i < 4; i++) {
                if (offset + length == bufferLen) {
                    return;
                }
                if (!isDigit(charAt(offset + length))) {
                    return;
                }
                length++;
            }
        }
    }

    protected void scanStringLiteral() {
        if (offset + length == bufferLen) {
            return;
        }
        char ch;
        while (offset + length < bufferLen && (ch = charAt(offset + length)) != '\"') {
            if (ch == '\\') {
                length++;
                scanTrans();
            } else {
                if (ch == '\n') {
                    return;
                }
                length++;
                if (offset + length == bufferLen) {
                    return;
                }
            }
        }
        if (offset + length != bufferLen) {
            length++;
        }
    }

    protected void scanCharLiteral() {
        if (offset + length == bufferLen) {
            return;
        }
        char ch;
        while (offset + length < bufferLen && (ch = charAt(offset + length)) != '\'') {
            if (ch == '\\') {
                length++;
                scanTrans();
            } else {
                if (ch == '\n') {
                    return;
                }
                length++;
                if (offset + length == bufferLen) {
                    return;
                }
            }
        }
        if (offset + length != bufferLen) {
            length++;
        }
    }

    protected void scanNumber() {
        if (offset + length == bufferLen) {
            return;
        }
        boolean flag = false;
        char ch = charAt(offset);
        if (ch == '0') {
            if (charAt() == 'x') {
                length++;
            }
            flag = true;
        }
        while (offset + length < bufferLen && isDigit(charAt())) {
            length++;
        }
        if (offset + length == bufferLen) {
            return;
        }
        ch = charAt();
        if (ch == '.') {
            if (flag) {
                return;
            }
            if (offset + length + 1 == bufferLen) {
                return;
            }
            length++;
            while (offset + length < bufferLen && isDigit(charAt())) {
                length++;
            }
            if (offset + length == bufferLen) {
                return;
            }
            ch = charAt();
            if (ch == 'e' || ch == 'E') {
                length++;
                if (offset + length >= bufferLen) {
                    return;
                }
                if (charAt() == '-' || charAt() == '+') {
                    length++;
                    if (offset + length >= bufferLen) {
                        return;
                    }
                }
                while (offset + length < bufferLen && isPrimeDigit(charAt())) {
                    length++;
                }
                if (offset + length == bufferLen) {
                    return;
                }
                ch = charAt();
            }
            if (ch == 'f' || ch == 'F' || ch == 'D'
                    || ch == 'd') {
                length++;
            }
        } else if (ch == 'l' || ch == 'L') {
            length++;
        } else if (ch == 'F' || ch == 'f' || ch == 'D'
                || ch == 'd') {
            length++;
        }
    }

    protected UniversalTokens scanIdentifier(char ch) {
        TrieTree.Node<Object> n = mKeywords.root.map.get(ch);
        while (offset + length < bufferLen && isIdentifierPart(ch = charAt(offset + length))) {
            length++;
            n = n == null ? null : n.map.get(ch);
        }
        return n == null ? UniversalTokens.IDENTIFIER : (n.token == null ? UniversalTokens.IDENTIFIER : UniversalTokens.KEYWORD);
    }

    protected void scanNewline() {
        if (offset + length < bufferLen && charAt() == '\n') {
            length++;
        }
    }

    protected static boolean isDigit(char c) {
        return ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'));
    }

    protected static boolean isPrimeDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean isIdentifierPart(char ch) {
        return MyCharacter.isJavaIdentifierPart(ch);
    }

    private boolean isIdentifierStart(char ch) {
        return MyCharacter.isJavaIdentifierStart(ch);
    }

    protected static boolean isWhitespace(char c) {
        return (c == '\t' || c == ' ' || c == '\f' || c == '\n' || c == '\r');
    }

}
