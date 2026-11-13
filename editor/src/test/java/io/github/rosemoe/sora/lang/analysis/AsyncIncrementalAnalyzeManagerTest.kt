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
package io.github.rosemoe.sora.lang.analysis

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
class AsyncIncrementalAnalyzeManagerTest {

    private class PlainTextAnalyzer : AsyncIncrementalAnalyzeManager<Int, Span>() {
        override fun getInitialState() = 0

        override fun stateEquals(state: Int?, another: Int?) = state == another

        override fun tokenizeLine(line: CharSequence, state: Int, lineIndex: Int) =
            IncrementalAnalyzeManager.LineTokenizeResult<Int, Span>(0, null, mutableListOf(SpanFactory.obtain(0, 0L)))

        override fun generateSpansForLine(tokens: IncrementalAnalyzeManager.LineTokenizeResult<Int, Span>): List<Span> =
            tokens.spans

        override fun computeBlocks(text: Content, delegate: CodeBlockAnalyzeDelegate): List<CodeBlock> = emptyList()
    }

    /**
     * TextMateAnalyzer.tokenizeLine is synchronized on the manager, so the analysis thread holds this
     * monitor for as long as a line takes to tokenize. An edit, which arrives on the UI thread, must
     * not have to wait for it.
     */
    @Test
    fun `edits do not wait for the manager's monitor`() {
        val manager = PlainTextAnalyzer()
        manager.reset(ContentReference(Content("first\nsecond")), Bundle())

        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val tokenizer = thread {
            synchronized(manager) {
                held.countDown()
                release.await()
            }
        }
        held.await()

        val edited = CountDownLatch(1)
        val editor = thread {
            manager.insert(CharPosition(0, 5, 5), CharPosition(0, 6, 6), "!")
            manager.delete(CharPosition(0, 5, 5), CharPosition(0, 6, 6), "!")
            manager.rerun()
            edited.countDown()
        }

        try {
            assertThat(edited.await(2, TimeUnit.SECONDS)).isTrue()
        } finally {
            release.countDown()
            tokenizer.join()
            editor.join()
            manager.destroy()
        }
    }
}
