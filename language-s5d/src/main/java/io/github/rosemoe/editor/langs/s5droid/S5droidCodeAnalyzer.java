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

import android.graphics.Color;

import io.github.rosemoe.editor.langs.internal.TrieTree;
import io.github.rosemoe.editor.widget.EditorColorScheme;
import io.github.rosemoe.editor.struct.Span;
import io.github.rosemoe.editor.text.LineNumberCalculator;
import io.github.rosemoe.editor.text.TextAnalyzer.AnalyzeThread.Delegate;
import io.github.rosemoe.editor.text.TextAnalyzeResult;
import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.struct.BlockLine;
import io.github.rosemoe.editor.struct.NavigationLabel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import static io.github.rosemoe.editor.langs.s5droid.Tokens.CHARACTER_LITERAL;
import static io.github.rosemoe.editor.langs.s5droid.Tokens.END;
import static io.github.rosemoe.editor.langs.s5droid.Tokens.EOF;
import static io.github.rosemoe.editor.langs.s5droid.Tokens.EVENT;
import static io.github.rosemoe.editor.langs.s5droid.Tokens.METHOD;
import static io.github.rosemoe.editor.langs.s5droid.Tokens.NEWLINE;
import static io.github.rosemoe.editor.langs.s5droid.Tokens.VARIANT;
import static io.github.rosemoe.editor.langs.s5droid.Tokens.WHITESPACE;

/**
 * @author Rose
 */
public class S5droidCodeAnalyzer implements CodeAnalyzer {
    public final static String[] basicTools = {
            "应用操作", "文本操作", "文件操作", "上下文操作", "对话框", "进度对话框", "线程", "定时器", "时钟",
            "位运算", "媒体操作", "拼音操作", "数据库操作", "数组操作", "时间操作", "正则表达式", "算术运算",
            "音量操作", "颜色值操作", "像素单位操作", "加解密操作", "压缩操作", "存储卡操作", "系统操作",
            "转换操作", "共享数据", "哈希表", "集合", "网络操作", "组件容器", "事件监听器", "适配器", "JSON操作",
            "事件监听器", "R", "Root操作", "编码操作"
    };

    private final static TrieTree<Tokens> names;
    private final static Comparator<NavigationLabel> NAVI_COMP =
            new Comparator<NavigationLabel>() {

                @Override
                public int compare(NavigationLabel p1, NavigationLabel p2) {
                    return (p1.label).compareTo(p2.label);
                }

            };

    static {
        names = new TrieTree<>();
        for (String s : basicTools) {
            names.put(s, Tokens.VARIANT);
        }
    }

    //Single instance
    //Avoid new object creating
    private final S5dTextTokenizer theTokenizer = new S5dTextTokenizer("");

    private final Stack<BlockLine> blocks = new Stack<>();

    private static boolean checkHexColorForString(CharSequence str, int start, int count) {
        //We must check the whole expression
        //Because the tokenizer will ignore some errors
        //And whether they are hex expression is not checked.
        if (count != 11 && count != 9) {
            return false;
        }
        if (str.charAt(start + 1) != '#') {
            return false;
        }
        if (str.charAt(start + count - 1) != '"') {
            return false;
        }
        for (int i = 2; i < count - 1; i++) {
            if (!isHexChar(str.charAt(i + start))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexChar(char ch) {
        return ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'));
    }

    private static boolean checkHexColor(CharSequence str, int start, int count) {
        //Just check the length and whether it is hex literal
        //Because the hex character check has been done
        //in tokenizer
        return !((count != 10 && count != 8) || str.charAt(start) != '0' || (str.charAt(start + 1) != 'x' && str.charAt(start + 1) != 'X'));
    }

    /*
        States:
        0:Idle
        1:'Var' occurred
        2:Var Name occurred
        3:'As' Occurred
        4:Type occurred,waiting to finish
        5:'.'occurred while waiting,continue.
        -------------------------------------
        6:'Event' occurred for code block beginning
        12:'Method' occurred
        13:idt occurred(method)
        16:first idt occurred
        14:':'occurred
        15:second idt occurred
        7:'(' occurred
        8:Var name occurred
        9:'As' occurred
        10:Var type ocurred ,waiting to finish by ',' or ')'
        11:'.' occurred while waiting,continue
    */
    @Override
    public void analyze(CharSequence contentOrigin, TextAnalyzeResult colors, Delegate delegate) {
        List<NavigationLabel> labels = new ArrayList<>();
        S5droidTree tree = new S5droidTree();
        TrieTree<Tokens> vars = new TrieTree<>();
        StringBuilder content = (contentOrigin instanceof StringBuilder) ? ((StringBuilder) contentOrigin) : new StringBuilder(contentOrigin);
        String name = null;
        //String type = null;
        StringBuilder type = new StringBuilder();
        int varLine = 0;
        int state = 0;
        int suppressSwitch = 1;
        int currSwitch = 1;
        S5dTextTokenizer tokenizer = theTokenizer;
        tokenizer.reset(content);
        //tokenizer.reset(content.toStringBuilder());
        //class {Content} already have the duty.
        //Disable it to make it quicker
        Tokens token;
        Tokens previous = Tokens.UNKNOWN;
        int idx = 0, line = 0, column = 0, length;
        LineNumberCalculator helper = new LineNumberCalculator(content);
        boolean markHex = false;
        while (delegate.shouldAnalyze()) {
            try {
                token = tokenizer.directNextToken();
            } catch (RuntimeException e) {
                //May be inputting spells
                token = CHARACTER_LITERAL;
            }
            if (token == EOF) {
                break;
            }
            length = tokenizer.getTokenLength();
            switch (token) {
                case IDENTIFIER: {
                    boolean add = false;
                    if (state == 1) {
                        state = 2;
                        name = content.substring(idx, idx + length);
                    } else if (state == 3) {
                        state = 4;
                        type.append(content, idx, idx + length);
                    } else if (state == 5) {
                        state = 4;
                        type.append(content, idx, idx + length);
                    } else if (state == 7) {
                        state = 8;
                        add = true;
                        name = content.substring(idx, idx + length);
                    } else if (state == 9) {
                        state = 10;
                        type.append(content, idx, idx + length);
                    } else if (state == 11) {
                        state = 10;
                        type.append(content, idx, idx + length);
                    } else if (state == 6) {
                        state = 16;
                    } else if (state == 12) {
                        state = 13;
                    } else if (state == 14) {
                        state = 15;
                    } else {
                        state = 0;
                        type.setLength(0);
                    }
                    int color = EditorColorScheme.TEXT_NORMAL;
                    if (previous == VARIANT || add) {
                        vars.put(content, idx, length, Tokens.VARIANT);
                        color = EditorColorScheme.IDENTIFIER_VAR;
                    } else if (vars.get(content, idx, length) == Tokens.VARIANT) {
                        color = EditorColorScheme.IDENTIFIER_VAR;
                    } else if (names.get(content, idx, length) == Tokens.VARIANT) {
                        color = EditorColorScheme.IDENTIFIER_NAME;
                    }
                    colors.addIfNeeded(line, column, color);
                    break;
                }
                case CHARACTER_LITERAL:
                case FLOATING_POINT_LITERAL: {
                    state = 0;
                    type.setLength(0);
                    colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                    break;
                }
                case STRING: {
                    state = 0;
                    type.setLength(0);
                    if (checkHexColorForString(content, idx, length)) {
                        colors.add(line, new Span(column, EditorColorScheme.LITERAL).setUnderlineColor(tryParseColor(tokenizer.getTokenString(), false)));
                        //Optimize NEWLINE and WHITESPACE
                        markHex = true;
                    } else {
                        colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                    }
                    break;
                }
                case INTEGER_LITERAL: {
                    state = 0;
                    type.setLength(0);
                    //Here we can use addIfNeeded() because
                    //the integer literal will not be two.
                    if (checkHexColor(content, idx, length)) {
                        colors.add(line, new Span(column, EditorColorScheme.LITERAL).setUnderlineColor(tryParseColor(tokenizer.getTokenString(), true)));
                        //Optimize NEWLINE and WHITESPACE
                        markHex = true;
                    } else {
                        colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                    }
                    break;
                }
                case LONG_COMMENT:
                case LINE_COMMENT: {
                    colors.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                    break;
                }
                case FORLOOP:
                case WHILELOOP:
                case SWITCH:
                case METHOD:
                case EVENT:
                case IF:
                case TRY: {
                    if (previous != END) {
                        if (blocks.size() == 0) {
                            if (suppressSwitch < currSwitch) {
                                suppressSwitch = currSwitch;
                            }
                            currSwitch = 1;
                        } else {
                            currSwitch++;
                        }
                        if (token == METHOD || token == EVENT) {
                            labels.add(new NavigationLabel(line, content.substring(helper.findLineStart(), helper.findLineEnd()).trim()));
                            state = token == EVENT ? 6 : 12;
                        } else {
                            state = 0;
                            type.setLength(0);
                        }
                        BlockLine block = colors.obtainNewBlock();
                        block.startLine = line;
                        block.startColumn = column;
                        blocks.add(block);
                        tree.enterCodeBlock(line);
                    } else {
                        state = 0;
                        type.setLength(0);
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                }
                case ELSEIF:
                case ELSE:
                case CATCH: {
                    if (!blocks.isEmpty()) {
                        BlockLine block = blocks.pop();
                        block.endLine = line;
                        block.endColumn = column;
                        if (block.endLine - block.startLine > 1) {
                            colors.addBlockLine(block);
                        }
                        block = colors.obtainNewBlock();
                        block.startLine = line;
                        block.startColumn = column;
                        blocks.add(block);
                        currSwitch += 2;
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                }
                case END: {
                    if (!blocks.isEmpty()) {
                        BlockLine block = blocks.pop();
                        block.endLine = line;
                        block.endColumn = column;
                        if (block.endLine - block.startLine > 1) {
                            colors.addBlockLine(block);
                        }
                    }
                    state = 0;
                    List<S5droidTree.Node> nodes = tree.exitCodeBlock(line).children;
                    int size = nodes.size();
                    S5droidTree.Node sub;
                    for (int i = 0; i < size; i++) {
                        sub = nodes.get(i);
                        if (!sub.isBlock) {
                            vars.put(sub.varName, null);
                        }
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                }
                case LONGV:
                case DOUBLEV:
                case FLOATV:
                case BOOLEANV:
                case INTV:
                case STRINGV:
                case OBJECT:
                case CHARV: {
                    if (state == 3) {
                        state = 4;
                        type.append(content, idx, idx + length);
                    } else if (state == 9) {
                        state = 10;
                        type.append(content, idx, idx + length);
                    } else {
                        state = 0;
                        type.setLength(0);
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                }
                case LBRACK: {
                    if (state == 4 || state == 10) {
                        type.append('[');
                    } else {
                        state = 0;
                        type.setLength(0);
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                case RBRACK: {
                    if (state == 4 || state == 10) {
                        type.append(']');
                    } else {
                        state = 0;
                        type.setLength(0);
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                case VARIANT: {
                    state = 1;
                    varLine = line;
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                }
                case AS: {
                    if (state == 2) {
                        state = 3;
                    } else if (state == 8) {
                        state = 9;
                    } else {
                        state = 0;
                        type.setLength(0);
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                }
                case DOT: {
                    if (state == 4) {
                        state = 5;
                        type.append('.');
                    } else if (state == 10) {
                        state = 11;
                        type.append('.');
                    } else {
                        state = 0;
                        type.setLength(0);
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                case LPAREN: {
                    if (state == 15 || state == 13) {
                        state = 7;
                    } else {
                        state = 0;
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                case COMMA: {
                    if (state == 10) {
                        state = 7;
                        if (type.length() != 0) {
                            tree.addVariant(line, name, type.toString());
                            type.setLength(0);
                        }
                    } else {
                        state = 0;
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                case RPAREN: {
                    if (state == 10) {
                        if (type.length() != 0) {
                            tree.addVariant(line, name, type.toString());
                            type.setLength(0);
                        }
                    }
                    state = 0;
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                case COLON: {
                    if (state == 16) {
                        state = 14;
                    } else {
                        state = 0;
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                case STATIC:
                case CASE:
                case LOOP:
                case RETURN:
                case NEW:
                case NULL:
                case FALSE:
                case TRUE:
                case TO:
                case THEN:
                case ANDK:
                case ORK:
                case FORWARD:
                case BACK:
                case SIMPLE_TRY:
                case THIS:
                case ASSERT:
                case BREAK:
                case CONTINUE:
                case INSTANCEOF: {
                    state = 0;
                    type.setLength(0);
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                }
                case EQ:
                case SEMICOLON: {
                    if (type.length() != 0) {
                        tree.addVariant(varLine, name, type.toString());
                        type.setLength(0);
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    break;
                }
                default: {
                    if (token != NEWLINE && token != WHITESPACE) {
                        state = 0;
                        type.setLength(0);
                        colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                    } else {
                        if (markHex) {
                            colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                            markHex = false;
                        }
                    }
                }
            }
            if (token != NEWLINE && token != WHITESPACE) {
                previous = token;
            }
            idx = idx + length;
            helper.update(length);
            line = helper.getLine();
            column = helper.getColumn();
        }
        colors.determine(line);
        if (currSwitch > suppressSwitch) {
            suppressSwitch = currSwitch;
        }
        //A suppress switch is to optimize the process
        //of drawing block line and finding cursor block
        //You should give the max code block count(as well as children,Layer >= 1)
        //in one code block(Layer = 1)
        //If you are unsure,do not set it
        colors.setSuppressSwitch(suppressSwitch * 2);
        //This can be slow when the data is large
        //If you do not want to sort them,please disable it
        Collections.sort(labels, NAVI_COMP);
        colors.setNavigation(labels);
        colors.mExtra = tree;
        blocks.clear();
    }

    private static int tryParseColor(CharSequence color, boolean type) {
        String colorStr;
        try {
            if(type) {
                //Number
                colorStr = "#" + color.subSequence(2, color.length());
            } else {
                //String
                color = color.subSequence(1, color.length() - 1);
                colorStr = color instanceof String ? (String)color : color.toString();
            }
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            return 0;
        }
    }

}

