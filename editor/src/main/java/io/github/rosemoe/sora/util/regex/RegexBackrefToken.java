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
import androidx.annotation.Nullable;

import java.util.regex.Matcher;

public class RegexBackrefToken {

    private final boolean isRef;

    private final String text;

    private final int group;

    public RegexBackrefToken(boolean isRef, String text, int group) {
        this.isRef = isRef;
        this.text = text;
        this.group = group;
    }

    public String getReplacementText(@NonNull Matcher matcher) {
        if (isReference()) {
            return matcher.group(getGroup());
        }
        return text;
    }

    public boolean isReference() {
        return isRef;
    }

    public String getText() {
        return text;
    }

    public int getGroup() {
        return group;
    }

}
