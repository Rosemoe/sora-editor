/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.util.regex;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RegexBackrefParser {

    private final RegexBackrefGrammar grammar;

    public RegexBackrefParser(@NonNull RegexBackrefGrammar grammar) {
        this.grammar = Objects.requireNonNull(grammar, "grammar can not be null");
    }

    @NonNull
    public List<RegexBackrefToken> parse(@NonNull String pattern, int groupCount) {
        pattern = pattern + '\0'; // add an extra char to truncate trailing backref
        List<RegexBackrefToken> result = new ArrayList<>();
        char escapeChar = grammar.escapeChar, backrefChar = grammar.backrefStartChar;
        int index = 0;
        int len = pattern.length();
        // State 0: Text
        // State 1: Right after escape character
        // State 2: Require first digit for backref
        // State 3: Scan reset digits for backref
        int state = 0;
        int textStart = 0;
        long currentGroup = 0;
        while (index < len) {
            char ch = pattern.charAt(index);
            switch (state) {
                case 0: {
                    if (ch == escapeChar) {
                        result.add(new RegexBackrefToken(false, pattern.substring(textStart, index), -1));
                        state = 1;
                    } else if (ch == backrefChar) {
                        result.add(new RegexBackrefToken(false, pattern.substring(textStart, index), -1));
                        state = 2;
                    }
                    break;
                }
                case 1: {
                    if (ch == escapeChar || ch == backrefChar) {
                        result.add(new RegexBackrefToken(false, String.valueOf(ch), -1));
                    } else {
                        result.add(new RegexBackrefToken(false, pattern.substring(index - 1, index + 1), -1));
                    }
                    state = 0;
                    textStart = index + 1;
                    break;
                }
                case 2: {
                    if (ch >= '0' && ch <= '9') {
                        currentGroup = ch - '0';
                        if (currentGroup <= groupCount) {
                            state = 3; // scan rest digits
                            break;
                        }
                    }
                    // not backref, fallback to plain text
                    textStart = index - 1;
                    index--;
                    state = 0;
                    break;
                }
                case 3: {
                    if (ch >= '0' && ch <= '9') {
                        long newGroup = currentGroup * 10 + (ch - '0');
                        if (newGroup <= groupCount) {
                            currentGroup = newGroup;
                            break;
                        }
                    }
                    result.add(new RegexBackrefToken(true, null, (int) currentGroup));
                    textStart = index;
                    state = 0;
                    break;
                }
            }

            index++;
        }

        if (state != 0) {
            throw new IllegalArgumentException("illegal backref expression");
        } else {
            result.add(new RegexBackrefToken(false, pattern.substring(textStart, len - 1), -1));
        }

        return result;
    }

}
