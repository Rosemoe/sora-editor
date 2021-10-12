/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.github.rosemoe.sora.textmate.core.internal.css;

import org.w3c.css.sac.LexicalUnit;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSValue;

public class Measure extends CSSValueImpl {

    LexicalUnit value;

    public Measure(LexicalUnit value) {
        super();
        this.value = value;
    }

    /**
     * Return a float representation of the receiver's value.
     *
     * @param valueType a short representing the value type, see
     *                  {@link CSSValue#getCssValueType()}
     */
    public float getFloatValue(short valueType) throws DOMException {
        // If it's actually a SAC_INTEGER return the integer value, callers tend
        // to expect and cast
        // There is no getIntegerFloat(short)
        // TODO Not sure the purpose of arg valyeType, its not referenced in
        // this method
        if (value.getLexicalUnitType() == LexicalUnit.SAC_INTEGER)
            return value.getIntegerValue();
        // TODO not sure what to do if it's not one of the lexical unit types
        // that are specified in LexicalUnit#getFloatValue()
        // ie. SAC_DEGREE, SAC_GRADIAN, SAC_RADIAN, SAC_MILLISECOND, SAC_SECOND,
        // SAC_HERTZ or SAC_KILOHERTZ
        return value.getFloatValue();
    }

    /**
     * Return an int representation of the receiver's value.
     *
     * @param valueType a short representing the value type, see
     *                  {@link CSSValue#getCssValueType()}
     */
    public int getIntegerValue(short valueType) throws DOMException {
        return value.getIntegerValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#getStringValue()
     */
    public String getStringValue() throws DOMException {
        short lexicalUnit = value.getLexicalUnitType();
        if ((lexicalUnit == LexicalUnit.SAC_IDENT) || (lexicalUnit == LexicalUnit.SAC_STRING_VALUE)
                || (lexicalUnit == LexicalUnit.SAC_URI))
            return value.getStringValue();
        // TODO There are more cases to catch of getLexicalUnitType()
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSPrimitiveValue#getPrimitiveType()
     */
    public short getPrimitiveType() {
        switch (value.getLexicalUnitType()) {
            case LexicalUnit.SAC_IDENT:
                return CSS_IDENT;
            case LexicalUnit.SAC_INTEGER:
            case LexicalUnit.SAC_REAL:
                return CSS_NUMBER;
            case LexicalUnit.SAC_URI:
                return CSS_URI;
            case LexicalUnit.SAC_PERCENTAGE:
                return CSS_PERCENTAGE;
            case LexicalUnit.SAC_PIXEL:
                return CSS_PX;
            case LexicalUnit.SAC_CENTIMETER:
                return CSS_CM;
            case LexicalUnit.SAC_EM:
                return CSS_EMS;
            case LexicalUnit.SAC_EX:
                return CSS_EXS;
            case LexicalUnit.SAC_INCH:
                return CSS_IN;
            case LexicalUnit.SAC_STRING_VALUE:
                return CSS_STRING;
            case LexicalUnit.SAC_DIMENSION:
                return CSS_DIMENSION;
            case LexicalUnit.SAC_OPERATOR_COMMA:
                return CSS_CUSTOM; // TODO don't think this is right, see bug
            // #278139
            case LexicalUnit.SAC_INHERIT:
                return CSS_INHERIT;
        }
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
                "NOT YET IMPLEMENTED - LexicalUnit type: " + value.getLexicalUnitType());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.w3c.dom.css.CSSValue#getCssText()
     */
    public String getCssText() {
        // TODO: All LexicalUnit.SAC_OPERATOR_* except for COMMA left undone for
        // now as it's not even clear whether they should be treated as measures
        // see bug #278139
        switch (value.getLexicalUnitType()) {
            case LexicalUnit.SAC_INTEGER:
                return String.valueOf(value.getIntegerValue());
            case LexicalUnit.SAC_REAL:
                return String.valueOf(value.getFloatValue());
            case LexicalUnit.SAC_PERCENTAGE:
            case LexicalUnit.SAC_PIXEL:
            case LexicalUnit.SAC_CENTIMETER:
            case LexicalUnit.SAC_EM:
            case LexicalUnit.SAC_EX:
            case LexicalUnit.SAC_PICA:
            case LexicalUnit.SAC_POINT:
            case LexicalUnit.SAC_INCH:
            case LexicalUnit.SAC_DEGREE:
                return String.valueOf(value.getFloatValue()) + value.getDimensionUnitText();
            case LexicalUnit.SAC_URI:
                return "url(" + value.getStringValue() + ")";
            case LexicalUnit.SAC_OPERATOR_COMMA:
                return ",";
            case LexicalUnit.SAC_INHERIT:
                return "inherit";
        }
        return value.getStringValue();
    }
}