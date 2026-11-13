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
package io.github.rosemoe.sora.widget

import android.os.Looper
import android.view.inputmethod.EditorInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Input methods address text by index, and see the '\r' and '\n' of a CRLF as two characters, so
 * they can point between them. Each test is an input method call sequence that used to end in a
 * StringIndexOutOfBoundsException from Content.checkLineAndColumn.
 */
@RunWith(RobolectricTestRunner::class)
class EditorCrlfImeTest {

    // a0 b1 \r2 \n3 \r4 \n5 c6 d7: indexes 3 and 5 are inside a line break.
    private val text = "ab\r\n\r\ncd"

    private lateinit var editor: CodeEditor
    private lateinit var connection: EditorInputConnection

    @Before
    fun setUp() {
        editor = CodeEditor(RuntimeEnvironment.getApplication())
        editor.setText(text)
        // The layout is built off the main thread, and the editor is not editable until it is
        val deadline = System.currentTimeMillis() + 10_000
        while (!editor.isEditable) {
            check(System.currentTimeMillis() < deadline) { "layout never finished" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        connection = editor.onCreateInputConnection(EditorInfo()) as EditorInputConnection
    }

    @Test
    fun `setSelection inside a CRLF puts the caret before it`() {
        connection.setSelection(3, 3)
        assertThat(editor.cursor.leftLine).isEqualTo(0)
        assertThat(editor.cursor.leftColumn).isEqualTo(2)
    }

    @Test
    fun `setSelection of a region with an end inside a CRLF takes the whole break`() {
        connection.setSelection(3, 5)
        assertThat(editor.cursor.left().run { line to column }).isEqualTo(0 to 2)
        assertThat(editor.cursor.right().run { line to column }).isEqualTo(2 to 0)
    }

    @Test
    fun `backspace after setSelection inside a CRLF deletes the character before it`() {
        connection.setSelection(3, 3)
        connection.deleteSurroundingText(1, 0)
        assertThat(editor.text.toString()).isEqualTo("a\r\n\r\ncd")
    }

    @Test
    fun `commitText after setSelection inside a CRLF inserts before it`() {
        connection.setSelection(5, 5)
        connection.commitText("x", 1)
        assertThat(editor.text.toString()).isEqualTo("ab\r\nx\r\ncd")
    }

    @Test
    fun `replaceText ending inside a CRLF takes the whole break`() {
        connection.replaceText(4, 5, "x", 1, null)
        assertThat(editor.text.toString()).isEqualTo("ab\r\nxcd")
    }

    @Test
    fun `replaceText at an index inside a CRLF inserts before it`() {
        connection.replaceText(5, 5, "x", 1, null)
        assertThat(editor.text.toString()).isEqualTo("ab\r\nx\r\ncd")
    }

    @Test
    fun `forward delete at the end of a CRLF line deletes the break`() {
        editor.setSelection(0, 2)
        connection.deleteSurroundingText(0, 1)
        assertThat(editor.text.toString()).isEqualTo("ab\r\ncd")
    }

    @Test
    fun `backspace at the start of a CRLF line while composing deletes the break`() {
        editor.setSelection(2, 0)
        connection.setComposingRegion(6, 8)
        connection.deleteSurroundingText(1, 0)
        assertThat(editor.text.toString()).isEqualTo("ab\r\ncd")
    }
}
