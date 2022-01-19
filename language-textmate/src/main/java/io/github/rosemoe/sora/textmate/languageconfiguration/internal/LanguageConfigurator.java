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
package io.github.rosemoe.sora.textmate.languageconfiguration.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import io.github.rosemoe.sora.textmate.languageconfiguration.ILanguageConfiguration;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.CharacterPairSupport;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.CommentSupport;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.OnEnterSupport;

/**
 * Language configuration definition.
 *
 */
public class LanguageConfigurator {

	private CharacterPairSupport characterPair;
	private OnEnterSupport onEnter;
	private CommentSupport comment;
	private LanguageConfiguration languageConfiguration;

	/**
	 * Constructor for user preferences (loaded from Json with Gson).
	 */
	public LanguageConfigurator(Reader reader) {
		try {
			 languageConfiguration= LanguageConfiguration.load(reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Returns the "character pair" support and null otherwise.
	 *
	 * @return the "character pair" support and null otherwise.
	 */
	public CharacterPairSupport getCharacterPair() {
		if (this.characterPair == null) {
			ILanguageConfiguration conf = getLanguageConfiguration();
			if (conf != null) {
				this.characterPair = new CharacterPairSupport(conf.getBrackets(), conf.getAutoClosingPairs(),
						conf.getSurroundingPairs());
			}
		}
		return characterPair;
	}

	/**
	 * Returns the "on enter" support and null otherwise.
	 *
	 * @return the "on enter" support and null otherwise.
	 */
	public OnEnterSupport getOnEnter() {
		if (this.onEnter == null) {
			ILanguageConfiguration conf = getLanguageConfiguration();
			if (conf != null && (conf.getBrackets() != null || conf.getOnEnterRules() != null)) {
				this.onEnter = new OnEnterSupport(conf.getBrackets(), conf.getOnEnterRules());
			}
		}
		return onEnter;
	}

	/**
	 * Returns the "commment" support and null otherwise.
	 *
	 * @return the "commment" support and null otherwise.
	 */
	public CommentSupport getCommentSupport() {
		if (this.comment == null) {
			ILanguageConfiguration conf = getLanguageConfiguration();
			if (conf != null) {
				this.comment = new CommentSupport(conf.getComments());
			}
		}
		return comment;
	}

	public ILanguageConfiguration getLanguageConfiguration() {
		return languageConfiguration;
	}


}
