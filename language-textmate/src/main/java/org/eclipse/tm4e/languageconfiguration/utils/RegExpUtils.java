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
package org.eclipse.tm4e.languageconfiguration.utils;

import android.util.Log;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Regex utilities.
 */
public final class RegExpUtils {

    /**
     * Escapes regular expression characters in a given string
     */
    public static String escapeRegExpCharacters(final String value) {
        return value.replaceAll("[\\-\\\\\\{\\}\\*\\+\\?\\|\\^\\$\\.\\[\\]\\(\\)\\#]", "\\\\$0"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Create Java Regexp and null otherwise.
     *
     * @return Java Regexp and null otherwise.
     */
    @Nullable
    public static Pattern create(final String regex) {
        try {
            return Pattern.compile(regex);
        } catch (final Exception ex) {
            Log.e(RegExpUtils.class.getSimpleName(), "Failed to parse pattern: " + regex, ex);
            return null;
        }
    }
}
