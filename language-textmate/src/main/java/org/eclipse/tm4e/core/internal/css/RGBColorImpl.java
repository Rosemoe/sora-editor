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

import org.w3c.css.sac.LexicalUnit;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.RGBColor;

public class RGBColorImpl extends CSSValueImpl implements RGBColor {

    private CSSPrimitiveValue red;
    private CSSPrimitiveValue green;
    private CSSPrimitiveValue blue;

    public RGBColorImpl(LexicalUnit lexicalUnit) {
        LexicalUnit nextUnit = lexicalUnit.getParameters();
        red = new Measure(nextUnit);
        nextUnit = nextUnit.getNextLexicalUnit().getNextLexicalUnit();
        green = new Measure(nextUnit);
        nextUnit = nextUnit.getNextLexicalUnit().getNextLexicalUnit();
        blue = new Measure(nextUnit);
    }

    /*
     * (non-Javadoc)
     * @see org.w3c.dom.css.RGBColor#getRed()
     */
    public CSSPrimitiveValue getRed() {
        return red;
    }

    /*
     * (non-Javadoc)
     * @see org.w3c.dom.css.RGBColor#getGreen()
     */
    public CSSPrimitiveValue getGreen() {
        return green;
    }

    /*
     * (non-Javadoc)
     * @see org.w3c.dom.css.RGBColor#getBlue()
     */
    public CSSPrimitiveValue getBlue() {
        return blue;
    }

    /*
     * (non-Javadoc)
     * @see org.w3c.dom.css.CSSValue#getRGBColorValue()
     */
    public RGBColor getRGBColorValue() throws DOMException {
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.w3c.dom.css.CSSValue#getPrimitiveType()
     */
    public short getPrimitiveType() {
        return CSS_RGBCOLOR;
    }

    /*
     * (non-Javadoc)
     * @see org.w3c.dom.css.CSSValue#getCssText()
     */
    public String getCssText() {
        return "rgb(" + red.getCssText() + ", " + green.getCssText() + ", "
                + blue.getCssText() + ")";
    }
}