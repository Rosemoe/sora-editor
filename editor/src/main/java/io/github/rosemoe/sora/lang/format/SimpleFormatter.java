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
package io.github.rosemoe.sora.lang.format;

import androidx.annotation.WorkerThread;

import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;

public abstract class SimpleFormatter extends AsyncFormatter {

    @Override
    public void formatAsync(Content text) {
        var newText = format(text.toStringBuilder());
        if (newText == null) {
            return;
        }
        text.delete(0, 0, text.getLineCount() - 1, text.getColumnCount(
                text.getLineCount() - 1) - 1);
        text.insert(0, 0, newText);
    }

    @Override
    public void formatRegionAsync(Content text, CharPosition start, CharPosition end) {
        var newText = formatRegion(text.toStringBuilder(), start, end);
        if (newText == null) {
            return;
        }
        text.replace(start.line,start.column,end.line,end.column,newText);
    }

    /**
     * Format the given content
     *
     * @param text Content to format
     * @return Formatted code
     */
    @WorkerThread
    public abstract CharSequence format(CharSequence text);

    /**
     * Format the given content from {@code start} position to {@code end} position
     * <p>
     * Note: Make sure that you return formatted code in the given region. The original text in this region
     * will be <strong>replaced</strong> by the returned text.
     *
     * @return Formatted code from {@code start} to {@code end}.
     */
    @WorkerThread
    public abstract CharSequence formatRegion(CharSequence text, CharPosition start, CharPosition end);
}
