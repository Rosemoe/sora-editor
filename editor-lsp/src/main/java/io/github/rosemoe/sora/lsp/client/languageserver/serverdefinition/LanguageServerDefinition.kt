/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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

package io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition

import android.util.Log
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler

import org.eclipse.lsp4j.ServerCapabilities

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap


/*
* A trait representing a ServerDefinition
*/
abstract class LanguageServerDefinition {
    var ext = "unknown"

    open val exts: List<String>
        get() = listOf(ext)

    open val name: String
        get() = ext

    private val streamConnectionProviders: MutableMap<String, StreamConnectionProvider> =
        ConcurrentHashMap()

    open val disabledFeatures: Set<LspFeature> = emptySet()

    /**
     * Starts a Language server for the given directory and returns a tuple (InputStream, OutputStream)
     *
     * @param workingDir The root directory
     * @return The input and output streams of the server
     * @throws IOException if the stream connection provider is crashed
     */
    @Throws(IOException::class)
    fun start(workingDir: String): Pair<InputStream, OutputStream> {
        var streamConnectionProvider = streamConnectionProviders[workingDir]
        return if (streamConnectionProvider != null && !streamConnectionProvider.isClosed) {
            streamConnectionProvider.inputStream to streamConnectionProvider.outputStream
        } else {
            streamConnectionProvider = streamConnectionProvider ?: createConnectionProvider(workingDir)
            streamConnectionProvider.start()
            streamConnectionProviders[workingDir] = streamConnectionProvider
            streamConnectionProvider.inputStream to streamConnectionProvider.outputStream
        }
    }

    open fun callExitForLanguageServer(): Boolean {
        return true
    }

    /**
     * Stops the Language server corresponding to the given working directory
     *
     * @param workingDir The root directory
     */
    fun stop(workingDir: String) {
        val streamConnectionProvider = streamConnectionProviders[workingDir]
        if (streamConnectionProvider != null) {
            streamConnectionProvider.close()
            streamConnectionProviders.remove(workingDir)
        } else {
            Log.w(
                "LanguageServerDefinition",
                "No connection for workingDir $workingDir and ext $ext"
            )
        }
    }

    open fun getInitializationOptions(uri: URI?): Any? {
        return null
    }

    open fun expectedCapabilities(): ServerCapabilities? {
        return null
    }

    override fun toString(): String {
        return "ServerDefinition(name=$name, ext=$ext)"
    }

    /**
     * Creates a StreamConnectionProvider given the working directory
     *
     * @param workingDir The root directory
     * @return The stream connection provider
     */
    open fun createConnectionProvider(workingDir: String): StreamConnectionProvider {
        throw UnsupportedOperationException()
    }

    open val eventListener: EventHandler.EventListener
        get() = EventHandler.EventListener.DEFAULT

}
