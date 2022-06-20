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
package org.eclipse.tm4e.languageconfiguration.internal;

import org.eclipse.tm4e.languageconfiguration.ILanguageConfiguration;
import org.eclipse.tm4e.languageconfiguration.internal.supports.CharacterPairSupport;
import org.eclipse.tm4e.languageconfiguration.internal.supports.CommentSupport;
import org.eclipse.tm4e.languageconfiguration.internal.supports.OnEnterSupport;

import java.io.Reader;

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
