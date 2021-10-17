/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/atom/node-oniguruma
 * Initial copyright Copyright (c) 2013 GitHub Inc.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - GitHub Inc.: Initial code, written in JavaScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 * - Fabio Zadrozny <fabiofz@gmail.com> - Convert uniqueId to Object (for identity compare)
 * - Fabio Zadrozny <fabiofz@gmail.com> - Fix recursion error on creation of OnigRegExp with unicode chars
 */

package io.github.rosemoe.sora.textmate.core.internal.oniguruma;

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
