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
package io.github.rosemoe.sora.textmate.core.internal.theme;

import java.util.Collection;
import java.util.HashMap;

import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;
import io.github.rosemoe.sora.textmate.core.theme.IRawThemeSetting;
import io.github.rosemoe.sora.textmate.core.theme.IThemeSetting;

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
