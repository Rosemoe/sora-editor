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
package org.eclipse.tm4e.core.theme.css;

import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Parser;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

import org.eclipse.tm4e.core.internal.css.CSSConditionFactory;
import org.eclipse.tm4e.core.internal.css.CSSDocumentHandler;
import org.eclipse.tm4e.core.internal.css.CSSSelectorFactory;
import org.eclipse.tm4e.core.internal.css.ExtendedSelector;
import org.eclipse.tm4e.core.theme.IStyle;

/**
 * CSS Parser to parse style for TextMate syntax coloration.
 *
 */
public class CSSParser {

    private final CSSDocumentHandler handler;

    public CSSParser(InputStream source) throws Exception {
        this(toSource(source));
    }

    public CSSParser(InputSource source) throws Exception {
        this(source, SACParserFactory.newInstance().makeParser());
    }

    public CSSParser(String source) throws Exception {
        this(new InputSource(new StringReader(source)));
    }

    public CSSParser(InputSource source, Parser parser) throws CSSException, IOException {
        this.handler = new CSSDocumentHandler();
        parser.setDocumentHandler(handler);
        parser.setConditionFactory(CSSConditionFactory.INSTANCE);
        parser.setSelectorFactory(CSSSelectorFactory.INSTANCE);
        parser.parseStyleSheet(source);
    }

    private static InputSource toSource(InputStream source) {
        InputSource in = new InputSource();
        in.setByteStream(source);
        return in;
    }

    public IStyle getBestStyle(String... names) {
        int bestSpecificity = 0;
        IStyle bestStyle = null;
        for (IStyle style : handler.getList()) {
            SelectorList list = ((CSSStyle) style).getSelectorList();
            for (int i = 0; i < list.getLength(); i++) {
                Selector selector = list.item(i);
                if (selector instanceof ExtendedSelector) {
                    ExtendedSelector s = ((ExtendedSelector) selector);
                    int nbMatch = s.nbMatch(names);
                    if (nbMatch > 0 && nbMatch == s.nbClass()) {
                        if (bestStyle == null || (nbMatch >= bestSpecificity)) {
                            bestStyle = style;
                            bestSpecificity = nbMatch;
                        }
                    }
                }
            }
        }
        return bestStyle;
    }

    public List<IStyle> getStyles() {
        return handler.getList();
    }

}
