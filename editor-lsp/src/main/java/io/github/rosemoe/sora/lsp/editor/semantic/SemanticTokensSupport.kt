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

package io.github.rosemoe.sora.lsp.editor.semantic

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.rosemoe.sora.lang.styling.patching.SparseStylePatches
import io.github.rosemoe.sora.lang.styling.patching.StylePatchProvider
import io.github.rosemoe.sora.lang.styling.patching.StylePatchRequest
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensDeltaParams
import org.eclipse.lsp4j.SemanticTokensLegend
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SemanticTokensRangeParams
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions

/** One document's request, server-local delta base and style snapshot. State is confined to Main. */
internal class SemanticTokensSupport(private val owner: LspEditor) : StylePatchProvider {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var editorRef = WeakReference<CodeEditor>(null)
    private val editor get() = editorRef.get()
    private var job: Job? = null
    private var server: RequestManager? = null
    private data class Snapshot(
        val tokens: SemanticTokens, val legend: SemanticTokensLegend,
        val content: Content, val version: Long, val lineLengths: IntArray, val lineStarts: IntArray
    )
    private var snapshot: Snapshot? = null
    private var styleJob: Job? = null
    private var styleProvider: SemanticTokenStyleProvider? = null
    private var styleSubscription: AutoCloseable? = null
    private var patches = SparseStylePatches.EMPTY
    private var version = -1L

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post { action() }
    }

    fun attach(target: CodeEditor) {
        onMain {
            if (editor === target) return@onMain
            detachCurrent()
            editorRef = WeakReference(target)
            target.registerStylePatchProvider(this@SemanticTokensSupport)
        }
    }

    fun detach() {
        // Cleanup must still run when the project coroutine scope is being canceled.
        onMain { detachCurrent() }
    }

    private fun detachCurrent() {
        job?.cancel()
        job = null
        styleJob?.cancel()
        styleJob = null
        styleSubscription?.close()
        styleSubscription = null
        styleProvider = null
        editor?.unregisterStylePatchProvider(this)
        editorRef.clear()
        server = null
        snapshot = null
        patches = SparseStylePatches.EMPTY
        version = -1
    }

    fun refresh() {
        onMain {
            job?.cancel()
            val target = editor ?: return@onMain
            bindStyleProvider()
            if (!owner.isEnableSemanticTokens || !owner.isConnected || styleProvider == null) {
                snapshot = null
                styleJob?.cancel()
                patches = SparseStylePatches.EMPTY
                version = target.text.documentVersion
                target.refreshStylePatches(this@SemanticTokensSupport)
                return@onMain
            }
            job = owner.coroutineScope.launch(Dispatchers.Main.immediate) {
                delay(50)
                owner.uiDelegate.contentChangeReceiver.awaitChanges()
                val manager = owner.requestManager.semanticTokensManager
                if (manager == null) {
                    snapshot = null
                    server = null
                    styleJob?.cancel()
                    patches = SparseStylePatches.EMPTY
                    version = target.text.documentVersion
                    target.refreshStylePatches(this@SemanticTokensSupport)
                    return@launch
                }
                if (server !== manager) {
                    server = manager
                    snapshot = null
                }
                val options = manager.semanticTokensOptions ?: return@launch
                val content = target.text
                val requestedVersion = content.documentVersion
                val lengths = IntArray(content.lineCount) { content.getColumnCount(it) }
                var offset = 0
                val starts = IntArray(content.lineCount) {
                    val start = offset
                    offset += lengths[it] + content.getLine(it).lineSeparator.length
                    start
                }
                val previous = snapshot?.tokens
                try {
                    val next = withContext(Dispatchers.Default) {
                        withTimeout(Timeout[Timeouts.SEMANTIC_TOKENS, owner].toLong()) {
                            request(manager, options, previous, lengths)
                        }
                    }
                    ensureActive()
                    if (editor !== target || target.text !== content || content.documentVersion != requestedVersion ||
                        owner.requestManager.semanticTokensManager !== manager) return@launch
                    snapshot = Snapshot(next, options.legend, content, requestedVersion, lengths, starts)
                    restyle()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snapshot = null // Retry with a full result next time; never reuse a broken delta base.
                    Log.w("LspSemanticTokens", "Semantic token request failed", e)
                }
            }
        }
    }

    private fun bindStyleProvider() {
        val provider = owner.semanticTokenStyleProvider
        if (provider === styleProvider) return
        styleSubscription?.close()
        styleProvider = provider
        styleSubscription = provider?.observeChanges(::restyle)
    }

    /** Reuse server classifications when only the language theme changes. */
    fun restyle() {
        onMain {
            styleJob?.cancel()
            val target = editor ?: return@onMain
            bindStyleProvider()
            val provider = styleProvider
            if (provider == null || !owner.isEnableSemanticTokens) {
                job?.cancel()
                patches = SparseStylePatches.EMPTY
                version = target.text.documentVersion
                target.refreshStylePatches(this@SemanticTokensSupport)
                return@onMain
            }
            val cached = snapshot
            if (cached == null) {
                if (job?.isActive != true) refresh()
                return@onMain
            }
            if (cached.content !== target.text || cached.version != target.text.documentVersion) return@onMain
            styleJob = owner.coroutineScope.launch(Dispatchers.Main.immediate) {
                try {
                    val decorations = withContext(Dispatchers.Default) {
                        SemanticTokensDecoder.decode(cached.tokens.data, cached.legend,
                            cached.lineLengths, cached.lineStarts, provider, owner.languageId ?: owner.fileExt)
                    }
                    ensureActive()
                    if (editor !== target || snapshot !== cached || cached.content !== target.text ||
                        cached.version != target.text.documentVersion || styleProvider !== provider) return@launch
                    patches = decorations
                    version = cached.version
                    target.refreshStylePatches(this@SemanticTokensSupport)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snapshot = null
                    Log.w("LspSemanticTokens", "Semantic token styling failed", e)
                }
            }
        }
    }

    private suspend fun request(
        manager: RequestManager, options: SemanticTokensWithRegistrationOptions,
        previous: SemanticTokens?, lengths: IntArray
    ): SemanticTokens {
        val identifier = owner.uri.createTextDocumentIdentifier()
        val full = options.full?.left == true || options.full?.right != null
        if (full && options.full?.right?.delta == true && previous?.resultId != null) {
            val result = manager.semanticTokensFullDelta(SemanticTokensDeltaParams(identifier, previous.resultId))?.await()
            if (result?.isLeft == true) return result.left ?: SemanticTokens(emptyList())
            if (result?.isRight == true) {
                val delta = result.right
                try {
                    val data = SemanticTokensDecoder.applyDelta(previous.data, delta.edits)
                    require(data.size % 5 == 0)
                    return SemanticTokens(delta.resultId, data)
                } catch (_: IllegalArgumentException) {
                    // An invalid edit sequence cannot be applied safely; obtain a fresh full result.
                }
            } else return SemanticTokens(emptyList())
        }
        return if (full) {
            manager.semanticTokensFull(SemanticTokensParams(identifier))?.await()
        } else {
            // Range-only servers can cover the entire document; scrolling then needs no new request.
            manager.semanticTokensRange(SemanticTokensRangeParams(identifier,
                Range(Position(0, 0), Position(lengths.lastIndex, lengths.last()))))?.await()
        } ?: SemanticTokens(emptyList())
    }

    override fun provideStylePatches(editor: CodeEditor, request: StylePatchRequest, receiver: StylePatchProvider.Receiver) {
        // These snapshots cover the whole document. Scrolling does not change them; publishing
        // them again would invalidate every cached render node on each viewport change.
        if (request.reason == StylePatchRequest.Reason.VISIBLE_RANGE_CHANGED) return
        if (request.reason == StylePatchRequest.Reason.TEXT_CHANGED) {
            job?.cancel()
            styleJob?.cancel()
            version = -1
            // StylePatchManager shifts the existing decorations. Request only after didChange.
            return
        }
        if (version == editor.text.documentVersion) receiver.set(patches)
        if (request.reason == StylePatchRequest.Reason.INITIAL) refresh()
    }
}
