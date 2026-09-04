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
package io.github.rosemoe.sora.widget.internal.util;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.Spans;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class SpansUtils {

    private static final List<Span> sDefaultSpans;

    private static final String LOG_TAG = "EditorSpanUtils";

    static {
        sDefaultSpans = Collections.singletonList(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL));
    }

    private SpansUtils() {

    }

    @NonNull
    public static List<Span> getDefaultLineSpans() {
        return sDefaultSpans;
    }

    @NonNull
    public static List<Span> getSpansOnLine(@NonNull Spans.Reader reader, int line) {
        try {
            var spans = reader.getSpansOnLine(line);
            if (spans == null || spans.isEmpty()) {
                Log.e(LOG_TAG, "Empty line spans from " + reader);
                return sDefaultSpans;
            }
            return spans;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to get line spans for line " + line, e);
            return sDefaultSpans;
        }
    }

}
