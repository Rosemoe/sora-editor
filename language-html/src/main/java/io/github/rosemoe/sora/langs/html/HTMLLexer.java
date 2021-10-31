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
package io.github.rosemoe.sora.langs.html;
// Generated from HTMLLexer.g4 by ANTLR 4.9.1

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class HTMLLexer extends Lexer {
    public static final int
            HTML_COMMENT = 1, HTML_CONDITIONAL_COMMENT = 2, XML = 3, CDATA = 4, DTD = 5, SCRIPTLET = 6,
            SEA_WS = 7, SCRIPT_OPEN = 8, STYLE_OPEN = 9, TAG_OPEN = 10, HTML_TEXT = 11, TAG_CLOSE = 12,
            TAG_SLASH_CLOSE = 13, TAG_SLASH = 14, TAG_EQUALS = 15, TAG_NAME = 16, TAG_WHITESPACE = 17,
            SCRIPT_BODY = 18, SCRIPT_SHORT_BODY = 19, STYLE_BODY = 20, STYLE_SHORT_BODY = 21,
            ATTVALUE_VALUE = 22, ATTRIBUTE = 23;
    public static final int
            TAG = 1, SCRIPT = 2, STYLE = 3, ATTVALUE = 4;
    public static final String[] ruleNames = makeRuleNames();
    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;
    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\31\u017d\b\1\b\1" +
                    "\b\1\b\1\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4" +
                    "\t\t\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20" +
                    "\4\21\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27" +
                    "\4\30\t\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36" +
                    "\4\37\t\37\4 \t \4!\t!\4\"\t\"\3\2\3\2\3\2\3\2\3\2\3\2\7\2P\n\2\f\2\16" +
                    "\2S\13\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\7\3^\n\3\f\3\16\3a\13\3\3" +
                    "\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\7\4m\n\4\f\4\16\4p\13\4\3\4\3\4" +
                    "\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\7\5\177\n\5\f\5\16\5\u0082" +
                    "\13\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\7\6\u008c\n\6\f\6\16\6\u008f\13" +
                    "\6\3\6\3\6\3\7\3\7\3\7\3\7\7\7\u0097\n\7\f\7\16\7\u009a\13\7\3\7\3\7\3" +
                    "\7\3\7\3\7\3\7\7\7\u00a2\n\7\f\7\16\7\u00a5\13\7\3\7\3\7\5\7\u00a9\n\7" +
                    "\3\b\3\b\5\b\u00ad\n\b\3\b\6\b\u00b0\n\b\r\b\16\b\u00b1\3\t\3\t\3\t\3" +
                    "\t\3\t\3\t\3\t\3\t\3\t\7\t\u00bd\n\t\f\t\16\t\u00c0\13\t\3\t\3\t\3\t\3" +
                    "\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\7\n\u00ce\n\n\f\n\16\n\u00d1\13\n\3" +
                    "\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\f\6\f\u00dc\n\f\r\f\16\f\u00dd\3" +
                    "\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\20\3\20" +
                    "\3\21\3\21\7\21\u00f1\n\21\f\21\16\21\u00f4\13\21\3\22\3\22\3\22\3\22" +
                    "\3\23\3\23\3\24\3\24\3\25\3\25\3\25\3\25\5\25\u0102\n\25\3\26\5\26\u0105" +
                    "\n\26\3\27\7\27\u0108\n\27\f\27\16\27\u010b\13\27\3\27\3\27\3\27\3\27" +
                    "\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\30\7\30\u011a\n\30\f\30\16" +
                    "\30\u011d\13\30\3\30\3\30\3\30\3\30\3\30\3\30\3\31\7\31\u0126\n\31\f\31" +
                    "\16\31\u0129\13\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3" +
                    "\31\3\32\7\32\u0137\n\32\f\32\16\32\u013a\13\32\3\32\3\32\3\32\3\32\3" +
                    "\32\3\32\3\33\7\33\u0143\n\33\f\33\16\33\u0146\13\33\3\33\3\33\3\33\3" +
                    "\33\3\34\3\34\3\34\3\34\3\34\5\34\u0151\n\34\3\35\6\35\u0154\n\35\r\35" +
                    "\16\35\u0155\3\35\5\35\u0159\n\35\3\36\5\36\u015c\n\36\3\37\3\37\6\37" +
                    "\u0160\n\37\r\37\16\37\u0161\3 \6 \u0165\n \r \16 \u0166\3 \5 \u016a\n" +
                    " \3!\3!\7!\u016e\n!\f!\16!\u0171\13!\3!\3!\3\"\3\"\7\"\u0177\n\"\f\"\16" +
                    "\"\u017a\13\"\3\"\3\"\17Q_n\u0080\u008d\u0098\u00a3\u00be\u00cf\u0109" +
                    "\u011b\u0127\u0138\2#\7\3\t\4\13\5\r\6\17\7\21\b\23\t\25\n\27\13\31\f" +
                    "\33\r\35\16\37\17!\20#\21%\22\'\23)\2+\2-\2/\2\61\24\63\25\65\26\67\27" +
                    "9\30;\31=\2?\2A\2C\2E\2G\2\7\2\3\4\5\6\r\4\2\13\13\"\"\3\2>>\5\2\13\f" +
                    "\17\17\"\"\5\2\62;CHch\3\2\62;\4\2/\60aa\5\2\u00b9\u00b9\u0302\u0371\u2041" +
                    "\u2042\n\2<<C\\c|\u2072\u2191\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2" +
                    "\uffff\t\2%%-=??AAC\\aac|\4\2$$>>\4\2))>>\2\u0190\2\7\3\2\2\2\2\t\3\2" +
                    "\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2" +
                    "\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\3\35\3\2\2\2\3\37\3" +
                    "\2\2\2\3!\3\2\2\2\3#\3\2\2\2\3%\3\2\2\2\3\'\3\2\2\2\4\61\3\2\2\2\4\63" +
                    "\3\2\2\2\5\65\3\2\2\2\5\67\3\2\2\2\69\3\2\2\2\6;\3\2\2\2\7I\3\2\2\2\t" +
                    "X\3\2\2\2\13e\3\2\2\2\rs\3\2\2\2\17\u0087\3\2\2\2\21\u00a8\3\2\2\2\23" +
                    "\u00af\3\2\2\2\25\u00b3\3\2\2\2\27\u00c5\3\2\2\2\31\u00d6\3\2\2\2\33\u00db" +
                    "\3\2\2\2\35\u00df\3\2\2\2\37\u00e3\3\2\2\2!\u00e8\3\2\2\2#\u00ea\3\2\2" +
                    "\2%\u00ee\3\2\2\2\'\u00f5\3\2\2\2)\u00f9\3\2\2\2+\u00fb\3\2\2\2-\u0101" +
                    "\3\2\2\2/\u0104\3\2\2\2\61\u0109\3\2\2\2\63\u011b\3\2\2\2\65\u0127\3\2" +
                    "\2\2\67\u0138\3\2\2\29\u0144\3\2\2\2;\u0150\3\2\2\2=\u0153\3\2\2\2?\u015b" +
                    "\3\2\2\2A\u015d\3\2\2\2C\u0164\3\2\2\2E\u016b\3\2\2\2G\u0174\3\2\2\2I" +
                    "J\7>\2\2JK\7#\2\2KL\7/\2\2LM\7/\2\2MQ\3\2\2\2NP\13\2\2\2ON\3\2\2\2PS\3" +
                    "\2\2\2QR\3\2\2\2QO\3\2\2\2RT\3\2\2\2SQ\3\2\2\2TU\7/\2\2UV\7/\2\2VW\7@" +
                    "\2\2W\b\3\2\2\2XY\7>\2\2YZ\7#\2\2Z[\7]\2\2[_\3\2\2\2\\^\13\2\2\2]\\\3" +
                    "\2\2\2^a\3\2\2\2_`\3\2\2\2_]\3\2\2\2`b\3\2\2\2a_\3\2\2\2bc\7_\2\2cd\7" +
                    "@\2\2d\n\3\2\2\2ef\7>\2\2fg\7A\2\2gh\7z\2\2hi\7o\2\2ij\7n\2\2jn\3\2\2" +
                    "\2km\13\2\2\2lk\3\2\2\2mp\3\2\2\2no\3\2\2\2nl\3\2\2\2oq\3\2\2\2pn\3\2" +
                    "\2\2qr\7@\2\2r\f\3\2\2\2st\7>\2\2tu\7#\2\2uv\7]\2\2vw\7E\2\2wx\7F\2\2" +
                    "xy\7C\2\2yz\7V\2\2z{\7C\2\2{|\7]\2\2|\u0080\3\2\2\2}\177\13\2\2\2~}\3" +
                    "\2\2\2\177\u0082\3\2\2\2\u0080\u0081\3\2\2\2\u0080~\3\2\2\2\u0081\u0083" +
                    "\3\2\2\2\u0082\u0080\3\2\2\2\u0083\u0084\7_\2\2\u0084\u0085\7_\2\2\u0085" +
                    "\u0086\7@\2\2\u0086\16\3\2\2\2\u0087\u0088\7>\2\2\u0088\u0089\7#\2\2\u0089" +
                    "\u008d\3\2\2\2\u008a\u008c\13\2\2\2\u008b\u008a\3\2\2\2\u008c\u008f\3" +
                    "\2\2\2\u008d\u008e\3\2\2\2\u008d\u008b\3\2\2\2\u008e\u0090\3\2\2\2\u008f" +
                    "\u008d\3\2\2\2\u0090\u0091\7@\2\2\u0091\20\3\2\2\2\u0092\u0093\7>\2\2" +
                    "\u0093\u0094\7A\2\2\u0094\u0098\3\2\2\2\u0095\u0097\13\2\2\2\u0096\u0095" +
                    "\3\2\2\2\u0097\u009a\3\2\2\2\u0098\u0099\3\2\2\2\u0098\u0096\3\2\2\2\u0099" +
                    "\u009b\3\2\2\2\u009a\u0098\3\2\2\2\u009b\u009c\7A\2\2\u009c\u00a9\7@\2" +
                    "\2\u009d\u009e\7>\2\2\u009e\u009f\7\'\2\2\u009f\u00a3\3\2\2\2\u00a0\u00a2" +
                    "\13\2\2\2\u00a1\u00a0\3\2\2\2\u00a2\u00a5\3\2\2\2\u00a3\u00a4\3\2\2\2" +
                    "\u00a3\u00a1\3\2\2\2\u00a4\u00a6\3\2\2\2\u00a5\u00a3\3\2\2\2\u00a6\u00a7" +
                    "\7\'\2\2\u00a7\u00a9\7@\2\2\u00a8\u0092\3\2\2\2\u00a8\u009d\3\2\2\2\u00a9" +
                    "\22\3\2\2\2\u00aa\u00b0\t\2\2\2\u00ab\u00ad\7\17\2\2\u00ac\u00ab\3\2\2" +
                    "\2\u00ac\u00ad\3\2\2\2\u00ad\u00ae\3\2\2\2\u00ae\u00b0\7\f\2\2\u00af\u00aa" +
                    "\3\2\2\2\u00af\u00ac\3\2\2\2\u00b0\u00b1\3\2\2\2\u00b1\u00af\3\2\2\2\u00b1" +
                    "\u00b2\3\2\2\2\u00b2\24\3\2\2\2\u00b3\u00b4\7>\2\2\u00b4\u00b5\7u\2\2" +
                    "\u00b5\u00b6\7e\2\2\u00b6\u00b7\7t\2\2\u00b7\u00b8\7k\2\2\u00b8\u00b9" +
                    "\7r\2\2\u00b9\u00ba\7v\2\2\u00ba\u00be\3\2\2\2\u00bb\u00bd\13\2\2\2\u00bc" +
                    "\u00bb\3\2\2\2\u00bd\u00c0\3\2\2\2\u00be\u00bf\3\2\2\2\u00be\u00bc\3\2" +
                    "\2\2\u00bf\u00c1\3\2\2\2\u00c0\u00be\3\2\2\2\u00c1\u00c2\7@\2\2\u00c2" +
                    "\u00c3\3\2\2\2\u00c3\u00c4\b\t\2\2\u00c4\26\3\2\2\2\u00c5\u00c6\7>\2\2" +
                    "\u00c6\u00c7\7u\2\2\u00c7\u00c8\7v\2\2\u00c8\u00c9\7{\2\2\u00c9\u00ca" +
                    "\7n\2\2\u00ca\u00cb\7g\2\2\u00cb\u00cf\3\2\2\2\u00cc\u00ce\13\2\2\2\u00cd" +
                    "\u00cc\3\2\2\2\u00ce\u00d1\3\2\2\2\u00cf\u00d0\3\2\2\2\u00cf\u00cd\3\2" +
                    "\2\2\u00d0\u00d2\3\2\2\2\u00d1\u00cf\3\2\2\2\u00d2\u00d3\7@\2\2\u00d3" +
                    "\u00d4\3\2\2\2\u00d4\u00d5\b\n\3\2\u00d5\30\3\2\2\2\u00d6\u00d7\7>\2\2" +
                    "\u00d7\u00d8\3\2\2\2\u00d8\u00d9\b\13\4\2\u00d9\32\3\2\2\2\u00da\u00dc" +
                    "\n\3\2\2\u00db\u00da\3\2\2\2\u00dc\u00dd\3\2\2\2\u00dd\u00db\3\2\2\2\u00dd" +
                    "\u00de\3\2\2\2\u00de\34\3\2\2\2\u00df\u00e0\7@\2\2\u00e0\u00e1\3\2\2\2" +
                    "\u00e1\u00e2\b\r\5\2\u00e2\36\3\2\2\2\u00e3\u00e4\7\61\2\2\u00e4\u00e5" +
                    "\7@\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e7\b\16\5\2\u00e7 \3\2\2\2\u00e8" +
                    "\u00e9\7\61\2\2\u00e9\"\3\2\2\2\u00ea\u00eb\7?\2\2\u00eb\u00ec\3\2\2\2" +
                    "\u00ec\u00ed\b\20\6\2\u00ed$\3\2\2\2\u00ee\u00f2\5/\26\2\u00ef\u00f1\5" +
                    "-\25\2\u00f0\u00ef\3\2\2\2\u00f1\u00f4\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f2" +
                    "\u00f3\3\2\2\2\u00f3&\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f5\u00f6\t\4\2\2" +
                    "\u00f6\u00f7\3\2\2\2\u00f7\u00f8\b\22\7\2\u00f8(\3\2\2\2\u00f9\u00fa\t" +
                    "\5\2\2\u00fa*\3\2\2\2\u00fb\u00fc\t\6\2\2\u00fc,\3\2\2\2\u00fd\u0102\5" +
                    "/\26\2\u00fe\u0102\t\7\2\2\u00ff\u0102\5+\24\2\u0100\u0102\t\b\2\2\u0101" +
                    "\u00fd\3\2\2\2\u0101\u00fe\3\2\2\2\u0101\u00ff\3\2\2\2\u0101\u0100\3\2" +
                    "\2\2\u0102.\3\2\2\2\u0103\u0105\t\t\2\2\u0104\u0103\3\2\2\2\u0105\60\3" +
                    "\2\2\2\u0106\u0108\13\2\2\2\u0107\u0106\3\2\2\2\u0108\u010b\3\2\2\2\u0109" +
                    "\u010a\3\2\2\2\u0109\u0107\3\2\2\2\u010a\u010c\3\2\2\2\u010b\u0109\3\2" +
                    "\2\2\u010c\u010d\7>\2\2\u010d\u010e\7\61\2\2\u010e\u010f\7u\2\2\u010f" +
                    "\u0110\7e\2\2\u0110\u0111\7t\2\2\u0111\u0112\7k\2\2\u0112\u0113\7r\2\2" +
                    "\u0113\u0114\7v\2\2\u0114\u0115\7@\2\2\u0115\u0116\3\2\2\2\u0116\u0117" +
                    "\b\27\5\2\u0117\62\3\2\2\2\u0118\u011a\13\2\2\2\u0119\u0118\3\2\2\2\u011a" +
                    "\u011d\3\2\2\2\u011b\u011c\3\2\2\2\u011b\u0119\3\2\2\2\u011c\u011e\3\2" +
                    "\2\2\u011d\u011b\3\2\2\2\u011e\u011f\7>\2\2\u011f\u0120\7\61\2\2\u0120" +
                    "\u0121\7@\2\2\u0121\u0122\3\2\2\2\u0122\u0123\b\30\5\2\u0123\64\3\2\2" +
                    "\2\u0124\u0126\13\2\2\2\u0125\u0124\3\2\2\2\u0126\u0129\3\2\2\2\u0127" +
                    "\u0128\3\2\2\2\u0127\u0125\3\2\2\2\u0128\u012a\3\2\2\2\u0129\u0127\3\2" +
                    "\2\2\u012a\u012b\7>\2\2\u012b\u012c\7\61\2\2\u012c\u012d\7u\2\2\u012d" +
                    "\u012e\7v\2\2\u012e\u012f\7{\2\2\u012f\u0130\7n\2\2\u0130\u0131\7g\2\2" +
                    "\u0131\u0132\7@\2\2\u0132\u0133\3\2\2\2\u0133\u0134\b\31\5\2\u0134\66" +
                    "\3\2\2\2\u0135\u0137\13\2\2\2\u0136\u0135\3\2\2\2\u0137\u013a\3\2\2\2" +
                    "\u0138\u0139\3\2\2\2\u0138\u0136\3\2\2\2\u0139\u013b\3\2\2\2\u013a\u0138" +
                    "\3\2\2\2\u013b\u013c\7>\2\2\u013c\u013d\7\61\2\2\u013d\u013e\7@\2\2\u013e" +
                    "\u013f\3\2\2\2\u013f\u0140\b\32\5\2\u01408\3\2\2\2\u0141\u0143\7\"\2\2" +
                    "\u0142\u0141\3\2\2\2\u0143\u0146\3\2\2\2\u0144\u0142\3\2\2\2\u0144\u0145" +
                    "\3\2\2\2\u0145\u0147\3\2\2\2\u0146\u0144\3\2\2\2\u0147\u0148\5;\34\2\u0148" +
                    "\u0149\3\2\2\2\u0149\u014a\b\33\5\2\u014a:\3\2\2\2\u014b\u0151\5E!\2\u014c" +
                    "\u0151\5G\"\2\u014d\u0151\5=\35\2\u014e\u0151\5A\37\2\u014f\u0151\5C " +
                    "\2\u0150\u014b\3\2\2\2\u0150\u014c\3\2\2\2\u0150\u014d\3\2\2\2\u0150\u014e" +
                    "\3\2\2\2\u0150\u014f\3\2\2\2\u0151<\3\2\2\2\u0152\u0154\5?\36\2\u0153" +
                    "\u0152\3\2\2\2\u0154\u0155\3\2\2\2\u0155\u0153\3\2\2\2\u0155\u0156\3\2" +
                    "\2\2\u0156\u0158\3\2\2\2\u0157\u0159\7\"\2\2\u0158\u0157\3\2\2\2\u0158" +
                    "\u0159\3\2\2\2\u0159>\3\2\2\2\u015a\u015c\t\n\2\2\u015b\u015a\3\2\2\2" +
                    "\u015c@\3\2\2\2\u015d\u015f\7%\2\2\u015e\u0160\t\5\2\2\u015f\u015e\3\2" +
                    "\2\2\u0160\u0161\3\2\2\2\u0161\u015f\3\2\2\2\u0161\u0162\3\2\2\2\u0162" +
                    "B\3\2\2\2\u0163\u0165\t\6\2\2\u0164\u0163\3\2\2\2\u0165\u0166\3\2\2\2" +
                    "\u0166\u0164\3\2\2\2\u0166\u0167\3\2\2\2\u0167\u0169\3\2\2\2\u0168\u016a" +
                    "\7\'\2\2\u0169\u0168\3\2\2\2\u0169\u016a\3\2\2\2\u016aD\3\2\2\2\u016b" +
                    "\u016f\7$\2\2\u016c\u016e\n\13\2\2\u016d\u016c\3\2\2\2\u016e\u0171\3\2" +
                    "\2\2\u016f\u016d\3\2\2\2\u016f\u0170\3\2\2\2\u0170\u0172\3\2\2\2\u0171" +
                    "\u016f\3\2\2\2\u0172\u0173\7$\2\2\u0173F\3\2\2\2\u0174\u0178\7)\2\2\u0175" +
                    "\u0177\n\f\2\2\u0176\u0175\3\2\2\2\u0177\u017a\3\2\2\2\u0178\u0176\3\2" +
                    "\2\2\u0178\u0179\3\2\2\2\u0179\u017b\3\2\2\2\u017a\u0178\3\2\2\2\u017b" +
                    "\u017c\7)\2\2\u017cH\3\2\2\2&\2\3\4\5\6Q_n\u0080\u008d\u0098\u00a3\u00a8" +
                    "\u00ac\u00af\u00b1\u00be\u00cf\u00dd\u00f2\u0101\u0104\u0109\u011b\u0127" +
                    "\u0138\u0144\u0150\u0155\u0158\u015b\u0161\u0166\u0169\u016f\u0178\b\7" +
                    "\4\2\7\5\2\7\3\2\6\2\2\7\6\2\2\3\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());
    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    private static final String[] _LITERAL_NAMES = makeLiteralNames();
    private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };
    public static String[] modeNames = {
            "DEFAULT_MODE", "TAG", "SCRIPT", "STYLE", "ATTVALUE"
    };

    static {
        RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION);
    }

    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }

    public HTMLLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    private static String[] makeRuleNames() {
        return new String[]{
                "HTML_COMMENT", "HTML_CONDITIONAL_COMMENT", "XML", "CDATA", "DTD", "SCRIPTLET",
                "SEA_WS", "SCRIPT_OPEN", "STYLE_OPEN", "TAG_OPEN", "HTML_TEXT", "TAG_CLOSE",
                "TAG_SLASH_CLOSE", "TAG_SLASH", "TAG_EQUALS", "TAG_NAME", "TAG_WHITESPACE",
                "HEXDIGIT", "DIGIT", "TAG_NameChar", "TAG_NameStartChar", "SCRIPT_BODY",
                "SCRIPT_SHORT_BODY", "STYLE_BODY", "STYLE_SHORT_BODY", "ATTVALUE_VALUE",
                "ATTRIBUTE", "ATTCHARS", "ATTCHAR", "HEXCHARS", "DECCHARS", "DOUBLE_QUOTE_STRING",
                "SINGLE_QUOTE_STRING"
        };
    }

    private static String[] makeLiteralNames() {
        return new String[]{
                null, null, null, null, null, null, null, null, null, null, "'<'", null,
                "'>'", "'/>'", "'/'", "'='"
        };
    }

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "HTML_COMMENT", "HTML_CONDITIONAL_COMMENT", "XML", "CDATA", "DTD",
                "SCRIPTLET", "SEA_WS", "SCRIPT_OPEN", "STYLE_OPEN", "TAG_OPEN", "HTML_TEXT",
                "TAG_CLOSE", "TAG_SLASH_CLOSE", "TAG_SLASH", "TAG_EQUALS", "TAG_NAME",
                "TAG_WHITESPACE", "SCRIPT_BODY", "SCRIPT_SHORT_BODY", "STYLE_BODY", "STYLE_SHORT_BODY",
                "ATTVALUE_VALUE", "ATTRIBUTE"
        };
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String getGrammarFileName() {
        return "HTMLLexer.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public String[] getChannelNames() {
        return channelNames;
    }

    @Override
    public String[] getModeNames() {
        return modeNames;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }
}
