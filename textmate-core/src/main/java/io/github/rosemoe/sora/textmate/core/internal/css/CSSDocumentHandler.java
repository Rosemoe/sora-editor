/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.internal.css;

import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.DocumentHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.css.sac.SACMediaList;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.RGBColor;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.textmate.core.theme.IStyle;
import io.github.rosemoe.sora.textmate.core.theme.RGB;
import io.github.rosemoe.sora.textmate.core.theme.css.CSSStyle;

public class CSSDocumentHandler implements DocumentHandler {

    private final List<IStyle> list;
    private CSSStyle currentStyle;

    public CSSDocumentHandler() {
        list = new ArrayList<>();
    }

    @Override
    public void comment(String arg0) throws CSSException {

    }

    @Override
    public void endDocument(InputSource arg0) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endFontFace() throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endMedia(SACMediaList arg0) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endPage(String arg0, String arg1) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endSelector(SelectorList selector) throws CSSException {
        currentStyle = null;
    }

    @Override
    public void ignorableAtRule(String arg0) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void importStyle(String arg0, SACMediaList arg1, String arg2) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void namespaceDeclaration(String arg0, String arg1) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void property(String name, LexicalUnit value, boolean arg2) throws CSSException {
        if (currentStyle != null) {
            if ("color".equals(name)) {
                currentStyle.setColor(createRGB(value));
            } else if ("background-color".equals(name)) {
                currentStyle.setBackgroundColor(createRGB(value));
            } else if ("font-weight".equals(name)) {
                currentStyle.setBold(value.getStringValue().toUpperCase().contains("BOLD"));
            } else if ("font-style".equals(name)) {
                currentStyle.setItalic(value.getStringValue().toUpperCase().contains("ITALIC"));
            }
            if ("text-decoration".equals(name)) {
                String decoration = value.getStringValue().toUpperCase();
                if (decoration.contains("UNDERLINE")) {
                    currentStyle.setUnderline(true);
                }
                if (decoration.contains("LINE-THROUGH")) {
                    currentStyle.setStrikeThrough(true);
                }
            }
        }
    }

    private RGB createRGB(LexicalUnit value) {
        RGBColor rgbColor = new RGBColorImpl(value);
        int green = ((int) rgbColor.getGreen().getFloatValue(CSSPrimitiveValue.CSS_NUMBER));
        int red = ((int) rgbColor.getRed().getFloatValue(CSSPrimitiveValue.CSS_NUMBER));
        int blue = ((int) rgbColor.getBlue().getFloatValue(CSSPrimitiveValue.CSS_NUMBER));
        return new RGB(red, green, blue);
    }

    @Override
    public void startDocument(InputSource arg0) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startFontFace() throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startMedia(SACMediaList arg0) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startPage(String arg0, String arg1) throws CSSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startSelector(SelectorList selector) throws CSSException {
        currentStyle = new CSSStyle(selector);
        list.add(currentStyle);
    }

    public List<IStyle> getList() {
        return list;
    }
}
