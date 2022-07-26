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
package io.github.rosemoe.sora.langs.textmate;

import android.graphics.Color;

import java.util.List;

import org.eclipse.tm4e.core.internal.theme.ThemeRaw;
import org.eclipse.tm4e.core.theme.IRawTheme;
import org.eclipse.tm4e.core.theme.Theme;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class TextMateColorScheme extends EditorColorScheme {

    private final Theme theme;
    private final IRawTheme iRawTheme;

    public TextMateColorScheme(IRawTheme iRawTheme) {
        this.iRawTheme = iRawTheme;
        this.theme = Theme.createFromRawTheme(iRawTheme);
        applyDefault();
    }

    public static TextMateColorScheme create(IRawTheme iRawTheme) {
        return new TextMateColorScheme(iRawTheme);
    }

    @Override
    public void applyDefault() {
        if (iRawTheme != null) {
            super.applyDefault();
            ThemeRaw themeRaw = (ThemeRaw) ((List<?>) iRawTheme.getSettings()).get(0);
            themeRaw = (ThemeRaw) themeRaw.getSetting();

            setColor(LINE_DIVIDER, Color.TRANSPARENT);

            String caret = (String) themeRaw.get("caret");
            if (caret != null) {
                setColor(SELECTION_INSERT, Color.parseColor(caret));
            }


            String selection = (String) themeRaw.get("selection");
            if (selection != null) {
                setColor(SELECTED_TEXT_BACKGROUND, Color.parseColor(selection));
            }

            String invisibles = (String) themeRaw.get("invisibles");
            if (invisibles != null) {
                setColor(NON_PRINTABLE_CHAR, Color.parseColor(invisibles));
            }

            String lineHighlight = (String) themeRaw.get("lineHighlight");
            if (lineHighlight != null) {
                setColor(CURRENT_LINE, Color.parseColor(lineHighlight));
            }

            String background = (String) themeRaw.get("background");
            if (background != null) {
                setColor(WHOLE_BACKGROUND, Color.parseColor(background));
                setColor(LINE_NUMBER_BACKGROUND, Color.parseColor(background));
            }

            String foreground = (String) themeRaw.get("foreground");
            if (foreground != null) {
                setColor(TEXT_NORMAL, Color.parseColor(foreground));
            }

            //TMTheme seems to have no fields to control BLOCK_LINE colors
            int blockLineColor=((getColor(WHOLE_BACKGROUND)+getColor(TEXT_NORMAL))/2)&0x00FFFFFF|0x88000000;
            setColor(BLOCK_LINE, blockLineColor);
            int blockLineColorCur=(blockLineColor)|0xFF000000;
            setColor(BLOCK_LINE_CURRENT, blockLineColorCur);
        }

    }

    @Override
    public int getColor(int type) {
        if (type >= 255) {
            // Cache colors in super class
            var superColor = super.getColor(type);
            if (superColor == 0) {
                if (theme != null) {
                    String color = theme.getColor(type - 255);
                    var newColor = color != null ? Color.parseColor(color) : super.getColor(TEXT_NORMAL);
                    super.colors.put(type, newColor);
                    return newColor;
                }
                return super.getColor(TEXT_NORMAL);
            } else {
                return superColor;
            }
        }
        return super.getColor(type);
    }

    public IRawTheme getRawTheme() {
        return iRawTheme;
    }

}
