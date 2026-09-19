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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap


class LspLanguageServerService : Service() {
    private var socket: LocalServerSocket? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var acceptJob: Job? = null
    private val clients = ConcurrentHashMap<LocalSocket, Process>()

    companion object {
        private const val TAG = "LanguageServer"
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (socket == null) socket = LocalServerSocket("lua-lsp")

        if (acceptJob?.isActive != true) {
            val serverSocket = socket ?: return START_NOT_STICKY
            acceptJob = serviceScope.launch {
                Log.d(TAG, "Starting accept loop on address ${serverSocket.localSocketAddress.namespace}")
                while (true) {
                    try {
                        val socketClient = serverSocket.accept()
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

    private suspend fun handleClient(socketClient: LocalSocket) = coroutineScope {
        val resources = File(cacheDir, "emmylua").apply { mkdirs() }
        var process: Process? = null
        try {
            val executable = File(applicationInfo.nativeLibraryDir, "libemmylua_ls.so")
            val server = ProcessBuilder(executable.absolutePath, "--resources-path", resources.absolutePath)
                .directory(resources)
                .redirectError(File(resources, "stderr.log"))
                .start()
            process = server
            clients[socketClient] = server
            currentCoroutineContext().ensureActive()
            // Preserve the example's local socket transport; the native server speaks standard LSP stdio.
            val input = launch {
                try {
                    socketClient.inputStream.forwardTo(server.outputStream)
                } catch (e: Exception) {
                    Log.d(TAG, "Client input closed", e)
                } finally {
                    runCatching { server.outputStream.close() }
                }
            }
            try {
                server.inputStream.forwardTo(socketClient.outputStream)
            } finally {
                runCatching { socketClient.close() }
                server.destroy()
                input.cancel()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Language server connection closed", e)
        } finally {
            clients.remove(socketClient)
            process?.destroy()
            runCatching { process?.outputStream?.close() }
            runCatching { process?.inputStream?.close() }
            runCatching { process?.errorStream?.close() }
            runCatching { socketClient.close() }
        }
    }

    private fun InputStream.forwardTo(output: OutputStream) {
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) return
            output.write(buffer, 0, count)
            output.flush()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        acceptJob = null
        runCatching { socket?.close() }
        socket = null
        clients.forEach { (client, process) ->
            runCatching { client.close() }
            process.destroy()
        }
        super.onDestroy()
    }
}
