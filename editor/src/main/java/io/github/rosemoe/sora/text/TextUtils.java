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
package io.github.rosemoe.sora.text;

import androidx.annotation.NonNull;

/**
 * Utility class for texts
 */
public class TextUtils {

    /**
     * Compute leading space count
     *
     * @param tabWidth Tab is considered in {@code tabWidth} spaces
     */
    public static int countLeadingSpaceCount(@NonNull CharSequence text, int tabWidth) {
        int p = 0, count = 0;
        while (p < text.length()) {
            if (isWhitespace(text.charAt(p))) {
                if (text.charAt(p) == '\t') {
                    count += tabWidth;
                } else {
                    count++;
                }
                p++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Create indent space
     *
     * @return Generated space string
     */
    public static String createIndent(int indentSize, int tabWidth, boolean useTab) {
        indentSize = Math.max(0, indentSize);
        int tab = 0;
        int space;
        if (useTab) {
            tab = indentSize / tabWidth;
            space = indentSize % tabWidth;
        } else {
            space = indentSize;
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < tab; i++) {
            s.append('\t');
        }
        for (int i = 0; i < space; i++) {
            s.append(' ');
        }
        return s.toString();
    }

    public static int indexOf(@NonNull CharSequence text, @NonNull CharSequence pattern, boolean ignoreCase, int fromIndex) {
        var max = text.length() - pattern.length();
        var len = pattern.length();
        label:
        for (int i = fromIndex; i <= max; i++) {
            // Compare
            for (int j = 0; j < len; j++) {
                char s = text.charAt(i + j);
                char p = pattern.charAt(j);
                if (!(s == p || (ignoreCase && Character.toLowerCase(s) == Character.toLowerCase(p)))) {
                    continue label;
                }
            }
            return i;
        }
        return -1;
    }

    public static int lastIndexOf(@NonNull CharSequence text, @NonNull CharSequence pattern, boolean ignoreCase, int fromIndex) {
        var len = pattern.length();
        fromIndex = Math.min(fromIndex, text.length() - len);
        label:
        for (int i = fromIndex; i >= 0; i--) {
            // Compare
            for (int j = 0; j < len; j++) {
                char s = text.charAt(i + j);
                char p = pattern.charAt(j);
                if (!(s == p || (ignoreCase && Character.toLowerCase(s) == Character.toLowerCase(p)))) {
                    continue label;
                }
            }
            return i;
        }
        return -1;
    }

    public static boolean startsWith(@NonNull CharSequence text, @NonNull CharSequence pattern, boolean ignoreCase) {
        if (text.length() < pattern.length()) {
            return false;
        }
        var len = pattern.length();
        for (int i = 0; i < len; i++) {
            char s = text.charAt(i);
            char p = pattern.charAt(i);
            if (!((s == p) || (ignoreCase && Character.toLowerCase(s) == Character.toLowerCase(p)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWhitespace(char ch) {
        return ch == '\t' || ch == ' ';
    }

}
