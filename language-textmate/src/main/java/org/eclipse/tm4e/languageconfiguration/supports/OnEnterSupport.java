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
package org.eclipse.tm4e.languageconfiguration.supports;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.languageconfiguration.model.CharacterPair;
import org.eclipse.tm4e.languageconfiguration.model.EnterAction;
import org.eclipse.tm4e.languageconfiguration.model.OnEnterRule;
import org.eclipse.tm4e.languageconfiguration.model.EnterAction.IndentAction;
import org.eclipse.tm4e.languageconfiguration.utils.RegExpUtils;

/**
 * On enter support.
 *
 * @see <a href="https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/supports/onEnter.ts">
 * https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/supports/onEnter.ts</a>
 */
public class OnEnterSupport {

    private static final List<CharacterPair> DEFAULT_BRACKETS = List.of(
            new CharacterPair("(", ")"), //$NON-NLS-1$ //$NON-NLS-2$
            new CharacterPair("{", "}"), //$NON-NLS-1$ //$NON-NLS-2$
            new CharacterPair("[", "]")); //$NON-NLS-1$ //$NON-NLS-2$

    private final List<ProcessedBracketPair> brackets;
    private final List<OnEnterRule> regExpRules;

    public OnEnterSupport(@Nullable final List<CharacterPair> brackets, @Nullable final List<OnEnterRule> regExpRules) {
        this.brackets = (brackets != null ? brackets : DEFAULT_BRACKETS)
                .stream()
                .filter(Objects::nonNull)
                .map(ProcessedBracketPair::new)
                .collect(Collectors.toList());

        this.regExpRules = regExpRules != null ? regExpRules : Collections.emptyList();
    }

    @Nullable
    public EnterAction onEnter(
            // TODO autoIndent: EditorAutoIndentStrategy,
            // TODO final String previousLineText,
            final String beforeEnterText,
            final String afterEnterText) {
        // (1): `regExpRules`
        for (final OnEnterRule rule : regExpRules) {
            if (rule == null) {
                continue;
            }
            final var beforeText = rule.beforeText;
            if (beforeText.matcher(beforeEnterText).find()) {
                final var afterText = rule.afterText;
                if (afterText != null) {
                    if (afterText.matcher(afterEnterText).find()) {
                        return rule.action;
                    }
                } else {
                    return rule.action;
                }
            }
        }

        // (2): Special indent-outdent
        if (!beforeEnterText.isEmpty() && !afterEnterText.isEmpty()) {
            for (final ProcessedBracketPair bracket : brackets) {
                if (bracket.matchOpen(beforeEnterText) && bracket.matchClose(afterEnterText)) {
                    return new EnterAction(IndentAction.IndentOutdent);
                }
            }
        }

        // (3): Open bracket based logic
        if (!beforeEnterText.isEmpty()) {
            for (final ProcessedBracketPair bracket : brackets) {
                if (bracket.matchOpen(beforeEnterText)) {
                    return new EnterAction(IndentAction.Indent);
                }
            }
        }

        return null;
    }

    private static final class ProcessedBracketPair {

        private static final Pattern B_REGEXP = Pattern.compile("\\B"); //$NON-NLS-1$

        @Nullable
        private final Pattern openRegExp;

        @Nullable
        private final Pattern closeRegExp;

        private ProcessedBracketPair(final CharacterPair charPair) {
            openRegExp = createOpenBracketRegExp(charPair.open);
            closeRegExp = createCloseBracketRegExp(charPair.close);
        }

        private boolean matchOpen(final String beforeEnterText) {
            return openRegExp != null && openRegExp.matcher(beforeEnterText).find();
        }

        private boolean matchClose(final String afterEnterText) {
            return closeRegExp != null && closeRegExp.matcher(afterEnterText).find();
        }

        @Nullable
        private static Pattern createOpenBracketRegExp(final String bracket) {
            final var str = new StringBuilder(RegExpUtils.escapeRegExpCharacters(bracket));
            final var c = String.valueOf(str.charAt(0));
            if (!B_REGEXP.matcher(c).find()) {
                str.insert(0, "\\b"); //$NON-NLS-1$
            }
            str.append("\\s*$"); //$NON-NLS-1$
            return RegExpUtils.create(str.toString());
        }

        @Nullable
        private static Pattern createCloseBracketRegExp(final String bracket) {
            final var str = new StringBuilder(RegExpUtils.escapeRegExpCharacters(bracket));
            final var c = String.valueOf(str.charAt(str.length() - 1));
            if (!B_REGEXP.matcher(c).find()) {
                str.append("\\b"); //$NON-NLS-1$
            }
            str.insert(0, "^\\s*"); //$NON-NLS-1$
            return RegExpUtils.create(str.toString());
        }
    }
}
