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
import android.util.Log
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
import io.github.rosemoe.sora.lsp.client.languageserver.ServerStatus
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.languageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.semantic.TextMateSemanticTokenStyleProvider
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

class LspTestActivity : BaseEditorActivity() {

    private lateinit var lspEditor: LspEditor
    private lateinit var lspProject: LspProject
    private val projectPath by lazy(LazyThreadSafetyMode.NONE) {
        externalCacheDir?.resolve("testProject")?.absolutePath ?: ""
    }

    private lateinit var rootMenu: Menu

    private val ref = WeakReference(this)

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
            setEditorText()
            connectToLanguageServer()
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


        startService(Intent(this@LspTestActivity, LspLanguageServerService::class.java))

        val luaServerDefinition = languageServerDefinition {
            name("lua-lsp")
            ext("lua")
            connection {
                local("lua-lsp")
            }
            eventListener(EventListener(this@LspTestActivity.ref))

        }

        lspProject = LspProject(projectPath)

        lspProject.addServerDefinition(luaServerDefinition)

        withContext(Dispatchers.Main) {
            lspEditor = lspProject.createEditor("$projectPath/sample.lua")
            val wrapperLanguage = createTextMateLanguage()
            lspEditor.wrapperLanguage = wrapperLanguage
            lspEditor.semanticTokenStyleProvider = TextMateSemanticTokenStyleProvider(ThemeRegistry.getInstance())
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

        try {
            lspEditor.connectWithTimeout()
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
        } else if (id == R.id.restart_server) {
            lifecycleScope.launch(Dispatchers.Main) {
                val languageServerWrapper =
                    lspProject.getLanguageServerWrapper("lua", "lua-lsp") ?: return@launch
                toast("Restarting language server...")
                languageServerWrapper.restartAndReconnect()
                if (languageServerWrapper.status == ServerStatus.INITIALIZED) {
                    toast("Initialized language server")
                } else {
                    toast("Unable to connect language server")
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()

        ref.clear()
        editor.release()
        if (::lspProject.isInitialized) {
            val project = lspProject
            project.coroutineScope.launch(Dispatchers.IO) { project.dispose() }
        }
        stopService(Intent(this@LspTestActivity, LspLanguageServerService::class.java))
    }


    class EventListener(
        private val activityRef: WeakReference<LspTestActivity>,
    ) : EventHandler.EventListener {
        override fun initialize(server: LanguageServer?, result: InitializeResult) {
            val activity = activityRef.get() ?: return
            activity.apply {
                runOnUiThread {
                    if (::rootMenu.isInitialized) {
                        rootMenu.findItem(R.id.code_format).isEnabled =
                            result.capabilities.documentFormattingProvider != null
                    }
                }
            }
        }

        override fun onStatusChange(newStatus: ServerStatus, oldStatus: ServerStatus) {
            Log.d("LSP_TEST_ACTIVITY", "New status: $newStatus; Old status: $oldStatus")
        }
    }
}
