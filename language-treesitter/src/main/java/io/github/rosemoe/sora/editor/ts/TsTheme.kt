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

import android.util.SparseLongArray
import com.itsaky.androidide.treesitter.TSQuery

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
class TsTheme(private val tsQuery: TSQuery) {

    private val styles = mutableMapOf<String, Long>()
    private val mapping = SparseLongArray()

    /**
     * Set text style for the given rule string.
     *
     * @param rule The rule for locating nodes
     * @param style The style value for those nodes
     * @see io.github.rosemoe.sora.lang.styling.TextStyle
     */
    fun putStyleRule(rule: String, style: Long) {
        styles[rule] = style
        mapping.clear()
    }

    /**
     * Remove rule
     * @param rule The rule for locating nodes
     */
    fun eraseStyleRule(rule: String) = putStyleRule(rule, 0L)

    fun resolveStyleForPattern(pattern: Int): Long {
        val index = mapping.indexOfKey(pattern)
        return if (index >= 0) {
            mapping.valueAt(index)
        } else {
            val mappedName = tsQuery.getCaptureNameForId(pattern)
            val style = styles[mappedName] ?: 0
            mapping.put(pattern, style)
            style
        }
    }

}

/**
 * Builder class for tree-sitter themes
 */
class TsThemeBuilder(tsQuery: TSQuery) {

    internal val theme = TsTheme(tsQuery)

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
fun tsTheme(tsQuery: TSQuery, description: TsThemeBuilder.() -> Unit) =
    TsThemeBuilder(tsQuery).also { it.description() }.theme