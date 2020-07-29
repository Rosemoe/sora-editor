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


import java.util.Stack;

/**
 * The S5dFormatter is a simple formatter for S5droid language code
 * It provides a lots of code styles for users to choose
 * The ability of this class is based on the {@link S5dTextTokenizer} and {@link Tokens}
 * <strong>More detail about the styles please see the comment on fields!!!</strong>
 *
 * @author Rose
 */
public class S5dFormatter {

    public final static int DEFAULT_STYLE_1 = 4;

    /* The source of code */
    private S5dTextTokenizer lexer;

    /* Whether add white space between operator and expression */
    private boolean style0;

    /* Indent count on new line */
    private int style1;

    /* Whether use tab to create indent */
    private boolean style2;

    /* Whether take down the error while formatting */
    private boolean style3;

    /* Use user spaces between END and END target  */
    private boolean style4;

    /* Indent empty line */
    private boolean style5;

    /* Whether we have finished the work */
    private boolean finished;

    /* Whether we have started(even we have finished) */
    private boolean started;

    /* Listener */
    private FormatListener listener;

    /* Name */
    private String name;

    /* Result cache */
    private StringBuilder sb;

    /* Log cache */
    private StringBuilder log;
    private int errors;

    /* Stack for layer */
    private Stack<Tokens> stack;
    private Stack<Integer> cases;
    private Stack<Integer> case2;

    /**
     * Create a formatter from a String object
     *
     * @param content The content of S5droid language code
     */
    public S5dFormatter(CharSequence content) {
        this(new S5dTextTokenizer(content));
    }

    /**
     * This will create a new formatter from the given S5dLexer
     * To ensure the target S5dLexer is at the start of content,we have made it private
     *
     * @param lexer The lexer
     */
    private S5dFormatter(S5dTextTokenizer lexer) {
        if (lexer == null) {
            throw new IllegalArgumentException("lexer can not be null");
        }
        this.lexer = lexer;
        setName("s5droid formatter");
        setStyle0(true);
        setStyle1(S5dFormatter.DEFAULT_STYLE_1);
        setStyle2(true);
        setStyle3(true);
        setStyle4(true);
        setStyle5(false);
        errors = 0;
        synchronized (this) {
            this.finished = false;
            this.started = false;
        }
    }

    /*
     * These are style controling methods
     */

    public void setStyle0(boolean enabled) {
        style0 = enabled;
    }

    public void setStyle1(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length can not be under zero");
        }
        style1 = length;
    }

    public void setStyle2(boolean enabled) {
        style2 = enabled;
    }

    public void setStyle3(boolean enabled) {
        style3 = enabled;
    }

    public void setStyle4(boolean enabled) {
        style4 = enabled;
    }

    public void setStyle5(boolean enabled) {
        style5 = enabled;
    }

    /**
     * Format the code now
     */
    @SuppressWarnings("incomplete-switch")
    public void format() {
        if (isStarted()) {
            throw new IllegalStateException("format() has been called");
        }
        synchronized (this) {
            this.started = true;
            final FormatListener lis = this.listener;
            if (lis != null) {
                lis.onStarted(this);
            }
        }

        sb = new StringBuilder();
        stack = new Stack<>();
        cases = new Stack<>();
        case2 = new Stack<>();
        lexer.setCalculateLineColumn(true);

        int layer = 0;

        Tokens lastToken = Tokens.NEWLINE;
        Tokens currentToken;

        Tokens cacheToken = Tokens.NEWLINE;
        String cacheTokenContent = null;

        while ((currentToken = (cacheToken != Tokens.NEWLINE ? cacheToken : lexer.nextToken())) != Tokens.EOF) {
            //Do not call yytext() for too many times.It will cost more time to create new String
            String content;
            if (cacheToken != Tokens.NEWLINE) {
                content = cacheTokenContent;
                //Reset cache state
                cacheToken = Tokens.NEWLINE;
                cacheTokenContent = "";
            } else {
                content = (String) lexer.getTokenString();
            }

            //Just append it and do not update the tokens
            if (currentToken == Tokens.LONG_COMMENT || currentToken == Tokens.LINE_COMMENT) {
                int len = content.length();
                while (Character.isWhitespace(content.charAt(len - 1))) {
                    len--;
                }
                sb.append(content.substring(0, len));
                lexer.pushBack(content.length() - len);
                continue;
            }

            //Add space to operator and operator target
            if (style0 && (lastToken != Tokens.LINE_COMMENT && lastToken != Tokens.LONG_COMMENT)) {
                //Ensure the space will only be added once
                if (isOperator(currentToken) && lastToken != Tokens.WHITESPACE && lastToken != Tokens.NEWLINE) {
                    sb.append(' ');
                } else if (currentToken != Tokens.WHITESPACE && currentToken != Tokens.NEWLINE && !isSeparator(currentToken) && !isOperator(currentToken) && isOperator(lastToken)) {
                    sb.append(' ');
                } else if (lastToken == Tokens.COMP && currentToken != Tokens.WHITESPACE) {
                    sb.append(' ');
                }
            }

            //Add tabs and spaces to new lines
            if (currentToken == Tokens.NEWLINE) {
                sb.append('\n');
                Tokens token;
                while ((token = lexer.nextToken()) == Tokens.WHITESPACE || token == Tokens.NEWLINE) {
                    //Do not indent the black lines
                    //As well as clear useless spaces on empty line
                    if (token == Tokens.NEWLINE) {
                        sb.append('\n');
                        if (style5) {
                            appendIndent(layer);
                        }
                    }
                }
                if (!style5) {
                    appendIndent(layer);
                }
                lastToken = Tokens.NEWLINE;
                cacheToken = token;
                cacheTokenContent = (String) lexer.getTokenString();
                continue;
            }

            //Make advances
            {

                switch (currentToken) {
                    case EVENT:
                    case METHOD:
                    case IF:
                    case SWITCH:
                    case WHILELOOP:
                    case FORLOOP:
                    case TRY:
                        if (lastToken != Tokens.NEWLINE) {
                            sb.append('\n');
                            appendIndent(layer);
                        }
                        layer++;
                        if (currentToken == Tokens.WHILELOOP || currentToken == Tokens.FORLOOP) {
                            stack.push(Tokens.LOOP);
                        } else if (currentToken == Tokens.TRY) {
                            stack.push(Tokens.SIMPLE_TRY);
                        } else {
                            stack.push(currentToken);
                        }
                        if (currentToken == Tokens.SWITCH) {
                            cases.add(0);
                            case2.add(0);
                        }
                        break;
                    case ELSE:
                    case ELSEIF:
                    case CATCH:
                        layer--;
                        if (lastToken != Tokens.NEWLINE) {
                            sb.append('\n');
                        } else {
                            trackToStartOfLine();
                        }
                        appendIndent(layer);
                        layer++;
                        break;
                    case CASE:
                        if (!case2.isEmpty()) {
                            int top = case2.lastElement();
                            if (top != 0) {
                                layer--;
                                if (lastToken != Tokens.NEWLINE) {
                                    sb.append('\n');
                                } else {
                                    trackToStartOfLine();
                                }
                                appendIndent(layer);
                            }
                            case2.pop();
                            case2.add(top + 1);
                            cases.add(cases.pop() + 1);
                        } else {
                            error("非法的分支定义" + lexer.yyDesc());
                        }
                }

                if (currentToken == Tokens.COLON && !cases.isEmpty()) {
                    int colon = cases.lastElement();
                    if (colon > 0) {
                        layer++;
                        cases.pop();
                        cases.add(colon - 1);
                    }
                }

                if (currentToken == Tokens.END) {
                    StringBuilder local = new StringBuilder();
                    local.append(content);
                    Tokens token;
                    while ((token = lexer.nextToken()) == Tokens.WHITESPACE) {
                        if (style4) {
                            local.append(lexer.getTokenString());
                        }
                    }
                    if (!style4) {
                        local.append(' ');
                    }
                    local.append(lexer.getTokenString());
                    //Pop layers
                    layer -= popTop(token);

                    if (layer < 0) {
                        layer = 0;
                    }
                    if (lastToken != Tokens.NEWLINE) {
                        sb.append('\n');
                    } else {
                        trackToStartOfLine();
                    }
                    appendIndent(layer);

                    sb.append(local);

                    lastToken = currentToken;
                    continue;
                }

                if (layer < 0) {
                    layer = 0;
                }
            }

            sb.append(content);

            //Update last token
            lastToken = currentToken;
        }

        while (!stack.isEmpty()) {
            error("代码中有未结束的代码块:" + stack.pop() + lexer.yyDesc());
        }

        synchronized (this) {
            this.finished = true;
            final FormatListener lis = this.listener;
            if (lis != null) {
                lis.onFinished(this);
            }
        }
    }

    /**
     * Pop the layer(s) for given token
     *
     * @param token The token to pop
     * @return layer decrease count
     */
    private int popTop(Tokens token) {
        if (stack.isEmpty()) {
            error("不能继续结束代码块了:预料之外的 : " + getBlockName(token) + lexer.yyDesc());
            return 0;
        }
        //This is will cancel the illegal code!!
        int count = 0;
        Tokens top = stack.lastElement();
        if (top != token) {
            int index = stack.size() - 1;
            while (index != -1) {
                if (stack.elementAt(index) == token) {
                    break;
                }
                index--;
            }
            if (index == -1) {
                count = 1;
                error("预料之外的 : " + getBlockName(token) + ",应该在此处的是 " + getBlockName(top) + lexer.yyDesc());
            } else {
                StringBuilder local = new StringBuilder("在 ").append(getBlockName(token)).append(" 结束前需要先结束:");
                while (stack.size() > index) {
                    Tokens tokenPop;
                    local.append(getBlockName(tokenPop = stack.pop())).append(',');
                    if (tokenPop == Tokens.SWITCH) {
                        cases.pop();
                        case2.pop();
                        count++;
                    }
                    count++;
                }
                local.deleteCharAt(local.length() - 1);
                while (local.charAt(local.length() - 1) != ',') {
                    local.deleteCharAt(local.length() - 1);
                }
                local.deleteCharAt(local.length() - 1);
                local.append(lexer.yyDesc());
                error(local.toString());
            }
        } else {
            count = 1;
            stack.pop();
            if (token == Tokens.SWITCH) {
                cases.pop();
                case2.pop();
                count++;
            }
        }
        return count;
    }

    /**
     * Add error message
     *
     * @param msg Message
     */
    private void error(String msg) {
        if (style3) {
            if (log == null) {
                log = new StringBuilder();
            }
            log.append(msg).append('\n');
        }
        errors++;
    }

    /**
     * Delete content in string builder until it reach first character that is not '\t' and ' '
     */
    private void trackToStartOfLine() {
        char ch;
        while (sb.length() > 0 && ((ch = sb.charAt(sb.length() - 1)) == ' ' || ch == '\t')) {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    /**
     * Whether this token is a separator
     *
     * @param token The token to check
     * @return Whether a separator
     */
    private static boolean isSeparator(Tokens token) {
        switch (token) {
            case LPAREN:
            case RPAREN:
            case LBRACE:
            case RBRACE:
            case LBRACK:
            case RBRACK:
            case SEMICOLON:
            case DOT:
            case COMMA:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether this token is an operator
     *
     * @param token Token to check
     * @return Whether a operator
     */
    private static boolean isOperator(Tokens token) {
        switch (token) {
            case DIV:
            case MULT:
            case PLUS:
            case MINUS:
            case MOD:
            case XOR:
            case OROR:
            case ANDAND:
            case AND:
            case OR:
            case ORK:
            case ANDK:
            case LT:
            case LTEQ:
            case GT:
            case GTEQ:
            case EQEQ:
            case NOTEQ:
                return true;
            default:
                return false;
        }
    }

    /**
     * Append spaces and tabs to the string builder for the given layer count
     *
     * @param layer Current layer of code block
     */
    private void appendIndent(int layer) {
        if (style1 == 0 || layer <= 0) {
            return;
        }
        int count = layer * style1;
        if (count == 0) {
            return;
        }
        if (style2) {
            int tab = count / 4;
            int space = count % 4;
            for (int i = 0; i < tab; i++) {
                sb.append('\t');
            }
            for (int i = 0; i < space; i++) {
                sb.append(' ');
            }
        } else {
            for (int i = 0; i < count; i++) {
                sb.append(' ');
            }
        }
    }

    /**
     * Return the name of token
     *
     * @param token Token to get name
     * @return Chinese name of the token
     */
    private static String getBlockName(Tokens token) {
        switch (token) {
            case SIMPLE_TRY:
                return "容错处理";
            case IF:
                return "如果";
            case SWITCH:
                return "判断";
            case LOOP:
                return "循环";
            case EVENT:
                return "事件";
            case METHOD:
                return "方法";
            default:
                return String.valueOf(token);
        }
    }

    /**
     * Whether the process finished
     *
     * @return Whether finished
     */
    public synchronized boolean isFinished() {
        return finished;
    }

    /**
     * Whether the process once started
     *
     * @return Whether started
     */
    public synchronized boolean isStarted() {
        return started;
    }

    /**
     * Get the result of formation
     *
     * @return The result of formation
     */
    public String getResult() {
        if (!isStarted()) {
            throw new IllegalStateException("The work have not started");
        }
        if (!isFinished()) {
            throw new IllegalStateException("The work have not finished");
        }

        return sb.toString();
    }

    /**
     * Get count of error(s)
     *
     * @return Error count
     */
    public int getErrorCount() {
        return errors;
    }

    /**
     * Whether there is error
     *
     * @return Whether error occured in user's code
     */
    public boolean hasError() {
        return getErrorCount() != 0;
    }

    /**
     * Get messages
     * To use this,you'd better enable style3
     *
     * @return errors
     */
    public String getErrorMessage() {
        if (log != null) {
            log.insert(0, "错误数目:" + getErrorCount() + "\n");
        }
        return (log == null ? (hasError() ? "错误数目:" + getErrorCount() + "\n未启用详细信息" : "无错误") : log.toString());
    }

    /**
     * Listener for multiple process
     *
     * @param listener New listener to add
     */
    public void setFormatListener(FormatListener listener) {
        this.listener = listener;
    }

    /**
     * Name for multiple process
     *
     * @param name The name of formatter
     */
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Formatter must have a name");
        }
        this.name = name;
    }

    /**
     * Name for multiple process
     *
     * @return The name of formatter
     */
    public String getName() {
        return this.name;
    }

    /**
     * Interface class
     * Listener for multiple process
     *
     * @author Rose
     */
    public interface FormatListener {

        /**
         * It is to notify you that the given formatter has started
         *
         * @param formatter Formatter starting
         */
        void onStarted(S5dFormatter formatter);

        /**
         * It is to notify you that the given formatter has finished
         *
         * @param formatter Formatter finished
         */
        void onFinished(S5dFormatter formatter);

    }

}


