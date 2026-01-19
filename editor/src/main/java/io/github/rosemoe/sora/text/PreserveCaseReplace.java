/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
package io.github.rosemoe.sora.text;

import androidx.annotation.NonNull;


public class PreserveCaseReplace {

    private PreserveCaseReplace() {

    }

    public static String getReplacementSimple(@NonNull String oldStr, @NonNull String newStr) {
        if (oldStr.isEmpty() || newStr.isEmpty()) return newStr;
        // Analyze the case of old string
        var isUpperCase = true;
        var isLowerCase = true;
        var isCapitalized = true;
        for (int i = 0; i < oldStr.length(); i++) {
            var ch = oldStr.charAt(i);
            if (!Character.isLetter(ch)) continue;
            var upper = Character.isUpperCase(ch);
            var lower = Character.isLowerCase(ch);
            if (isUpperCase && isLowerCase) {
                isCapitalized = upper;
            } else {
                isCapitalized &= lower;
            }
            isUpperCase &= upper;
            isLowerCase &= lower;
        }
        // No letter found
        if (isUpperCase && isLowerCase) return newStr;
        // Upper / Lower / Capitalized
        if (isUpperCase) return newStr.toUpperCase();
        if (isLowerCase) return newStr.toLowerCase();
        if (isCapitalized)
            return newStr.substring(0, 1).toUpperCase() + newStr.substring(1).toLowerCase();
        // Mixed case
        var sb = new StringBuilder();
        for (int i = 0; i < newStr.length(); i++) {
            var ch = newStr.charAt(i);
            if (!Character.isLetter(ch) || i >= oldStr.length()) {
                sb.append(ch);
                continue;
            }
            var oldCh = oldStr.charAt(i);
            if (Character.isLetter(oldCh)) {
                sb.append(Character.isUpperCase(oldCh) ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

}
