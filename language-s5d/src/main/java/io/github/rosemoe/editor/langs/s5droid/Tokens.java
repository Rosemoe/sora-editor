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
package io.github.rosemoe.editor.langs.s5droid;

/**
 * @author Rose
 * Tokens of S5droid language
 */
public enum Tokens {
    //空白符号类 Whitespaces
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

    //关键字
    FORLOOP,//变量循环
    WHILELOOP,//判断循环
    LONGV,//长整数型
    DOUBLEV,//双精度型
    FLOATV,//浮点数型
    BOOLEANV,//逻辑型
    INTV,//整数型
    STRINGV,//文本型
    OBJECT,//对象
    VARIANT,//变量
    ELSE,//否则
    IF,//如果
    STATIC,//静态
    CASE,//分支
    SWITCH,//判断
    LOOP,//循环
    METHOD,//方法
    EVENT,//事件
    END,//结束
    RETURN,//返回
    NEW,//创建
    NULL,//空
    FALSE,//假
    TRUE,//真
    TO,//至
    THEN,//则
    AS,//为
    ANDK,//与
    ORK,//或
    ELSEIF,//否则如果
    FORWARD,//步进
    BACK,//步退
    TRY,//容错处理
    CATCH,//捕获
    SIMPLE_TRY,//容错
    THIS,//本对象
    ASSERT,//断言
    BREAK,//跳出
    CONTINUE,//跳过
    INSTANCEOF,//从属于
    CHARV,//字节型
    
    ENUM,
    CLASS,
    EXTENDS,
    PRIVATE,
}


