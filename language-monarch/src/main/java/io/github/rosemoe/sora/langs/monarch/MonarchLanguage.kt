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

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete.SyncIdentifiers
import io.github.rosemoe.sora.langs.monarch.registery.MonarchGrammarRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter

class MonarchLanguage : EmptyLanguage() {
    var tabSize = 4

    var useTab = false

    val autoCompleter = IdentifierAutoComplete()

    var autoCompleteEnabled = false

    var createIdentifiers = false

   // var monarchAnalyzer: MonarchAnalyzer? = null

    private lateinit var grammarRegistry: MonarchGrammarRegistry
   /*
    var newlineHandlers: Array<TextMateNewlineHandler>

    var symbolPairMatch: TextMateSymbolPairMatch? = null*/

    override fun useTab() = useTab

    override fun destroy() {
        super.destroy()
    }

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        if (!autoCompleteEnabled) {
            return
        }
        val prefix = CompletionHelper.computePrefix(
            content, position
        ) { key: Char -> MyCharacter.isJavaIdentifierPart(key) }
        /*val idt: SyncIdentifiers = textMateAnalyzer.syncIdentifiers
        autoComplete.requireAutoComplete(content, position, prefix, publisher, idt)*/
    }

    fun setCompleterKeywords(keywords: Array<String?>?) {
        autoCompleter.setKeywords(keywords, false)
    }
}