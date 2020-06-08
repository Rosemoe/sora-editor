/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
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
import com.rose.editor.langs.internal.TrieTree;

/**
 * Note:Navigation not supported
 * @author Rose
 */
public class JavaCodeAnalyzer implements CodeAnalyzer {
    
    private final static Object OBJECT = new Object();

    @Override
    public void analyze(CharSequence content, TextColorProvider.TextColors colors, TextColorProvider.AnalyzeThread.Delegate delegate) {
        StringBuilder text = content instanceof StringBuilder ? (StringBuilder)content : new StringBuilder(content);
        JavaTextTokenizer tokenizer = new JavaTextTokenizer(text);
        tokenizer.setCalculateLineColumn(false);
        Tokens token, previous = Tokens.UNKNOWN;
        int index = 0,line = 0,column = 0;
        LineNumberHelper helper = new LineNumberHelper(text);
        IdentifierAutoComplete.Identifiers identifiers = new IdentifierAutoComplete.Identifiers();
        identifiers.begin();
        Stack<BlockLine> stack = new Stack<>();
        List<NavigationLabel> labels = new ArrayList<>();
        int maxSwitch = 1,currSwitch = 0;
        //Tree to save class names and query
        TrieTree<Object> classNames = new TrieTree<>();
        //Whether previous token is class name
        boolean classNamePrevious = false;
        //Add default class name
        classNames.put("String",OBJECT);
        while(!delegate.shouldReAnalyze()) {
            try{
                // directNextToekn() does not skip any token
                token = tokenizer.directNextToken();
            }catch (RuntimeException e) {
                //When a spelling input is in process, this will happen because of format mismatch
                token = Tokens.CHARACTER_LITERAL;
            }
            // Backup values because looking ahead in function name match will change them
            int thisIndex = tokenizer.getIndex();
            int thisLength = tokenizer.getTokenLength();
            switch(token) {
                case WHITESPACE:
                case NEWLINE:
                    break;
                case IDENTIFIER:
                    //Add a identifier to auto complete
                    identifiers.addIdentifier(text.substring(tokenizer.getIndex(),tokenizer.getTokenLength() + tokenizer.getIndex()));
                    //The previous so this will be the annotation's type name
                    if(previous == Tokens.AT) {
                        colors.addIfNeeded(index,line,column,ColorScheme.ANNOTATION);
                        break;
                    }
                    //Here we have to get next toekn to see if it is function
                    //We can only get the next token in stream.
                    //If more tokens required, we have to use a stack in tokenizer
                    Tokens next = tokenizer.directNextToken(); 
                    //The next is LPAREN,so this is funtion name or type name
                    if(next == Tokens.LPAREN) {
                        colors.addIfNeeded(index,line,column,ColorScheme.FUNCTION_NAME);
                        tokenizer.pushBack(tokenizer.getTokenLength());
                        break;
                    }
                    //Push back the next token
                    tokenizer.pushBack(tokenizer.getTokenLength());
                    //This is a class definition
                    if(previous == Tokens.CLASS) {
                        colors.addIfNeeded(index,line,column,ColorScheme.IDENTIFIER_NAME);
                        //Add class name
                        classNames.put(text,thisIndex,thisLength,OBJECT);
                        break;
                    }
                    //Has class name
                    if(classNames.get(text,thisIndex,thisLength) == OBJECT) {
                        colors.addIfNeeded(index,line,column,ColorScheme.IDENTIFIER_NAME);
                        //Mark it
                        classNamePrevious = true;
                        break;
                    }
                    if(classNamePrevious) {
                        //Var name
                        colors.addIfNeeded(index,line,column,ColorScheme.IDENTIFIER_VAR);
                        classNamePrevious = false;
                        break;
                    }
                    colors.addIfNeeded(index,line,column,ColorScheme.TEXT_NORMAL);
                    break;
                case CHARACTER_LITERAL:
                case STRING:
                case FLOATING_POINT_LITERAL:
                case INTEGER_LITERAL:
                    classNamePrevious = false;
                    colors.addIfNeeded(index,line,column,ColorScheme.LITERAL);
                    break;
                case  INT:
                case  LONG:
                case  BOOLEAN:
                case  BYTE:
                case  CHAR:
                case  FLOAT:
                case  DOUBLE:
                case  SHORT:
                case VOID:
                    classNamePrevious = true;
                    colors.addIfNeeded(index,line,column, ColorScheme.KEYWORD);
                    break;
                case ABSTRACT:
                case ASSERT:
                case  CLASS:
                case  DO:
                case  FINAL:
                case  FOR:
                case  IF:
                case  NEW:
                case  PUBLIC:
                case  PRIVATE:
                case  PROTECTED:
                case  PACKAGE:
                case  RETURN:
                case  STATIC:
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
                    classNamePrevious = false;
                    colors.addIfNeeded(index,line,column, ColorScheme.KEYWORD);
                    break;
                case LBRACE: {
                    classNamePrevious = false;
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
                    classNamePrevious = false;
                    colors.addIfNeeded(index, line, column, ColorScheme.OPERATOR);
                    if(!stack.isEmpty()) {
                        BlockLine block = stack.pop();
                        block.endLine = line;
                        block.endColumn = column;
                        if(block.startLine != block.endLine) {
                            colors.addBlockLine(block);
                        }
                    }
                    break;
                }
                default:
                    if(token == Tokens.LBRACK || (token == Tokens.RBRACK && previous == Tokens.LBRACK)) {
                        colors.addIfNeeded(index,line,column,ColorScheme.OPERATOR);
                        break;
                    }
                    classNamePrevious = false;
                    colors.addIfNeeded(index,line,column,ColorScheme.OPERATOR);
            }
            helper.update(thisLength);
            index = thisIndex + thisLength;
            line = helper.getLine();
            column = helper.getColumn();
            if(token != Tokens.WHITESPACE && token != Tokens.NEWLINE) {
                previous = token;
            }
        }
		if(stack.isEmpty()) {
			if(currSwitch > maxSwitch) {
				maxSwitch = currSwitch;
			}
			currSwitch = 0;
		}
        identifiers.finish();
        colors.mExtra = identifiers;
        colors.setSuppressSwitch(maxSwitch + 10);
        colors.setNavigation(labels);
    }

}
