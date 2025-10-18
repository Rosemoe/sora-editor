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

import android.util.Pair
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import org.eclipse.tm4e.languageconfiguration.internal.model.CompleteEnterAction
import org.eclipse.tm4e.languageconfiguration.internal.model.EnterAction
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import org.eclipse.tm4e.languageconfiguration.internal.supports.IndentRulesSupport
import org.eclipse.tm4e.languageconfiguration.internal.supports.OnEnterSupport
import org.eclipse.tm4e.languageconfiguration.internal.utils.TextUtils
import kotlin.math.max


open class TextMateNewlineHandler(private val language: TextMateLanguage) : NewlineHandler {
    private var enterSupport: OnEnterSupport? = null

    private var indentRulesSupport: IndentRulesSupport? = null

    private var enterAction: CompleteEnterAction? = null

    private var indentForEnter: Pair<String, String>? = null

    var isEnabled: Boolean = true


    //private static final Pattern precedingValidPattern = Pattern.compile("^\\s+$");
    private val languageConfiguration: LanguageConfiguration? = language.languageConfiguration

    init {
        if (languageConfiguration != null) {
            val enterRules = languageConfiguration.onEnterRules
            val brackets = languageConfiguration.brackets
            val indentationsRules = languageConfiguration.indentationRules

            if (enterRules != null) {
                enterSupport = OnEnterSupport(brackets, enterRules)
            }

            if (indentationsRules != null) {
                indentRulesSupport = IndentRulesSupport(indentationsRules)
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

    override fun handleNewline(
        text: Content,
        position: CharPosition,
        style: Styles?,
        tabSize: Int
    ): NewlineHandleResult {
        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/cursor/cursorTypeOperations.ts#L309

        val delimiter = "\n" /*command.text;*/


        if (indentForEnter != null) {
            val normalIndent = normalizeIndentation(indentForEnter!!.second)
            val typeText = delimiter + normalIndent

            //var caretOffset = normalIndent.length() ;
            return NewlineHandleResult(typeText, 0)
        }

        val enterAction = enterAction ?: return NewlineHandleResult("", 0)

        when (enterAction.indentAction) {
            EnterAction.IndentAction.None, EnterAction.IndentAction.Indent -> {
                // Nothing special
                val increasedIndent =
                    normalizeIndentation(enterAction.indentation + enterAction.appendText)
                val typeText = delimiter + increasedIndent

                // var caretOffset = typeText.length();
                // offset value is not needed because the editor ignores the position of invisible characters when moving the cursor
                return NewlineHandleResult(typeText, 0)
            } // Indent once
            EnterAction.IndentAction.IndentOutdent -> {
                // Ultra special
                val normalIndent = normalizeIndentation(enterAction.indentation)
                val increasedIndent =
                    normalizeIndentation(enterAction.indentation + enterAction.appendText)
                val typeText = delimiter + increasedIndent + delimiter + normalIndent

                val caretOffset = normalIndent.length + 1
                return NewlineHandleResult(typeText, caretOffset)
            }

            EnterAction.IndentAction.Outdent -> {
                val indentation = TextUtils.getIndentationFromWhitespace(
                    enterAction.indentation,
                    language.tabSize,
                    language.useTab()
                )
                val outdentedText =
                    outdentString(normalizeIndentation(indentation + enterAction.appendText))

                val caretOffset = outdentedText.length + 1
                return NewlineHandleResult(outdentedText, caretOffset)
            }
        }
    }


    protected fun getIndentForEnter(
        text: Content,
        position: CharPosition
    ): Pair<String, String>? {
        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/languages/autoIndent.ts#L278

        val currentLineText = text.getLineString(position.line)

        val beforeEnterText = currentLineText.substring(0, position.column)

        val afterEnterText = currentLineText.substring(position.column)

        if (indentRulesSupport == null) {
            return null
        }

        val beforeEnterIndent =
            TextUtils.getLeadingWhitespace(beforeEnterText, 0, beforeEnterText.length)

        val afterEnterAction = getInheritIndentForLine(
            WrapperContentImp(text, position.line, beforeEnterText),
            true,
            position.line + 1
        )


        if (afterEnterAction == null) {
            return Pair(beforeEnterIndent, beforeEnterIndent)
        }

        var afterEnterIndent = afterEnterAction.indentation

        val indent: String = if (language.useTab()) {
            "\t"
        } else {
            " ".repeat(language.tabSize)
        }


        //var firstNonScapeIndex = TextUtils.firstNonWhitespaceIndex(beforeEnterIndent);
        if (afterEnterAction.action == EnterAction.IndentAction.Indent) {
            // 	afterEnterIndent = indentConverter.shiftIndent(afterEnterIndent)

            //var invisibleColumn = TextUtils.invisibleColumnFromColumn(text, position.line, position.column, tabSpaces);

            afterEnterIndent = beforeEnterIndent + indent
        }

        if (indentRulesSupport!!.shouldDecrease(afterEnterText)) {
            // afterEnterIndent = indentConverter.unshiftIndent(afterEnterIndent);

            afterEnterIndent = beforeEnterIndent.substring(
                0,
                max(
                    max(0, beforeEnterIndent.length - 1),
                    beforeEnterIndent.length - indent.length /*- 1*/
                )
            )
        }

        return Pair(beforeEnterIndent, afterEnterIndent)
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
     *
     *
     * This function only return the inherited indent based on above lines, it doesn't check whether current line should decrease or not.
     */
    private fun getInheritIndentForLine(
        wrapperContent: WrapperContent,
        honorIntentialIndent: Boolean,
        line: Int
    ): InheritIndentResult? {
        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/languages/autoIndent.ts#L73

        if (line <= 0) {
            return InheritIndentResult("", null)
        }

        val precedingUnIgnoredLine = getPrecedingValidLine(wrapperContent, line)

        if (precedingUnIgnoredLine <= -1) {
            return null
        } /*else if (precedingUnIgnoredLine < 0) {
            return new InheritIndentResult("", null);
        }*/

        val precedingUnIgnoredLineContent = wrapperContent.getLineContent(precedingUnIgnoredLine)
        val indentation = TextUtils.getLeadingWhitespace(precedingUnIgnoredLineContent,
            0, precedingUnIgnoredLineContent.length)
        if (indentRulesSupport!!.shouldIncrease(precedingUnIgnoredLineContent) ||
            indentRulesSupport!!.shouldIndentNextLine(precedingUnIgnoredLineContent)) {
            return InheritIndentResult(indentation, EnterAction.IndentAction.Indent, precedingUnIgnoredLine)
        } else if (indentRulesSupport!!.shouldDecrease(precedingUnIgnoredLineContent)) {
            return InheritIndentResult(indentation, null, precedingUnIgnoredLine)
        } else {
            // precedingUnIgnoredLine can not be ignored.
            // it doesn't increase indent of following lines
            // it doesn't increase just next line
            // so current line is not affect by precedingUnIgnoredLine
            // and then we should get a correct inheritted indentation from above lines
            if (precedingUnIgnoredLine == 0) {
                val lineContent = wrapperContent.getLineContent(precedingUnIgnoredLine)
                val indentation = TextUtils.getLeadingWhitespace(lineContent)
                return InheritIndentResult(indentation, null, precedingUnIgnoredLine)
            }

            val previousLine = precedingUnIgnoredLine - 1
            val previousLineIndentMetadata =
                indentRulesSupport!!.getIndentMetadata(wrapperContent.getLineContent(previousLine))

            // 	if (!(previousLineIndentMetadata & (IndentConsts.INCREASE_MASK | IndentConsts.DECREASE_MASK)) &&
            //			(previousLineIndentMetadata & IndentConsts.INDENT_NEXTLINE_MASK)) {
            if (previousLineIndentMetadata and (IndentRulesSupport.IndentConsts.INCREASE_MASK or IndentRulesSupport.IndentConsts.DECREASE_MASK) == 0 &&
                previousLineIndentMetadata and IndentRulesSupport.IndentConsts.INDENT_NEXTLINE_MASK == 0 &&
                previousLineIndentMetadata > 0) {
                val stopLine = (previousLine - 1 downTo 1).firstOrNull { i ->
                    val lineContent = wrapperContent.getLineContent(i)
                    !indentRulesSupport!!.shouldIndentNextLine(lineContent)
                } ?: 0

                val lineContent = wrapperContent.getLineContent(stopLine + 1)
                val indentation = TextUtils.getLeadingWhitespace(lineContent)
                return InheritIndentResult(indentation, null, stopLine + 1)
            }

            if (honorIntentialIndent) {
                val lineContent = wrapperContent.getLineContent(precedingUnIgnoredLine)
                val indentation = TextUtils.getLeadingWhitespace(lineContent)
                return InheritIndentResult(indentation, null, precedingUnIgnoredLine)
            }

            // search from precedingUnIgnoredLine until we find one whose indent is not temporary
            for (i in precedingUnIgnoredLine downTo 1) {
                val lineContent = wrapperContent.getLineContent(i)
                if (indentRulesSupport!!.shouldIncrease(lineContent)) {
                    return InheritIndentResult(
                        TextUtils.getLeadingWhitespace(lineContent),
                        EnterAction.IndentAction.Indent,
                        i
                    )
                } else if (indentRulesSupport!!.shouldIndentNextLine(lineContent)) {
                    val stopLine = (i - 1 downTo 1).firstOrNull {
                        val lineContent = wrapperContent.getLineContent(i)
                        !indentRulesSupport!!.shouldIndentNextLine(lineContent)
                    } ?: 0

                    val lineContent = wrapperContent.getLineContent(stopLine + 1)
                    val indentation = TextUtils.getLeadingWhitespace(lineContent)
                    return InheritIndentResult(indentation, null, stopLine + 1)
                } else if (indentRulesSupport!!.shouldDecrease(lineContent)) {
                    return InheritIndentResult(TextUtils.getLeadingWhitespace(lineContent), null, i)
                }
            }

            val lineCount = wrapperContent.getLineContent(1)
            val indentation = TextUtils.getLeadingWhitespace(lineCount)
            return InheritIndentResult(indentation, null, 1)
        }
    }


    /**
     * Get nearest preceding line which doesn't match unIndentPattern or contains all whitespace.
     * Result:
     * -1: run into the boundary of embedded languages
     * 0: every line above are invalid
     * else: nearest preceding line of the same language
     */
    fun getPrecedingValidLine(content: WrapperContent, lineNumber: Int): Int {
        // remove embeddedLanguages support
        // const languageId = model.tokenization.getLanguageIdAtPosition(lineNumber, 0);
        if (lineNumber > 0) {
            for (lastLineNumber in lineNumber - 1 downTo 0) {
                val text = content.getLineContent(lastLineNumber)
                if (indentRulesSupport?.shouldIgnore(text) == true/*|| precedingValidPattern.matcher(text).matches()*/ || text.isEmpty()) {
                    continue
                }
                return lastLineNumber
            }
        }
        return -1
    }

    /**
     * See [enterAction.ts](https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/enterAction.ts)
     */
    fun getEnterAction(content: Content, position: CharPosition): CompleteEnterAction? {
        var indentation = TextUtils.getLinePrefixingWhitespaceAtPosition(content, position)
        // let scopedLineTokens = this.getScopedLineTokens(model, range.startLineNumber, range.startColumn);
        val onEnterSupport = this.enterSupport


        if (onEnterSupport == null) {
            return null
        }

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
        var previousLineText = ""

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
            // onUnexpectedError(e);
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
            appendText = if (indentAction == EnterAction.IndentAction.Indent
                || indentAction == EnterAction.IndentAction.IndentOutdent
            ) {
                "\t"
            } else {
                ""
            }
        } else if (indentAction == EnterAction.IndentAction.Indent) {
            appendText = "\t" + appendText
        }

        if (removeText != null) {
            indentation = indentation.substring(0, indentation.length - removeText)
        }

        return CompleteEnterAction(indentAction, appendText, removeText, indentation)
    }


    private fun outdentString(str: String): String {
        if (str.startsWith("\t")) { // $NON-NLS-1$
            return str.substring(1)
        }

        if (language.useTab()) {
            val spaces = " ".repeat(language.tabSize)
            if (str.startsWith(spaces)) {
                return str.substring(spaces.length)
            }
        }
        return str
    }


    private fun normalizeIndentation(str: String?): String {
        return TextUtils.normalizeIndentation(str, language.tabSize, !language.useTab())
    }


    private data class InheritIndentResult(
        var indentation: String,
        var action: EnterAction.IndentAction?,
        var line: Int = 0
    )

    /*
        const virtualModel: IVirtualModel = {
                tokenization: {
            getLineTokens: (lineNumber: number) => {
                return model.tokenization.getLineTokens(lineNumber);
            },
            getLanguageId: () => {
                return model.getLanguageId();
            },
            getLanguageIdAtPosition: (lineNumber: number, column: number) => {
                return model.getLanguageIdAtPosition(lineNumber, column);
            },
        },
        getLineContent: (lineNumber: number) => {
            if (lineNumber === range.startLineNumber) {
                return beforeEnterResult;
            } else {
                return model.getLineContent(lineNumber);
            }
        }
       };
       */
    private data class WrapperContentImp(
        private val content: Content,
        private val line: Int,
        private val currentLineContent: String
    ) : WrapperContent {

        override val origin: Content = content

        override fun getLineContent(line: Int): String {
            return if (line == this.line) {
                currentLineContent
            } else {
                content.getLineString(line)
            }
        }
    }

    interface WrapperContent {
        val origin: Content

        fun getLineContent(line: Int): String
    }
}
