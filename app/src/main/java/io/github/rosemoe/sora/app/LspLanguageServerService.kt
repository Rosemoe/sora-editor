/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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
import android.os.Debug
import android.os.IBinder
import kotlinx.coroutines.runBlocking
import org.eclipse.lemminx.XMLServerLauncher
import java.lang.RuntimeException
import java.net.ServerSocket
import java.security.Permission
import kotlin.concurrent.thread

class LspLanguageServerService : Service() {


    private lateinit var hookThread: Thread

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Only used in test



        thread {
            val port = intent?.getIntExtra("port", 0) ?: 0

            val socket = ServerSocket(port)

            val socketClient = socket.accept()


            runCatching {

                XMLServerLauncher.launch(
                    socketClient.getInputStream(),
                    socketClient.getOutputStream()
                ).get()
            }.onFailure {
                it.printStackTrace()
            }

            socketClient.close()

            socket.close()


        }

        return START_STICKY
    }



}

