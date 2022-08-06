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
package io.github.rosemoe.sora.text.bidi;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentListener;
import io.github.rosemoe.sora.util.IntPair;

public class ContentBidi implements ContentListener {

    public final static int MAX_BIDI_CACHE_ENTRY_COUNT = 128;

    private final List<DirectionsEntry> entries = new ArrayList<>();
    private final Content text;
    private boolean enabled;

    public ContentBidi(@NonNull Content content) {
        text = Objects.requireNonNull(content);
        text.addContentListener(this);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            entries.clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Directions getLineDirections(int line) {
        if (!enabled) {
            return new Directions(new long[]{IntPair.pack(0, 0)}, text.getLine(line).length());
        }
        for (DirectionsEntry entry : entries) {
            if (entry.line == line) {
                return entry.dir;
            }
        }
        var dir = TextBidi.getDirections(text.getLine(line));
        entries.add(new DirectionsEntry(dir, line));
        if (MAX_BIDI_CACHE_ENTRY_COUNT >= 0 && entries.size() > MAX_BIDI_CACHE_ENTRY_COUNT) {
            entries.remove(0);
        }
        return dir;
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent) {
        var itr = entries.iterator();
        var delta = endLine - startLine;
        while (itr.hasNext()) {
            var entry = itr.next();
            if (entry.line >= startLine) {
                if (entry.line > endLine) {
                    entry.line -= delta;
                } else {
                    itr.remove();
                }
            }
        }
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence insertedContent) {
        var itr = entries.iterator();
        var delta = endLine - startLine;
        while (itr.hasNext()) {
            var entry = itr.next();
            if (entry.line > startLine) {
                entry.line += delta;
            } else if (entry.line == startLine) {
                itr.remove();
            }
        }
    }

    @Override
    public void beforeReplace(Content content) {

    }

    public void destroy() {
        text.removeContentListener(this);
        entries.clear();
    }

    private static class DirectionsEntry {

        Directions dir;

        int line;

        public DirectionsEntry(Directions dir, int line) {
            this.dir = dir;
            this.line = line;
        }
    }

}
