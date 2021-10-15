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
package io.github.rosemoe.sora.widget.schemes;

import io.github.rosemoe.sora.widget.EditorColorScheme;

/**
 * ColorScheme for editor
 * picked from GitHub site
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
   *Theme by ninja coder
 */

public class SchemeDarcula extends EditorColorScheme {
   public void applyDefault() {
      super.applyDefault();
      setColor(ANNOTATION, 0xffbbb529);
        setColor(FUNCTION_NAME, 0xffffffff);
        setColor(IDENTIFIER_NAME, 0xffffffff);
        setColor(IDENTIFIER_VAR, 0xff9876aa);
        setColor(LITERAL, 0xffe60b3e);
        setColor(OPERATOR, 0xFF27B0F5);
        setColor(COMMENT, 0xFF06D718); // yellow
        setColor(KEYWORD, 0xFFFE7400);
        setColor(WHOLE_BACKGROUND, 0xFF01132B);
        setColor(TEXT_NORMAL, 0xFFD3C7EA);
        setColor(LINE_NUMBER_BACKGROUND, 0xFF01132B);
        setColor(LINE_NUMBER, 0xFF812AD2);
        setColor(LINE_DIVIDER, 0xFF812AD2);
        setColor(SCROLL_BAR_THUMB, 0xffa6a6a6);
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xff565656);
        setColor(SELECTED_TEXT_BACKGROUND, 0xff96993f);
		// search result highlight color
        setColor(MATCHED_TEXT_BACKGROUND, 0xFFF91D00);
        setColor(CURRENT_LINE, 0x4B00D9BC);
        setColor(SELECTION_INSERT, 0xffffffff);
        setColor(SELECTION_HANDLE, 0xffffffff);
        setColor(BLOCK_LINE, 0xFFDE3030);
        setColor(BLOCK_LINE_CURRENT, 0xff40a8a3);
        setColor(NON_PRINTABLE_CHAR, 0xFFA8340A);
   }
}



