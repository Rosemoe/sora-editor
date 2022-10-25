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
package io.github.rosemoe.sora.langs.textmate.registry.model;

import org.eclipse.tm4e.core.internal.theme.IRawTheme;
import org.eclipse.tm4e.core.internal.theme.Theme;
import org.eclipse.tm4e.core.internal.theme.ThemeReader;
import org.eclipse.tm4e.core.registry.IThemeSource;

import io.github.rosemoe.sora.langs.textmate.StringUtil;

public class ThemeModel {

    public static final ThemeModel EMPTY = new ThemeModel("EMPTY");
    private IThemeSource themeSource;

    private IRawTheme rawTheme;

    private Theme theme;

    private String name;

    public ThemeModel(IThemeSource themeSource) {
        this.themeSource = themeSource;
        this.name = StringUtil.getFileNameWithoutExtension(themeSource.getFilePath());
    }

    private ThemeModel(String name) {
        themeSource = null;
        rawTheme = null;
        this.name = name;
        theme = Theme.createFromRawTheme(null,null);
    }

    //TODO colorMap support
    public void load() throws Exception {
        rawTheme = ThemeReader.readTheme(themeSource);
        theme = Theme.createFromRawTheme(rawTheme, null);

    }

    public boolean isLoaded() {
        return theme != null;
    }

    public IThemeSource getThemeSource() {
        return themeSource;
    }

    public IRawTheme getRawTheme() {
        return rawTheme;
    }

    public Theme getTheme() {
        return theme;
    }

    public String getName() {
        return name;
    }
}
