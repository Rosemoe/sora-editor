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
package org.eclipse.tm4e.core.model;

import java.util.List;

import org.eclipse.tm4e.core.grammar.IGrammar;

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
