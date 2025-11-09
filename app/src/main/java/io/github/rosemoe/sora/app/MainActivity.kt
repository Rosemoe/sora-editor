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

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.dingyi222666.monarch.languages.JavaLanguage
import io.github.dingyi222666.monarch.languages.KotlinLanguage
import io.github.dingyi222666.monarch.languages.PythonLanguage
import io.github.dingyi222666.monarch.languages.TypescriptLanguage
import io.github.rosemoe.sora.app.databinding.ActivityMainBinding
import io.github.rosemoe.sora.app.lsp.LspTestActivity
import io.github.rosemoe.sora.app.lsp.LspTestJavaActivity
import io.github.rosemoe.sora.app.tests.TestActivity
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.KeyBindingEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SideIconClickEvent
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.JavaLanguageSpec
import io.github.rosemoe.sora.lang.TsLanguageJava
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.TextInlayHint
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.monarch.MonarchColorScheme
import io.github.rosemoe.sora.langs.monarch.MonarchLanguage
import io.github.rosemoe.sora.langs.monarch.registry.MonarchGrammarRegistry
import io.github.rosemoe.sora.langs.monarch.registry.dsl.monarchLanguages
import io.github.rosemoe.sora.langs.monarch.registry.model.ThemeSource
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
import io.github.rosemoe.sora.util.regex.RegexBackrefGrammar
import io.github.rosemoe.sora.utils.CrashHandler
import io.github.rosemoe.sora.utils.codePointStringAt
import io.github.rosemoe.sora.utils.escapeCodePointIfNecessary
import io.github.rosemoe.sora.utils.toast
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import io.github.rosemoe.sora.widget.SelectionMovement
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.ext.EditorSpanInteractionHandler
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import java.util.regex.PatternSyntaxException

/**
 * Demo and debug Activity for the code editor
 */
class MainActivity : AppCompatActivity() {

    companion object {
        init {
            // Load tree-sitter libraries
            System.loadLibrary("android-tree-sitter")
            System.loadLibrary("tree-sitter-java")
        }

        private const val TAG = "MainActivity"
        const val LOG_FILE = "crash-journal.log"

        /**
         * Symbols to be displayed in symbol input view
         */
        val SYMBOLS = arrayOf(
            "->", "{", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )

        /**
         * Texts to be committed to editor for symbols above
         */
        val SYMBOL_INSERT_TEXT = arrayOf(
            "\t", "{}", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )

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

        setSupportActionBar(binding.activityToolbar)
        applyEdgeToEdgeForViews(binding.toolbarContainer, binding.root)

        val typeface = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")


        // Setup Listeners
        binding.apply {
            btnGotoPrev.setOnClickListener(::gotoPrev)
            btnGotoNext.setOnClickListener(::gotoNext)
            btnReplace.setOnClickListener(::replace)
            btnReplaceAll.setOnClickListener(::replaceAll)
            searchOptions.setOnClickListener(::showSearchOptions)
        }

        // Configure symbol input view
        val inputView = binding.symbolInput
        inputView.bindEditor(binding.editor)
        inputView.addSymbols(SYMBOLS, SYMBOL_INSERT_TEXT)
        inputView.forEachButton { it.typeface = typeface }

        // Commit search when text changed
        binding.searchEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                tryCommitSearch()
            }
        })

        // Search options
        searchMenu = PopupMenu(this, binding.searchOptions)
        searchMenu.inflate(R.menu.menu_search_options)
        searchMenu.setOnMenuItemClickListener {
            // Update option states
            it.isChecked = !it.isChecked
            if (it.isChecked) {
                // Regex and whole word mode can not be both chose
                when (it.itemId) {
                    R.id.search_option_regex -> {
                        searchMenu.menu.findItem(R.id.search_option_whole_word)!!.isChecked = false
                    }

                    R.id.search_option_whole_word -> {
                        searchMenu.menu.findItem(R.id.search_option_regex)!!.isChecked = false
                    }
                }
            }
            // Update search options and commit search with the new options
            computeSearchOptions()
            tryCommitSearch()
            true
        }

        // Configure editor
        binding.editor.apply {
            registerInlayHintRenderer(TextInlayHintRenderer)
            typefaceText = typeface
            props.stickyScroll = true
            setLineSpacing(2f, 1.1f)
            nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION or CodeEditor.FLAG_DRAW_SOFT_WRAP
            // Update display dynamically
            // Use CodeEditor#subscribeEvent to add listeners of different events to editor
            subscribeAlways<SelectionChangeEvent> { updatePositionText() }
            subscribeAlways<PublishSearchResultEvent> { updatePositionText() }
            subscribeAlways<ContentChangeEvent> {
                postDelayedInLifecycle(
                    ::updateBtnState,
                    50
                )
            }
            subscribeAlways<SideIconClickEvent> {
                toast(R.string.tip_side_icon)
            }
            subscribeAlways<TextSizeChangeEvent> { event ->
                Log.d(
                    TAG,
                    "TextSizeChangeEvent onReceive() called with: oldTextSize = [${event.oldTextSize}], newTextSize = [${event.newTextSize}]"
                )
            }

            subscribeAlways<KeyBindingEvent> { event ->
                if (event.eventType == EditorKeyEvent.Type.DOWN) {
                    toast(
                        "Keybinding event: " + generateKeybindingString(event),
                        Toast.LENGTH_LONG
                    )
                }
            }

            // Handle span interactions
            EditorSpanInteractionHandler(this)
            getComponent<EditorAutoCompletion>()
                .setEnabledAnimation(true)
        }

        // Load textmate themes and grammars
        setupTextmate()

        // Load monarch themes and grammars
        setupMonarch()

        // Before using Textmate Language, TextmateColorScheme should be applied
        ensureTextmateTheme()

        // Set editor language to textmate Java
        val editor = binding.editor
        val language = TextMateLanguage.create(
            "source.java", true
        )
        editor.setEditorLanguage(language)

        // Open assets file
        openAssetsFile("samples/sample.txt")

        updatePositionText()
        updateBtnState()

        switchThemeIfRequired(this, binding.editor)
    }

    /**
     * Generate new [SearchOptions] for text searching in editor
     */
    private fun computeSearchOptions() {
        val caseInsensitive = !searchMenu.menu.findItem(R.id.search_option_match_case)!!.isChecked
        var type = SearchOptions.TYPE_NORMAL
        val regex = searchMenu.menu.findItem(R.id.search_option_regex)!!.isChecked
        if (regex) {
            type = SearchOptions.TYPE_REGULAR_EXPRESSION
        }
        val wholeWord = searchMenu.menu.findItem(R.id.search_option_whole_word)!!.isChecked
        if (wholeWord) {
            type = SearchOptions.TYPE_WHOLE_WORD
        }
        searchOptions = SearchOptions(type, caseInsensitive, RegexBackrefGrammar.DEFAULT)
    }

    /**
     * Commit a text search to editor
     */
    private fun tryCommitSearch() {
        val query = binding.searchEditor.editableText
        if (query.isNotEmpty()) {
            try {
                binding.editor.searcher.search(
                    query.toString(),
                    searchOptions
                )
            } catch (e: PatternSyntaxException) {
                // Regex error
            }
        } else {
            binding.editor.searcher.stopSearch()
        }
    }

    /**
     * Setup Textmate. Load our grammars and themes from assets
     */
    private fun setupTextmate() {
        // Add assets file provider so that files in assets can be loaded
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                applicationContext.assets // use application context
            )
        )
        loadDefaultTextMateThemes()
        loadDefaultTextMateLanguages()
    }


    /**
     * Load default textmate themes
     */
    private /*suspend*/ fun loadDefaultTextMateThemes() /*= withContext(Dispatchers.IO)*/ {
        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_dark")
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

    /**
     * Load default languages from JSON configuration
     *
     * @see loadDefaultLanguagesWithDSL Load by Kotlin DSL
     */
    private /*suspend*/ fun loadDefaultTextMateLanguages() /*= withContext(Dispatchers.Main)*/ {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    /**
     * Setup monarch. Load our grammars and themes from assets
     */
    private fun setupMonarch() {
        // Add assets file provider so that files in assets can be loaded
        io.github.rosemoe.sora.langs.monarch.registry.FileProviderRegistry.addProvider(
            io.github.rosemoe.sora.langs.monarch.registry.provider.AssetsFileResolver(
                applicationContext.assets // use application context
            )
        )
        loadDefaultMonarchThemes()
        loadDefaultMonarchLanguages()
    }


    /**
     * Load default monarch themes
     *
     */
    private /*suspend*/ fun loadDefaultMonarchThemes() /*= withContext(Dispatchers.IO)*/ {
        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_dark")

        themes.forEach { name ->
            val path = "textmate/$name.json"
            io.github.rosemoe.sora.langs.monarch.registry.ThemeRegistry.loadTheme(
                io.github.rosemoe.sora.langs.monarch.registry.model.ThemeModel(
                    ThemeSource(path, name)
                ).apply {
                    if (name != "quietlight") {
                        isDark = true
                    }
                }, false
            )
        }

        io.github.rosemoe.sora.langs.monarch.registry.ThemeRegistry.setTheme("quietlight")
    }

    /**
     * Load default languages from Monarch
     */
    private fun loadDefaultMonarchLanguages() {
        MonarchGrammarRegistry.INSTANCE.loadGrammars(
            monarchLanguages {
                language("java") {
                    monarchLanguage = JavaLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/java/language-configuration.json"
                }
                language("kotlin") {
                    monarchLanguage = KotlinLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/kotlin/language-configuration.json"
                }
                language("python") {
                    monarchLanguage = PythonLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/python/language-configuration.json"
                }
                language("typescript") {
                    monarchLanguage = TypescriptLanguage
                    defaultScopeName()
                    languageConfiguration = "textmate/javascript/language-configuration.json"
                }
            }
        )
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

    /**
     * Re-apply color scheme
     */
    private fun resetColorScheme() {
        binding.editor.apply {
            val colorScheme = this.colorScheme
            // reset
            this.colorScheme = colorScheme
        }
    }

    /**
     * Add diagnostic items to editor. For debug only.
     */
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

    /**
     * Ensure the editor uses a [TextMateColorScheme]
     */
    private fun ensureTextmateTheme() {
        val editor = binding.editor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            editorColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.colorScheme = editorColorScheme
        }
    }

    /**
     * Ensure the editor uses a [MonarchColorScheme]
     */
    private fun ensureMonarchTheme() {
        val editor = binding.editor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is MonarchColorScheme) {
            editorColorScheme =
                MonarchColorScheme.create(io.github.rosemoe.sora.langs.monarch.registry.ThemeRegistry.currentTheme)
            editor.colorScheme = editorColorScheme
            switchThemeIfRequired(this, editor)
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

    /**
     * Open file from assets, and set editor text
     */
    private fun openAssetsFile(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val text = ContentIO.createFrom(assets.open(name))
            withContext(Dispatchers.Main) {
                binding.editor.setText(text, null)

                updatePositionText()
                updateBtnState()

                if ("big_sample" !in name) {
                    binding.editor.inlayHints = InlayHintsContainer().also {
                        it.add(TextInlayHint(28, 0, "unit:"))
                        it.add(TextInlayHint(28, 7, "open"))
                        it.add(TextInlayHint(28, 22, "^class"))
                    }
                }
            }
        }
    }

    /**
     * Update buttons state for undo/redo
     */
    private fun updateBtnState() {
        undo?.isEnabled = binding.editor.canUndo()
        redo?.isEnabled = binding.editor.canRedo()
    }

    /**
     * Update editor position tracker text
     */
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
                "(" + content.getLine(cursor.leftLine)
                    .codePointStringAt(cursor.leftColumn)
                    .escapeCodePointIfNecessary() + ")"
            }
        }

        // Indicator for text matching
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
            text += if (idx == -1) {
                "($matchText)"
            } else {
                "(${idx + 1} of $matchText)"
            }
        }

        binding.positionDisplay.text = text
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
            R.id.open_test_activity -> startActivity<TestActivity>()
            R.id.open_lsp_activity -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.not_supported))
                        .setMessage(getString(R.string.dialog_api_warning_msg))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_lsp_entry_title)
                        .setMessage(R.string.dialog_lsp_entry_msg)
                        .setPositiveButton(R.string.choice_yes) { _, _ ->
                            startActivity<LspTestActivity>()
                        }
                        .setNegativeButton(R.string.choice_no) { _, _ ->
                            startActivity<LspTestJavaActivity>()
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                }
            }

            R.id.text_undo -> editor.undo()
            R.id.text_redo -> editor.redo()
            R.id.goto_end -> editor.setSelection(
                editor.text.lineCount - 1,
                editor.text.getColumnCount(editor.text.lineCount - 1)
            )

            R.id.move_up -> editor.moveSelection(SelectionMovement.UP)
            R.id.move_down -> editor.moveSelection(SelectionMovement.DOWN)
            R.id.home -> editor.moveSelection(SelectionMovement.LINE_START)
            R.id.end -> editor.moveSelection(SelectionMovement.LINE_END)
            R.id.move_left -> editor.moveSelection(SelectionMovement.LEFT)
            R.id.move_right -> editor.moveSelection(SelectionMovement.RIGHT)
            R.id.magnifier -> {
                item.isChecked = !item.isChecked
                editor.getComponent(Magnifier::class.java).isEnabled = item.isChecked
            }

            R.id.useIcu -> {
                item.isChecked = !item.isChecked
                editor.props.useICULibToSelectWords = item.isChecked
            }

            R.id.ln_panel_fixed -> chooseLineNumberPanelPosition()

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
            R.id.switch_language -> chooseLanguage()
            R.id.search_panel_st -> toggleSearchPanel(item)

            R.id.search_am -> {
                binding.replaceEditor.setText("")
                binding.searchEditor.setText("")
                editor.searcher.stopSearch()
                editor.beginSearchMode()
            }

            R.id.switch_colors -> chooseTheme()

            R.id.text_wordwrap -> {
                item.isChecked = !item.isChecked
                editor.isWordwrap = item.isChecked
            }

            R.id.completionAnim -> {
                item.isChecked = !item.isChecked
                editor.getComponent<EditorAutoCompletion>()
                    .setEnabledAnimation(item.isChecked)
            }

            R.id.open_logs -> openLogs()
            R.id.clear_logs -> clearLogs()

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

            R.id.softKbdEnabled -> {
                editor.isSoftKeyboardEnabled = !editor.isSoftKeyboardEnabled
                item.isChecked = editor.isSoftKeyboardEnabled
            }

            R.id.disableSoftKbdOnHardKbd -> {
                editor.isDisableSoftKbdIfHardKbdAvailable =
                    !editor.isDisableSoftKbdIfHardKbdAvailable
                item.isChecked = editor.isDisableSoftKbdIfHardKbdAvailable
            }

            R.id.switch_typeface -> chooseTypeface()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun chooseTypeface() {
        val fonts = arrayOf(
            "JetBrains Mono",
            "Ubuntu",
            "Roboto"
        )
        val assetsPaths = arrayOf(
            "JetBrainsMono-Regular.ttf",
            "Ubuntu-Regular.ttf",
            "Roboto-Regular.ttf"
        )
        AlertDialog.Builder(this)
            .setTitle(android.R.string.dialog_alert_title)
            .setSingleChoiceItems(fonts, -1) { dialog: DialogInterface, which: Int ->
                if (which in assetsPaths.indices) {
                    binding.editor.typefaceText =
                        Typeface.createFromAsset(assets, assetsPaths[which])
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun chooseLineNumberPanelPosition() {
        val editor = binding.editor
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

    private fun toggleSearchPanel(item: MenuItem) {
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
            binding.editor.searcher.stopSearch()
            item.isChecked = false
        }
    }

    private fun openLogs() {
        runCatching {
            openFileInput(LOG_FILE).reader().readText()
        }.onSuccess {
            binding.editor.setText(it)
        }.onFailure {
            toast(it.toString())
        }
    }

    private fun clearLogs() {
        runCatching {
            openFileOutput(LOG_FILE, MODE_PRIVATE)?.use {}
        }.onFailure {
            toast(it.toString())
        }.onSuccess {
            toast(R.string.deleting_log_success)
        }
    }

    private fun chooseLanguage() {
        val editor = binding.editor
        val languageOptions = arrayOf(
            "Java",
            "TextMate Java",
            "TextMate Kotlin",
            "TextMate Python",
            "TextMate Html",
            "TextMate JavaScript",
            "TextMate MarkDown",
            "TM Language from file",
            "Tree-sitter Java",
            "Monarch Java",
            "Monarch Kotlin",
            "Monarch Python",
            "Monarch TypeScript",
            "Text"
        )
        val tmLanguages = mapOf(
            "TextMate Java" to Pair("source.java", "source.java"),
            "TextMate Kotlin" to Pair("source.kotlin", "source.kotlin"),
            "TextMate Python" to Pair("source.java", "source.java"),
            "TextMate Html" to Pair("text.html.basic", "text.html.basic"),
            "TextMate JavaScript" to Pair("source.js", "source.js"),
            "TextMate MarkDown" to Pair("text.html.markdown", "text.html.markdown")
        )

        val monarchLanguages = mapOf(
            "Monarch Java" to "source.java",
            "Monarch Kotlin" to "source.kotlin",
            "Monarch Python" to "source.python",
            "Monarch TypeScript" to "source.typescript"
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.switch_language)
            .setSingleChoiceItems(languageOptions, -1) { dialog: DialogInterface, which: Int ->
                when (val selected = languageOptions[which]) {
                    in tmLanguages -> {
                        val info = tmLanguages[selected]!!
                        try {
                            ensureTextmateTheme()
                            val editorLanguage = editor.editorLanguage
                            val language = if (editorLanguage is TextMateLanguage) {
                                editorLanguage.updateLanguage(info.first)
                                editorLanguage
                            } else {
                                TextMateLanguage.create(info.second, true)
                            }
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    in monarchLanguages -> {
                        val info = monarchLanguages[selected]!!

                        try {
                            ensureMonarchTheme()

                            val editorLanguage = editor.editorLanguage

                            val language = if (editorLanguage is MonarchLanguage) {
                                editorLanguage.updateLanguage(info)
                                editorLanguage
                            } else {
                                MonarchLanguage.create(info, true)
                            }
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    else -> {
                        when (selected) {
                            "Java" -> editor.setEditorLanguage(JavaLanguage())
                            "Text" -> editor.setEditorLanguage(EmptyLanguage())
                            "TM Language from file" -> loadTMLLauncher.launch("*/*")
                            "Tree-sitter Java" -> {
                                editor.setEditorLanguage(
                                    TsLanguageJava(
                                        JavaLanguageSpec(
                                            highlightScmSource = assets.open("tree-sitter-queries/java/highlights.scm")
                                                .reader().readText(),
                                            codeBlocksScmSource = assets.open("tree-sitter-queries/java/blocks.scm")
                                                .reader().readText(),
                                            bracketsScmSource = assets.open("tree-sitter-queries/java/brackets.scm")
                                                .reader().readText(),
                                            localsScmSource = assets.open("tree-sitter-queries/java/locals.scm")
                                                .reader().readText()
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun chooseTheme() {
        val editor = binding.editor
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
                        ThemeRegistry.getInstance().setTheme("solarized_dark")
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

    fun gotoNext(view: View) {
        try {
            binding.editor.searcher.gotoNext()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun gotoPrev(view: View) {
        try {
            binding.editor.searcher.gotoPrevious()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replace(view: View) {
        try {
            binding.editor.searcher.replaceCurrentMatch(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replaceAll(view: View) {
        try {
            binding.editor.searcher.replaceAll(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun showSearchOptions(view: View) {
        searchMenu.show()
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

}