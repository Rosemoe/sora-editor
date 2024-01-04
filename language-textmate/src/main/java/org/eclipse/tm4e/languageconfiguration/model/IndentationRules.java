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
package org.eclipse.tm4e.languageconfiguration.model;

import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Describes indentation rules for a language.
 *
 * @see <a href= "https://github.com/microsoft/vscode/blob/339c3a9b60594411c62ea3ed9123a0b26cf722e9/src/vs/editor/common/languages/languageConfiguration.ts#L105" >
 *     https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/languageConfiguration</a>
 */
public class IndentationRules {

    /**
     * If a line matches this pattern, then all the lines after it should be unindented once (until another rule matches).
     */
    public final Pattern decreaseIndentPattern;
    /**
     * If a line matches this pattern, then all the lines after it should be indented once (until another rule matches).
     */
    public final Pattern increaseIndentPattern;
    /**
     * If a line matches this pattern, then **only the next line** after it should be indented once.
     */
    @Nullable
    public final Pattern indentNextLinePattern;
    /**
     * If a line matches this pattern, then its indentation should not be changed and it should not be evaluated against the other rules.
     */
    @Nullable
    public final Pattern unIndentedLinePattern;


    public IndentationRules(Pattern decreaseIndentPattern, Pattern increaseIndentPattern, @Nullable Pattern indentNextLinePattern, @Nullable Pattern unIndentedLinePattern) {
        this.decreaseIndentPattern = decreaseIndentPattern;
        this.increaseIndentPattern = increaseIndentPattern;
        this.indentNextLinePattern = indentNextLinePattern;
        this.unIndentedLinePattern = unIndentedLinePattern;
    }
}
