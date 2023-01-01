/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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
package io.github.rosemoe.sora.lang.styling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.rosemoe.sora.text.CharPosition;

public class StylesUtils {

    /**
     * Check cursor position's span.
     * If {@link io.github.rosemoe.sora.lang.styling.TextStyle#NO_COMPLETION_BIT} is set, true is returned.
     */
    public static boolean checkNoCompletion(@Nullable Styles styles, @NonNull CharPosition pos) {
        var line = pos.line;
        var column = pos.column;
        Spans spans;
        // Do not make completion without styles. The language may be empty or busy analyzing spans
        if (styles == null || (spans = styles.spans) == null) {
            return true;
        }
        var reader = spans.read();
        try {
            reader.moveToLine(line);
            int index = reader.getSpanCount() - 1;
            if (index == -1) {
                return true;
            }
            for (int i = 0; i < reader.getSpanCount(); i++) {
                if (reader.getSpanAt(i).column > column) {
                    index = i - 1;
                    break;
                }
            }
            index = Math.max(0, Math.min(index, reader.getSpanCount() - 1));
            if (TextStyle.isNoCompletion(reader.getSpanAt(index).style)) {
                reader.moveToLine(-1);
                return true;
            }
            reader.moveToLine(-1);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            // Unexpected exception. Maybe there is something wrong in language implementation
            return true;
        }
    }

}
