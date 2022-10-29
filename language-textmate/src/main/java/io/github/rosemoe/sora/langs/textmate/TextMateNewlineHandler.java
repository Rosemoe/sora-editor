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
package io.github.rosemoe.sora.langs.textmate;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.eclipse.tm4e.languageconfiguration.model.CompleteEnterAction;
import org.eclipse.tm4e.languageconfiguration.model.EnterAction;
import org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration;
import org.eclipse.tm4e.languageconfiguration.supports.OnEnterSupport;
import org.eclipse.tm4e.languageconfiguration.utils.TabSpacesInfo;

import java.util.Arrays;

import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lang.styling.Styles;

import org.eclipse.tm4e.languageconfiguration.utils.TextUtils;

import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;


public class TextMateNewlineHandler implements NewlineHandler {

    private final OnEnterSupport enterSupport;
    private final TextMateLanguage language;

    private CompleteEnterAction enterAction;

    private boolean isEnabled = true;

    private LanguageConfiguration languageConfiguration;

    public TextMateNewlineHandler(TextMateLanguage language) {
        this.language = language;
        var languageConfiguration = language.languageConfiguration;

        this.languageConfiguration = languageConfiguration;

        if (languageConfiguration == null) {
            enterSupport = null;
            return;
        }

        var enterRules = languageConfiguration.getOnEnterRules();
        var brackets = languageConfiguration.getBrackets();

        if (enterRules == null) {
            enterSupport = null;
            return;
        }

        enterSupport = new OnEnterSupport(brackets, enterRules);
    }

    @Override
    public boolean matchesRequirement(@NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {
        enterAction = getEnterAction(text, position);
        return isEnabled && enterAction != null;
    }

    @NonNull
    @Override
    public NewlineHandleResult handleNewline(@NonNull Content text, @NonNull CharPosition position, @Nullable Styles style, int tabSize) {
        NewlineHandleResult result = null;
        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/cursor/cursorTypeOperations.ts#L309

        var delim = "\n"; /*command.text;*/
        switch (enterAction.indentAction) {
            case None:
            case Indent: {
                // Nothing special
                final String increasedIndent = normalizeIndentation(enterAction.indentation + enterAction.appendText);
                final String typeText = delim + increasedIndent;

                // var caretOffset = typeText.length();
                // offset value is not needed because the editor ignores the position of invisible characters when moving the cursor

                result = new NewlineHandleResult(typeText, 0);
                break;
            }// Indent once
            case IndentOutdent: {
                // Ultra special
                final String normalIndent = normalizeIndentation(enterAction.indentation);
                final String increasedIndent = normalizeIndentation(enterAction.indentation + enterAction.appendText);
                final String typeText = delim + increasedIndent + delim + normalIndent;

                var caretOffset = normalIndent.length() + 1;
                result = new NewlineHandleResult(typeText, caretOffset);
                break;
            }
            case Outdent:
                final String indentation = TextUtils.getIndentationFromWhitespace(enterAction.indentation,
                        getTabSpaces());
                final String outdentedText = outdentString(
                        normalizeIndentation(indentation + enterAction.appendText));

                var caretOffset = outdentedText.length() + 1;
                result = new NewlineHandleResult(outdentedText, caretOffset);
                break;
        }

        return result;

    }


    /**
     * @see <a href="https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/enterAction.ts">
     * https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/enterAction.ts</a>
     */
    @Nullable
    public CompleteEnterAction getEnterAction(final Content document, final CharPosition position) {
        String indentation = TextUtils.getLinePrefixingWhitespaceAtPosition(document, position);
        // let scopedLineTokens = this.getScopedLineTokens(model, range.startLineNumber, range.startColumn);
        final var onEnterSupport = this.enterSupport;


        var scopedLineText = document.getLine(position.line).toString();

        var beforeEnterText = scopedLineText.substring(0, position.column  /*- 0*/ /*scopedLineTokens.firstCharOffset*/);


        // String afterEnterText = null;

        // selection support
        // if (range.isEmpty()) {
        var afterEnterText = scopedLineText.substring(position.column /*- scopedLineTokens.firstCharOffset*/);
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

        EnterAction enterResult = null;
        try {
            enterResult = onEnterSupport.onEnter(beforeEnterText, afterEnterText);
        } catch (final Exception e) {
            e.printStackTrace();
            // onUnexpectedError(e);
        }

        if (enterResult == null) {
            return null;
        }

        // Here we add `\t` to appendText first because enterAction is leveraging
        // appendText and removeText to change indentation.
        if (enterResult.appendText == null) {
            if ((enterResult.indentAction == EnterAction.IndentAction.Indent)
                    || (enterResult.indentAction == EnterAction.IndentAction.IndentOutdent)) {
                enterResult.appendText = "\t"; //$NON-NLS-1$
            } else {
                enterResult.appendText = ""; //$NON-NLS-1$
            }
        }

        final var removeText = enterResult.removeText;
        if (removeText != null) {
            indentation = indentation.substring(0, indentation.length() - removeText);
        }

        return new CompleteEnterAction(enterResult, indentation);

    }


    private String outdentString(final String str) {
        if (str.startsWith("\t")) { // $NON-NLS-1$
            return str.substring(1);
        }
        final TabSpacesInfo tabSpaces = getTabSpaces();
        if (tabSpaces.isInsertSpaces()) {
            final char[] chars = new char[tabSpaces.getTabSize()];
            Arrays.fill(chars, ' ');
            final String spaces = new String(chars);
            if (str.startsWith(spaces)) {
                return str.substring(spaces.length());
            }
        }
        return str;
    }

    private String normalizeIndentation(final String str) {
        final TabSpacesInfo tabSpaces = getTabSpaces();
        return TextUtils.normalizeIndentation(str, tabSpaces.getTabSize(), tabSpaces.isInsertSpaces());
    }

    private TabSpacesInfo getTabSpaces() {
        return TextUtils.getTabSpaces(language);
    }


    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
