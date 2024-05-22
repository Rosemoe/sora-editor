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

package io.github.rosemoe.sora.langs.monarch.utils

import io.github.dingyi222666.regex.Regex

fun String.checkSurrogate(): Boolean {
    for (element in this) {
        if (Character.isSurrogate(element)) {
            return true
        }
    }
    return false
}


fun String.convertUnicodeOffsetToUtf16(offset: Int, isSurrogatePairConsidered: Boolean): Int {
    if (offset < 0) {
        throw IllegalArgumentException("Offset cannot be negative.")
    }

    if (!isSurrogatePairConsidered) {
        return offset
    }

    var i = 0
    while (i < length) {
        if (i == offset) {
            return i
        }

        val ch = this[i]

        if (Character.isHighSurrogate(ch) && i + 1 < length && Character.isLowSurrogate(this[i + 1])) {
            i += 2
            continue
        }

        i++
    }

    return offset
}

fun Regex.matchesFully(text: String): Boolean {
    val result = search(text, 0) ?: return false
    return result.count == 1 && result.value.length == text.length
}

fun Regex.matchesPartially(text: String): Boolean {
    return search(text, 0) != null
}

inline fun String.matchesFully(regex: Regex): Boolean {
    return regex.matchesFully(this)
}

inline fun String.matchesPartially(regex: Regex): Boolean {
    return regex.matchesPartially(this)
}

/**
 * Escapes regular expression characters in a given string
 */
fun String.escapeRegExpCharacters(): String {
    return replace(
        "[\\-\\\\\\{\\}\\*\\+\\?\\|\\^\\$\\.\\[\\]\\(\\)\\#]".toRegex(),
        "\\\\$0"
    ) //$NON-NLS-1$ //$NON-NLS-2$
}

