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
package io.github.rosemoe.sora.langs.xml.analyzer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import io.github.rosemoe.sora.interfaces.CodeAnalyzer;
import io.github.rosemoe.sora.text.LineNumberCalculator;
import io.github.rosemoe.sora.text.TextAnalyzeResult;
import io.github.rosemoe.sora.text.TextAnalyzer;


/**
 * Basic syntax analyzer with PULL
 * You can get multiple syntax errors. But only the first one is accurate, and the others are not necessarily accurate
 * Note:If you want '&lt;test&gt; &lt;/test&gt;' but type '&lt;test&gt; /test&gt;'.
 * According to the rules, it will not prompt errors.
 * '/test>' is treated as TEXT.
 */
public class BasicSyntaxPullAnalyzer implements CodeAnalyzer {

    @Override
    public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        try {
            XmlPullParserFactory fact = XmlPullParserFactory.newInstance();
            //Setting to true will cause java.lang.RuntimeException: Undefined Prefix..
            //We can't get the exact location from the error
            fact.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            XmlPullParser xmlPullParser = fact.newPullParser();

            LineNumberCalculator calculator = new LineNumberCalculator(content);
            calculator.update(content.length());
            int errLine = 0;
            int errColumn = 0;
            xmlPullParser.setInput(new StringReader(content.toString()) {
                @Override
                public String toString() {
                    return "xml";
                }
            });
            xmlPullParser.getEventType();
            while (true) {
                try {
                    if (calculator.getLine() + 1 == xmlPullParser.getLineNumber() &&
                            calculator.getColumn() + 1 == xmlPullParser.getColumnNumber()) {
                        break;
                    }
                    xmlPullParser.next();
                } catch (XmlPullParserException e) {
                    //if we get error at the same position ,we will get an endless loop.
                    //So break it!
                    if (errLine == xmlPullParser.getLineNumber() &&
                            errColumn == xmlPullParser.getColumnNumber()) {
                        break;
                    }
                    errLine = xmlPullParser.getLineNumber();
                    errColumn = xmlPullParser.getColumnNumber();
                    int[] end = Utils.setErrorSpan(colors, errLine, errColumn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
