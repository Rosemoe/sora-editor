/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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