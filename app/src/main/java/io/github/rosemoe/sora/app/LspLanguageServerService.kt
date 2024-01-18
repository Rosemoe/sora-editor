/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tang.vscode.LuaLanguageClient
import com.tang.vscode.LuaLanguageServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.net.ServerSocket
import java.nio.channels.AsynchronousByteChannel
import java.nio.channels.Channels
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class LspLanguageServerService : Service() {

    companion object {
        private const val TAG = "LanguageServer"
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Only used in test

        thread {
            val port = intent?.getIntExtra("port", 0) ?: 0

            val socket = ServerSocket(port)

            Log.d(TAG, "Starting socket on port ${socket.localPort}")

            val socketClient = socket.accept()

            Log.d(TAG, "connected to the client on port ${socketClient.port}")

            runCatching {

                val server = LuaLanguageServer();

                val inputStream = socketClient.getInputStream()
                val outputStream = socketClient.getOutputStream()

                val launcher = Launcher.createLauncher(
                    server, LuaLanguageClient::class.java,
                    inputStream, outputStream
                )

                val remoteProxy = launcher.remoteProxy

                server.connect(remoteProxy);

                launcher.startListening()
                    .get()

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


}

