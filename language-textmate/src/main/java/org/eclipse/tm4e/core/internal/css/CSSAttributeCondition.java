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

public class CSSAttributeCondition extends AbstractAttributeCondition {

    /**
     * The attribute's local name.
     */
    protected String localName;

    /**
     * The attribute's namespace URI.
     */
    protected String namespaceURI;

    /**
     * Whether this condition applies to specified attributes.
     */
    protected boolean specified;

    public CSSAttributeCondition(String localName, String namespaceURI, boolean specified, String value) {
        super(value);
        this.localName = localName;
        this.namespaceURI = namespaceURI;
        this.specified = specified;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public String getNamespaceURI() {
        return namespaceURI;
    }

    @Override
    public boolean getSpecified() {
        return specified;
    }

    @Override
    public short getConditionType() {
        return SAC_ATTRIBUTE_CONDITION;
    }

    @Override
    public int nbMatch(String... names) {
//		String val = getValue();
//		if (val == null) {
//			return !e.getAttribute(getLocalName()).equals("");
//		}
//		return e.getAttribute(getLocalName()).equals(val);
        return 0;
    }

    @Override
    public int nbClass() {
        return 0;
    }
}
