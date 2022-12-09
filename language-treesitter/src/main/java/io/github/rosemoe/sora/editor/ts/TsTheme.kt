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

import io.github.rosemoe.sora.util.IntPair

/**
 * Theme for tree-sitter. This is different from [io.github.rosemoe.sora.widget.schemes.EditorColorScheme].
 * It is only used for colorizing spans in tree-sitter module. The real colors are still stored in editor
 * color schemes.
 *
 * ### Rule format:
 * - suffix matching: x.y.z
 *
 *   For example: class_declaration.name matches rule stack: `(*.)*class_declaration.name`
 * - prefix matching: ^x.y.z
 *
 *   For example: ^program.import_declaration matches rule stack: `program.import_declaration(.*)*`
 *
 * Tip: both node name (aka named node) and node type name can be used in rule matching.
 * Dot symbol ('.') should be replaced with letters 'dot'
 *
 * Note that we always try to find color for every node, and try to match the longest path, which means:
 *   If we have rules: package_declaration -> A, scope -> B
 *   node 'package_declaration' contains node of type 'scope', you will get 'scope' of 'package_declaration' with style B,
 *   and other parts of 'package_declaration' with style A.
 *
 *   If we add rule: package_declaration.scope -> C,
 *   you will get 'scope' of 'package_declaration' with style C,and other parts of package_declaration with style A.
 *
 * @author Rosemoe
 */
class TsTheme {

    private val suffixStyle = RuleNode()
    private val prefixStyle = RuleNode()
    private val styles = arrayListOf(0L)

    /**
     * Set text style for the given rule string.
     *
     * @param rule The rule for locating nodes
     * @param style The style value for those nodes
     * @see io.github.rosemoe.sora.lang.styling.TextStyle
     */
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

    /**
     * Remove rule*
     * @param rule The rule for locating nodes
     */
    fun eraseStyleRule(rule: String) = putStyleRule(rule, 0L)

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

    private fun resolveStyleForTypeStack(
        node: RuleNode,
        currentPos: Int,
        terminalPos: Int,
        deltaPos: Int,
        typeStack: NonConcStack<Array<String>>
    ): Long {
        if (currentPos == terminalPos) {
            return 0L
        }
        var maxDepth = 0
        var childStyle = 0
        for (typeAlias in typeStack[currentPos]) {
            val sub = node.getChild(typeAlias)
            if (sub != null) {
                val subResult = resolveStyleForTypeStack(
                    sub,
                    currentPos + deltaPos,
                    terminalPos,
                    deltaPos,
                    typeStack
                )
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

    internal fun resolveStyleForTypeStack(typeStack: NonConcStack<Array<String>>): Long {
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

/**
 * Builder class for tree-sitter themes
 */
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

/**
 * Build tree-sitter theme
 */
fun tsTheme(description: TsThemeBuilder.() -> Unit) =
    TsThemeBuilder().also { it.description() }.theme