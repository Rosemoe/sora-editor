/*
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
 */
package io.github.rosemoe.sora.app

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import io.github.rosemoe.sora.app.databinding.ActivityMainBinding
import io.github.rosemoe.sora.event.*
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.text.ContentCreator
import org.eclipse.tm4e.core.internal.theme.reader.ThemeReader
import io.github.rosemoe.sora.utils.CrashHandler
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.schemes.*
import io.github.rosemoe.sorakt.subscribeEvent
import java.io.*
import java.util.regex.PatternSyntaxException


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var undo: MenuItem? = null
    private var redo: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.INSTANCE.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val inputView = binding.symbolInput
        inputView.bindEditor(binding.editor)
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
        binding.searchEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.isNotEmpty()) {
                    try {
                        binding.editor.searcher.search(
                            editable.toString(),
                            EditorSearcher.SearchOptions(true, true)
                        )
                    } catch (e: PatternSyntaxException) {
                        // Regex error
                    }
                } else {
                    binding.editor.searcher.stopSearch()
                }
            }
        })
        binding.editor.apply {
            typefaceText = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")
            setLineSpacing(2f, 1.1f)
            nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION
            // Update display dynamically
            subscribeEvent<SelectionChangeEvent> { _, _ -> updatePositionText() }
            subscribeEvent<ContentChangeEvent> { _, _ ->
                postDelayed(
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
        }

        ensureTextmateTheme()
        val editor = binding.editor
        val language: Language = TextMateLanguage.create(
            "java.tmLanguage.json",
            assets.open("textmate/java/syntaxes/java.tmLanguage.json"),
            InputStreamReader(assets.open("textmate/java/language-configuration.json")),
            (editor.colorScheme as TextMateColorScheme).rawTheme
        )
        editor.setEditorLanguage(language)

        openAssetsFile("sample.txt")
        updatePositionText()
        updateBtnState()
    }

    private fun setupDiagnostics() {
        val editor = binding.editor
        val container = DiagnosticsContainer()
        for (i in 0 until editor.text.lineCount) {
            val index = editor.text.getCharIndex(i, 0)
            container.addDiagnostic(DiagnosticRegion(
                index,
                index + editor.text.getColumnCount(i),
                DiagnosticRegion.SEVERITY_ERROR
            ))
        }
        editor.diagnostics = container
    }

    private fun ensureTextmateTheme() {
        val editor = binding.editor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            val iRawTheme = ThemeReader.readThemeSync(
                "QuietLight.tmTheme",
                assets.open("textmate/QuietLight.tmTheme")
            )
            editorColorScheme = TextMateColorScheme.create(iRawTheme)
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
                val text = ContentCreator.fromStream(assets.open(name))
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
        if (undo == null) {
            return
        }
        undo!!.isEnabled = binding.editor.canUndo()
        redo!!.isEnabled = binding.editor.canRedo()
    }

    private fun updatePositionText() {
        val cursor = binding.editor.cursor
        var text = (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn
        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            "(" + escapeIfNecessary(
                binding.editor.text.charAt(
                    cursor.leftLine,
                    cursor.leftColumn
                )
            ) + ")"
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
            //TextMateLanguage only support TextMateColorScheme
            var editorColorScheme = binding.editor.colorScheme
            if (editorColorScheme !is TextMateColorScheme) {
                val iRawTheme = ThemeReader.readThemeSync(
                    "QuietLight.tmTheme",
                    assets.open("textmate/QuietLight.tmTheme")
                )
                editorColorScheme = TextMateColorScheme.create(iRawTheme)
                binding.editor.colorScheme = editorColorScheme
            }
            val language: Language = TextMateLanguage.create(
                result.path,
                contentResolver.openInputStream(result),
                (editorColorScheme as TextMateColorScheme).rawTheme
            )
            binding.editor.setEditorLanguage(language)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val loadTMTLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult
            val iRawTheme = ThemeReader.readThemeSync(
                result.path, contentResolver.openInputStream(result)
            )
            val colorScheme = TextMateColorScheme.create(iRawTheme)
            binding.editor.colorScheme = colorScheme
            val language = binding.editor.editorLanguage
            if (language is TextMateLanguage) {
                language.updateTheme(iRawTheme)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        if (id == R.id.open_test_activity) {
            startActivity(Intent(this, TestActivity::class.java))
        } else if (id == R.id.open_lsp_activity) {
            startActivity(Intent(this, LspTestActivity::class.java))
        } else if (id == R.id.text_undo) {
            editor.undo()
        } else if (id == R.id.text_redo) {
            editor.redo()
        } else if (id == R.id.goto_end) {
            editor.setSelection(
                editor.text.lineCount - 1,
                editor.text.getColumnCount(editor.text.lineCount - 1)
            )
        } else if (id == R.id.move_up) {
            editor.moveSelectionUp()
        } else if (id == R.id.move_down) {
            editor.moveSelectionDown()
        } else if (id == R.id.home) {
            editor.moveSelectionHome()
        } else if (id == R.id.end) {
            editor.moveSelectionEnd()
        } else if (id == R.id.move_left) {
            editor.moveSelectionLeft()
        } else if (id == R.id.move_right) {
            editor.moveSelectionRight()
        } else if (id == R.id.magnifier) {
            item.isChecked = !item.isChecked
            editor.getComponent(Magnifier::class.java).isEnabled = item.isChecked
        } else if (id == R.id.useIcu) {
            item.isChecked = !item.isChecked
            editor.props.useICULibToSelectWords = item.isChecked
        } else if (id == R.id.code_format) {
            editor.formatCodeAsync()
        } else if (id == R.id.switch_language) {
            AlertDialog.Builder(this)
                .setTitle(R.string.switch_language)
                .setSingleChoiceItems(
                    arrayOf(
                        "Java",
                        "TextMate Java",
                        "TextMate Kotlin",
                        "TextMate Python",
                        "TM Language from file",
                        "None"
                    ), -1
                ) { dialog: DialogInterface, which: Int ->
                    when (which) {
                        0 -> editor.setEditorLanguage(JavaLanguage())
                        1 -> try {
                            //TextMateLanguage only support TextMateColorScheme
                            ensureTextmateTheme()
                            val language: Language = TextMateLanguage.create(
                                "java.tmLanguage.json",
                                assets.open("textmate/java/syntaxes/java.tmLanguage.json"),
                                InputStreamReader(assets.open("textmate/java/language-configuration.json")),
                                (editor.colorScheme as TextMateColorScheme).rawTheme
                            )
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        2 -> try {
                            ensureTextmateTheme()
                            val language: Language = TextMateLanguage.create(
                                "Kotlin.tmLanguage",
                                assets.open("textmate/kotlin/syntaxes/Kotlin.tmLanguage"),
                                InputStreamReader(assets.open("textmate/kotlin/language-configuration.json")),
                                (editor.colorScheme as TextMateColorScheme).rawTheme
                            )
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        3 -> try {
                            ensureTextmateTheme()
                            val language: Language = TextMateLanguage.create(
                                "Python.tmLanguage.json",
                                assets.open("textmate/python/syntaxes/python.tmLanguage.json"),
                                InputStreamReader(assets.open("textmate/python/language-configuration.json")),
                                (editor.colorScheme as TextMateColorScheme).rawTheme
                            )
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        4 -> loadTMLLauncher.launch("*/*")
                        else -> editor.setEditorLanguage(EmptyLanguage())
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else if (id == R.id.search_panel_st) {
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
        } else if (id == R.id.search_am) {
            binding.replaceEditor.setText("")
            binding.searchEditor.setText("")
            editor.searcher.stopSearch()
            editor.beginSearchMode()
        } else if (id == R.id.switch_colors) {
            val themes = arrayOf(
                "Default",
                "GitHub",
                "Eclipse",
                "Darcula",
                "VS2019",
                "NotepadXX",
                "QuietLight for TM",
                "Darcula for TM",
                "Abyss for TM",
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
                            val iRawTheme = ThemeReader.readThemeSync(
                                "QuietLight.tmTheme",
                                assets.open("textmate/QuietLight.tmTheme")
                            )
                            val colorScheme = TextMateColorScheme.create(iRawTheme)
                            editor.colorScheme = colorScheme
                            val language = editor.editorLanguage
                            if (language is TextMateLanguage) {
                                language.updateTheme(iRawTheme)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        7 -> try {
                            val iRawTheme = ThemeReader.readThemeSync(
                                "darcula.json",
                                assets.open("textmate/darcula.json")
                            )
                            val colorScheme = TextMateColorScheme.create(iRawTheme)
                            editor.colorScheme = colorScheme
                            val language = editor.editorLanguage
                            if (language is TextMateLanguage) {
                                language.updateTheme(iRawTheme)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        8 -> try {
                            val iRawTheme = ThemeReader.readThemeSync(
                                "abyss-color-theme.json",
                                assets.open("textmate/abyss-color-theme.json")
                            )
                            val colorScheme = TextMateColorScheme.create(iRawTheme)
                            editor.colorScheme = colorScheme
                            val language = editor.editorLanguage
                            if (language is TextMateLanguage) {
                                language.updateTheme(iRawTheme)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        9 -> loadTMTLauncher.launch("*/*")
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else if (id == R.id.text_wordwrap) {
            item.isChecked = !item.isChecked
            editor.isWordwrap = item.isChecked
        } else if (id == R.id.open_logs) {
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
        } else if (id == R.id.clear_logs) {
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
        } else if (id == R.id.open_debug_logs) {
            //ignored
            //editor.setText(Logs.getLogs());
        } else if (id == R.id.editor_line_number) {
            editor.isLineNumberEnabled = !editor.isLineNumberEnabled
            item.isChecked = editor.isLineNumberEnabled
        } else if (id == R.id.pin_line_number) {
            editor.setPinLineNumber(!editor.isLineNumberPinned)
            item.isChecked = editor.isLineNumberPinned
        } else if (id == R.id.enable_highlight) {
            item.isChecked = !item.isChecked
            // no implementation
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
}