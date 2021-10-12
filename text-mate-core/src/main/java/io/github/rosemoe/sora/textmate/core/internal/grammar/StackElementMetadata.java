/**
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

import io.github.rosemoe.sora.textmate.core.grammar.StackElement;
import io.github.rosemoe.sora.textmate.core.theme.FontStyle;

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
