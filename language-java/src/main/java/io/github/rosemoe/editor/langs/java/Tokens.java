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
package io.github.rosemoe.editor.langs.java;

/**
 * Tokens for Java
 * @author Rose
 */
public enum Tokens {
    WHITESPACE,
    NEWLINE,
    UNKNOWN,
    EOF,

    //注释类 Comments
    LONG_COMMENT,//长注释 Long comment
    LINE_COMMENT,//单行注释 Single line comment

    DIV,//除
    MULT,//乘
    IDENTIFIER,//标识符
    INTEGER_LITERAL,//整数
    DOT,//点
    MINUS,//减
    STRING,//字符串
    CHARACTER_LITERAL,//字符
    LPAREN,//左小括号
    RPAREN,//右小括号
    LBRACE,//左大括号
    RBRACE,//右大括号
    LBRACK,//左中括号
    RBRACK,//右中括号
    SEMICOLON,//分号
    COMMA,//逗号
    EQ,//等于
    GT,//大于
    LT,//小于
    NOT,//非
    COMP,//~
    QUESTION,//问号
    COLON,//冒号
    AND,//与
    OR,//或
    PLUS,//加
    XOR,//异或
    MOD,//百分号
    DIVEQ,
    MULTEQ,
    FLOATING_POINT_LITERAL,//浮点数
    MINUSMINUS,//减减
    MINUSEQ,
    EQEQ,//等于等于
    GTEQ,
    RSHIFT,//右位移
    LTEQ,
    LSHIFT,//左位移
    NOTEQ,
    ANDEQ,
    ANDAND,//与与
    OREQ,
    OROR,//或或
    PLUSEQ,
    PLUSPLUS,//加加
    XOREQ,
    MODEQ,
    RSHIFTEQ,
    URSHIFT,
    LSHIFTEQ,
    URSHIFTEQ,

    //Keywords
    ABSTRACT,
    ASSERT,
    BOOLEAN,
    BYTE,
    CHAR,
    CLASS,
    DO,
    DOUBLE,
    FINAL,
    FLOAT,
    FOR,
    IF,
    INT,
    LONG,
    NEW,
    PUBLIC,
    PRIVATE,
    PROTECTED,
    PACKAGE,
    RETURN,
    STATIC,
    SHORT,
    SUPER,
    SWITCH,
    ELSE,
    VOLATILE,
    SYNCHRONIZED,
    STRICTFP,
    GOTO,
    CONTINUE,
    BREAK,
    TRANSIENT,
    VOID,
    TRY,
    CATCH,
    FINALLY,
    WHILE,
    CASE,
    DEFAULT,
    CONST,
    ENUM,
    EXTENDS,
    IMPLEMENTS,
    IMPORT,
    INSTANCEOF,
    INTERFACE,
    NATIVE,
    THIS,
    THROW,
    THROWS,
    AT,

    TRUE,
    FALSE

}
