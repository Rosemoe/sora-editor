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

import io.github.rosemoe.sora.text.ContentIO
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.text.BreakIterator
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

class PagedEditSession @Throws(IOException::class) constructor(
    source: Reader,
    val tmpDir: File,
    pageSize: Int = DefaultPageSize
) : Closeable {

    companion object {
        const val DefaultPageSize = 10000000
        const val MinPageSize = 16
        val InternalStorageCharset = Charsets.UTF_16BE
        private const val PagePrefix = "page-"
        private const val SwapTmpPrefix = "tmp-"
        private const val NumberPadLen = 5

        @Throws(IOException::class)
        fun restoreSessionFile(tmpDir: File, outputFile: File, charset: Charset = Charsets.UTF_8) {
            outputFile.writer(charset).use { output ->
                tmpDir.listFiles()?.filter {
                    it.name.startsWith(PagePrefix)
                }?.sortedBy {
                    outputFile.name.removePrefix(PagePrefix).toInt()
                }?.forEach { file ->
                    file.reader(InternalStorageCharset).use {
                        it.copyTo(output)
                    }
                }
            }
        }
    }

    private val operationLock = ReentrantLock()

    internal val pages = mutableListOf<Page>()

    private var tmpId = 0

    val pageCount: Int
        get() = pages.size

    init {
        if (pageSize < MinPageSize) {
            throw IllegalArgumentException("Page size must be at least $MinPageSize")
        }
        tmpDir.mkdirs()

        val buffer = CharBuffer.wrap(CharArray(8192))
        var count = source.read(buffer.array(), buffer.arrayOffset(), buffer.limit())
        if (count >= 0) {
            buffer.limit(count)
        }

        var currPageIndex = 0
        var currWritten = 0
        var currOutput = getPageFileForIndex(currPageIndex).writer(InternalStorageCharset)
        val directWriteSize = pageSize - MinPageSize
        val itr = BreakIterator.getCharacterInstance()

        while (true) {
            val charsToWrite = (directWriteSize - currWritten).coerceIn(0, buffer.remaining())
            var needInput = false
            if (charsToWrite == 0 && (buffer.hasRemaining() || !buffer.hasRemaining() && count == -1)) {
                // Direct write range is full
                if (buffer.remaining() < 2 * MinPageSize && count != -1) {
                    // Need more input for character range detection
                    needInput = true
                } else {
                    var pageLength = currWritten
                    if (buffer.hasRemaining()) {
                        val limit = buffer.remaining().coerceAtMost(MinPageSize * 2)
                        val text = buffer.substring(0, limit)
                        itr.setText(text)
                        val nextBoundary =
                            itr.following((MinPageSize - 1).coerceAtMost(text.length))
                        val sliceLength = if (nextBoundary == BreakIterator.DONE) {
                            text.length
                        } else {
                            nextBoundary
                        }
                        currOutput.write(text.substring(0, sliceLength))
                        pageLength += sliceLength
                        buffer.position(buffer.position() + sliceLength)
                    }

                    currOutput.flush()
                    currOutput.close()
                    pages.add(Page(pageLength.toLong()))
                    if (!buffer.hasRemaining() && count == -1) {
                        break
                    }
                    currOutput = getPageFileForIndex(++currPageIndex).writer(InternalStorageCharset)
                    currWritten = 0
                }
            } else if (charsToWrite > 0) {
                currOutput.write(
                    buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    charsToWrite
                )
                buffer.position(buffer.position() + charsToWrite)
                currWritten += charsToWrite
            }
            if ((needInput || !buffer.hasRemaining()) && count != -1) {
                buffer.compact()
                val remaining = buffer.limit() - buffer.position()
                if (remaining > 0) {
                    count = source.read(
                        buffer.array(),
                        buffer.arrayOffset() + buffer.position(),
                        remaining
                    )
                    buffer.limit(buffer.position() + count.coerceAtLeast(0))
                    buffer.position(0)
                }
            }
        }
    }

    internal fun getPageFileForIndex(index: Int): File {
        val pageFile = tmpDir.resolve("$PagePrefix${index.toString().padStart(NumberPadLen, '0')}")
        return pageFile
    }

    private fun newTmpFile(): File {
        val pageFile =
            tmpDir.resolve("$SwapTmpPrefix-${(tmpId++).toString().padStart(NumberPadLen, '0')}")
        return pageFile
    }

    @Throws(IOException::class)
    suspend fun loadPageToEditor(pageIndex: Int, editor: CodeEditor) {
        val page = pages[pageIndex]
        val content = withContext(Dispatchers.IO) {
            operationLock.withLock {
                InputStreamReader(
                    FileInputStream(getPageFileForIndex(pageIndex)),
                    InternalStorageCharset
                ).use {
                    ContentIO.createFrom(it)
                }
            }
        }
        withContext(Dispatchers.Main) {
            editor.setText(content)
        }
    }

    @Throws(IOException::class)
    suspend fun unloadPageFromEditor(pageIndex: Int, editor: CodeEditor) {
        val page = pages[pageIndex]
        val text = editor.text.copyTextShallow()
        withContext(Dispatchers.IO) {
            operationLock.withLock {
                val tmp = newTmpFile()
                FileOutputStream(tmp).use {
                    ContentIO.writeTo(text, it, InternalStorageCharset, true)
                }
                val pageFile = getPageFileForIndex(pageIndex)
                page.charsLength = text.length.toLong()
                pageFile.delete()
                tmp.renameTo(pageFile)
            }
            text.release()
        }
    }

    @Throws(IOException::class)
    suspend fun writeTo(file: File, charset: Charset = Charsets.UTF_8) {
        withContext(Dispatchers.IO) {
            file.writer(charset).use {
                writeTo(it)
            }
        }
    }

    @Throws(IOException::class)
    suspend fun writeTo(output: Writer) {
        withContext(Dispatchers.IO) {
            operationLock.withLock {
                pages.indices.forEach { index ->
                    val pageFile = getPageFileForIndex(index)
                    pageFile.reader(InternalStorageCharset).use {
                        it.copyTo(output)
                    }
                }
            }
        }
    }

//    fun isModified() = pages.any { it.isModified }

    override fun close() {
        tmpDir.deleteRecursively()
    }

    internal data class Page(
        var charsLength: Long,
//        var isModified: Boolean = false
    )

}