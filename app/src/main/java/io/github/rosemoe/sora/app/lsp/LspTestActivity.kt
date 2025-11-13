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

package io.github.rosemoe.sora.app.lsp

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.app.BaseEditorActivity
import io.github.rosemoe.sora.app.R
import io.github.rosemoe.sora.app.switchThemeIfRequired
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.lsp.client.connection.LocalSocketStreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.editor.text.MarkdownCodeHighlighterRegistry
import io.github.rosemoe.sora.lsp.editor.text.withEditorHighlighter
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.code.codeAction
import io.github.rosemoe.sora.lsp.utils.asLspRange
import io.github.rosemoe.sora.text.ContentIO
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

class LspTestActivity : BaseEditorActivity() {

    private lateinit var lspEditor: LspEditor
    private lateinit var lspProject: LspProject

    private lateinit var rootMenu: Menu


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("LSP Test - Kotlin")

        val font = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")

        editor.apply {
            typefaceText = font
            typefaceLineNumber = font
        }

        ensureTextmateTheme()
        switchThemeIfRequired(this, editor)

        lifecycleScope.launch {
            unAssets()
            connectToLanguageServer()
            setEditorText()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        switchThemeIfRequired(this, editor)
    }

    private suspend fun setEditorText() {
        val text = withContext(Dispatchers.IO) {
            ContentIO.createFrom(
                externalCacheDir?.resolve("testProject/sample.lua")!!.inputStream()
            )
        }
        editor.setText(text, null)
        editor.getComponent<EditorAutoCompletion>().setEnabledAnimation(true)
        editor.getComponent<EditorTextActionWindow>().isEnabled = false
    }

    private suspend fun unAssets() = withContext(Dispatchers.IO) {
        //externalCacheDir?.deleteRecursively()
        val zipFile = ZipFile(packageResourcePath)
        val zipEntries = zipFile.entries()
        while (zipEntries.hasMoreElements()) {
            val zipEntry = zipEntries.nextElement()
            val fileName = zipEntry.name
            if (fileName.startsWith("assets/testProject/")) {
                val inputStream = zipFile.getInputStream(zipEntry)
                //The compiler will be optimized here, don't worry
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
        withContext(Dispatchers.Main) {
            toast("(Kotlin Activity) Starting Language Server...")
            editor.editable = false
        }

        val projectPath = externalCacheDir?.resolve("testProject")?.absolutePath ?: ""

        startService(
            Intent(this@LspTestActivity, LspLanguageServerService::class.java)
        )

        val luaServerDefinition =
            object : CustomLanguageServerDefinition(
                "lua",
                ServerConnectProvider {
                    LocalSocketStreamConnectionProvider("lua-lsp")
                }
            ) {
                /* override fun getInitializationOptions(uri: URI?): Any {
                     return InitializationOption(
                         stdFolder = "file:/$projectPath/std/Lua53",
                     )
                 }*/

                private val _eventListener = EventListener(this@LspTestActivity)

                override val eventListener: EventHandler.EventListener
                    get() = _eventListener

            }

        lspProject = LspProject(projectPath)

        lspProject.addServerDefinition(luaServerDefinition)



        withContext(Dispatchers.Main) {
            lspEditor = lspProject.createEditor("$projectPath/sample.lua")
            val wrapperLanguage = createTextMateLanguage()
            lspEditor.wrapperLanguage = wrapperLanguage
            lspEditor.editor = editor
            lspEditor.isEnableInlayHint = true
            LspEditorTextActionWindow(lspEditor).setOnMoreButtonClickListener { window, lspEditor ->
                lspEditor.coroutineScope.launch {
                    lspEditor.eventManager.emitAsync(EventType.codeAction) {
                        put(editor.cursor.range.asLspRange())
                    }
                }
            }
        }

        var connected: Boolean

        // delay(Timeout[Timeouts.INIT].toLong()) //wait for server start

        try {
            lspEditor.connectWithTimeout()

            lspEditor.requestManager?.didChangeWorkspaceFolders(
                DidChangeWorkspaceFoldersParams().apply {
                    this.event = WorkspaceFoldersChangeEvent().apply {
                        added =
                            listOf(WorkspaceFolder("file://$projectPath/std/Lua53", "MyLuaProject"))
                    }
                }
            )

            connected = true

        } catch (e: Exception) {
            connected = false
            e.printStackTrace()
        }

        lifecycleScope.launch(Dispatchers.Main) {
            if (connected) {
                toast("Initialized Language server")
            } else {
                toast("Unable to connect language server")
            }
            editor.editable = true
        }
    }

    private fun toast(text: String) {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun createTextMateLanguage(): TextMateLanguage {
        GrammarRegistry.getInstance().loadGrammars(
            languages {
                language("lua") {
                    grammar = "textmate/lua/syntaxes/lua.tmLanguage.json"
                    scopeName = "source.lua"
                    languageConfiguration = "textmate/lua/language-configuration.json"
                }
            }
        )

        MarkdownCodeHighlighterRegistry.global.withEditorHighlighter { languageName ->
            if (languageName == "lua") {
                Pair(
                    TextMateLanguage.create("source.lua", false),
                    TextMateColorScheme.create(ThemeRegistry.getInstance()).apply {
                    }
                )
            } else null
        }

        return TextMateLanguage.create(
            "source.lua", false
        )
    }

    private fun ensureTextmateTheme() {
        var editorColorScheme = editor.colorScheme

        if (editorColorScheme is TextMateColorScheme) {
            return
        }

        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                assets
            )
        )

        val themeRegistry = ThemeRegistry.getInstance()

        val path = "textmate/ayu-dark.json"
        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                ), "ayu-dark"
            )
        )

        themeRegistry.setTheme("ayu-dark")

        editorColorScheme = TextMateColorScheme.create(themeRegistry)
        editor.colorScheme = editorColorScheme

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_lsp, menu)
        rootMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.code_format) {
            val cursor = editor.text.cursor
            if (cursor.isSelected) {
                editor.formatCodeAsync(cursor.left(), cursor.right())
            } else {
                editor.formatCodeAsync()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()

        editor.release()
        lifecycleScope.launch {
            lspEditor.dispose()
            lspProject.dispose()
        }
        stopService(Intent(this@LspTestActivity, LspLanguageServerService::class.java))
    }


    class EventListener(
        activity: LspTestActivity
    ) : EventHandler.EventListener {
        private val activityRef = WeakReference(activity)
        override fun initialize(server: LanguageServer?, result: InitializeResult) {
            activityRef.get()?.apply {
                runOnUiThread {
                    rootMenu.findItem(R.id.code_format).isEnabled =
                        result.capabilities.documentFormattingProvider != null
                }
            }
        }
    }


}

