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
