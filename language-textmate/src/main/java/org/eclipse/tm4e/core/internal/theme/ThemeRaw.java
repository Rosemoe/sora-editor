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
package org.eclipse.tm4e.core.internal.theme;

import org.eclipse.tm4e.core.theme.IRawTheme;
import org.eclipse.tm4e.core.theme.IRawThemeSetting;
import org.eclipse.tm4e.core.theme.IThemeSetting;

import java.util.Collection;
import java.util.HashMap;

public class ThemeRaw extends HashMap<String, Object> implements IRawTheme, IRawThemeSetting, IThemeSetting {

    private static final long serialVersionUID = -3622927264735492387L;

    @Override
    public String getName() {
        return (String) super.get("name");
    }

    @Override
    public Collection<IRawThemeSetting> getSettings() {
        return (Collection<IRawThemeSetting>) super.get("settings");
    }

    @Override
    public Object getScope() {
        return super.get("scope");
    }

    @Override
    public IThemeSetting getSetting() {
        return (IThemeSetting) super.get("settings");
    }

    @Override
    public Object getFontStyle() {
        return super.get("fontStyle");
    }

    @Override
    public String getBackground() {
        return (String) super.get("background");
    }

    @Override
    public String getForeground() {
        return (String) super.get("foreground");
    }

}
