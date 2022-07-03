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

import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import io.github.rosemoe.sora.lsp.client.connection.SocketStreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.mock.MockLanguageConnection
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.*
import java.io.*
import java.net.ServerSocket
import kotlin.concurrent.thread

class LspTestActivity : AppCompatActivity() {
    private lateinit var editor: CodeEditor

    private lateinit var lspEditor: LspEditor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editor = CodeEditor(this)
        setContentView(editor)
        editor.typefaceText = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")

        connectLanguageServer()
    }

    private fun randomPort():Int {
        val serverSocket = ServerSocket(0) //读取空闲的可用端口

        val port = serverSocket.localPort
        serverSocket.close()
        return port
    }

    private fun connectLanguageServer() {

        val port = randomPort()


        thread {
            MockLanguageConnection.createConnect(port)
        }

        val serverDefinition = CustomLanguageServerDefinition(".lua") {
            SocketStreamConnectionProvider {
                port
            }
        }

        thread {
            val wrapper = LanguageServerWrapper(serverDefinition, "")

            wrapper.start();

            runOnUiThread {
                lspEditor = LspEditor(editor, "", "")
                thread {
                    wrapper.connect(lspEditor)
                }
            }

        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_lsp, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.code_format) {
            editor.formatCodeAsync()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        editor.release()
    }
}