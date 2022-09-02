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
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.languageconfiguration.model.AutoClosingPair;
import org.eclipse.tm4e.languageconfiguration.model.AutoClosingPairConditional;
import org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration;

/**
 * The "character pair" support.
 *
 * @see <a href=
 *      "https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/supports/characterPair.ts">
 *      https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/supports/characterPair.ts</a>
 */
public final class CharacterPairSupport {

	public static final String DEFAULT_AUTOCLOSE_BEFORE_LANGUAGE_DEFINED = ";:.,=}])> \r\n\t";
	public static final String DEFAULT_AUTOCLOSE_BEFORE_WHITESPACE = " \r\n\t";

	public final List<AutoClosingPairConditional> autoClosingPairs;
	public final List<AutoClosingPair> surroundingPairs;
	public final String autoCloseBefore;

	@SuppressWarnings("unchecked")
	public CharacterPairSupport(LanguageConfiguration config) {
		final var autoClosingPairs = config.getAutoClosingPairs();
		final var brackets = config.getBrackets();

		if (autoClosingPairs != null) {
			this.autoClosingPairs = autoClosingPairs.stream().filter(Objects::nonNull)
					.map(el -> new AutoClosingPairConditional(el.open, el.close, el.notIn))
					.collect(Collectors.toList());
		} else if (brackets != null) {
			this.autoClosingPairs = brackets.stream().filter(Objects::nonNull)
					.map(el -> new AutoClosingPairConditional(el.open, el.close, Collections.emptyList()))
					.collect(Collectors.toList());
		} else {
			this.autoClosingPairs = Collections.emptyList();
		}

		final var autoCloseBefore = config.getAutoCloseBefore();
		this.autoCloseBefore = autoCloseBefore != null
				? autoCloseBefore
				: CharacterPairSupport.DEFAULT_AUTOCLOSE_BEFORE_LANGUAGE_DEFINED;

		final var surroundingPairs = config.getSurroundingPairs();
		this.surroundingPairs = surroundingPairs != null
				? surroundingPairs.stream().filter(Objects::nonNull).collect(Collectors.toList())
				: (List<AutoClosingPair>) (List<?>) this.autoClosingPairs;
	}

	/**
	 * TODO not declared in upstream project
	 */
	@Nullable
	public AutoClosingPairConditional getAutoClosingPair(final String text, final int offset,
			final String newCharacter) {
		if (newCharacter.isEmpty()) {
			return null;
		}
		for (final AutoClosingPairConditional autoClosingPair : autoClosingPairs) {
			final String opening = autoClosingPair.open;
			if (!opening.endsWith(newCharacter)) {
				continue;
			}
			if (opening.length() > 1) {
				final String offsetPrefix = text.substring(0, offset);
				if (!offsetPrefix.endsWith(opening.substring(0, opening.length() - 1))) {
					continue;
				}
			}
			return autoClosingPair;
		}
		return null;
	}
}
