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

package io.github.rosemoe.sora.editor.ts

import android.os.Bundle
import com.itsaky.androidide.treesitter.TSLanguage
import com.itsaky.androidide.treesitter.TSQuery
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch

/**
 * Tree-sitter based language.
 * @param language TSLanguage instance
 * @param tsTheme Theme for colorizing
 * @param tab whether tab should be used
 * @see TsTheme
 * @author Rosemoe
 */
open class TsLanguage(
    val language: TSLanguage,
    val tab: Boolean = false,
    scmSource: String,
    themeDescription: TsThemeBuilder.() -> Unit
) : Language {

    val tsQuery = TSQuery(language, scmSource)

    private var tsTheme = TsThemeBuilder(tsQuery).let {
        it.themeDescription()
        it.theme
    }

    open val analyzer by lazy {
        TsAnalyzeManager(language, tsTheme, tsQuery)
    }

    fun updateTheme(themeDescription: TsThemeBuilder.() -> Unit) = updateTheme(TsThemeBuilder(tsQuery).let {
            it.themeDescription()
            it.theme
        })

    fun updateTheme(theme: TsTheme) {
        this.tsTheme = theme
        analyzer.updateTheme(theme)
    }

    override fun getAnalyzeManager() = analyzer

    override fun getInterruptionLevel() = Language.INTERRUPTION_LEVEL_STRONG

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        // Nothing
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int) = 0

    override fun useTab() = tab

    override fun getFormatter(): Formatter = EmptyLanguage.EmptyFormatter.INSTANCE

    override fun getSymbolPairs(): SymbolPairMatch = EmptyLanguage.EMPTY_SYMBOL_PAIRS

    override fun getNewlineHandlers() = emptyArray<NewlineHandler>()

    override fun destroy() {

    }

}