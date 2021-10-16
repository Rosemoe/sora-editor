/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.internal.grammar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.textmate.core.TMException;
import io.github.rosemoe.sora.textmate.core.theme.IThemeProvider;
import io.github.rosemoe.sora.textmate.core.theme.ThemeTrieElementRule;

public class ScopeMetadataProvider {

    private static final ScopeMetadata _NULL_SCOPE_METADATA = new ScopeMetadata("", 0, StandardTokenType.Other, null);

    private static final Pattern STANDARD_TOKEN_TYPE_REGEXP = Pattern.compile("\\b(comment|string|regex)\\b");
    private static final String COMMENT_TOKEN_TYPE = "comment";
    private static final String STRING_TOKEN_TYPE = "string";
    private static final String REGEX_TOKEN_TYPE = "regex";

    private final int initialLanguage;
    private final IThemeProvider themeProvider;
    private final Map<String, ScopeMetadata> cache;
    private final Map<String, Integer> embeddedLanguages;
    private ScopeMetadata defaultMetaData;
    private Pattern embeddedLanguagesRegex;

    public ScopeMetadataProvider(int initialLanguage, IThemeProvider themeProvider,
                                 Map<String, Integer> embeddedLanguages) {
        this.initialLanguage = initialLanguage;
        this.themeProvider = themeProvider;
        this.cache = new HashMap<>();
        this.onDidChangeTheme();

        // embeddedLanguages handling
        this.embeddedLanguages = new HashMap<>();
        if (embeddedLanguages != null) {
            // If embeddedLanguages are configured, fill in
            // `this._embeddedLanguages`
            Set<Entry<String, Integer>> languages = embeddedLanguages.entrySet();
            for (Entry<String, Integer> language : languages) {
                String scope = language.getKey();
                int languageId = language.getValue();
                /*
                 * if (typeof language !== 'number' || language === 0) {
                 * console.warn('Invalid embedded language found at scope ' +
                 * scope + ': <<' + language + '>>'); // never hurts to be too
                 * careful continue; }
                 */
                this.embeddedLanguages.put(scope, languageId);
            }
        }

        // create the regex
        Set<String> escapedScopes = this.embeddedLanguages.keySet().stream()
                .map(ScopeMetadataProvider::escapeRegExpCharacters)
                .collect(Collectors.toSet());
        if (escapedScopes.isEmpty()) {
            // no scopes registered
            this.embeddedLanguagesRegex = null;
        } else {
            // TODO!!!
            this.embeddedLanguagesRegex = null;
            // escapedScopes.sort();
            // escapedScopes.reverse();
            // this._embeddedLanguagesRegex = new
            // RegExp(`^((${escapedScopes.join(')|(')}))($|\\.)`, '');
        }
    }

    /**
     * Escapes regular expression characters in a given string
     */
    private static String escapeRegExpCharacters(String value) {
        // TODO!!!
        return value; //value.replace(/[\-\\\{\}\*\+\?\|\^\$\.\,\[\]\(\)\#\s]/g, '\\$&');
    }

    private static int toStandardTokenType(String tokenType) {
        Matcher m = STANDARD_TOKEN_TYPE_REGEXP.matcher(tokenType); // tokenType.match(ScopeMetadataProvider.STANDARD_TOKEN_TYPE_REGEXP);
        if (!m.find()) {
            return StandardTokenType.Other;
        }
        String group = m.group();
        if (COMMENT_TOKEN_TYPE.equals(group)) {
            return StandardTokenType.Comment;
        } else if (STRING_TOKEN_TYPE.equals(group)) {
            return StandardTokenType.String;
        }
        if (REGEX_TOKEN_TYPE.equals(group)) {
            return StandardTokenType.RegEx;
        }
        throw new TMException("Unexpected match for standard token type!");
    }

    public void onDidChangeTheme() {
        this.cache.clear();
        this.defaultMetaData = new ScopeMetadata(
                "",
                this.initialLanguage,
                StandardTokenType.Other,
                Arrays.asList(this.themeProvider.getDefaults())
        );
    }

    public ScopeMetadata getDefaultMetadata() {
        return this.defaultMetaData;
    }

    public ScopeMetadata getMetadataForScope(String scopeName) {
        if (scopeName == null) {
            return ScopeMetadataProvider._NULL_SCOPE_METADATA;
        }
        ScopeMetadata value = this.cache.get(scopeName);
        if (value != null) {
            return value;
        }
        value = this.doGetMetadataForScope(scopeName);
        this.cache.put(scopeName, value);
        return value;
    }

    private ScopeMetadata doGetMetadataForScope(String scopeName) {
        int languageId = this.scopeToLanguage(scopeName);
        int standardTokenType = ScopeMetadataProvider.toStandardTokenType(scopeName);
        List<ThemeTrieElementRule> themeData = this.themeProvider.themeMatch(scopeName);

        return new ScopeMetadata(scopeName, languageId, standardTokenType, themeData);
    }

    /**
     * Given a produced TM scope, return the language that token describes or
     * null if unknown. e.g. source.html => html, source.css.embedded.html =>
     * css, punctuation.definition.tag.html => null
     */
    private int scopeToLanguage(String scope) {
        if (scope == null) {
            return 0;
        }
        if (this.embeddedLanguagesRegex == null) {
            // no scopes registered
            return 0;
        }

        // TODO!!!!

		/*let m = scope.match(this._embeddedLanguagesRegex);
		if (!m) {
			// no scopes matched
			return 0;
		}

		let language = this._embeddedLanguages[m[1]] || 0;
		if (!language) {
			return 0;
		}

		return language;*/
        return 0;
    }
}
