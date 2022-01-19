/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.internal.grammar;

/**
 * Helpers to manage the "collapsed" metadata of an entire StackElement stack.
 * The following assumptions have been made:
 *  - languageId < 256 => needs 8 bits
 *  - unique color count < 512 => needs 9 bits
 *
 * The binary format is:
 * - -------------------------------------------
 *     3322 2222 2222 1111 1111 1100 0000 0000
 *     1098 7654 3210 9876 5432 1098 7654 3210
 * - -------------------------------------------
 *     xxxx xxxx xxxx xxxx xxxx xxxx xxxx xxxx
 *     bbbb bbbb bfff ffff ffFF FTTT LLLL LLLL
 * - -------------------------------------------
 *  - L = LanguageId (8 bits)
 *  - T = StandardTokenType (3 bits)
 *  - F = FontStyle (3 bits)
 *  - f = foreground color (9 bits)
 *  - b = background color (9 bits)
 */
public class MetadataConsts {

    public static final int LANGUAGEID_MASK = 0b00000000000000000000000011111111;
    public static final int TOKEN_TYPE_MASK = 0b00000000000000000000011100000000;
    public static final int FONT_STYLE_MASK = 0b00000000000000000011100000000000;
    public static final int FOREGROUND_MASK = 0b00000000011111111100000000000000;
    public static final int BACKGROUND_MASK = 0b11111111100000000000000000000000;
    public static final int LANGUAGEID_OFFSET = 0;
    public static final int TOKEN_TYPE_OFFSET = 8;
    public static final int FONT_STYLE_OFFSET = 11;
    public static final int FOREGROUND_OFFSET = 14;
    public static final int BACKGROUND_OFFSET = 23;
    /**
     * content should be accessed statically
     */
    private MetadataConsts() {
    }
}
