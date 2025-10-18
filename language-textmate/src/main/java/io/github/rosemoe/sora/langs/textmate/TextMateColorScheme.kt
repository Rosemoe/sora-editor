/*
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
 */
package io.github.rosemoe.sora.langs.textmate

import android.graphics.Color
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.core.internal.theme.raw.IRawTheme
import org.eclipse.tm4e.core.internal.theme.raw.RawTheme
import org.eclipse.tm4e.core.registry.IThemeSource

class TextMateColorScheme(
    private var currentTheme: ThemeModel?
) : EditorColorScheme(), ThemeRegistry.ThemeChangeListener {
    private var theme: Theme? = null
    private var _rawTheme: IRawTheme? = null
    private var _themeSource: IThemeSource? = null

    @Deprecated("")
    val rawTheme: IRawTheme? get() = _rawTheme

    @Deprecated("")
    val themeSource: IThemeSource? get() = _themeSource

    fun setTheme(themeModel: ThemeModel) {
        currentTheme = themeModel
        super.colors.clear()
        this.theme = themeModel.theme
        this._rawTheme = themeModel.rawTheme
        this._themeSource = themeModel.themeSource
        applyDefault()
    }

    override fun onChangeTheme(newTheme: ThemeModel) {
        setTheme(newTheme)
    }

    override fun applyDefault() {
        super.applyDefault()

        ThemeRegistry.getInstance().addListener(this)

        val rawTheme = _rawTheme ?: return
        val settings = rawTheme.getSettings() as? List<*>

        if (settings == null) {
            val rawSubTheme = (rawTheme as RawTheme)["colors"] as RawTheme?
            if (rawSubTheme != null) applyVSCTheme(rawSubTheme)
        } else {
            val rawSubTheme = (settings[0] as RawTheme?)?.setting as RawTheme?
            if (rawSubTheme != null) applyTMTheme(rawSubTheme)
        }
    }

    private fun applyColors(rawTheme: RawTheme, mapping: Map<Int, String>) {
        mapping.forEach { (type, field) ->
            val colorString = rawTheme[field] as? String?
            if (!colorString.isNullOrEmpty()) {
                setColor(type, Color.parseColor(colorString))
            }
        }
    }

    private fun applyVSCTheme(rawTheme: RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT)

        applyColors(rawTheme, mapOf(
            SELECTION_INSERT to "editorCursor.foreground", // caret
            SELECTED_TEXT_BACKGROUND to "editor.selectionBackground", // selection
            NON_PRINTABLE_CHAR to "editorWhitespace.foreground", // invisibles
            CURRENT_LINE to "editor.lineHighlightBackground", // lineHighlight
            WHOLE_BACKGROUND to "editor.background", // background
            LINE_NUMBER_BACKGROUND to "editor.background", // background
            LINE_NUMBER to "editorLineNumber.foreground", // lineHighlightBackground
            LINE_NUMBER_CURRENT to "editorLineNumber.activeForeground", // lineHighlightActiveForeground
            TEXT_NORMAL to "editor.foreground", // foreground
            COMPLETION_WND_BACKGROUND to "completionWindowBackground", // completionWindowBackground
            COMPLETION_WND_ITEM_CURRENT to "completionWindowBackgroundCurrent", // completionWindowBackgroundCurrent
            HIGHLIGHTED_DELIMITERS_FOREGROUND to "highlightedDelimitersForeground", // highlightedDelimitersForeground
            DIAGNOSTIC_TOOLTIP_BACKGROUND to "tooltipBackground", // tooltipBackground
            DIAGNOSTIC_TOOLTIP_BRIEF_MSG to "tooltipBriefMessageColor", // tooltipBriefMessageColor
            DIAGNOSTIC_TOOLTIP_DETAILED_MSG to "tooltipDetailedMessageColor", // tooltipDetailedMessageColor
            DIAGNOSTIC_TOOLTIP_ACTION to "tooltipActionColor", // tooltipActionColor
        ))

        val editorIndentGuideBackground = rawTheme.get("editorIndentGuide.background") as String?
        val blockLineColor =
            (getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2 and 0x00FFFFFF or -0x78000000
        val blockLineColorCur = blockLineColor or -0x1000000

        if (editorIndentGuideBackground != null) {
            setColor(BLOCK_LINE, Color.parseColor(editorIndentGuideBackground))
        } else {
            setColor(BLOCK_LINE, blockLineColor)
        }

        val editorIndentGuideActiveBackground = rawTheme["editorIndentGuide.activeBackground"] as String?
        if (editorIndentGuideActiveBackground != null) {
            setColor(BLOCK_LINE_CURRENT, Color.parseColor(editorIndentGuideActiveBackground))
        } else {
            setColor(BLOCK_LINE_CURRENT, blockLineColorCur)
        }
    }

    override fun isDark(): Boolean {
        return super.isDark() || currentTheme?.isDark ?: false
    }

    private fun applyTMTheme(rawTheme: RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT)

        applyColors(rawTheme, mapOf(
            SELECTION_INSERT to "caret", // caret
            SELECTED_TEXT_BACKGROUND to "selection", // selection
            NON_PRINTABLE_CHAR to "invisibles", // invisibles
            CURRENT_LINE to "lineHighlight", // lineHighlight
            WHOLE_BACKGROUND to "background", // background
            LINE_NUMBER_BACKGROUND to "background", // background
            LINE_NUMBER to "editorLineNumber.foreground", // lineHighlightBackground
            LINE_NUMBER_CURRENT to "editorLineNumber.activeForeground", // lineHighlightActiveForeground
            TEXT_NORMAL to "foreground", // foreground
            HIGHLIGHTED_DELIMITERS_FOREGROUND to "highlightedDelimitersForeground", // highlightedDelimitersForeground
        ))

        //TMTheme seems to have no fields to control BLOCK_LINE colors
        val blockLineColor =
            (getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2 and 0x00FFFFFF or -0x78000000
        setColor(BLOCK_LINE, blockLineColor)
        val blockLineColorCur = blockLineColor or -0x1000000
        setColor(BLOCK_LINE_CURRENT, blockLineColorCur)
    }

    override fun getColor(type: Int): Int {
        if (type >= 255) {
            // Cache colors in super class
            val superColor = super.getColor(type)
            if (superColor == 0) {
                val currentTheme = theme
                if (currentTheme != null) {
                    val colorString = try {
                        currentTheme.getColor(type - 255)
                    } catch (_: IndexOutOfBoundsException) {
                        return super.getColor(TEXT_NORMAL)
                    }
                    val newColor = if (!"@default".equals(colorString, ignoreCase = true)) {
                        Color.parseColor(colorString)
                    } else {
                        super.getColor(TEXT_NORMAL)
                    }
                    super.colors.put(type, newColor)
                    return newColor
                }
                return super.getColor(TEXT_NORMAL)
            } else {
                return superColor
            }
        }
        return super.getColor(type)
    }

    override fun detachEditor(editor: CodeEditor) {
        super.detachEditor(editor)
        ThemeRegistry.getInstance().removeListener(this)
    }

    override fun attachEditor(editor: CodeEditor) {
        super.attachEditor(editor)
        currentTheme?.let {
            try {
                ThemeRegistry.getInstance().loadTheme(it)
            } catch (e: Exception) {
                //throw new RuntimeException(e);
            }
            setTheme(it)
        }
    }


    companion object {
        @Deprecated("")
        @JvmStatic
        fun create(themeSource: IThemeSource): TextMateColorScheme {
            return create(ThemeModel(themeSource))
        }

        @JvmStatic
        fun create(themeModel: ThemeModel): TextMateColorScheme {
            return TextMateColorScheme(themeModel)
        }

        @JvmStatic
        fun create(themeRegistry: ThemeRegistry): TextMateColorScheme {
            return create(themeRegistry.currentThemeModel)
        }
    }
}
