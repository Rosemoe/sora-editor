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
package io.github.rosemoe.sora.textmate.core.model;

import java.util.List;

import io.github.rosemoe.sora.textmate.core.grammar.IGrammar;

/**
 * TextMate model API.
 *
 */
public interface ITMModel {

    /**
     * Returns the TextMate grammar to use to parse for each lines of the
     * document the TextMate tokens.
     *
     * @return the TextMate grammar to use to parse for each lines of the
     *         document the TextMate tokens.
     */
    IGrammar getGrammar();

    /**
     * Set the TextMate grammar to use to parse for each lines of the document
     * the TextMate tokens.
     *
     * @param grammar
     */
    void setGrammar(IGrammar grammar);

    /**
     * Add model tokens changed listener.
     *
     * @param listener
     *            to add
     */
    void addModelTokensChangedListener(IModelTokensChangedListener listener);

    /**
     * Remove model tokens changed listener.
     *
     * @param listener
     *            to remove
     */
    void removeModelTokensChangedListener(IModelTokensChangedListener listener);

    void dispose();

    List<TMToken> getLineTokens(int line);

    void forceTokenization(int lineNumber);

}
