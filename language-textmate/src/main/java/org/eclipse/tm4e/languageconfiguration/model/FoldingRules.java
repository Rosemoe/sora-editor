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

import java.util.regex.Pattern;

/**
 * Describes folding rules for a language.
 *
 * @see <a href=
 *      "https://github.com/microsoft/vscode/blob/8e2ec5a7ee1ae5500c645c05145359f2a814611c/src/vs/editor/common/languages/languageConfiguration.ts#L139">
 *      github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/languageConfiguration.ts#L139</a>
 */
public final class FoldingRules {

	/**
	 * Used by the indentation based strategy to decide whether empty lines belong to the previous or the next block.
	 * A language adheres to the off-side rule if blocks in that language are expressed by their indentation.
	 * See [wikipedia](https://en.wikipedia.org/wiki/Off-side_rule) for more information.
	 */
	public final boolean offSide;
	public final Pattern markersStart;
	public final Pattern markersEnd;

	public FoldingRules(final boolean offSide, final Pattern markersStart, final Pattern markersEnd) {
		this.offSide = offSide;
		this.markersStart = markersStart;
		this.markersEnd = markersEnd;
	}
}