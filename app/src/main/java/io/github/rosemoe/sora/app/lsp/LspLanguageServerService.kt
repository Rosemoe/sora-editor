/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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

package io.github.rosemoe.sora.app.lsp

import android.app.Service
import android.content.Intent
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.IBinder
import android.util.Log
import com.tang.vscode.LuaLanguageClient
import com.tang.vscode.LuaLanguageServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.util.concurrent.Future


class LspLanguageServerService : Service() {
    private lateinit var socket: LocalServerSocket

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var acceptJob: Job? = null

    companion object {
        private const val TAG = "LanguageServer"
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::socket.isInitialized) {
            socket = LocalServerSocket("lua-lsp")
        }

        if (acceptJob == null) {
            acceptJob = serviceScope.launch {
                Log.d(TAG, "Starting accept loop on address ${socket.localSocketAddress.namespace}")
                while (true) {
                    try {
                        val socketClient = socket.accept()
                        Log.d(TAG, "Accepted client $socketClient")
                        launch { handleClient(socketClient) }
                    } catch (e: Exception) {
                        Log.d(TAG, "Error accepting connection", e)
                        break
                    }
                }
            }
        }

        return START_STICKY
    }

    private suspend fun handleClient(socketClient: LocalSocket) {
        var future: Future<Void>? = null

        val server = LuaLanguageServer()

        runCatching {

            val inputStream = socketClient.inputStream
            val outputStream = socketClient.outputStream

            val launcher = Launcher.createLauncher(
                server, LuaLanguageClient::class.java,
                inputStream, outputStream
            )

            server.connect(launcher.remoteProxy)

            future = launcher.startListening()

            // Suspend until the session ends, without blocking the dispatcher thread
            withContext(Dispatchers.IO) {
                future?.get()
            }
        }.onFailure {
            Log.d(TAG, "Unexpected exception in Language Server client thread.", it)
        }

        Log.d(TAG, "Closed client $socketClient")
        future?.cancel(true)
        runCatching { socketClient.close() }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        runCatching { socket.close() }
        super.onDestroy()
    }
}
