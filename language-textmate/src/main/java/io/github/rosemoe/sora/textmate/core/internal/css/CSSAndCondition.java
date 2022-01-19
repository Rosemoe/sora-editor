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

import org.w3c.css.sac.Condition;

public class CSSAndCondition extends AbstractCombinatorCondition {

    /**
     * Creates a new CombinatorCondition object.
     */
    public CSSAndCondition(Condition c1, Condition c2) {
        super(c1, c2);
    }

    @Override
    public short getConditionType() {
        return SAC_AND_CONDITION;
    }

    @Override
    public int nbMatch(String... names) {
        return ((ExtendedCondition) getFirstCondition()).nbMatch(names)
                + ((ExtendedCondition) getSecondCondition()).nbMatch(names);
    }

    @Override
    public int nbClass() {
        return ((ExtendedCondition) getFirstCondition()).nbClass()
                + ((ExtendedCondition) getSecondCondition()).nbClass();
    }
}
