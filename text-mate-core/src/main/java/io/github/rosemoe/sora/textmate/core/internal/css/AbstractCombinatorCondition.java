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

import org.w3c.css.sac.CombinatorCondition;
import org.w3c.css.sac.Condition;

public abstract class AbstractCombinatorCondition implements CombinatorCondition, ExtendedCondition {
    protected Condition firstCondition;

    protected Condition secondCondition;

    /**
     * Creates a new CombinatorCondition object.
     */
    protected AbstractCombinatorCondition(Condition c1, Condition c2) {
        firstCondition = c1;
        secondCondition = c2;
    }

    @Override
    public Condition getFirstCondition() {
        return firstCondition;
    }

    @Override
    public Condition getSecondCondition() {
        return secondCondition;
    }

    public int getSpecificity() {
        return ((ExtendedCondition) getFirstCondition()).getSpecificity()
                + ((ExtendedCondition) getSecondCondition()).getSpecificity();
    }
}
