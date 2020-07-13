/*
 *   Copyright 2020 Rose2073
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
package com.rose.editor.langs.s5droid;


import static com.rose.editor.langs.s5droid.Tokens.*;

import com.rose.editor.langs.internal.TrieTree;
import com.rose.editor.langs.internal.MyCharacter;

/**
 * @author Rose
 * S5droid code Tokenizer.
 * Simplified for highlight!!!
 */
public class S5dTextTokenizer {

    private static TrieTree<Tokens> keywords;

    static {
        doStaticInit();
    }

    public static TrieTree getTree(){
        return keywords;
    }

    private CharSequence source;
    protected int bufferLen;
    private int line;
    private int column;
    private int index;
    protected int offset;
    protected int length;
    private Tokens currToken;
    private boolean lcCal;
    private boolean skipWS;
    private boolean skipComment;

    public S5dTextTokenizer(CharSequence src) {
        if(src == null) {
            throw new IllegalArgumentException("src can not be null");
        }
        this.source = src;
        init();
    }

    private void init() {
        line = 0;
        column = 0;
        length = 0;
        index = 0;
        currToken = WHITESPACE;
        lcCal = false;
        skipWS = false;
        skipComment = false;
        this.bufferLen = source.length();
    }

    public void setCalculateLineColumn(boolean cal) {
        this.lcCal = cal;
    }

    public void setSkipWhitespace(boolean skip) {
        this.skipWS = skip;
    }

    public void setSkipComment(boolean skip) {
        this.skipComment = skip;
    }

    public void pushBack(int length) {
        if (length > getTokenLength()) {
            throw new IllegalArgumentException("pushBack length too large");
        }
        this.length -= length;
    }

    private boolean isIdentifierPart(char ch) {
        return MyCharacter.isJavaIdentifierPart(ch);
    }

    private boolean isIdentifierStart(char ch) {
        return MyCharacter.isJavaIdentifierStart(ch);
    }

    public CharSequence getTokenString() {
        return source.subSequence(offset, offset + length);
    }

    public int getTokenLength() {
        return length;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public int getIndex() {
        return index;
    }

    public Tokens getToken() {
        return currToken;
    }

    public String yyDesc() {
        return " 行：" + line + " 列：" + column;
    }

    private char charAt(int i) {
        return source.charAt(i);
    }

    private char charAt() {
        return source.charAt(offset + length);
    }

    public Tokens nextToken() {
        Tokens token;
        do {
            token = directNextToken();
        } while ((skipWS && token == WHITESPACE) || (skipComment && (token == LINE_COMMENT || token == LONG_COMMENT)));
        currToken = token;
        return token;
    }

    public Tokens directNextToken() {
        if (lcCal) {
            boolean r = false;
            for (int i = offset; i < offset + length; i++) {
                char ch = charAt(i);
                if (ch == '\r') {
                    r = true;
                    line++;
                    column = 0;
                } else if (ch == '\n') {
                    if (r) {
                        r = false;
                        continue;
                    }
                    line++;
                    column = 0;
                } else {
                    r = false;
                    column++;
                }
            }
        }
        index = index + length;
        offset = offset + length;
        if (offset == bufferLen) {
            return EOF;
        }
        char ch = source.charAt(offset);
        length = 1;
        if (ch == '\n') {
            return NEWLINE;
        } else if (ch == '\r') {
            scanNewline();
            return NEWLINE;
        } else if (isWhitespace(ch)) {
            char chLocal;
            while (offset + length < bufferLen && isWhitespace(chLocal = charAt(offset + length)) ) {
                if (chLocal == '\r' || chLocal == '\n') {
                    break;
                }
                length++;
            }
            return WHITESPACE;
        } else {
            if (isIdentifierStart(ch)) {
                return scanIdentifier(ch);
            }
            if (isPrimeDigit(ch)) {
                return scanNumber();
            }
            /* Scan usual symbols first */
            if(ch == ';') {
                return SEMICOLON;
            }else if(ch == '(') {
                return LPAREN;
            }else if(ch == ')') {
                return RPAREN;
            }else if(ch == ':') {
                return COLON;
            }else if(ch == '<') {
                return scanLT();
            }else if(ch == '>') {
                return scanGT();
            }
            /* Scan secondly symbols */
            switch (ch) {
                case '=':
                    return scanOperatorTwo(EQ);
                case '.':
                    return DOT;
                case '{':
                    return LBRACE;
                case '}':
                    return RBRACE;
                case '/':
                    return scanDIV();
                case '*':
                    return scanOperatorTwo(MULT);
                case '-':
                    return scanOperatorTwo(MINUS);
                case '+':
                    return scanOperatorTwo(PLUS);
                case '[':
                    return LBRACK;
                case ']':
                    return RBRACK;
                case ',':
                    return COMMA;
                case '!':
                    return NOT;
                case '~':
                    return COMP;
                case '?':
                    return QUESTION;
                case '&':
                    return scanOperatorTwo(AND);
                case '|':
                    return scanOperatorTwo(OR);
                case '^':
                    return scanOperatorTwo(XOR);
                case '%':
                    return scanOperatorTwo(MOD);
                case '\'':
                    scanCharLiteral();
                    return CHARACTER_LITERAL;
                case '\"':
                    scanStringLiteral();
                    return STRING;
                default:
                    return UNKNOWN;
            }
        }
    }

    protected final void throwIfNeeded() {
        if(offset + length == bufferLen) {
            throw new RuntimeException("Token too long");
        }
    }

    protected void scanNewline() {
        if (offset + length < bufferLen && charAt(offset + length) == '\n') {
            length++;
        }
    }

    protected Tokens scanIdentifier(char ch) {
        TrieTree.Node<Tokens> n = keywords.root.map.get(ch);
        while (offset + length < bufferLen && isIdentifierPart(ch = charAt(offset + length))) {
            length++;
            n = n == null ? null : n.map.get(ch);
        }
        return n == null ? IDENTIFIER : (n.token == null ? IDENTIFIER : n.token);
    }

    protected void scanTrans() {
        throwIfNeeded();
        char ch = charAt(offset + length);
        if (ch == '\\' || ch == 't' || ch == 'f' || ch == 'n' || ch == 'r' || ch == '0' || ch == '\"' || ch == '\''
                || ch == 'b') {
            length++;
        } else if (ch == 'u') {
            length++;
            for (int i = 0; i < 4; i++) {
                throwIfNeeded();
                if (!isDigit(charAt(offset + length))) {
                    return;
                }
                length++;
            }
        }
    }

    protected void scanStringLiteral() {
        throwIfNeeded();
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
                throwIfNeeded();
            }
        }
        if (offset + length != bufferLen) {
            length++;
        }
    }

    protected void scanCharLiteral() {
        throwIfNeeded();
        char ch = charAt();
        if (ch == '\\') {
            length++;
            scanTrans();
        } else if (ch == '\'') {
            length++;
            return;
        } else {
            if (ch == '\n') {
                return;
            }
            length++;
        }
        throwIfNeeded();
        if (charAt() == '\'') {
            length++;
        }
    }

    protected Tokens scanNumber() {
        if(offset + length == bufferLen) {
            return INTEGER_LITERAL;
        }
        boolean flag = false;
        char ch = charAt(offset);
        if (ch == '0') {
            if(charAt() == 'x') {
                length++;
            }
            flag = true;
        }
        while (offset + length < bufferLen && isDigit(charAt())) {
            length++;
        }
        if(offset + length == bufferLen) {
            return INTEGER_LITERAL;
        }
        ch = charAt();
        if (ch == '.') {
            if (flag) {
                return INTEGER_LITERAL;
            }
            if(offset + length + 1 == bufferLen) {
                return INTEGER_LITERAL;
            }
            length++;
            throwIfNeeded();
            while (offset + length < bufferLen && isDigit(charAt())) {
                length++;
            }
            if(offset + length == bufferLen) {
                return FLOATING_POINT_LITERAL;
            }
            ch = charAt();
            if (ch == 'e' || ch == 'E') {
                length++;
                throwIfNeeded();
                if (charAt() == '-' || charAt() == '+') {
                    length++;
                    throwIfNeeded();
                }
                while (offset + length < bufferLen && isPrimeDigit(charAt())) {
                    length++;
                }
                if(offset + length == bufferLen) {
                    return FLOATING_POINT_LITERAL;
                }
                ch = charAt();
                if (ch == 'f' || ch == 'F' || ch == 'D'
                        || ch == 'd') {
                    length++;
                }
            } else if (ch == 'f' || ch == 'F'
                    || ch == 'D' || ch == 'd') {
                length++;
            }
            return FLOATING_POINT_LITERAL;
        } else if (ch == 'l' || ch == 'L') {
            length++;
            return INTEGER_LITERAL;
        } else if (ch == 'F' || ch == 'f' || ch == 'D'
                || ch == 'd') {
            length++;
            return FLOATING_POINT_LITERAL;
        } else {
            return INTEGER_LITERAL;
        }
    }

    /* The following methods have been simplified for syntax high light */

    protected Tokens scanDIV() {
        if (offset + 1 == bufferLen) {
            return DIV;
        }
        char ch = charAt();
        if (ch == '/') {
            length++;
            while (offset + length < bufferLen && charAt() != '\n') {
                length++;
            }
            return LINE_COMMENT;
        } else if (ch == '*') {
            length++;
            char pre, curr = '?';
            while (offset + length < bufferLen) {
                pre = curr;
                curr = charAt();
                if (curr == '/' && pre == '*') {
                    length++;
                    break;
                }
                length++;
            }
            return LONG_COMMENT;
        } else {
            return DIV;
        }
    }

    @SuppressWarnings("SameReturnValue")
    protected Tokens scanLT() {
        return LT;
    }

    @SuppressWarnings("SameReturnValue")
    protected Tokens scanGT() {
        return GT;
    }

    protected Tokens scanOperatorTwo(Tokens ifWrong) {
        return ifWrong;
    }

    public void reset(CharSequence src) {
        if(src == null) {
            throw new IllegalArgumentException();
        }
        this.source = src;
        line = 0;
        column = 0;
        length = 0;
        index = 0;
        offset = 0;
        currToken = WHITESPACE;
        bufferLen = src.length();
    }

    protected static void doStaticInit() {
        // Initialize TrieTree for keywords
        keywords = new TrieTree<>();

        keywords.put("变量循环", FORLOOP);
        keywords.put("判断循环", WHILELOOP);
        keywords.put("长整数型", LONGV);
        keywords.put("双精度型", DOUBLEV);
        keywords.put("浮点数型", FLOATV);
        keywords.put("逻辑型", BOOLEANV);
        keywords.put("整数型", INTV);
        keywords.put("文本型", STRINGV);
        keywords.put("对象", OBJECT);
        keywords.put("变量", VARIANT);
        keywords.put("否则", ELSE);
        keywords.put("如果", IF);
        keywords.put("静态", STATIC);
        keywords.put("分支", CASE);
        keywords.put("判断", SWITCH);
        keywords.put("循环", LOOP);
        keywords.put("方法",METHOD);
        keywords.put("事件", EVENT);
        keywords.put("结束", END);
        keywords.put("返回", RETURN);
        keywords.put("创建", NEW);
        keywords.put("空", NULL);
        keywords.put("真", TRUE);
        keywords.put("假", FALSE);
        keywords.put("至", TO);
        keywords.put("则", THEN);
        keywords.put("为", AS);
        keywords.put("与", ANDK);
        keywords.put("或", ORK);
        keywords.put("字符型", CHARV);
        keywords.put("从属于", INSTANCEOF);
        keywords.put("跳过", CONTINUE);
        keywords.put("跳出", BREAK);
        keywords.put("断言", ASSERT);
        keywords.put("本对象", THIS);
        keywords.put("容错", SIMPLE_TRY);
        keywords.put("捕捉", CATCH);
        keywords.put("容错处理", TRY);
        keywords.put("否则如果", ELSEIF);
        keywords.put("步退", BACK);
        keywords.put("步进", FORWARD);
        keywords.put("私有",PRIVATE);
        keywords.put("类",CLASS);
        keywords.put("枚举",ENUM);
        keywords.put("继承",EXTENDS);

        MyCharacter.initMap();
    }

    protected static boolean isDigit(char c) {
        return ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'));
    }

    protected static boolean isPrimeDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    protected static boolean isWhitespace(char c) {
        return (c == '\t' || c == ' ' || c == '\f' || c == '\n' || c == '\r');
    }
}

