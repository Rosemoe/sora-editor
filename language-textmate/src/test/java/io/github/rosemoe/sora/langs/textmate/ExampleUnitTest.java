/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.FileInputStream;
import java.util.Arrays;

import io.github.rosemoe.sora.textmate.core.internal.theme.ThemeRaw;
import io.github.rosemoe.sora.textmate.core.internal.theme.reader.ThemeReader;
import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {

        IRawTheme iRawTheme = ThemeReader.readThemeSync("ideal-color-theme.json", new FileInputStream("C:\\Users\\Coyamo\\.vscode\\extensions\\karsany.vscode-ideal-theme-0.0.3\\themes\\ideal-color-theme.json"));

        ThemeRaw themeRaw = (ThemeRaw) iRawTheme;
        System.out.println(Arrays.toString(themeRaw.keySet().toArray()));
        assertEquals(4, 2 + 2);
    }
}