
/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
 *    Copyright (C) 2020-2021  Rosemoe
 * code by ninja coder 
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


public class SchemeGreenhile extends EditorColorScheme {

    @Override
    public void applyDefault() {
        super.applyDefault();
        setColor(ANNOTATION, 0xff0000ff);
        setColor(FUNCTION_NAME, 0xFFFFFFFF);
        setColor(IDENTIFIER_NAME, 0xFFFFFFFF);
        setColor(IDENTIFIER_VAR, 0xFFFFFFFF);
        setColor(LITERAL, 0xFFE9A49B);
        setColor(OPERATOR, 0xFF67C1C8);
        setColor(COMMENT, 0xFFFF6FB1);
        setColor(KEYWORD, 0xFFE41456);
        setColor(WHOLE_BACKGROUND, 0xFF00331D);
        setColor(TEXT_NORMAL, 0xFFFFFFFF);
        setColor(LINE_NUMBER_BACKGROUND, 0xFF00331D);
        setColor(LINE_NUMBER, 0xFFFFFFFF);
        setColor(SELECTED_TEXT_BACKGROUND, 0xFF01A71A);
        setColor(MATCHED_TEXT_BACKGROUND, 0xFF01A71A);
        setColor(CURRENT_LINE, 0x6820BE00);
        setColor(SELECTION_INSERT, 0xFFEA9E10);
        setColor(SELECTION_HANDLE, 0xFFE3B100);
        setColor(BLOCK_LINE, 0xFF7059F5);
        setColor(BLOCK_LINE_CURRENT, 0xFF7059F5);
    }

}
