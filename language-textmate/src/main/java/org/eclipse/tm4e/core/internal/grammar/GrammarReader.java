/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.internal.grammar;

import org.eclipse.tm4e.core.internal.parser.PListParser;
import org.eclipse.tm4e.core.internal.parser.PListParserJSON;
import org.eclipse.tm4e.core.internal.parser.PListParserXML;
import org.eclipse.tm4e.core.internal.parser.PListParserYAML;
import org.eclipse.tm4e.core.internal.parser.PListPath;
import org.eclipse.tm4e.core.internal.parser.PropertySettable;
import org.eclipse.tm4e.core.internal.types.IRawGrammar;
import org.eclipse.tm4e.core.registry.IGrammarSource;

/**
 * TextMate Grammar reader utilities.
 */
public final class GrammarReader {

    public static final PropertySettable.Factory<PListPath> OBJECT_FACTORY = path -> {
        if (path.size() == 0) {
            return new RawGrammar();
        }

        switch (path.last()) {
            case RawRule.REPOSITORY:
                return new RawRepository();
            case RawRule.BEGIN_CAPTURES:
            case RawRule.CAPTURES:
            case RawRule.END_CAPTURES:
            case RawRule.WHILE_CAPTURES:
                return new RawCaptures();
            default:
                return new RawRule();
        }

      /*  return switch (path.last()) {
            case RawRule.REPOSITORY -> new RawRepository();
            case RawRule.BEGIN_CAPTURES, RawRule.CAPTURES, RawRule.END_CAPTURES, RawRule.WHILE_CAPTURES ->
                    new RawCaptures();
            default -> new RawRule();
        };*/
    };

    private static final PListParser<RawGrammar> JSON_PARSER = new PListParserJSON<>(OBJECT_FACTORY);
    private static final PListParser<RawGrammar> XML_PARSER = new PListParserXML<>(OBJECT_FACTORY);
    private static final PListParser<RawGrammar> YAML_PARSER = new PListParserYAML<>(OBJECT_FACTORY);

    public static IRawGrammar readGrammar(final IGrammarSource source) throws Exception {
        try (var reader = source.getReader()) {
            switch (source.getContentType()) {
                case JSON:
                    return JSON_PARSER.parse(reader);
                case YAML:
                    return YAML_PARSER.parse(reader);
                case XML:
                default:
                    return XML_PARSER.parse(reader);
            }
        }
    }

    /**
     * methods should be accessed statically
     */
    private GrammarReader() {
    }
}
