/*******************************************************************************
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Sebastian Thomschke (Vegard IT GmbH) - add previousLineText support
 *
 ******************************************************************************/

package io.github.rosemoe.sora.langs.monarch.languageconfiguration.support

import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentationRule
import io.github.rosemoe.sora.langs.monarch.utils.matchesFully

class IndentRulesSupport(
    private val indentationRules: IndentationRule
) {

    fun shouldIncrease(text: String): Boolean {
        return indentationRules.increaseIndentPattern.matchesFully(text)
    }

    fun shouldDecrease(text: String): Boolean {
        return indentationRules.decreaseIndentPattern.matchesFully(text)
    }

    fun shouldIndentNextLine(text: String): Boolean {
        return indentationRules.indentNextLinePattern?.matchesFully(text) ?: false
    }


    fun shouldIgnore(text: String): Boolean {
        return indentationRules.unIndentedLinePattern?.matchesFully(text) ?: false
    }

    fun getIndentMetadata(text: String): Int {
        var ret = 0
        if (shouldIncrease(text)) {
            ret = ret or IndentConsts.INCREASE_MASK
        }
        if (shouldDecrease(text)) {
            ret = ret or IndentConsts.DECREASE_MASK
        }
        if (shouldIndentNextLine(text)) {
            ret = ret or IndentConsts.INDENT_NEXTLINE_MASK
        }
        if (shouldIgnore(text)) {
            ret = ret or IndentConsts.UNINDENT_MASK
        }
        return ret
    }

    object IndentConsts {
        const val INCREASE_MASK = 1
        const val DECREASE_MASK = 2
        const val INDENT_NEXTLINE_MASK = 4
        const val UNINDENT_MASK = 8

    }


}