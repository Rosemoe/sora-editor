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

public class CSSClassCondition extends CSSAttributeCondition {

    public CSSClassCondition(String localName, String namespaceURI, String value) {
        super(localName, namespaceURI, true, value);
    }

    @Override
    public int nbMatch(String... names) {
        String value = getValue();
        for (String name : names) {
            if (name.equals(value)) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int nbClass() {
        return 1;
    }

}
