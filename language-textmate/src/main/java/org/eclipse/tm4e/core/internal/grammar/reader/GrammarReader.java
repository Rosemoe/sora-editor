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
package org.eclipse.tm4e.core.internal.grammar.reader;

import java.io.InputStream;

import org.eclipse.tm4e.core.internal.parser.json.JSONPListParser;
import org.eclipse.tm4e.core.internal.parser.xml.XMLPListParser;
import org.eclipse.tm4e.core.internal.types.IRawGrammar;

/**
 * TextMate Grammar reader utilities.
 *
 */
public class GrammarReader {

    public static final IGrammarParser XML_PARSER = new IGrammarParser() {

        private XMLPListParser<IRawGrammar> parser = new XMLPListParser<>(false);

        @Override
        public IRawGrammar parse(InputStream contents) throws Exception {
            return parser.parse(contents);
        }
    };
    public static final IGrammarParser JSON_PARSER = new IGrammarParser() {

        private JSONPListParser<IRawGrammar> parser = new JSONPListParser<>(false);

        @Override
        public IRawGrammar parse(InputStream contents) throws Exception {
            return parser.parse(contents);
        }
    };

    /**
     * methods should be accessed statically
     */
    private GrammarReader() {

    }

    public static IRawGrammar readGrammarSync(String filePath, InputStream in) throws Exception {
        SyncGrammarReader reader = new SyncGrammarReader(in, getGrammarParser(filePath));
        return reader.load();
    }

    private static IGrammarParser getGrammarParser(String filePath) {
        if (filePath.endsWith(".json")) {
            return JSON_PARSER;
        }
        return XML_PARSER;
    }
}
