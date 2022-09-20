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

package io.github.rosemoe.sora.text

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests for the [Content] class.
 *
 * @author Akash Yadav
 */
@RunWith(JUnit4::class)
class ContentTest {

    @Test
    fun `test ContentReference should respect the line separator in ContentLine`() {
        val content = Content("public class Main {\n" +
                "    public static void main(String[] args) {\r\n" +
                "        System.out.println(\"Hello World!\");\n" +
                "    }\r\n" +
                "}")

        val ref = ContentReference(content)
        val cString = ref.toReaderString()

        // content[100] = 'W'
        assert(cString[100] == content[100])
    }


    private fun ContentReference.toReaderString() = createReader().use { it.readText() }
}