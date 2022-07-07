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
package org.eclipse.tm4e.core.internal.css;

import org.eclipse.tm4e.core.theme.IStyle;
import org.eclipse.tm4e.core.theme.RGB;
import org.eclipse.tm4e.core.theme.css.CSSStyle;
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

    }

    @Override
    public void endFontFace() throws CSSException {

    }

    @Override
    public void endMedia(SACMediaList arg0) throws CSSException {

    }

    @Override
    public void endPage(String arg0, String arg1) throws CSSException {

    }

    @Override
    public void endSelector(SelectorList selector) throws CSSException {
        currentStyle = null;
    }

    @Override
    public void ignorableAtRule(String arg0) throws CSSException {

    }

    @Override
    public void importStyle(String arg0, SACMediaList arg1, String arg2) throws CSSException {

    }

    @Override
    public void namespaceDeclaration(String arg0, String arg1) throws CSSException {

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

    }

    @Override
    public void startFontFace() throws CSSException {

    }

    @Override
    public void startMedia(SACMediaList arg0) throws CSSException {

    }

    @Override
    public void startPage(String arg0, String arg1) throws CSSException {

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
