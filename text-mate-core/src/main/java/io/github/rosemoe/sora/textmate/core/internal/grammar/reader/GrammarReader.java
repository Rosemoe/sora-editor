/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/Microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package io.github.rosemoe.sora.textmate.core.internal.grammar.reader;

import java.io.InputStream;

import io.github.rosemoe.sora.textmate.core.internal.parser.json.JSONPListParser;
import io.github.rosemoe.sora.textmate.core.internal.parser.xml.XMLPListParser;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawGrammar;

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
