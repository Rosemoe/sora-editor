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
package org.eclipse.tm4e.languageconfiguration.internal.supports;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The "character pair" support.
 *
 */
public class CharacterPairSupport {

	private List<CharacterPair> autoClosingPairs;
	private List<CharacterPair> surroundingPairs;

	public CharacterPairSupport(List<CharacterPair> brackets, List<AutoClosingPairConditional> autoClosingPairs,
			List<CharacterPair> surroundingPairs) {
		if (autoClosingPairs != null) {
			this.autoClosingPairs = autoClosingPairs.stream().filter(el -> el != null)
					.map(el -> new AutoClosingPairConditional(el.getKey(), el.getValue(), el.getNotIn()))
					.collect(Collectors.toList());
		} else if (brackets != null) {
			this.autoClosingPairs = brackets.stream().filter(el -> el != null)
					.map(el -> new AutoClosingPairConditional(el.getKey(), el.getValue(), null))
					.collect(Collectors.toList());
		} else {
			this.autoClosingPairs = new ArrayList<>();
		}

		this.surroundingPairs = surroundingPairs != null
				? surroundingPairs.stream().filter(el -> el != null).collect(Collectors.toList())
				: this.autoClosingPairs;
	}

	public CharacterPair getAutoClosePair(String text, Integer offset,
			String newCharacter/* : string, context: ScopedLineTokens, column: number */) {
		if (newCharacter.isEmpty()) {
			return null;
		}
		for (CharacterPair autoClosingPair : autoClosingPairs) {
			String openning = autoClosingPair.getKey();
			if (!openning.endsWith(newCharacter)) {
				continue;
			}
			if (openning.length() > 1) {
				String offsetPrefix = text.substring(0, offset);
				if (!offsetPrefix.endsWith(openning.substring(0, openning.length() - 1))) {
					continue;
				}
			}
			return autoClosingPair;
		}
		return null;
	}

	public List<CharacterPair> getAutoClosingPairs() {
		return autoClosingPairs;
	}

	public List<CharacterPair> getSurroundingPairs() {
		return surroundingPairs;
	}
}
