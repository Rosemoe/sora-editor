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
package org.eclipse.tm4e.core.internal.theme.reader;

import org.eclipse.tm4e.core.internal.parser.json.JSONPListParser;
import org.eclipse.tm4e.core.internal.parser.xml.XMLPListParser;
import org.eclipse.tm4e.core.theme.IRawTheme;

import java.io.InputStream;

/**
 * TextMate Theme reader utilities.
 *
 */
public class ThemeReader {

    public static final IThemeParser XML_PARSER = new IThemeParser() {

        private final XMLPListParser<IRawTheme> parser = new XMLPListParser<>(true);

        @Override
        public IRawTheme parse(InputStream contents) throws Exception {
            return parser.parse(contents);
        }
    };
    public static final IThemeParser JSON_PARSER = new IThemeParser() {

        private final JSONPListParser<IRawTheme> parser = new JSONPListParser<>(true);

        @Override
        public IRawTheme parse(InputStream contents) throws Exception {
            return parser.parse(contents);
        }
    };

    /**
     * Helper class, use methods statically
     */
    private ThemeReader() {
    }

    public static IRawTheme readThemeSync(String filePath, InputStream in) throws Exception {
        SyncThemeReader reader = new SyncThemeReader(in, getThemeParser(filePath));
        return reader.load();
    }

    private static IThemeParser getThemeParser(String filePath) {
        if (filePath.endsWith(".json")) {
            return JSON_PARSER;
        }
        return XML_PARSER;
    }
}
