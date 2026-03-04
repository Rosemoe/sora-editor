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

package io.github.rosemoe.sora.app.tests.paged

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.io.StringReader

class PagedEditSessionTest {

    private fun useTempDir(block: (tmpDir: File) -> Unit) {
        val tmpRoot = File(System.getProperty("java.io.tmpdir", ".")!!)
        val tmpDir = tmpRoot.resolve("sora-editor-test-${System.currentTimeMillis()}")
        tmpDir.mkdirs()
        try {
            block(tmpDir)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `test simple pages`() {
        val pageSize = 512 * 1024
        val chars = "abcdefghijklmnopqrstuvwxyz"
        val text = chars.repeat(pageSize)
        useTempDir { tmpDir ->
            PagedEditSession(
                StringReader(text),
                tmpDir,
                pageSize
            ).use {
                assertThat(it.pageCount).isEqualTo(text.length / pageSize)
            }
        }
    }

    @Test
    fun `test simple surrogates`() {
        val emoji = "\uD83E\uDD14" // 🤔
        val text = emoji.repeat(16)
        useTempDir { tmpDir ->
            PagedEditSession(
                StringReader(text),
                tmpDir,
                17
            ).use {
                assertThat(it.pageCount).isEqualTo(2)
                assertThat(it.pages[0].charsLength).isEqualTo(18)
                assertThat(it.pages[1].charsLength).isEqualTo(text.length - 18)
            }
        }
    }

    @Test
    fun `test complex emoji pages`() {
        // Emoji 👨‍👩‍👧‍👦, represented by 11 chars
        val text =
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66".repeat(2)
        useTempDir { tmpDir ->
            PagedEditSession(
                StringReader(text),
                tmpDir,
                16
            ).use {
                assertThat(it.pageCount).isEqualTo(1)
                assertThat(
                    it.getPageFileForIndex(0).readText(PagedEditSession.InternalStorageCharset)
                ).isEqualTo(text)
            }
        }
    }

}
