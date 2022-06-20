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

import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CombinatorCondition;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionFactory;
import org.w3c.css.sac.ContentCondition;
import org.w3c.css.sac.LangCondition;
import org.w3c.css.sac.NegativeCondition;
import org.w3c.css.sac.PositionalCondition;

public class CSSConditionFactory implements ConditionFactory {

    public static final ConditionFactory INSTANCE = new CSSConditionFactory();

    @Override
    public AttributeCondition createClassCondition(String namespaceURI, String value) throws CSSException {
        return new CSSClassCondition(null, "class", value);
    }

    @Override
    public AttributeCondition createAttributeCondition(String localName, String namespaceURI, boolean specified,
                                                       String value) throws CSSException {
        return new CSSAttributeCondition(localName, namespaceURI, specified, value);
    }

    @Override
    public CombinatorCondition createAndCondition(Condition first,
                                                  Condition second) throws CSSException {
        return new CSSAndCondition(first, second);
    }

    @Override
    public AttributeCondition createBeginHyphenAttributeCondition(String arg0, String arg1, boolean arg2, String arg3)
            throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public ContentCondition createContentCondition(String arg0) throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public AttributeCondition createIdCondition(String arg0) throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public LangCondition createLangCondition(String arg0) throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public NegativeCondition createNegativeCondition(Condition arg0) throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public AttributeCondition createOneOfAttributeCondition(String arg0, String arg1, boolean arg2, String arg3)
            throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public Condition createOnlyChildCondition() throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public Condition createOnlyTypeCondition() throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public CombinatorCondition createOrCondition(Condition arg0, Condition arg1) throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public PositionalCondition createPositionalCondition(int arg0, boolean arg1, boolean arg2) throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

    @Override
    public AttributeCondition createPseudoClassCondition(String arg0, String arg1) throws CSSException {
        throw new CSSException("Not implemented in CSS2");
    }

}
