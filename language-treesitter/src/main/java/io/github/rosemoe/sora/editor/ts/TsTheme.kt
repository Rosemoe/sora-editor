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
import com.itsaky.androidide.treesitter.TSLanguage
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class TsTheme {

    private val styleMapping = SparseLongArray()

    fun styleFor(symbol: Int): Long {
        val value = styleMapping[symbol]
        if (value == 0L) {
            return TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)
        }
        return value
    }

    fun isStyleSetFor(symbol: Int) : Boolean {
        return styleMapping.indexOfKey(symbol) >= 0
    }

    fun setStyleFor(symbol: Int, style: Long) {
        println("style $style to symbol $symbol")
        styleMapping.put(symbol, style)
    }

}

class TsThemeBuilder(internal val language: TSLanguage?) {

    internal val theme = TsTheme()

    infix fun Long.styleFor(target: String): StyleWrapper {
        var symbol = language!!.getSymbolForTypeString(target, true)
        if (symbol == 0) {
            symbol = language.getSymbolForTypeString(target, false)
        }
        theme.setStyleFor(symbol, this)
        return StyleWrapper(this)
    }

    inner class StyleWrapper(private val textStyle: Long) {

        infix fun and(target: String): StyleWrapper {
            var symbol = language!!.getSymbolForTypeString(target, true)
            if (symbol == 0) {
                symbol = language.getSymbolForTypeString(target, false)
            }
            theme.setStyleFor(symbol, textStyle)
            return this
        }

        infix fun and(target: Array<String>): StyleWrapper {
            target.forEach {
                and(it)
            }
            return this
        }

        infix fun and(symbol: Int): StyleWrapper {
            theme.setStyleFor(symbol, textStyle)
            return this
        }

    }

}

fun tsTheme(language: TSLanguage? = null, description: TsThemeBuilder.() -> Unit) =
    TsThemeBuilder(language).also { it.description() }.theme