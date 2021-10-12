/**
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

public class CSSElementSelector extends AbstractElementSelector {

    public CSSElementSelector(String uri, String name) {
        super(uri, name);
    }

    @Override
    public short getSelectorType() {
        return SAC_ELEMENT_NODE_SELECTOR;
    }

    @Override
    public int getSpecificity() {
        return (getLocalName() == null) ? 0 : 1;
    }

    @Override
    public int nbMatch(String... names) {
        return 0;
    }

    @Override
    public int nbClass() {
        return 0;
    }
}
