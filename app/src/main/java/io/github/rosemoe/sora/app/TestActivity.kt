/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor

class TestActivity : AppCompatActivity() {
    private lateinit var editor: CodeEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editor = CodeEditor(this)
        setContentView(editor)
        editor.setEditorLanguage(JavaLanguage())
        editor.setText("private final PopupWindow mWindow;\n" +
                "    private final CodeEditor mEditor;\n" +
                "    private final int mFeatures;\n" +
                "    private final int[] mLocationBuffer = new int[2];\n" +
                "    private final EventReceiver<ScrollEvent> mScrollListener;\n" +
                "    private boolean mShowState;\n" +
                "    private boolean mRegisterFlag;\n" +
                "    private boolean mRegistered;\n" +
                "    private int mOffsetX, mOffsetY, mX, mY, mWidth, mHeight;")
    }
}