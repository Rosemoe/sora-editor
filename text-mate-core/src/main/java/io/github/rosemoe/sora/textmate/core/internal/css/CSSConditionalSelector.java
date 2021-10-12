/**
 * Copyright (c) 2015-2018 Angelo ZERR.
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
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.SimpleSelector;

public class CSSConditionalSelector implements ConditionalSelector, ExtendedSelector {

    /**
     * The simple selector.
     */
    protected SimpleSelector simpleSelector;

    /**
     * The condition.
     */
    protected Condition condition;

    /**
     * Creates a new ConditionalSelector object.
     */
    public CSSConditionalSelector(SimpleSelector simpleSelector, Condition condition) {
        this.simpleSelector = simpleSelector;
        this.condition = condition;
    }

    @Override
    public short getSelectorType() {
        return SAC_CONDITIONAL_SELECTOR;
    }

    @Override
    public Condition getCondition() {
        return condition;
    }

    @Override
    public SimpleSelector getSimpleSelector() {
        return simpleSelector;
    }

    @Override
    public int getSpecificity() {
        return ((ExtendedSelector) getSimpleSelector()).getSpecificity()
                + ((ExtendedCondition) getCondition()).getSpecificity();
    }

    @Override
    public int nbMatch(String... names) {
        return ((ExtendedSelector) getSimpleSelector()).nbMatch(names) +
                ((ExtendedCondition) getCondition()).nbMatch(names);
    }

    @Override
    public int nbClass() {
        return ((ExtendedSelector) getSimpleSelector()).nbClass()
                + ((ExtendedCondition) getCondition()).nbClass();
    }

}
