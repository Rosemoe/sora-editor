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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Utility class for creating {@link Content} objects
 *
 * @author Rosemoe
 */
public class ContentCreator {

    /**
     * Create a {@link Content} from stream
     */
    public static Content fromStream(InputStream stream) throws IOException {
        return fromReader(new InputStreamReader(stream));
    }

    /**
     * Create a {@link Content} from reader
     */
    public static Content fromReader(Reader reader) throws IOException {
        var content = new Content();
        content.setUndoEnabled(false);
        var buffer = new char[8192 * 2];
        var wrapper = new CharArrayWrapper(buffer, 0);
        int count;
        while ((count = reader.read(buffer)) != -1) {
            wrapper.setDataCount(count);
            var line = content.getLineCount() - 1;
            content.insert(line, content.getColumnCount(line), wrapper);
        }
        reader.close();
        content.setUndoEnabled(true);
        return content;
    }

}
