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
