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

import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.CompleteEnterAction
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.EnterAction
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentAction
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentAction.Indent
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentAction.IndentOutdent
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentAction.None
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentAction.Outdent
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.support.IndentRulesSupport
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.support.OnEnterSupport
import io.github.rosemoe.sora.langs.monarch.utils.getIndentationFromWhitespace
import io.github.rosemoe.sora.langs.monarch.utils.getLeadingWhitespace
import io.github.rosemoe.sora.langs.monarch.utils.getLinePrefixingWhitespaceAtPosition
import io.github.rosemoe.sora.langs.monarch.utils.normalizeIndentation
import io.github.rosemoe.sora.langs.monarch.utils.outdentString
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import kotlin.math.max


class MonarchNewlineHandler(
    private val language: MonarchLanguage
) : NewlineHandler {

    private var enterSupport: OnEnterSupport? = null
    private var indentRulesSupport: IndentRulesSupport? = null

    private var enterAction: CompleteEnterAction? = null
    private var indentForEnter: Pair<String, String>? = null

    private var isEnabled = true

    init {
        val languageConfiguration = language.languageConfiguration

        run {
            if (languageConfiguration == null) {
                return@run
            }

            val enterRules =
                languageConfiguration.onEnterRules
            val brackets = languageConfiguration.brackets

            val indentationsRules = languageConfiguration.indentationRules
            enterSupport = OnEnterSupport(brackets, enterRules)

            if (indentationsRules != null) {
                indentRulesSupport = IndentRulesSupport(
                    indentationsRules
                )
            }

        }
    }

    override fun matchesRequirement(
        text: Content,
        position: CharPosition,
        style: Styles?
    ): Boolean {
        if (!isEnabled) {
            return false
        }

        enterAction = getEnterAction(text, position)
        indentForEnter = null

        if (enterAction == null) {
            indentForEnter = getIndentForEnter(text, position)
        }

        return enterAction != null || indentForEnter != null
    }

    fun getIndentForEnter(text: Content, position: CharPosition): Pair<String, String>? {
        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/languages/autoIndent.ts#L278

        val currentLineText = text.getLineString(position.line)
        val beforeEnterText = currentLineText.substring(0, position.column)
        val afterEnterText = currentLineText.substring(position.column)

        val beforeEnterIndent =
            beforeEnterText.getLeadingWhitespace(0, beforeEnterText.length)

        val afterEnterAction =
            getInheritIndentForLine(
                WrapperContent(text, position.line, beforeEnterText),
                position.line + 1
            ) ?: return beforeEnterIndent to beforeEnterIndent

        var afterEnterIndent = afterEnterAction.indentation
        val indent = if (language.useTab()) {
            "\t"
        } else {
            " ".repeat(language.tabSize)
        }


        //var firstNonScapeIndex = TextUtils.firstNonWhitespaceIndex(beforeEnterIndent);
        if (afterEnterAction.action == IndentAction.Indent) {
            // 	afterEnterIndent = indentConverter.shiftIndent(afterEnterIndent)

            //var invisibleColumn = TextUtils.invisibleColumnFromColumn(text, position.line, position.column, tabSpaces);

            afterEnterIndent = beforeEnterIndent.toString() + indent
        }

        if (indentRulesSupport?.shouldDecrease(afterEnterText) == true) {
            // afterEnterIndent = indentConverter.unshiftIndent(afterEnterIndent);

            afterEnterIndent = beforeEnterIndent.substring(
                0,
                max(
                    0.coerceAtLeast(beforeEnterIndent.length - 1),
                    beforeEnterIndent.length - indent.length /*- 1*/
                )
            )
        }

        return beforeEnterIndent to afterEnterIndent
    }


    override fun handleNewline(
        text: Content,
        position: CharPosition,
        style: Styles?,
        tabSize: Int
    ): NewlineHandleResult {

        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/cursor/cursorTypeOperations.ts#L309
        val delim = "\n" /*command.text;*/


        indentForEnter?.let {
            val normalIndent = it.second.normalizeIndentation()
            val typeText = delim + normalIndent

            //var caretOffset = normalIndent.length() ;
            return NewlineHandleResult(typeText, 0)
        }

        val enterAction = enterAction ?: return NewlineHandleResult("", 0)


        when (enterAction.indentAction) {
            None, Indent -> {
                // Nothing special
                val increasedIndent =
                    (enterAction.indentation + enterAction.appendText).normalizeIndentation()
                val typeText = delim + increasedIndent

                // var caretOffset = typeText.length();
                // offset value is not needed because the editor ignores the position of invisible characters when moving the cursor
                return NewlineHandleResult(typeText, 0)
            } // Indent once
            IndentOutdent -> {
                // Ultra special
                val normalIndent = enterAction.indentation.normalizeIndentation()
                val increasedIndent =
                    (enterAction.indentation + enterAction.appendText).normalizeIndentation()
                val typeText = delim + increasedIndent + delim + normalIndent

                val caretOffset = normalIndent.length + 1
                return NewlineHandleResult(typeText, caretOffset)
            }

            Outdent -> {
                val indentation = enterAction.indentation.getIndentationFromWhitespace(
                    language.tabSize,
                    language.useTab()
                )
                val outdentedText = (indentation + enterAction.appendText).normalizeIndentation()
                    .outdentString(language.useTab(), language.tabSize)

                val caretOffset = outdentedText.length + 1
                return NewlineHandleResult(outdentedText, caretOffset)
            }
        }

        return NewlineHandleResult("", 0)
    }


    /**
     * @see [
     * https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/enterAction.ts](https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/enterAction.ts)
     */
    fun getEnterAction(content: Content, position: CharPosition): CompleteEnterAction? {
        var indentation = content.getLinePrefixingWhitespaceAtPosition(position)
        // let scopedLineTokens = this.getScopedLineTokens(model, range.startLineNumber, range.startColumn);
        val onEnterSupport = this.enterSupport
            ?: return null


        val scopedLineText = content.getLineString(position.line)


        val beforeEnterText = scopedLineText.substring(
            0,
            position.column /*- 0*/ /*scopedLineTokens.firstCharOffset*/
        )


        // String afterEnterText = null;

        // selection support
        // if (range.isEmpty()) {
        val afterEnterText =
            scopedLineText.substring(position.column /*- scopedLineTokens.firstCharOffset*/)

        // afterEnterText = scopedLineText.substr(range.startColumn - 1 - scopedLineTokens.firstCharOffset);
        // } else {
        // const endScopedLineTokens = this.getScopedLineTokens(model,
        // range.endLineNumber, range.endColumn);
        // afterEnterText = endScopedLineTokens.getLineContent().substr(range.endColumn - 1 -
        // scopedLineTokens.firstCharOffset);
        // }

        /*
         * let lineNumber = range.startLineNumber; let oneLineAboveText = '';
         *
         * if (lineNumber > 1 && scopedLineTokens.firstCharOffset === 0) { // This is
         * not the first line and the entire line belongs to this mode let
         * oneLineAboveScopedLineTokens = this.getScopedLineTokens(model, lineNumber -
         * 1); if (oneLineAboveScopedLineTokens.languageId ===
         * scopedLineTokens.languageId) { // The line above ends with text belonging to
         * the same mode oneLineAboveText =
         * oneLineAboveScopedLineTokens.getLineContent(); } }
         */

        /*
        let previousLineText = '';
        if (range.startLineNumber > 1 && scopedLineTokens.firstCharOffset === 0) {
            // This is not the first line and the entire line belongs to this mode
		const oneLineAboveScopedLineTokens = getScopedLineTokens(model, range.startLineNumber - 1);
            if (oneLineAboveScopedLineTokens.languageId === scopedLineTokens.languageId) {
                // The line above ends with text belonging to the same mode
                previousLineText = oneLineAboveScopedLineTokens.getLineContent();
            }
        } */
        var previousLineText: String? = ""

        if (position.line > 1 /*|| position.column == 0*/) {
            // This is not the first line and the entire line belongs to this mode
            // The line above ends with text belonging to the same mode
            previousLineText = content.getLineString(position.line - 1)
        }

        var enterResult: EnterAction? = null
        try {
            enterResult = onEnterSupport.onEnter(previousLineText, beforeEnterText, afterEnterText)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (enterResult == null) {
            return null
        }

        val indentAction = enterResult.indentAction
        var appendText = enterResult.appendText
        val removeText = enterResult.removeText
        // Here we add `\t` to appendText first because enterAction is leveraging
        // appendText and removeText to change indentation.

        if (appendText == null) {
            appendText = if (indentAction == IndentAction.Indent
                || indentAction == IndentOutdent
            ) {
                "\t"
            } else {
                ""
            }
        } else if (indentAction == IndentAction.Indent) {
            appendText = "\t" + appendText
        }

        if (removeText != null) {
            indentation = indentation.substring(0, indentation.length - removeText)
        }

        return CompleteEnterAction(
            indentAction, appendText,
            removeText, indentation
        )
    }

    /**
     * Get nearest preceding line which doesn't match unIndentPattern or contains all whitespace.
     * Result:
     * -2: run into the boundary of embedded languages
     * -1: every line above are invalid
     * else: nearest preceding line of the same language
     */
    fun getPrecedingValidLine(content: WrapperContent, lineNumber: Int): Int {
        // remove embeddedLanguages support
        // const languageId = model.tokenization.getLanguageIdAtPosition(lineNumber, 0);
        if (lineNumber > 0) {
            for (lastLineNumber in lineNumber - 1 downTo 0) {
                val lineContent = content.getLineContent(lastLineNumber);
                if (indentRulesSupport?.shouldIgnore(lineContent) == true || precedingValidPattern.matches(
                        lineContent
                    ) || lineContent.isEmpty()
                ) {
                    continue
                }
                return lastLineNumber
            }
        }

        return -2
    }

    /**
     * Get inherited indentation from above lines.
     * 1. Find the nearest preceding line which doesn't match unIndentedLinePattern.
     * 2. If this line matches indentNextLinePattern or increaseIndentPattern, it means that the indent level of `lineNumber` should be 1 greater than this line.
     * 3. If this line doesn't match any indent rules
     * a. check whether the line above it matches indentNextLinePattern
     * b. If not, the indent level of this line is the result
     * c. If so, it means the indent of this line is *temporary*, go upward utill we find a line whose indent is not temporary (the same workflow a -> b -> c).
     * 4. Otherwise, we fail to get an inherited indent from aboves. Return null and we should not touch the indent of `lineNumber`
     * <p>
     * This function only return the inherited indent based on above lines, it doesn't check whether current line should decrease or not.
     */

    private fun getInheritIndentForLine(
        model: WrapperContent,
        line: Int,
        honorIntentialIndent: Boolean = true
    ): InheritIndentResult? {
        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/languages/autoIndent.ts#L73

        if (indentRulesSupport == null) {
            return null
        }
        if (line < 1) {
            return InheritIndentResult("", 0)
        }

        val indentRulesSupport = requireNotNull(indentRulesSupport)

        val precedingUnIgnoredLine = getPrecedingValidLine(model, line)

        if (precedingUnIgnoredLine < -1) {
            return null
        } else if (precedingUnIgnoredLine < 0) {
            return InheritIndentResult("", 0)
        }

        val precedingUnIgnoredLineContent = model.getLineContent(precedingUnIgnoredLine)

        if (indentRulesSupport.shouldIncrease(precedingUnIgnoredLineContent) || indentRulesSupport.shouldIndentNextLine(
                precedingUnIgnoredLineContent
            )
        ) {
            return InheritIndentResult(
                precedingUnIgnoredLineContent.getLeadingWhitespace(),
                IndentAction.Indent,
                precedingUnIgnoredLine
            )
        } else if (indentRulesSupport.shouldDecrease(precedingUnIgnoredLineContent)) {
            return InheritIndentResult(
                precedingUnIgnoredLineContent.getLeadingWhitespace(),
                IndentAction.None,
                precedingUnIgnoredLine
            )
        } else {
            // precedingUnIgnoredLine can not be ignored.
            // it doesn't increase indent of following lines
            // it doesn't increase just next line
            // so current line is not affect by precedingUnIgnoredLine
            // and then we should get a correct inheritted indentation from above lines
            if (precedingUnIgnoredLine == 0) {
                return InheritIndentResult(
                    precedingUnIgnoredLineContent.getLeadingWhitespace(),
                    IndentAction.None,
                    precedingUnIgnoredLine
                )
            }


            val previousLine = precedingUnIgnoredLine - 1;

            val previousLineIndentMetadata =
                indentRulesSupport.getIndentMetadata(model.getLineContent(previousLine));

            if (((previousLineIndentMetadata and (IndentRulesSupport.IndentConsts.INCREASE_MASK or IndentRulesSupport.IndentConsts.DECREASE_MASK)) === 0) && ((previousLineIndentMetadata and IndentRulesSupport.IndentConsts.INDENT_NEXTLINE_MASK) === 0) && (previousLineIndentMetadata > 0)) {

                var stopLine = 0
                for (i in previousLine - 1 downTo 1) {
                    if (indentRulesSupport.shouldIndentNextLine(model.getLineContent((i)))) {
                        continue
                    }
                    stopLine = i
                    break
                }

                return InheritIndentResult(
                    model.getLineContent(
                        stopLine + 1
                    ).getLeadingWhitespace(), IndentAction.None, stopLine + 1
                )

            }


            // search from precedingUnIgnoredLine until we find one whose indent is not temporary
            for (i in precedingUnIgnoredLine downTo 1) {
                val lineContent = model.getLineContent(i)
                if (indentRulesSupport.shouldIncrease(lineContent)) {
                    return InheritIndentResult(
                        lineContent.getLeadingWhitespace(),
                        IndentAction.Indent,
                        i
                    )
                } else if (indentRulesSupport.shouldIndentNextLine(lineContent)) {
                    var stopLine = 0
                    for (j in i - 1 downTo 1) {
                        if (indentRulesSupport.shouldIndentNextLine(model.getLineContent(i))) {
                            continue
                        }
                        stopLine = j
                        break
                    }


                    return InheritIndentResult(

                        model.getLineContent(
                            stopLine + 1
                        ).getLeadingWhitespace(), IndentAction.None, stopLine + 1
                    )
                } else if (indentRulesSupport.shouldDecrease(lineContent)) {
                    return InheritIndentResult(
                        lineContent.getLeadingWhitespace(),
                        IndentAction.None,
                        i
                    )
                }
            }

            return InheritIndentResult(
                model.getLineContent(
                    1
                ).getLeadingWhitespace(), IndentAction.None, 1
            )

        }
    }


    private fun String.normalizeIndentation(): String {
        return this.normalizeIndentation(language.tabSize, !language.useTab())
    }

    data class InheritIndentResult(
        var indentation: String,
        var action: Int = IndentAction.None,
        var line: Int = 0
    )

    class WrapperContent(
        private val origin: Content,
        private val line: Int,
        private val currentLineContent: String
    ) {
        fun getLineContent(line: Int): String {
            return if (line == this.line) {
                currentLineContent
            } else {
                origin.getLineString(line)
            }
        }
    }

    companion object {
        private val precedingValidPattern = Regex("^\\s+$")
    }
}