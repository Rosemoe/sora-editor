/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Copyright (C) 1998-2015  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * License: BSD                                                            *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

/* Java 1.2 language lexer specification */

/* Use together with unicode.flex for Unicode preprocesssing */
/* and java12.cup for a Java 1.2 parser                      */


package com.rose.example;

%%

%public
%class JavaLexer

%unicode
%type JavaType
%line
%column
%char


%{
  public int getLine() {
      return yyline;
  }
  
  public int getColumn() {
      return yycolumn;
  }
  
  public int getIndex() {
      return yychar;
  }
%}

/* main character classes */
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

WhiteSpace = {LineTerminator} | [ \t\f]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment} | 
          {DocumentationComment}

TraditionalComment = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/*" "*"+ [^/*] ~"*/"

/* identifiers */
Identifier = [:jletter:][:jletterdigit:]*

/* integer literals */
DecIntegerLiteral = 0 | [1-9][0-9]*
DecLongLiteral    = {DecIntegerLiteral} [lL]

HexIntegerLiteral = 0 [xX] 0* {HexDigit} {1,8}
HexLongLiteral    = 0 [xX] 0* {HexDigit} {1,16} [lL]
HexDigit          = [0-9a-fA-F]

OctIntegerLiteral = 0+ [1-3]? {OctDigit} {1,15}
OctLongLiteral    = 0+ 1? {OctDigit} {1,21} [lL]
OctDigit          = [0-7]
    
/* floating point literals */        
FloatLiteral  = ({FLit1}|{FLit2}|{FLit3}) {Exponent}? [fF]
DoubleLiteral = ({FLit1}|{FLit2}|{FLit3}) {Exponent}?

FLit1    = [0-9]+ \. [0-9]* 
FLit2    = \. [0-9]+ 
FLit3    = [0-9]+ 
Exponent = [eE] [+-]? [0-9]+

/* string and character literals */
StringCharacter = [^\r\n\"\\]
SingleCharacter = [^\r\n\'\\]

%state STRING, CHARLITERAL

%%

<YYINITIAL> {

  /* keywords */
  "abstract"                     { return JavaType.KEYWORD; }
  "boolean"                      { return JavaType.KEYWORD; }
  "break"                        { return JavaType.KEYWORD;}
  "byte"                         { return JavaType.KEYWORD; }
  "case"                         { return JavaType.KEYWORD; }
  "catch"                        { return JavaType.KEYWORD; }
  "char"                         { return JavaType.KEYWORD; }
  "class"                        { return JavaType.KEYWORD; }
  "const"                        { return JavaType.KEYWORD; }
  "continue"                     { return JavaType.KEYWORD; }
  "do"                           { return JavaType.KEYWORD; }
  "double"                       { return JavaType.KEYWORD; }
  "else"                         { return JavaType.KEYWORD; }
  "extends"                      { return JavaType.KEYWORD; }
  "final"                        { return JavaType.KEYWORD; }
  "finally"                      { return JavaType.KEYWORD; }
  "float"                        { return JavaType.KEYWORD; }
  "for"                          { return JavaType.KEYWORD; }
  "default"                      { return JavaType.KEYWORD; }
  "implements"                   { return JavaType.KEYWORD; }
  "import"                       { return JavaType.KEYWORD; }
  "instanceof"                   { return JavaType.KEYWORD; }
  "int"                          { return JavaType.KEYWORD; }
  "interface"                    { return JavaType.KEYWORD; }
  "long"                         { return JavaType.KEYWORD; }
  "native"                       { return JavaType.KEYWORD; }
  "new"                          { return JavaType.KEYWORD; }
  "goto"                         { return JavaType.KEYWORD; }
  "if"                           { return JavaType.KEYWORD; }
  "public"                       { return JavaType.KEYWORD; }
  "short"                        { return JavaType.KEYWORD; }
  "super"                        { return JavaType.KEYWORD; }
  "switch"                       { return JavaType.KEYWORD; }
  "synchronized"                 { return JavaType.KEYWORD; }
  "package"                      { return JavaType.KEYWORD; }
  "private"                      { return JavaType.KEYWORD; }
  "protected"                    { return JavaType.KEYWORD; }
  "transient"                    { return JavaType.KEYWORD; }
  "return"                       { return JavaType.KEYWORD; }
  "void"                         { return JavaType.KEYWORD; }
  "static"                       { return JavaType.KEYWORD; }
  "while"                        { return JavaType.KEYWORD; }
  "this"                         { return JavaType.KEYWORD; }
  "throw"                        { return JavaType.KEYWORD; }
  "throws"                       { return JavaType.KEYWORD; }
  "try"                          { return JavaType.KEYWORD; }
  "volatile"                     { return JavaType.KEYWORD; }
  "strictfp"                     { return JavaType.KEYWORD; }
  
  /* boolean literals */
  "true"                         { return JavaType.BOOLEAN_LITERAL; }
  "false"                        { return JavaType.BOOLEAN_LITERAL; }
  
  /* null literal */
  "null"                         {  return JavaType.NULL_LITERAL; }
  
  
  /* separators */
  "("                            {  return JavaType.LPAREN; }
  ")"                            {  return JavaType.RPAREN; }
  "{"                            {  return JavaType.LBRACE; }
  "}"                            {  return JavaType.RBRACE; }
  "["                            {  return JavaType.LBRACK; }
  "]"                            {  return JavaType.RBRACK; }
  ";"                            {  return JavaType.SEMICOLON; }
  ","                            {  return JavaType.COMMA; }
  "."                            {  return JavaType.DOT; }
  
  /* operators */
  "="                            {  return JavaType.EQ; }
  ">"                            {  return JavaType.GT; }
  "<"                            {  return JavaType.LT; }
  "!"                            {  return JavaType.NOT; }
  "~"                            {  return JavaType.COMP; }
  "?"                            {  return JavaType.QUESTION; }
  ":"                            {  return JavaType.COLON; }
  "=="                           {  return JavaType.EQEQ; }
  "<="                           {  return JavaType.LTEQ; }
  ">="                           {  return JavaType.GTEQ; }
  "!="                           {  return JavaType.NOTEQ; }
  "&&"                           {  return JavaType.ANDAND; }
  "||"                           {  return JavaType.OROR; }
  "++"                           {  return JavaType.PLUSPLUS; }
  "--"                           {  return JavaType.MINUSMINUS; }
  "+"                            {  return JavaType.PLUS; }
  "-"                            {  return JavaType.MINUS; }
  "*"                            {  return JavaType.MULT; }
  "/"                            {  return JavaType.DIV; }
  "&"                            {  return JavaType.AND; }
  "|"                            {  return JavaType.OR; }
  "^"                            {  return JavaType.XOR; }
  "%"                            {  return JavaType.MOD; }
  "<<"                           {  return JavaType.LSHIFT; }
  ">>"                           {  return JavaType.RSHIFT; }
  ">>>"                          {  return JavaType.URSHIFT; }
  "+="                           {  return JavaType.PLUSEQ; }
  "-="                           {  return JavaType.MINUSEQ; }
  "*="                           {  return JavaType.MULTEQ; }
  "/="                           {  return JavaType.DIVEQ; }
  "&="                           {  return JavaType.ANDEQ; }
  "|="                           {  return JavaType.OREQ; }
  "^="                           {  return JavaType.XOREQ; }
  "%="                           {  return JavaType.MODEQ; }
  "<<="                          {  return JavaType.LSHIFTEQ; }
  ">>="                          {  return JavaType.RSHIFTEQ; }
  ">>>="                         {  return JavaType.URSHIFTEQ; }
  
  /* string literal */
  \"                             { yybegin(STRING);return JavaType.STRING; }

  /* character literal */
  \'                             { yybegin(CHARLITERAL);return JavaType.CHARACTER_LITERAL;  }

  /* numeric literals */

  /* This is matched together with the minus, because the number is too big to 
     be represented by a positive integer. */
  "-2147483648"                  {  return JavaType.INTEGER_LITERAL; }
  
  {DecIntegerLiteral}            {  return JavaType.INTEGER_LITERAL; }
  {DecLongLiteral}               {  return JavaType.INTEGER_LITERAL; }
  
  {HexIntegerLiteral}            {  return JavaType.INTEGER_LITERAL; }
  {HexLongLiteral}               {  return JavaType.INTEGER_LITERAL; }
 
  {OctIntegerLiteral}            {  return JavaType.INTEGER_LITERAL; }  
  {OctLongLiteral}               {  return JavaType.INTEGER_LITERAL; }
  
  {FloatLiteral}                 {  return JavaType.FLOATING_POINT_LITERAL; }
  {DoubleLiteral}                {  return JavaType.FLOATING_POINT_LITERAL; }
  {DoubleLiteral}[dD]            {  return JavaType.FLOATING_POINT_LITERAL; }
  
  /* comments */
  {Comment}                      { return JavaType.COMMENT; }

  /* whitespace */
  {WhiteSpace}                   { return JavaType.WHITESPACE; }

  /* identifiers */ 
  {Identifier}                   {  return JavaType.IDENTIFIER; }  
}

<STRING> {
  \"                             { yybegin(YYINITIAL);return JavaType.STRING;  }
  
  {StringCharacter}+             { return JavaType.STRING; }
  
  /* escape sequences */
  "\\b"                          { return JavaType.STRING; }
  "\\t"                          { return JavaType.STRING; }
  "\\n"                          { return JavaType.STRING; }
  "\\f"                          { return JavaType.STRING; }
  "\\r"                          { return JavaType.STRING; }
  "\\\""                         { return JavaType.STRING; }
  "\\'"                          { return JavaType.STRING; }
  "\\\\"                         { return JavaType.STRING; }
  \\[0-3]?{OctDigit}?{OctDigit}  { return JavaType.STRING; }
  
  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new RuntimeException("Unterminated string at end of line"); }
}

<CHARLITERAL> {
  {SingleCharacter}\'            { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL; }
  
  /* escape sequences */
  "\\b"\'                        { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL;}
  "\\t"\'                        { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL;}
  "\\n"\'                        { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL;}
  "\\f"\'                        { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL;}
  "\\r"\'                        { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL;}
  "\\\""\'                       { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL;}
  "\\'"\'                        { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL;}
  "\\\\"\'                       { yybegin(YYINITIAL);  return JavaType.CHARACTER_LITERAL; }
  \\[0-3]?{OctDigit}?{OctDigit}\' { yybegin(YYINITIAL); return JavaType.CHARACTER_LITERAL; }
  
  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new RuntimeException("Unterminated character literal at end of line"); }
}

/* error fallback */
[^]                              { throw new RuntimeException("Illegal character \""+yytext()+
                                                              "\" at line "+yyline+", column "+yycolumn); }
<<EOF>>                          {  return JavaType.EOF; }
