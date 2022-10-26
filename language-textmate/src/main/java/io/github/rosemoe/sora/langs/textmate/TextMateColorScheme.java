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

import java.util.Collections;
import java.util.List;

import org.eclipse.tm4e.core.internal.theme.IRawTheme;
import org.eclipse.tm4e.core.internal.theme.Theme;
import org.eclipse.tm4e.core.internal.theme.ThemeRaw;
import org.eclipse.tm4e.core.internal.theme.ThemeReader;
import org.eclipse.tm4e.core.registry.IThemeSource;

import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class TextMateColorScheme extends EditorColorScheme implements ThemeRegistry.ThemeChangeListener {

    private Theme theme;

    private IRawTheme rawTheme;

    @Deprecated
    private IThemeSource themeSource;

    private final ThemeRegistry themeRegistry;

    public TextMateColorScheme(ThemeRegistry themeRegistry, ThemeModel themeModel) throws Exception {
        this.themeRegistry = themeRegistry;
        themeRegistry.loadTheme(themeModel);
        if (!themeRegistry.hasListener(this)) {
            themeRegistry.addListener(this);
        }
        setTheme(themeModel);

    }

    @Deprecated
    public static TextMateColorScheme create(IThemeSource themeSource) throws Exception {
        return create(new ThemeModel(themeSource));
    }

    public static TextMateColorScheme create(ThemeModel themeModel) throws Exception {
        return create(ThemeRegistry.getInstance(), themeModel);
    }

    public static TextMateColorScheme create(ThemeRegistry themeRegistry) throws Exception {
        return create(ThemeRegistry.getInstance(), themeRegistry.getCurrentThemeModel());
    }

    public static TextMateColorScheme create(ThemeRegistry themeRegistry, ThemeModel themeModel) throws Exception {
        return new TextMateColorScheme(themeRegistry, themeModel);
    }


    public void setTheme(ThemeModel themeModel) {
        super.colors.clear();
        this.rawTheme = themeModel.getRawTheme();
        this.theme = themeModel.getTheme();
        this.themeSource = themeModel.getThemeSource();
        applyDefault();
    }

    @Override
    public void onChangeTheme(ThemeModel newTheme) {
        setTheme(newTheme);
    }

    @Override
    public void applyDefault() {
        if (rawTheme != null) {
            super.applyDefault();
            ThemeRaw themeRaw = (ThemeRaw) ((List<?>) rawTheme.getSettings()).get(0);
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

            String highlightedDelimetersForeground =
                    (String) themeRaw.get("highlightedDelimetersForeground");
            if (highlightedDelimetersForeground != null) {
                setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, Color.parseColor(highlightedDelimetersForeground));
            }

            //TMTheme seems to have no fields to control BLOCK_LINE colors
            int blockLineColor = ((getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2) & 0x00FFFFFF | 0x88000000;
            setColor(BLOCK_LINE, blockLineColor);
            int blockLineColorCur = (blockLineColor) | 0xFF000000;
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

    @Override
    public void detachEditor(CodeEditor editor) {
        super.detachEditor(editor);
        themeRegistry.removeListener(this);
    }

    @Deprecated
    public IRawTheme getRawTheme() {
        return rawTheme;
    }


    @Deprecated
    public IThemeSource getThemeSource() {
        return themeSource;
    }
}
