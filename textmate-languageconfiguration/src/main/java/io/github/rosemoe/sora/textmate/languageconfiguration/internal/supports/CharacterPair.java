/**
 *  Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports;

import java.util.AbstractMap.SimpleEntry;

/**
 * A tuple of two characters, like a pair of opening and closing brackets.
 */
@SuppressWarnings("serial")
public class CharacterPair extends SimpleEntry<String, String> {

	public CharacterPair(String key, String value) {
		super(key, value);
	}

}
