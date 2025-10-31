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
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.util.concurrent.Future
import kotlin.concurrent.thread


class LspLanguageServerService : Service() {

    private lateinit var future: Future<Void>
    private lateinit var socket: LocalServerSocket
    private lateinit var socketClient: LocalSocket

    companion object {
        private const val TAG = "LanguageServer"
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Only used in test
        thread {
            socket = LocalServerSocket("lua-lsp")

            Log.d(TAG, "Starting socket on address ${socket.localSocketAddress}")

            socketClient = socket.accept()

            runCatching {

                val server = LuaLanguageServer();

                val inputStream = socketClient.inputStream
                val outputStream = socketClient.outputStream

                val launcher = Launcher.createLauncher(
                    server, LuaLanguageClient::class.java,
                    inputStream, outputStream
                )

                val remoteProxy = launcher.remoteProxy

                server.connect(remoteProxy);

                future = launcher.startListening()

                // Blocking call
                future.get()

                /* XMLServerLauncher.launch(
                     socketClient.getInputStream(),
                     socketClient.getOutputStream()
                 ).get()*/
            }.onFailure {
                Log.d(TAG, "Unexpected exception is thrown in the Language Server Thread.", it)
            }

            socketClient.close()

            socket.close()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        future.cancel(true)
        socket.close()
        socketClient.close()
        super.onDestroy()
    }


}

