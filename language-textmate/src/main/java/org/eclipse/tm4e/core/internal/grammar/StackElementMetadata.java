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

import org.eclipse.tm4e.core.grammar.StackElement;
import org.eclipse.tm4e.core.theme.FontStyle;

/**
 *
 * Metadata for {@link StackElement}.
 *
 */
public class StackElementMetadata {

    /**
     * Content should be referenced statically
     */
    private StackElementMetadata() {
    }

    public static String toBinaryStr(int metadata) {
        /*
         * let r = metadata.toString(2); while (r.length < 32) { r = '0' + r; }
         * return r;
         */
        // TODO!!!
        return null;
    }

    public static int getLanguageId(int metadata) {
        return (metadata & MetadataConsts.LANGUAGEID_MASK) >>> MetadataConsts.LANGUAGEID_OFFSET;
    }

    public static int getTokenType(int metadata) {
        return (metadata & MetadataConsts.TOKEN_TYPE_MASK) >>> MetadataConsts.TOKEN_TYPE_OFFSET;
    }

    public static int getFontStyle(int metadata) {
        return (metadata & MetadataConsts.FONT_STYLE_MASK) >>> MetadataConsts.FONT_STYLE_OFFSET;
    }

    public static int getForeground(int metadata) {
        return (metadata & MetadataConsts.FOREGROUND_MASK) >>> MetadataConsts.FOREGROUND_OFFSET;
    }

    public static int getBackground(int metadata) {
        return (metadata & MetadataConsts.BACKGROUND_MASK) >>> MetadataConsts.BACKGROUND_OFFSET;
    }

    public static int set(int metadata, int languageId, int tokenType, int fontStyle, int foreground, int background) {
        languageId = languageId == 0 ? StackElementMetadata.getLanguageId(metadata) : languageId;
        tokenType = tokenType == StandardTokenType.Other ? StackElementMetadata.getTokenType(metadata) : tokenType;
        fontStyle = fontStyle == FontStyle.NotSet ? StackElementMetadata.getFontStyle(metadata) : fontStyle;
        foreground = foreground == 0 ? StackElementMetadata.getForeground(metadata) : foreground;
        background = background == 0 ? StackElementMetadata.getBackground(metadata) : background;
        return ((languageId << MetadataConsts.LANGUAGEID_OFFSET) | (tokenType << MetadataConsts.TOKEN_TYPE_OFFSET)
                | (fontStyle << MetadataConsts.FONT_STYLE_OFFSET) | (foreground << MetadataConsts.FOREGROUND_OFFSET)
                | (background << MetadataConsts.BACKGROUND_OFFSET)) >>> 0;
    }

}
