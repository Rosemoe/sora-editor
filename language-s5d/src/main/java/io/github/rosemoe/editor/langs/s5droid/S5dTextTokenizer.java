/*
 *   Copyright 2020 Rosemoe
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
package io.github.rosemoe.editor.langs.s5droid;


import io.github.rosemoe.editor.langs.internal.MyCharacter;
import io.github.rosemoe.editor.langs.internal.TrieTree;

/**
 * @author Rose
 * S5droid code Tokenizer.
 * Simplified for highlight
 */
public class S5dTextTokenizer {

    private static TrieTree<Tokens> keywords;

    static {
        doStaticInit();
    }

    public static TrieTree<Tokens> getTree() {
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
        if (src == null) {
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
        currToken = Tokens.WHITESPACE;
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
        } while ((skipWS && token == Tokens.WHITESPACE) || (skipComment && (token == Tokens.LINE_COMMENT || token == Tokens.LONG_COMMENT)));
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
            return Tokens.EOF;
        }
        char ch = source.charAt(offset);
        length = 1;
        if (ch == '\n') {
            return Tokens.NEWLINE;
        } else if (ch == '\r') {
            scanNewline();
            return Tokens.NEWLINE;
        } else if (isWhitespace(ch)) {
            char chLocal;
            while (offset + length < bufferLen && isWhitespace(chLocal = charAt(offset + length))) {
                if (chLocal == '\r' || chLocal == '\n') {
                    break;
                }
                length++;
            }
            return Tokens.WHITESPACE;
        } else {
            if (isIdentifierStart(ch)) {
                return scanIdentifier(ch);
            }
            if (isPrimeDigit(ch)) {
                return scanNumber();
            }
            /* Scan usual symbols first */
            if (ch == ';') {
                return Tokens.SEMICOLON;
            } else if (ch == '(') {
                return Tokens.LPAREN;
            } else if (ch == ')') {
                return Tokens.RPAREN;
            } else if (ch == ':') {
                return Tokens.COLON;
            } else if (ch == '<') {
                return scanLT();
            } else if (ch == '>') {
                return scanGT();
            }
            /* Scan secondly symbols */
            switch (ch) {
                case '=':
                    return scanOperatorTwo(Tokens.EQ);
                case '.':
                    return Tokens.DOT;
                case '{':
                    return Tokens.LBRACE;
                case '}':
                    return Tokens.RBRACE;
                case '/':
                    return scanDIV();
                case '*':
                    return scanOperatorTwo(Tokens.MULT);
                case '-':
                    return scanOperatorTwo(Tokens.MINUS);
                case '+':
                    return scanOperatorTwo(Tokens.PLUS);
                case '[':
                    return Tokens.LBRACK;
                case ']':
                    return Tokens.RBRACK;
                case ',':
                    return Tokens.COMMA;
                case '!':
                    return Tokens.NOT;
                case '~':
                    return Tokens.COMP;
                case '?':
                    return Tokens.QUESTION;
                case '&':
                    return scanOperatorTwo(Tokens.AND);
                case '|':
                    return scanOperatorTwo(Tokens.OR);
                case '^':
                    return scanOperatorTwo(Tokens.XOR);
                case '%':
                    return scanOperatorTwo(Tokens.MOD);
                case '\'':
                    scanCharLiteral();
                    return Tokens.CHARACTER_LITERAL;
                case '\"':
                    scanStringLiteral();
                    return Tokens.STRING;
                default:
                    return Tokens.UNKNOWN;
            }
        }
    }

    protected final void throwIfNeeded() {
        if (offset + length == bufferLen) {
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
        return n == null ? Tokens.IDENTIFIER : (n.token == null ? Tokens.IDENTIFIER : n.token);
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
        if (offset + length == bufferLen) {
            return Tokens.INTEGER_LITERAL;
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
            return Tokens.INTEGER_LITERAL;
        }
        ch = charAt();
        if (ch == '.') {
            if (flag) {
                return Tokens.INTEGER_LITERAL;
            }
            if (offset + length + 1 == bufferLen) {
                return Tokens.INTEGER_LITERAL;
            }
            length++;
            throwIfNeeded();
            while (offset + length < bufferLen && isDigit(charAt())) {
                length++;
            }
            if (offset + length == bufferLen) {
                return Tokens.FLOATING_POINT_LITERAL;
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
                if (offset + length == bufferLen) {
                    return Tokens.FLOATING_POINT_LITERAL;
                }
                ch = charAt();
            }
            if (ch == 'f' || ch == 'F' || ch == 'D'
                    || ch == 'd') {
                length++;
            }
            return Tokens.FLOATING_POINT_LITERAL;
        } else if (ch == 'l' || ch == 'L') {
            length++;
            return Tokens.INTEGER_LITERAL;
        } else if (ch == 'F' || ch == 'f' || ch == 'D'
                || ch == 'd') {
            length++;
            return Tokens.FLOATING_POINT_LITERAL;
        } else {
            return Tokens.INTEGER_LITERAL;
        }
    }

    /* The following methods have been simplified for syntax high light */

    protected Tokens scanDIV() {
        if (offset + 1 == bufferLen) {
            return Tokens.DIV;
        }
        char ch = charAt();
        if (ch == '/') {
            length++;
            while (offset + length < bufferLen && charAt() != '\n') {
                length++;
            }
            return Tokens.LINE_COMMENT;
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
            return Tokens.LONG_COMMENT;
        } else {
            return Tokens.DIV;
        }
    }

    @SuppressWarnings("SameReturnValue")
    protected Tokens scanLT() {
        return Tokens.LT;
    }

    @SuppressWarnings("SameReturnValue")
    protected Tokens scanGT() {
        return Tokens.GT;
    }

    protected Tokens scanOperatorTwo(Tokens ifWrong) {
        return ifWrong;
    }

    public void reset(CharSequence src) {
        if (src == null) {
            throw new IllegalArgumentException();
        }
        this.source = src;
        line = 0;
        column = 0;
        length = 0;
        index = 0;
        offset = 0;
        currToken = Tokens.WHITESPACE;
        bufferLen = src.length();
    }

    protected static void doStaticInit() {
        // Initialize TrieTree for keywords
        keywords = new TrieTree<>();

        keywords.put("变量循环", Tokens.FORLOOP);
        keywords.put("判断循环", Tokens.WHILELOOP);
        keywords.put("长整数型", Tokens.LONGV);
        keywords.put("双精度型", Tokens.DOUBLEV);
        keywords.put("浮点数型", Tokens.FLOATV);
        keywords.put("逻辑型", Tokens.BOOLEANV);
        keywords.put("整数型", Tokens.INTV);
        keywords.put("文本型", Tokens.STRINGV);
        keywords.put("对象", Tokens.OBJECT);
        keywords.put("变量", Tokens.VARIANT);
        keywords.put("否则", Tokens.ELSE);
        keywords.put("如果", Tokens.IF);
        keywords.put("静态", Tokens.STATIC);
        keywords.put("分支", Tokens.CASE);
        keywords.put("判断", Tokens.SWITCH);
        keywords.put("循环", Tokens.LOOP);
        keywords.put("方法", Tokens.METHOD);
        keywords.put("事件", Tokens.EVENT);
        keywords.put("结束", Tokens.END);
        keywords.put("返回", Tokens.RETURN);
        keywords.put("创建", Tokens.NEW);
        keywords.put("空", Tokens.NULL);
        keywords.put("真", Tokens.TRUE);
        keywords.put("假", Tokens.FALSE);
        keywords.put("至", Tokens.TO);
        keywords.put("则", Tokens.THEN);
        keywords.put("为", Tokens.AS);
        keywords.put("与", Tokens.ANDK);
        keywords.put("或", Tokens.ORK);
        keywords.put("字符型", Tokens.CHARV);
        keywords.put("从属于", Tokens.INSTANCEOF);
        keywords.put("跳过", Tokens.CONTINUE);
        keywords.put("跳出", Tokens.BREAK);
        keywords.put("断言", Tokens.ASSERT);
        keywords.put("本对象", Tokens.THIS);
        keywords.put("容错", Tokens.SIMPLE_TRY);
        keywords.put("捕捉", Tokens.CATCH);
        keywords.put("容错处理", Tokens.TRY);
        keywords.put("否则如果", Tokens.ELSEIF);
        keywords.put("步退", Tokens.BACK);
        keywords.put("步进", Tokens.FORWARD);
        keywords.put("私有", Tokens.PRIVATE);
        keywords.put("类", Tokens.CLASS);
        keywords.put("枚举", Tokens.ENUM);
        keywords.put("继承", Tokens.EXTENDS);

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

