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
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextUtils
import io.github.rosemoe.sora.util.CharCode
import io.github.rosemoe.sora.util.IntPair


fun String.checkSurrogate(): Boolean {
    for (element in this) {
        if (Character.isSurrogate(element)) {
            return true
        }
    }
    return false
}


/**
 * Returns the leading whitespace of the string.
 * If the string contains only whitespaces, returns entire string
 */
fun String.getLeadingWhitespace(start: Int = 0, end: Int = length): String {
    for (index in start until end) {
        val chCode = this[index].code
        if (chCode != CharCode.Space && chCode != CharCode.Tab) {
            return substring(start, index)
        }
    }
    return substring(start, end);
}

/**
 * Returns first index of the string that is not whitespace. If string is empty
 * or contains only whitespaces, returns -1
 */
fun String.firstNonWhitespaceIndex(): Int {
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c != ' ' && c != '\t') {
            return i
        }
        i++
    }
    return -1
}

fun String.normalizeIndentation(tabSize: Int, insertSpaces: Boolean): String {
    var firstNonWhitespaceIndex = this.firstNonWhitespaceIndex()
    if (firstNonWhitespaceIndex == -1) {
        firstNonWhitespaceIndex = length
    }

    return substring(0, firstNonWhitespaceIndex).normalizeIndentationFromWhitespace(
        tabSize,
        insertSpaces
    ) + substring(firstNonWhitespaceIndex);
}

fun String.getIndentationFromWhitespace(tabSize: Int, insertSpaces: Boolean): String {
    val tab = "\t" //$NON-NLS-1$
    var indentOffset = 0
    var startsWithTab = true
    var startsWithSpaces = true
    val spaces = if (insertSpaces
    ) " ".repeat(tabSize)
    else ""
    while (startsWithTab || startsWithSpaces) {
        startsWithTab = this.startsWith(tab, indentOffset)
        startsWithSpaces = insertSpaces && this.startsWith(spaces, indentOffset)
        if (startsWithTab) {
            indentOffset += tab.length
        }
        if (startsWithSpaces) {
            indentOffset += spaces.length
        }
    }
    return this.substring(0, indentOffset)
}

fun String.normalizeIndentationFromWhitespace(
    tabSize: Int,
    insertSpaces: Boolean
): String {
    var spacesCnt = 0

    for (element in this) {
        if (element == '\t') {
            spacesCnt += tabSize
        } else {
            spacesCnt++
        }
    }

    val result = StringBuilder()
    if (!insertSpaces) {
        val tabsCnt = (spacesCnt / tabSize).toLong()
        spacesCnt %= tabSize
        for (i in 0 until tabsCnt) {
            result.append('\t')
        }
    }

    for (i in 0 until spacesCnt) {
        result.append(' ')
    }

    return result.toString()
}

fun Content.getLinePrefixingWhitespaceAtPosition(position: CharPosition): String {
    val line = getLine(position.line)

    val startIndex = IntPair.getFirst(
        TextUtils.findLeadingAndTrailingWhitespacePos(
            line
        )
    )

    return line.subSequence(0, startIndex).toString()
}


fun String.outdentString(useTab: Boolean = false, tabSize: Int = 4): String {
    if (startsWith("\t")) { // $NON-NLS-1$
        return substring(1)
    }

    if (useTab) {
        val chars = CharArray(tabSize) {
            ' '
        }
        val spaces = String(chars)
        if (startsWith(spaces)) {
            return substring(spaces.length)
        }
    }
    return this
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

