/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
}
