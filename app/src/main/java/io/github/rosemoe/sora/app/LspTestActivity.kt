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
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.lsp.client.connection.SocketStreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspEditorManager
import io.github.rosemoe.sora.lsp.mock.MockLanguageConnection
import io.github.rosemoe.sora.text.ContentCreator
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket
import java.util.zip.ZipFile

class LspTestActivity : AppCompatActivity() {
    private lateinit var editor: CodeEditor

    private lateinit var lspEditor: LspEditor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editor = CodeEditor(this)

        setContentView(editor)

        val font = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")

        editor.apply {
            typefaceText = font
            typefaceLineNumber = font
        }

        lifecycleScope.launch {

            unAssets()

            setEditorText()

            connectToLanguageServer()

        }
    }

    private suspend fun setEditorText() {
        val text = withContext(Dispatchers.IO) {
            ContentCreator.fromStream(
                externalCacheDir?.resolve("testProject/sample.xml")?.inputStream()
            )
        }
        editor.setText(text, null)
    }

    private suspend fun unAssets() = withContext(Dispatchers.IO) {

        val zipFile = ZipFile(packageResourcePath)
        val zipEntries = zipFile.entries()
        while (zipEntries.hasMoreElements()) {
            val zipEntry = zipEntries.nextElement()
            val fileName = zipEntry.name
            if (fileName.startsWith("assets/testProject/")) {
                val inputStream = zipFile.getInputStream(zipEntry)
                //这里编译器会帮你优化掉 不用担心
                val filePath = externalCacheDir?.resolve(fileName.substring("assets/".length))
                filePath?.parentFile?.mkdirs()
                val outputStream = FileOutputStream(filePath)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            }
        }
        zipFile.close()
    }

    private suspend fun connectToLanguageServer() = withContext(Dispatchers.IO) {

        val port = randomPort()

        val projectPath = externalCacheDir?.resolve("testProject")?.absolutePath ?: ""


        lifecycleScope.launch(Dispatchers.IO) {
            //FIXME: The language server should be started in another process, consider using service instead of thread
            MockLanguageConnection.createConnect(port)
        }

        val serverDefinition = CustomLanguageServerDefinition(".xml") {
            SocketStreamConnectionProvider {
                port
            }
        }


        withContext(Dispatchers.Main) {
            lspEditor = LspEditorManager.getOrCreateEditorManager(projectPath).createEditor(
                editor,
                "$projectPath/sample.xml",
                serverDefinition
            )
        }


        lifecycleScope.launch(Dispatchers.IO) {
            lspEditor.connect()
        }


    }

    private fun randomPort(): Int {
        val serverSocket = ServerSocket(0)

        val port = serverSocket.localPort
        serverSocket.close()
        return port
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