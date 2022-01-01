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
 * Reference of a content due to accessed in read-only mode.
 *
 * @author Rosemoe
 */
public class ContentReference extends TextReference {

    private final Content content;

    public ContentReference(@NonNull Content ref) {
        super(ref);
        this.content = ref;
    }

    public int getLineCount() {
        validateAccess();
        return content.getLineCount();
    }

    public int getColumnCount(int line) {
        validateAccess();
        return content.getColumnCount(line);
    }

    public String getLine(int line) {
        validateAccess();
        return content.getLineString(line);
    }

    public void getLineChars(int line, char[] dest) {
        validateAccess();
        content.getLineChars(line, dest);
    }

    @NonNull
    @Override
    public Content getReference() {
        return (Content) super.getReference();
    }

    @Override
    public ContentReference setValidator(Validator validator) {
        super.setValidator(validator);
        return this;
    }
}
