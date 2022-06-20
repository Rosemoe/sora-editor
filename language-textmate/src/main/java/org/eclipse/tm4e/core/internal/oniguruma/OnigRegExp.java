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

package org.eclipse.tm4e.core.internal.oniguruma;

import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.Syntax;
import org.joni.WarnCallback;

import java.nio.charset.StandardCharsets;

/**
 *
 * @see https://github.com/atom/node-oniguruma/blob/master/src/onig-reg-exp.cc
 *
 */
public class OnigRegExp {

    private OnigString lastSearchString;
    private int lastSearchPosition;
    private OnigResult lastSearchResult;
    private Regex regex;

    public OnigRegExp(String source) {
        lastSearchString = null;
        lastSearchPosition = -1;
        lastSearchResult = null;
        byte[] pattern = source.getBytes(StandardCharsets.UTF_8);
        this.regex = new Regex(pattern, 0, pattern.length, Option.CAPTURE_GROUP, UTF8Encoding.INSTANCE, Syntax.DEFAULT,
                WarnCallback.DEFAULT);
    }

    public OnigResult search(OnigString str, int position) {
        if (lastSearchString == str && lastSearchPosition <= position &&
                (lastSearchResult == null || lastSearchResult.locationAt(0) >= position)) {
            return lastSearchResult;
        }

        lastSearchString = str;
        lastSearchPosition = position;
        lastSearchResult = search(str.utf8_value, position, str.utf8_value.length);
        return lastSearchResult;
    }

    private OnigResult search(byte[] data, int position, int end) {
        Matcher matcher = regex.matcher(data);
        int status = matcher.search(position, end, Option.DEFAULT);
        if (status != Matcher.FAILED) {
            Region region = matcher.getEagerRegion();
            return new OnigResult(region, -1);
        }
        return null;
    }
}
