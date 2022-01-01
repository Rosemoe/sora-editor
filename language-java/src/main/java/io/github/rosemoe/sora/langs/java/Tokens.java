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

/**
 * Tokens for Java
 *
 * @author Rose
 */
@SuppressWarnings("SpellCheckingInspection")
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
    FALSE,
    NULL,
}
