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
package org.eclipse.tm4e.core.internal.grammar;

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
