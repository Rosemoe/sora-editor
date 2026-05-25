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

@file:OptIn(ExperimentalEditorApi::class)

package io.github.rosemoe.sora.compose

import android.util.Log
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer

private const val IO_BUFFER_SIZE = 64 * 1024

/**
 * Read text from the given [inputStream] and set it as the content of the editor.
 *
 * The [inputStream] will be closed automatically after reading.
 * This function executes on [Dispatchers.IO].
 */
suspend fun CodeEditorState.readTextFrom(inputStream: InputStream) = withContext(Dispatchers.IO) {
    inputStream
        .reader()
        .buffered(bufferSize = IO_BUFFER_SIZE)
        .use { reader ->
            val builder = StringBuilder(IO_BUFFER_SIZE)
            val buffer = CharArray(IO_BUFFER_SIZE)

            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                builder.appendRange(buffer, 0, read)
            }

            setText(builder)
        }
}

/**
 * Read text from the given [file] and set it as the content of the editor.
 *
 * This function executes on [Dispatchers.IO].
 *
 * @param file The file to read from. It must be a file and must be readable.
 * @throws java.io.IOException if an I/O error occurs.
 */
suspend fun CodeEditorState.readTextFrom(file: File) = withContext(Dispatchers.IO) {
    file.inputStream().use { readTextFrom(it) }
}

/**
 * Write the current content of the editor to the given [file].
 *
 * This function executes on [Dispatchers.IO].
 *
 * @param file The file to write to.
 * @throws java.io.IOException if an I/O error occurs.
 */
suspend fun CodeEditorState.writeTextTo(file: File) = withContext(Dispatchers.IO) {
    file
        .outputStream()
        .buffered(bufferSize = IO_BUFFER_SIZE)
        .writer()
        .use { text.writeTo(it) }
}

/**
 * Write the current content of the editor to the given [outputStream].
 *
 * The [outputStream] will be closed automatically after writing.
 * This function executes on [Dispatchers.IO].
 */
suspend fun CodeEditorState.writeTextTo(outputStream: OutputStream) = withContext(Dispatchers.IO) {
    outputStream
        .writer()
        .buffered(bufferSize = IO_BUFFER_SIZE)
        .use { text.writeTo(it) }
}

/**
 * Efficiently writes a CharSequence without creating huge temporary Strings.
 */
private fun CharSequence.writeTo(
    writer: Writer,
    bufferSize: Int = IO_BUFFER_SIZE
) {
    when (this) {

        is String -> {
            var offset = 0

            while (offset < length) {
                val len = minOf(bufferSize, length - offset)
                writer.write(this, offset, len)
                offset += len
            }
        }

        else -> {
            val buffer = CharArray(bufferSize)
            var offset = 0
            while (offset < length) {
                val len = minOf(bufferSize, length - offset)
                for (i in 0 until len) {
                    buffer[i] = this[offset + i]
                }
                writer.write(buffer, 0, len)
                offset += len
            }
        }
    }

    writer.flush()
}

/**
 * Clear the content of the editor.
 */
fun CodeEditorState.clear() = setText(null)

/**
 * Request focus for the editor composable.
 */
fun CodeEditorState.requestFocus() = host.requestFocus()

/**
 * Observe the content of the editor as a [Flow].
 *
 * The current [Content] is emitted immediately upon collection, followed by
 * any subsequent changes to the editor's text.
 */
val CodeEditorState.content: Flow<Content>
    get() = callbackFlow {
        val receipt = subscribeAlways<ContentChangeEvent> { event ->
            trySend(event.editor.text).onFailure { cause ->
                Log.w(
                    "CodeEditorState",
                    "Failed to emit content change because the flow is no longer accepting values.",
                    cause
                )
            }
        }

        trySend(text).onFailure { cause ->
            Log.w(
                "CodeEditorState",
                "Failed to emit initial editor content.",
                cause
            )
        }

        awaitClose {
            receipt.unsubscribe()
        }
    }
