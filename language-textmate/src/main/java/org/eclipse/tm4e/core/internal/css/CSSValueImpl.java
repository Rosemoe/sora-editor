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

import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.css.Counter;
import org.w3c.dom.css.RGBColor;
import org.w3c.dom.css.Rect;

public abstract class CSSValueImpl implements CSSPrimitiveValue, CSSValue {

    // W3C CSSValue API methods

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSValue#getCssText()
     */
    public String getCssText() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSValue#setCssText(java.lang.String)
     */
    public void setCssText(String cssText) throws DOMException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSValue#getCssValueType()
     */
    public short getCssValueType() {
        return CSS_PRIMITIVE_VALUE;
    }

    // W3C CSSPrimitiveValue API methods

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#getPrimitiveType()
     */
    public short getPrimitiveType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#getCounterValue()
     */
    public Counter getCounterValue() throws DOMException {
        throw new DOMException(DOMException.INVALID_ACCESS_ERR, "COUNTER_ERROR");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#getRGBColorValue()
     */
    public RGBColor getRGBColorValue() throws DOMException {
        throw new DOMException(DOMException.INVALID_ACCESS_ERR, "RGBCOLOR_ERROR");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#getRectValue()
     */
    public Rect getRectValue() throws DOMException {
        throw new DOMException(DOMException.INVALID_ACCESS_ERR, "RECT_ERROR");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#getStringValue()
     */
    public String getStringValue() throws DOMException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#setFloatValue(short, float)
     */
    public void setFloatValue(short arg0, float arg1) throws DOMException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#setStringValue(short,
     * java.lang.String)
     */
    public void setStringValue(short arg0, String arg1) throws DOMException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    // Additional methods

    public float getFloatValue(short valueType) throws DOMException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

}