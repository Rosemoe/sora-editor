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
package io.github.rosemoe.sora.app

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import com.itsaky.androidide.treesitter.java.TSLanguageJava
import io.github.rosemoe.sora.app.databinding.ActivityMainBinding
import io.github.rosemoe.sora.editor.ts.LocalsCaptureSpec
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.KeyBindingEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SideIconClickEvent
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.text.ContentIO
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.utils.CrashHandler
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode
import io.github.rosemoe.sora.widget.subscribeEvent
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.PatternSyntaxException


class MainActivity : AppCompatActivity() {

    companion object {
        init {
            // Load tree-sitter libraries
            System.loadLibrary("android-tree-sitter")
            System.loadLibrary("tree-sitter-java")
        }

    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var searchMenu: PopupMenu
    private var searchOptions = SearchOptions(false, false)
    private var undo: MenuItem? = null
    private var redo: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.INSTANCE.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val inputView = binding.symbolInput
        inputView.bindEditor(binding.editor)
        val typeface = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")
        inputView.addSymbols(
            arrayOf(
                "->",
                "{",
                "}",
                "(",
                ")",
                ",",
                ".",
                ";",
                "\"",
                "?",
                "+",
                "-",
                "*",
                "/"
            ), arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/")
        )
        inputView.forEachButton {
            it.typeface = typeface
        }
        binding.searchEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                tryCommitSearch()
            }
        })
        searchMenu = PopupMenu(this, binding.searchOptions)
        searchMenu.inflate(R.menu.menu_search_options)
        searchMenu.setOnMenuItemClickListener {
            it.isChecked = !it.isChecked
            val ignoreCase = !searchMenu.menu.findItem(R.id.search_option_match_case)!!.isChecked
            if (it.isChecked) {
                when (it.itemId) {
                    R.id.search_option_regex -> {
                        searchMenu.menu.findItem(R.id.search_option_whole_word)!!.isChecked = false
                    }

                    R.id.search_option_whole_word -> {
                        searchMenu.menu.findItem(R.id.search_option_regex)!!.isChecked = false
                    }
                }
            }
            var type = SearchOptions.TYPE_NORMAL
            val regex = searchMenu.menu.findItem(R.id.search_option_regex)!!.isChecked
            if (regex) {
                type = SearchOptions.TYPE_REGULAR_EXPRESSION
            }
            val wholeWord = searchMenu.menu.findItem(R.id.search_option_whole_word)!!.isChecked
            if (wholeWord) {
                type = SearchOptions.TYPE_WHOLE_WORD
            }
            searchOptions = SearchOptions(type, ignoreCase)
            tryCommitSearch()
            true
        }
        binding.editor.apply {
            typefaceText = typeface
            props.stickyScroll = true
            setLineSpacing(2f, 1.1f)
            nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION
            // Update display dynamically
            subscribeEvent<SelectionChangeEvent> { _, _ -> updatePositionText() }
            subscribeEvent<PublishSearchResultEvent> { _, _ -> updatePositionText() }
            subscribeEvent<ContentChangeEvent> { _, _ ->
                postDelayedInLifecycle(
                    ::updateBtnState,
                    50
                )
            }
            subscribeEvent<SideIconClickEvent> { _, _ ->
                Toast.makeText(this@MainActivity, "Side icon clicked", Toast.LENGTH_SHORT).show()
            }

            subscribeEvent<KeyBindingEvent> { event, _ ->
                if (event.eventType != EditorKeyEvent.Type.DOWN) {
                    return@subscribeEvent
                }

                Toast.makeText(
                    context,
                    "Keybinding event: " + generateKeybindingString(event),
                    Toast.LENGTH_LONG
                ).show()
            }

            getComponent<EditorAutoCompletion>()
                .setEnabledAnimation(true)
        }


        loadDefaultThemes()
        loadDefaultLanguages()

        ensureTextmateTheme()

        val editor = binding.editor
        val language = TextMateLanguage.create(
            "source.java", true
        )

        editor.setEditorLanguage(language)

        openAssetsFile("samples/sample.txt")
        updatePositionText()
        updateBtnState()

        switchThemeIfRequired(this, binding.editor)
    }

    private fun tryCommitSearch() {
        val editable = binding.searchEditor.editableText
        if (editable.isNotEmpty()) {
            try {
                binding.editor.searcher.search(
                    editable.toString(),
                    searchOptions
                )
            } catch (e: PatternSyntaxException) {
                // Regex error
            }
        } else {
            binding.editor.searcher.stopSearch()
        }
    }


    private /*suspend*/ fun loadDefaultThemes() /*= withContext(Dispatchers.IO)*/ {

        //add assets file provider
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                applicationContext.assets
            )
        )


        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_drak")
        val themeRegistry = ThemeRegistry.getInstance()
        themes.forEach { name ->
            val path = "textmate/$name.json"
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                    ), name
                ).apply {
                    if (name != "quietlight") {
                        isDark = true
                    }
                }
            )
        }

        themeRegistry.setTheme("quietlight")
    }

    private /*suspend*/ fun loadDefaultLanguages() /*= withContext(Dispatchers.Main)*/ {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    private fun loadDefaultLanguagesWithDSL() {
        GrammarRegistry.getInstance().loadGrammars(
            languages {
                language("java") {
                    grammar = "textmate/java/syntaxes/java.tmLanguage.json"
                    defaultScopeName()
                    languageConfiguration = "textmate/java/language-configuration.json"
                }
                language("kotlin") {
                    grammar = "textmate/kotlin/syntaxes/Kotlin.tmLanguage"
                    defaultScopeName()
                    languageConfiguration = "textmate/kotlin/language-configuration.json"
                }
                language("python") {
                    grammar = "textmate/python/syntaxes/python.tmLanguage.json"
                    defaultScopeName()
                    languageConfiguration = "textmate/python/language-configuration.json"
                }
            }
        )
    }

    private fun resetColorScheme() {
        binding.editor.apply {
            val colorScheme = this.colorScheme
            // reset
            this.colorScheme = colorScheme
        }
    }


    private fun setupDiagnostics() {
        val editor = binding.editor
        val container = DiagnosticsContainer()
        for (i in 0 until editor.text.lineCount) {
            val index = editor.text.getCharIndex(i, 0)
            container.addDiagnostic(
                DiagnosticRegion(
                    index,
                    index + editor.text.getColumnCount(i),
                    DiagnosticRegion.SEVERITY_ERROR
                )
            )
        }
        editor.diagnostics = container
    }

    private fun ensureTextmateTheme() {
        val editor = binding.editor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            editorColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.colorScheme = editorColorScheme
        }
    }

    private fun generateKeybindingString(event: KeyBindingEvent): String {
        val sb = StringBuilder()
        if (event.isCtrlPressed) {
            sb.append("Ctrl + ")
        }

        if (event.isAltPressed) {
            sb.append("Alt + ")
        }

        if (event.isShiftPressed) {
            sb.append("Shift + ")
        }

        sb.append(KeyEvent.keyCodeToString(event.keyCode))
        return sb.toString()
    }

    private fun openAssetsFile(name: String) {
        Thread {
            try {
                val text = ContentIO.createFrom(assets.open(name))
                runOnUiThread {
                    binding.editor.setText(text, null)
                    //setupDiagnostics()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
        updatePositionText()
        updateBtnState()
    }

    private fun updateBtnState() {
        undo?.isEnabled = binding.editor.canUndo()
        redo?.isEnabled = binding.editor.canRedo()
    }

    private fun updatePositionText() {
        val cursor = binding.editor.cursor
        var text =
            (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn + ";" + cursor.left + " "
        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            val content = binding.editor.text
            if (content.getColumnCount(cursor.leftLine) == cursor.leftColumn) {
                "(<" + content.getLine(cursor.leftLine).lineSeparator.let {
                    if (it == LineSeparator.NONE) {
                        "EOF"
                    } else {
                        it.name
                    }
                } + ">)"
            } else {
                val char = binding.editor.text.charAt(
                    cursor.leftLine,
                    cursor.leftColumn
                )
                if (char.isLowSurrogate() && cursor.leftColumn > 0) {
                    "(" + String(
                        charArrayOf(
                            binding.editor.text.charAt(
                                cursor.leftLine,
                                cursor.leftColumn - 1
                            ), char
                        )
                    ) + ")"
                } else if (char.isHighSurrogate() && cursor.leftColumn + 1 < binding.editor.text.getColumnCount(
                        cursor.leftLine
                    )
                ) {
                    "(" + String(
                        charArrayOf(
                            char, binding.editor.text.charAt(
                                cursor.leftLine,
                                cursor.leftColumn + 1
                            )
                        )
                    ) + ")"
                } else {
                    "(" + escapeIfNecessary(
                        binding.editor.text.charAt(
                            cursor.leftLine,
                            cursor.leftColumn
                        )
                    ) + ")"
                }
            }
        }
        val searcher = binding.editor.searcher
        if (searcher.hasQuery()) {
            val idx = searcher.currentMatchedPositionIndex
            val count = searcher.matchedPositionCount
            val matchText = if (count == 0) {
                "no match"
            } else if (count == 1) {
                "1 match"
            } else {
                "$count matches"
            }
            if (idx == -1) {
                text += "($matchText)"
            } else {
                text += "(${idx+1} of $matchText)"
            }
        }
        binding.positionDisplay.text = text
    }

    private fun escapeIfNecessary(c: Char): String {
        return when (c) {
            '\n' -> "\\n"
            '\t' -> "\\t"
            '\r' -> "\\r"
            ' ' -> "<ws>"
            else -> c.toString()
        }
    }

    private val loadTMLLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult


            val editorLanguage = binding.editor.editorLanguage

            val language = if (editorLanguage is TextMateLanguage) {
                editorLanguage.updateLanguage(
                    DefaultGrammarDefinition.withGrammarSource(
                        IGrammarSource.fromInputStream(
                            contentResolver.openInputStream(result),
                            result.path, null
                        ),
                    )
                )
                editorLanguage
            } else {
                TextMateLanguage.create(
                    DefaultGrammarDefinition.withGrammarSource(
                        IGrammarSource.fromInputStream(
                            contentResolver.openInputStream(result),
                            result.path, null
                        ),
                    ), true
                )
            }
            binding.editor.setEditorLanguage(language)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val loadTMTLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult

            ensureTextmateTheme()

            ThemeRegistry.getInstance().loadTheme(
                IThemeSource.fromInputStream(
                    contentResolver.openInputStream(result), result.path,
                    null
                )
            )

            resetColorScheme()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        switchThemeIfRequired(this, binding.editor)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        undo = menu.findItem(R.id.text_undo)
        redo = menu.findItem(R.id.text_redo)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val editor = binding.editor
        when (id) {
            R.id.open_test_activity -> startActivity(Intent(this, TestActivity::class.java))
            R.id.open_lsp_activity -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.not_supported))
                        .setMessage(getString(R.string.dialog_api_warning_msg))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    startActivity(Intent(this, LspTestActivity::class.java))
                }
            }

            R.id.text_undo -> editor.undo()
            R.id.text_redo -> editor.redo()
            R.id.goto_end -> editor.setSelection(
                editor.text.lineCount - 1,
                editor.text.getColumnCount(editor.text.lineCount - 1)
            )

            R.id.move_up -> editor.moveSelectionUp()
            R.id.move_down -> editor.moveSelectionDown()
            R.id.home -> editor.moveSelectionHome()
            R.id.end -> editor.moveSelectionEnd()
            R.id.move_left -> editor.moveSelectionLeft()
            R.id.move_right -> editor.moveSelectionRight()
            R.id.magnifier -> {
                item.isChecked = !item.isChecked
                editor.getComponent(Magnifier::class.java).isEnabled = item.isChecked
            }

            R.id.useIcu -> {
                item.isChecked = !item.isChecked
                editor.props.useICULibToSelectWords = item.isChecked
            }

            R.id.ln_panel_fixed -> {
                val themes = arrayOf(
                    getString(R.string.top),
                    getString(R.string.bottom),
                    getString(R.string.left),
                    getString(R.string.right),
                    getString(R.string.center),
                    getString(R.string.top_left),
                    getString(R.string.top_right),
                    getString(R.string.bottom_left),
                    getString(R.string.bottom_right)
                )
                AlertDialog.Builder(this)
                    .setTitle(R.string.fixed)
                    .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                        editor.lnPanelPositionMode = LineInfoPanelPositionMode.FIXED
                        when (which) {
                            0 -> editor.lnPanelPosition = LineInfoPanelPosition.TOP
                            1 -> editor.lnPanelPosition = LineInfoPanelPosition.BOTTOM
                            2 -> editor.lnPanelPosition = LineInfoPanelPosition.LEFT
                            3 -> editor.lnPanelPosition = LineInfoPanelPosition.RIGHT
                            4 -> editor.lnPanelPosition = LineInfoPanelPosition.CENTER
                            5 -> editor.lnPanelPosition =
                                LineInfoPanelPosition.TOP or LineInfoPanelPosition.LEFT

                            6 -> editor.lnPanelPosition =
                                LineInfoPanelPosition.TOP or LineInfoPanelPosition.RIGHT

                            7 -> editor.lnPanelPosition =
                                LineInfoPanelPosition.BOTTOM or LineInfoPanelPosition.LEFT

                            8 -> editor.lnPanelPosition =
                                LineInfoPanelPosition.BOTTOM or LineInfoPanelPosition.RIGHT
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            R.id.ln_panel_follow -> {
                val themes = arrayOf(
                    getString(R.string.top),
                    getString(R.string.center),
                    getString(R.string.bottom)
                )
                AlertDialog.Builder(this)
                    .setTitle(R.string.fixed)
                    .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                        editor.lnPanelPositionMode = LineInfoPanelPositionMode.FOLLOW
                        when (which) {
                            0 -> editor.lnPanelPosition = LineInfoPanelPosition.TOP
                            1 -> editor.lnPanelPosition = LineInfoPanelPosition.CENTER
                            2 -> editor.lnPanelPosition = LineInfoPanelPosition.BOTTOM
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            R.id.code_format -> editor.formatCodeAsync()
            R.id.switch_language -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.switch_language)
                    .setSingleChoiceItems(
                        arrayOf(
                            "Java",
                            "TextMate Java",
                            "TextMate Kotlin",
                            "TextMate Python",
                            "TextMate Html",
                            "TextMate JavaScript",
                            "TextMate MarkDown",
                            "TM Language from file",
                            "Tree-sitter Java",
                            "None"
                        ), -1
                    ) { dialog: DialogInterface, which: Int ->
                        when (which) {
                            0 -> editor.setEditorLanguage(JavaLanguage())
                            1 -> try {
                                //TextMateLanguage only support TextMateColorScheme
                                ensureTextmateTheme()
                                val editorLanguage = editor.editorLanguage
                                val language = if (editorLanguage is TextMateLanguage) {
                                    editorLanguage.updateLanguage(
                                        "source.java"
                                    )
                                    editorLanguage
                                } else {
                                    TextMateLanguage.create(
                                        "source.java",
                                        true
                                    )
                                }
                                editor.setEditorLanguage(
                                    language
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            2 -> try {
                                ensureTextmateTheme()
                                val editorLanguage = editor.editorLanguage
                                val language = if (editorLanguage is TextMateLanguage) {
                                    editorLanguage.updateLanguage(
                                        "source.kotlin"
                                    )
                                    editorLanguage
                                } else {
                                    TextMateLanguage.create(
                                        "source.kotlin",
                                        true
                                    )
                                }
                                editor.setEditorLanguage(
                                    language
                                )

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            3 -> try {
                                ensureTextmateTheme()
                                val editorLanguage = editor.editorLanguage
                                val language = if (editorLanguage is TextMateLanguage) {
                                    editorLanguage.updateLanguage(
                                        "source.python"
                                    )
                                    editorLanguage
                                } else {
                                    TextMateLanguage.create(
                                        "source.python",
                                        true
                                    )
                                }
                                editor.setEditorLanguage(
                                    language
                                )

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            4 -> try {
                                ensureTextmateTheme()
                                val editorLanguage = editor.editorLanguage
                                val language = if (editorLanguage is TextMateLanguage) {
                                    editorLanguage.updateLanguage(
                                        "text.html.basic"
                                    )
                                    editorLanguage
                                } else {
                                    TextMateLanguage.create(
                                        "text.html.basic",
                                        true
                                    )
                                }
                                editor.setEditorLanguage(
                                    language
                                )

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            5 -> try {
                                ensureTextmateTheme()
                                val editorLanguage = editor.editorLanguage
                                val language = if (editorLanguage is TextMateLanguage) {
                                    editorLanguage.updateLanguage(
                                        "source.js"
                                    )
                                    editorLanguage
                                } else {
                                    TextMateLanguage.create(
                                        "source.js",
                                        true
                                    )
                                }
                                editor.setEditorLanguage(
                                    language
                                )

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            6 -> try {
                                ensureTextmateTheme()
                                val editorLanguage = editor.editorLanguage
                                val language = if (editorLanguage is TextMateLanguage) {
                                    editorLanguage.updateLanguage(
                                        "text.html.markdown"
                                    )
                                    editorLanguage
                                } else {
                                    TextMateLanguage.create(
                                        "text.html.markdown",
                                        true
                                    )
                                }
                                editor.setEditorLanguage(
                                    language
                                )

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            7 -> loadTMLLauncher.launch("*/*")
                            8 -> {
                                val lang = TSLanguageJava.newInstance()
                                editor.setEditorLanguage(TsLanguage(
                                    TsLanguageSpec(
                                        TSLanguageJava.newInstance(),
                                        highlightScmSource = assets.open("tree-sitter-queries/java/highlights.scm").reader().readText(),
                                        codeBlocksScmSource = assets.open("tree-sitter-queries/java/blocks.scm").reader().readText(),
                                        bracketsScmSource = assets.open("tree-sitter-queries/java/brackets.scm").reader().readText(),
                                        localsScmSource = assets.open("tree-sitter-queries/java/locals.scm").reader().readText(),
                                        localsCaptureSpec = object : LocalsCaptureSpec() {

                                            override fun isScopeCapture(captureName: String): Boolean {
                                                return captureName == "scope"
                                            }

                                            override fun isReferenceCapture(captureName: String): Boolean {
                                                return captureName == "reference"
                                            }

                                            override fun isDefinitionCapture(captureName: String): Boolean {
                                                return captureName == "definition.var" || captureName == "definition.field"
                                            }

                                            override fun isMembersScopeCapture(captureName: String): Boolean {
                                                return captureName == "scope.members"
                                            }

                                        }
                                    )) {
                                    TextStyle.makeStyle(EditorColorScheme.COMMENT, 0, false, true, false) applyTo "comment"
                                    TextStyle.makeStyle(EditorColorScheme.KEYWORD, 0, true, false, false) applyTo "keyword"
                                    TextStyle.makeStyle(EditorColorScheme.LITERAL) applyTo arrayOf("constant.builtin", "string", "number")
                                    TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_VAR) applyTo arrayOf("variable.builtin", "variable", "constant")
                                    TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo arrayOf("type.builtin", "type", "attribute")
                                    TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME) applyTo arrayOf("function.method", "function.builtin", "variable.field")
                                    TextStyle.makeStyle(EditorColorScheme.OPERATOR) applyTo "operator"
                                })
                            }

                            else -> editor.setEditorLanguage(EmptyLanguage())
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            R.id.search_panel_st -> {
                if (binding.searchPanel.visibility == View.GONE) {
                    binding.apply {
                        replaceEditor.setText("")
                        searchEditor.setText("")
                        editor.searcher.stopSearch()
                        searchPanel.visibility = View.VISIBLE
                        item.isChecked = true
                    }
                } else {
                    binding.searchPanel.visibility = View.GONE
                    editor.searcher.stopSearch()
                    item.isChecked = false
                }
            }

            R.id.search_am -> {
                binding.replaceEditor.setText("")
                binding.searchEditor.setText("")
                editor.searcher.stopSearch()
                editor.beginSearchMode()
            }

            R.id.switch_colors -> {
                val themes = arrayOf(
                    "Default",
                    "GitHub",
                    "Eclipse",
                    "Darcula",
                    "VS2019",
                    "NotepadXX",
                    "QuietLight for TM(VSCode)",
                    "Darcula for TM",
                    "Abyss for TM",
                    "Solarized(Dark) for TM(VSCode)",
                    "TM theme from file"
                )
                AlertDialog.Builder(this)
                    .setTitle(R.string.color_scheme)
                    .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                        when (which) {
                            0 -> editor.colorScheme = EditorColorScheme()
                            1 -> editor.colorScheme = SchemeGitHub()
                            2 -> editor.colorScheme = SchemeEclipse()
                            3 -> editor.colorScheme = SchemeDarcula()
                            4 -> editor.colorScheme = SchemeVS2019()
                            5 -> editor.colorScheme = SchemeNotepadXX()
                            6 -> try {
                                ensureTextmateTheme()
                                ThemeRegistry.getInstance().setTheme("quietlight")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            7 -> try {
                                ensureTextmateTheme()
                                ThemeRegistry.getInstance().setTheme("darcula")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            8 -> try {
                                ensureTextmateTheme()
                                ThemeRegistry.getInstance().setTheme("abyss")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            9 -> try {
                                ensureTextmateTheme()
                                ThemeRegistry.getInstance().setTheme("solarized_drak")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            10 -> loadTMTLauncher.launch("*/*")
                        }
                        resetColorScheme()
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            R.id.text_wordwrap -> {
                item.isChecked = !item.isChecked
                editor.isWordwrap = item.isChecked
            }

            R.id.completionAnim -> {
                item.isChecked = !item.isChecked
                editor.getComponent<EditorAutoCompletion>()
                    .setEnabledAnimation(item.isChecked)
            }

            R.id.open_logs -> {
                var fis: FileInputStream? = null
                try {
                    fis = openFileInput("crash-journal.log")
                    val br = BufferedReader(InputStreamReader(fis))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        sb.append(line).append('\n')
                    }
                    Toast.makeText(this, "Succeeded", Toast.LENGTH_SHORT).show()
                    editor.setText(sb, null)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed:$e", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                } finally {
                    if (fis != null) {
                        try {
                            fis.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            R.id.clear_logs -> {
                var fos: FileOutputStream? = null
                try {
                    fos = openFileOutput("crash-journal.log", MODE_PRIVATE)
                    Toast.makeText(this, "Succeeded", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed:$e", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                } finally {
                    if (fos != null) {
                        try {
                            fos.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            R.id.open_debug_logs -> {
                //ignored
                //editor.setText(Logs.getLogs());
            }

            R.id.editor_line_number -> {
                editor.isLineNumberEnabled = !editor.isLineNumberEnabled
                item.isChecked = editor.isLineNumberEnabled
            }

            R.id.pin_line_number -> {
                editor.setPinLineNumber(!editor.isLineNumberPinned)
                item.isChecked = editor.isLineNumberPinned
            }

            R.id.load_test_file -> {
                openAssetsFile("samples/big_sample.txt")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun gotoNext(view: View?) {
        try {
            binding.editor.searcher.gotoNext()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun gotoLast(view: View?) {
        try {
            binding.editor.searcher.gotoPrevious()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replace(view: View?) {
        try {
            binding.editor.searcher.replaceThis(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replaceAll(view: View?) {
        try {
            binding.editor.searcher.replaceAll(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun showSearchOptions(view: View?) {
        searchMenu.show()
    }
}