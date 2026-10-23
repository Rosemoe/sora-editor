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

package io.github.rosemoe.sora.widget.layout

import android.view.View
import com.google.common.truth.Truth.assertThat
import io.github.rosemoe.sora.widget.CodeEditor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class WordwrapLayoutTest {

    @Test
    fun `edit during async break does not install stale rows`() {
        val editor = CodeEditor(RuntimeEnvironment.getApplication())
        editor.setText("word ".repeat(80))
        editor.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        editor.layout(0, 0, 1080, 1920)
        val text = editor.text
        pendingTasks.clear()
        val layout = ManualWordwrapLayout(editor)

        // Break the 400-char line, then shrink it before the results are installed
        runPendingTasks()
        text.delete(0, 20, 0, 400)
        layout.afterDelete(text, 0, 20, 0, 400, "")
        ShadowLooper.idleMainLooper()
        runPendingTasks()
        ShadowLooper.idleMainLooper()

        assertThat(layout.rowCount).isGreaterThan(0)
        for (i in 0 until layout.rowCount) {
            val row = layout.getRowAt(i)
            assertThat(row.endColumn).isAtMost(text.getColumnCount(row.lineIndex))
        }
    }

    private fun runPendingTasks() {
        val tasks = pendingTasks.toList()
        pendingTasks.clear()
        tasks.forEach { it.run() }
    }

    /** Holds break tasks until the test runs them, instead of running them on the shared executor */
    private class ManualWordwrapLayout(editor: CodeEditor) :
        WordwrapLayout(editor, editor.text, false, false, null, false) {
        override fun submitTask(task: LayoutTask<*>) {
            pendingTasks.add(task)
        }
    }

    private companion object {
        // Filled from the WordwrapLayout constructor, before subclass fields are initialized
        val pendingTasks = mutableListOf<Runnable>()
    }
}
