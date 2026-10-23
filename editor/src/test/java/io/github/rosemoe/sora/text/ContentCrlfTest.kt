/*
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
 */
package io.github.rosemoe.sora.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * An index between the '\r' and the '\n' of a CRLF maps to column `length + 1`. Edits there must
 * not split the line break, and must not throw.
 */
class ContentCrlfTest {

    // a0 b1 \r2 \n3 \r4 \n5 c6 d7: indexes 3 and 5 are inside a line break.
    private val text = "ab\r\n\r\ncd"

    @Test
    fun `an index inside a CRLF maps to column length + 1`() {
        val content = Content(text)
        val pos = content.indexer.getCharPosition(3)
        assertThat(pos.line).isEqualTo(0)
        assertThat(pos.column).isEqualTo(3)
        assertThat(content.isInLineSeparator(0, 3)).isTrue()
        assertThat(content.isInLineSeparator(0, 2)).isFalse()
        assertThat(content.isInLineSeparator(1, 1)).isTrue()
        assertThat(Content("ab\ncd").isInLineSeparator(0, 3)).isFalse()
    }

    @Test
    fun `deleting half of a CRLF deletes all of it`() {
        // Only the '\r'
        assertThat(Content(text).apply { delete(2, 3) }.toString()).isEqualTo("ab\r\ncd")
        // Only the '\n'
        assertThat(Content(text).apply { delete(3, 4) }.toString()).isEqualTo("ab\r\ncd")
        // From inside one break to inside the next
        assertThat(Content(text).apply { delete(3, 5) }.toString()).isEqualTo("abcd")
        // By line and column
        assertThat(Content(text).apply { delete(1, 1, 2, 1) }.toString()).isEqualTo("ab\r\nd")
    }

    @Test
    fun `a widened delete undoes to the original text`() {
        val content = Content(text)
        content.delete(3, 6)
        assertThat(content.toString()).isEqualTo("abcd")
        content.undo()
        assertThat(content.toString()).isEqualTo(text)
    }

    @Test
    fun `inserting inside a CRLF inserts before it`() {
        val content = Content(text)
        content.insert(0, 3, "x")
        assertThat(content.toString()).isEqualTo("abx\r\n\r\ncd")
        content.insert(1, 1, "y")
        assertThat(content.toString()).isEqualTo("abx\r\ny\r\ncd")
    }

    @Test
    fun `replacing from inside a CRLF takes the whole break`() {
        assertThat(Content(text).apply { replace(3, 5, "x") }.toString()).isEqualTo("abxcd")
        assertThat(Content(text).apply { replace(5, 5, "x") }.toString()).isEqualTo("ab\r\nx\r\ncd")
        assertThat(Content(text).apply { replace(0, 1, 1, 1, "x") }.toString()).isEqualTo("axcd")
    }
}
