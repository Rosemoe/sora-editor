/*******************************************************************************
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Sebastian Thomschke (Vegard IT GmbH) - add previousLineText support
 *
 ******************************************************************************/

package io.github.rosemoe.sora.langs.monarch.theme

import io.github.dingyi222666.monarch.types.ColorId
import io.github.dingyi222666.monarch.types.FontStyle
import io.github.dingyi222666.monarch.types.MetadataConsts

class ThemeTrieElementRule(
    private var fontStyle: Int,
    private var foreground: Int,
    private var background: Int
) {
    var metadata: Int = (fontStyle shl MetadataConsts.FONT_STYLE_OFFSET) or
            (foreground shl MetadataConsts.FOREGROUND_OFFSET) or
            (background shl MetadataConsts.BACKGROUND_OFFSET)

    fun clone(): ThemeTrieElementRule = ThemeTrieElementRule(fontStyle, foreground, background)

    fun acceptOverwrite(fontStyle: Int, foreground: Int, background: Int) {
        if (fontStyle != FontStyle.NotSet) {
            this.fontStyle = fontStyle
        }
        if (foreground != ColorId.None) {
            this.foreground = foreground
        }
        if (background != ColorId.None) {
            this.background = background
        }
        metadata = (this.fontStyle shl MetadataConsts.FONT_STYLE_OFFSET) or
                (this.foreground shl MetadataConsts.FOREGROUND_OFFSET) or
                (this.background shl MetadataConsts.BACKGROUND_OFFSET)
    }

    override fun toString(): String {
        return "ThemeTrieElementRule(fontStyle=$fontStyle, foreground=$foreground, background=$background, metadata=$metadata)"
    }


}
