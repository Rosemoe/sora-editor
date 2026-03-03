/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContentLineTest {

    @Test
    fun `latin1 path should preserve content and char operations`() {
        val line = ContentLine()
        line.insert(0, "ab")
        line.insert(2, '\u00FF')
        line.insert(3, "cd")

        assertThat(line.length).isEqualTo(5)
        assertThat(line.toString()).isEqualTo("ab\u00FFcd")
        assertThat(line[2]).isEqualTo('\u00FF')

        val out = CharArray(line.length)
        line.getChars(0, line.length, out, 0)
        assertThat(String(out)).isEqualTo("ab\u00FFcd")

        val sb = StringBuilder()
        line.appendTo(sb)
        assertThat(sb.toString()).isEqualTo("ab\u00FFcd")
    }

    @Test
    fun `utf16 upgrade should happen when inserting non latin1 char`() {
        val line = ContentLine("hello")
        line.insert(5, '中')
        line.insert(0, "前")

        assertThat(line.toString()).isEqualTo("前hello中")
        assertThat(line[0]).isEqualTo('前')
        assertThat(line[6]).isEqualTo('中')

        val out = CharArray(line.length)
        line.getChars(0, line.length, out, 0)
        assertThat(String(out)).isEqualTo("前hello中")

        line.delete(1, 6)
        assertThat(line.toString()).isEqualTo("前中")
    }

    @Test
    fun `subSequence and copy should work after utf16 upgrade`() {
        val line = ContentLine("a中b文c")

        val sub = line.subSequence(1, 4)
        assertThat(sub.toString()).isEqualTo("中b文")

        val copied = line.copy()
        copied.insert(copied.length, '末')
        assertThat(copied.toString()).isEqualTo("a中b文c末")
        assertThat(line.toString()).isEqualTo("a中b文c")
    }

}
