/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.editor.ts

import com.itsaky.androidide.treesitter.TSLanguage
import io.github.rosemoe.sora.util.IntPair

class TsTheme {

    companion object {
        val INTEGER_REGEX = Regex("[0-9]+")
    }

    private val suffixStyle = RuleNode()
    private val prefixStyle = RuleNode()
    private val styles = arrayListOf(0L)

    fun putStyleRule(rule: String, style: Long) {
        val styleIdx = if (style != 0L) {
            styles.add(style)
            styles.size - 1
        } else {
            0
        }
        if (rule.startsWith("^")) {
            setStyleForRuleSpec(prefixStyle, resolvePrimitiveRule(rule.substring(1)), styleIdx)
        } else {
            setStyleForRuleSpec(suffixStyle, resolvePrimitiveRule(rule).reversed(), styleIdx)
        }
    }

    fun eraseStyleRule(rule: String, language: TSLanguage? = null) = putStyleRule(rule, 0L)

    private fun resolvePrimitiveRule(rule: String) = rule.split('.').toMutableList().also {
        for (i in 0 until it.size) {
            if (it[i] == "dot") {
                it[i] = "."
            }
        }
    }

    private fun setStyleForRuleSpec(initialNode: RuleNode, ruleSpec: List<String>, style: Int) {
        var node = initialNode
        for (type in ruleSpec) {
            node = node.getOrCreateChild(type)
        }
        node.nodeStyle = style
    }

    private fun resolveStyleForTypeStack(node: RuleNode, currentPos:Int, terminalPos: Int, deltaPos: Int, typeStack: MyStack<Array<String>>) : Long {
        if (currentPos == terminalPos) {
            return 0L
        }
        var maxDepth = 0
        var childStyle = 0
        for (typeAlias in typeStack[currentPos]) {
            val sub = node.getChild(typeAlias)
            if (sub != null) {
                val subResult = resolveStyleForTypeStack(sub, currentPos + deltaPos, terminalPos, deltaPos, typeStack)
                val depth = (IntPair.getSecond(subResult) - currentPos) / deltaPos
                if (IntPair.getFirst(subResult) != 0 && depth > maxDepth) {
                    maxDepth = depth
                    childStyle = IntPair.getFirst(subResult)
                }
            }
        }
        if (childStyle != 0) {
            return IntPair.pack(childStyle, currentPos + maxDepth * deltaPos)
        }
        return IntPair.pack(node.nodeStyle, currentPos)
    }

    internal fun resolveStyleForTypeStack(typeStack: MyStack<Array<String>>) : Long {
        if (typeStack.size == 0) {
            return 0L
        }
        // Suffix matching first
        var style = resolveStyleForTypeStack(suffixStyle, typeStack.size - 1, -1, -1, typeStack)
        if (IntPair.getFirst(style) == 0) {
            style = resolveStyleForTypeStack(prefixStyle, 0, typeStack.size, 1, typeStack)
        }
        return styles[IntPair.getFirst(style)]
    }

    private class RuleNode(var nodeStyle: Int = 0) {

        private val children = mutableMapOf<String, RuleNode>()

        fun getOrCreateChild(type: String) = children[type].let {
            if (it == null) {
                val node = RuleNode()
                children[type] = node
                node
            } else {
                it
            }
        }

        fun getChild(type: String) = children[type]

    }

}

class TsThemeBuilder {

    internal val theme = TsTheme()

    infix fun Long.applyTo(targetRule: String) {
        theme.putStyleRule(targetRule, this)
    }

    infix fun Long.applyTo(targetRules: Array<String>) {
        targetRules.forEach {
            applyTo(it)
        }
    }

}

fun tsTheme(description: TsThemeBuilder.() -> Unit) =
    TsThemeBuilder().also { it.description() }.theme