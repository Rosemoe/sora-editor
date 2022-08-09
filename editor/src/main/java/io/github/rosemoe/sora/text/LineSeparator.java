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

import java.util.Objects;

/**
 * Line separator types
 *
 * @author Rosemoe
 */
public enum LineSeparator {

    /**
     * No separator. Used internally
     */
    NONE(""),
    LF("\n"),
    CR("\r"),
    CRLF("\r\n");

    private final String str;
    private final int length;

    LineSeparator(String str) {
        this.str = str;
        this.length = str.length();
    }

    public String getContent() {
        return str;
    }

    public int getLength() {
        return length;
    }

    public static LineSeparator fromSeparatorString(String str) {
        Objects.requireNonNull(str, "text must not be null");
        switch (str) {
            case "\r": return CR;
            case "\n": return LF;
            case "\r\n": return CRLF;
            case "": return NONE;
            default:
                throw new IllegalArgumentException("unknown line separator type");
        }
    }

    public static LineSeparator fromSeparatorString(@NonNull CharSequence text, int start, int end) {
        Objects.requireNonNull(text, "text must not be null");
        if (end == start) {
            return NONE;
        }
        if (end - start == 1) {
            var ch = text.charAt(start);
            if (ch == '\r') return CR;
            if (ch == '\n') return LF;
        }
        if (end - start == 2 && text.charAt(start) == '\r' && text.charAt(start + 1) == '\n') {
            return CRLF;
        }
        throw new IllegalArgumentException("unknown line separator type");
    }

}
