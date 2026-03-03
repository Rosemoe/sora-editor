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

package io.github.rosemoe.lsp.editor.text

import io.github.rosemoe.sora.lsp.editor.text.SimpleMarkdownRenderer
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class SimpleMarkdownRendererTest {

    @Test
    fun `basic multiline code block`() {
        val result = SimpleMarkdownRenderer.parseBlocks(
            """
            ```java
            public class Main {
                public static void main(String[] args) {}
            }
            ```
        """.trimIndent()
        )
        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(SimpleMarkdownRenderer.Block.CodeBlock::class.java)

        val codeBlock = result[0] as SimpleMarkdownRenderer.Block.CodeBlock
        assertThat(codeBlock.language).isEqualTo("java")
        val expectedContent = """
            public class Main {
                public static void main(String[] args) {}
            }
        """.trimIndent()
        assertThat(codeBlock.content).isEqualTo(expectedContent)
    }

    @Test
    fun `multiline code block with more than three backquotes`() {
        // Tracking issue: #814
        val result = SimpleMarkdownRenderer.parseBlocks(
            """
            ````java
            ```
            ````Test````
            ````
        """.trimIndent()
        )
        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(SimpleMarkdownRenderer.Block.CodeBlock::class.java)

        val codeBlock = result[0] as SimpleMarkdownRenderer.Block.CodeBlock
        assertThat(codeBlock.language).isEqualTo("java")
        assertThat(codeBlock.content).isEqualTo("```\n````Test````")
    }

}