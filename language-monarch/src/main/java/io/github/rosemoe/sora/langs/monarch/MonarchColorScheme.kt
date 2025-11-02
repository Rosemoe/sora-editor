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

package io.github.rosemoe.sora.langs.monarch

import android.graphics.Color
import io.github.rosemoe.sora.langs.monarch.registry.ThemeChangeListener
import io.github.rosemoe.sora.langs.monarch.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.monarch.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.monarch.registry.model.ThemeSource
import io.github.rosemoe.sora.langs.monarch.theme.ThemeDefaultColors
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme


class MonarchColorScheme(
    themeModel: ThemeModel
) : EditorColorScheme(), ThemeChangeListener {

    var currentThemeModel: ThemeModel = themeModel
        private set

    init {
        applyDefault()
    }

    override fun onChangeTheme(newTheme: ThemeModel) {
        this.currentThemeModel = newTheme
        super.colors.clear()
        applyDefault();
    }


    override fun applyDefault() {
        super.applyDefault()

        if (!ThemeRegistry.hasListener(this)) {
            ThemeRegistry.addListener(this)
        }

        val themeModel: ThemeModel? = currentThemeModel

        // no initialized
        if (themeModel == null) {
            return
        }

        if (!themeModel.isLoaded) {
            themeModel.load()
        }

        val value = themeModel.value

        if (value.defaults.oldTextMateStyle) {
            applyTMTheme(value.defaults)
        } else {
            applyThemeSettings(value.defaults)
        }
    }

    private fun applyThemeSettings(defaultColors: ThemeDefaultColors) {
        defaultColors["editorCursor.foreground"]?.let {
            setColor(SELECTION_INSERT, Color.parseColor(it))
        }

        defaultColors["editor.selectionBackground"]?.let {
            setColor(SELECTED_TEXT_BACKGROUND, Color.parseColor(it))
        }

        defaultColors["editorWhitespace.foreground"]?.let {
            setColor(NON_PRINTABLE_CHAR, Color.parseColor(it))
        }

        defaultColors["editor.lineHighlightBackground"]?.let {
            setColor(CURRENT_LINE, Color.parseColor(it))
        }

        defaultColors["editor.background"]?.let {
            setColor(WHOLE_BACKGROUND, Color.parseColor(it))
            setColor(LINE_NUMBER_BACKGROUND, Color.parseColor(it))
        }

        defaultColors["editorLineNumber.foreground"]?.let {
            setColor(LINE_NUMBER, Color.parseColor(it))
        }

        defaultColors["editorLineNumber.activeForeground"]?.let {
            setColor(LINE_NUMBER_CURRENT, Color.parseColor(it))
        }

        defaultColors["editor.foreground"]?.let {
            setColor(TEXT_NORMAL, Color.parseColor(it))
        }

        defaultColors["completionWindowBackground"]?.let {
            setColor(COMPLETION_WND_BACKGROUND, Color.parseColor(it))
        }

        defaultColors["completionWindowBackgroundCurrent"]?.let {
            setColor(COMPLETION_WND_ITEM_CURRENT, Color.parseColor(it))
        }

        defaultColors["highlightedDelimitersForeground"]?.let {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, Color.parseColor(it))
        }

        defaultColors["tooltipBackground"]?.let {
            setColor(DIAGNOSTIC_TOOLTIP_BACKGROUND, Color.parseColor(it))
        }

        defaultColors["tooltipBriefMessageColor"]?.let {
            setColor(DIAGNOSTIC_TOOLTIP_BRIEF_MSG, Color.parseColor(it))
        }

        defaultColors["tooltipDetailedMessageColor"]?.let {
            setColor(DIAGNOSTIC_TOOLTIP_DETAILED_MSG, Color.parseColor(it))
        }

        defaultColors["tooltipActionColor"]?.let {
            setColor(DIAGNOSTIC_TOOLTIP_ACTION, Color.parseColor(it))
        }

        val editorIndentGuideBackground = defaultColors["editorIndentGuide.background"]
        val blockLineColor =
            (getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2 and 0x00FFFFFF or 0x88000000.toInt()
        val blockLineColorCur = blockLineColor or 0xFF000000.toInt()

        if (editorIndentGuideBackground != null) {
            setColor(BLOCK_LINE, Color.parseColor(editorIndentGuideBackground))
        } else {
            setColor(BLOCK_LINE, blockLineColor)
        }

        val editorIndentGuideActiveBackground = defaultColors["editorIndentGuide.activeBackground"]

        if (editorIndentGuideActiveBackground != null) {
            setColor(BLOCK_LINE_CURRENT, Color.parseColor(editorIndentGuideActiveBackground))
        } else {
            setColor(BLOCK_LINE_CURRENT, blockLineColorCur)
        }
    }

    private fun applyTMTheme(defaultColors: ThemeDefaultColors) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT)

        defaultColors["caret"]?.let {
            setColor(SELECTION_INSERT, Color.parseColor(it))
        }

        defaultColors["selection"]?.let {
            setColor(SELECTED_TEXT_BACKGROUND, Color.parseColor(it))
        }

        defaultColors["invisibles"]?.let {
            setColor(NON_PRINTABLE_CHAR, Color.parseColor(it))
        }

        defaultColors["lineHighlight"]?.let {
            setColor(CURRENT_LINE, Color.parseColor(it))
        }

        defaultColors["background"]?.let {
            setColor(WHOLE_BACKGROUND, Color.parseColor(it))
            setColor(LINE_NUMBER_BACKGROUND, Color.parseColor(it))
        }

        defaultColors["foreground"]?.let {
            setColor(TEXT_NORMAL, Color.parseColor(it))
        }

        defaultColors["highlightedDelimitersForeground"]?.let {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, Color.parseColor(it))
        }

        // TMTheme seems to have no fields to control BLOCK_LINE colors
        val blockLineColor =
            (getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2 and 0x00FFFFFF or 0x88000000.toInt()
        setColor(BLOCK_LINE, blockLineColor)
        val blockLineColorCur = blockLineColor or 0xFF000000.toInt()
        setColor(BLOCK_LINE_CURRENT, blockLineColorCur)
    }

    override fun getColor(type: Int): Int {
        if (type < 255) {
            return super.getColor(type)
        }

        // Cache colors in super class
        val superColor = super.getColor(type)

        if (superColor != 0) {
            return superColor
        }

        val theme = currentThemeModel.value
        val color = theme.colorMap.getColor(type - 255)
        val newColor = if (color != null) Color.parseColor(color) else super.getColor(
            TEXT_NORMAL
        )

        super.colors.put(type, newColor)

        return newColor
    }

    override fun isDark(): Boolean {
        val superIsDark = super.isDark()
        if (superIsDark) {
            return true
        }
        val themeModel: ThemeModel? = currentThemeModel
        return themeModel?.isDark ?: false
    }

    override fun detachEditor(editor: CodeEditor) {
        super.detachEditor(editor)
        ThemeRegistry.removeListener(this)
    }

    override fun attachEditor(editor: CodeEditor) {
        super.attachEditor(editor)
        try {
            ThemeRegistry.loadTheme(currentThemeModel)
        } catch (e: Exception) {
            //throw new RuntimeException(e);
        }
        onChangeTheme(currentThemeModel)
        editor.rerunAnalysis()
    }

    companion object {
        fun create(themeSource: ThemeSource): MonarchColorScheme {
            return create(
                ThemeModel(themeSource)
            )
        }


        fun create(themeModel: ThemeModel): MonarchColorScheme {
            return MonarchColorScheme(themeModel)
        }

        fun create() = create(ThemeRegistry.currentTheme)

    }
}