package com.rose.editor.langs.java;

import com.rose.editor.android.ColorScheme;
import com.rose.editor.common.LineNumberHelper;
import com.rose.editor.common.TextColorProvider;
import com.rose.editor.interfaces.CodeAnalyzer;
import com.rose.editor.langs.IdentifierAutoComplete;
import com.rose.editor.simpleclass.BlockLine;
import com.rose.editor.simpleclass.NavigationLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Note:Navigation not supported
 * @author Rose
 */
public class JavaCodeAnalyzer implements CodeAnalyzer {

    @Override
    public void analyze(CharSequence content, TextColorProvider.TextColors colors, TextColorProvider.AnalyzeThread.Delegate delegate) {
        StringBuilder text = content instanceof StringBuilder ? (StringBuilder)content : new StringBuilder(content);
        JavaTextTokenizer tokenizer = new JavaTextTokenizer(text);
        tokenizer.setCalculateLineColumn(false);
        Tokens token;
        int index = 0,line = 0,column = 0;
        LineNumberHelper helper = new LineNumberHelper(text);
        IdentifierAutoComplete.Identifiers identifiers = new IdentifierAutoComplete.Identifiers();
        identifiers.begin();
        Stack<BlockLine> stack = new Stack<>();
        List<NavigationLabel> labels = new ArrayList<>();
        int maxSwitch = 1,currSwitch = 0;
        while(!delegate.shouldReAnalyze()) {
            try{
                token = tokenizer.directNextToken();
            }catch (RuntimeException e) {
                if(e instanceof IndexOutOfBoundsException) {
                    break;
                }
                token = Tokens.CHARACTER_LITERAL;
            }
            switch(token) {
                case WHITESPACE:
                case NEWLINE:
                    break;
                case IDENTIFIER:
                    colors.addIfNeeded(index,line,column,ColorScheme.TEXT_NORMAL);
                    identifiers.addIdentifier(text.substring(tokenizer.getIndex(),tokenizer.getTokenLength() + tokenizer.getIndex()));
                    break;
                case CHARACTER_LITERAL:
                case STRING:
                case FLOATING_POINT_LITERAL:
                case INTEGER_LITERAL:
                    colors.addIfNeeded(index,line,column,ColorScheme.LITERAL);
                    break;
                case ABSTRACT:
                case ASSERT:
                case  BOOLEAN:
                case  BYTE:
                case  CHAR:
                case  CLASS:
                case  DO:
                case  DOUBLE:
                case  FINAL:
                case  FLOAT:
                case  FOR:
                case  IF:
                case  INT:
                case  LONG:
                case  NEW:
                case  PUBLIC:
                case  PRIVATE:
                case  PROTECTED:
                case  PACKAGE:
                case  RETURN:
                case  STATIC:
                case  SHORT:
                case  SUPER:
                case  SWITCH:
                case  ELSE:
                case  VOLATILE:
                case  SYNCHRONIZED:
                case  STRICTFP:
                case  GOTO:
                case  CONTINUE:
                case  BREAK:
                case  TRANSIENT:
                case  VOID:
                case  TRY:
                case  CATCH:
                case  FINALLY:
                case  WHILE:
                case  CASE:
                case  DEFAULT:
                case  CONST:
                case  ENUM:
                case  EXTENDS:
                case  IMPLEMENTS:
                case  IMPORT:
                case  INSTANCEOF:
                case  INTERFACE:
                case  NATIVE:
                case  THIS:
                case  THROW:
                case  THROWS:
                    colors.addIfNeeded(index,line,column, ColorScheme.KEYWORD);
                    break;
                case LBRACE: {
                    colors.addIfNeeded(index, line, column, ColorScheme.OPERATOR);
                    if(stack.isEmpty()) {
                        if(currSwitch > maxSwitch) {
                            maxSwitch = currSwitch;
                        }
                        currSwitch = 0;
                    }
                    currSwitch++;
                    BlockLine block = colors.obtainNewBlock();
                    block.startLine = line;
                    block.startColumn = column;
                    stack.push(block);
                    break;
                }
                case RBRACE: {
                    colors.addIfNeeded(index, line, column, ColorScheme.OPERATOR);
                    if(!stack.isEmpty()) {
                        BlockLine block = stack.pop();
                        block.endLine = line;
                        block.endColumn = column;
                        colors.addBlockLine(block);
                    }
                    break;
                }
                default:
                    colors.addIfNeeded(index,line,column,ColorScheme.OPERATOR);
            }
            helper.update(tokenizer.getTokenLength());
            index = tokenizer.getIndex() + tokenizer.getTokenLength();
            line = helper.getLine();
            column = helper.getColumn();
        }
        identifiers.finish();
        colors.mExtra = identifiers;
        colors.setSuppressSwitch(maxSwitch + 10);
        colors.setNavigation(labels);
    }

}
