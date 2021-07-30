/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.test;

import android.app.Instrumentation;
import android.os.Looper;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.Random;

import io.github.rosemoe.editor.langs.EmptyLanguage;
import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.widget.CodeEditor;
import static org.junit.Assert.*;

public class BigTextTest {

    private CodeEditor editor;
    private final static String str = "public class Main {\npublic static void main(String[] args) {\n//test test test test test test\n}\n}\n";

    @Test(timeout = 5000)
    public void insertBigTextToContent() throws Exception {
        Content text = new Content();
        Content.useBlock = false;
        Content text2 = new Content();
        Random random = new Random();
        for (int i = 0;i < 10000;i++) {
            int line = random.nextInt(text.getLineCount());
            int m = text2.getColumnCount(line);
            int col = m > 0 ? random.nextInt(m) : 0;
            text.insert(line, col, str);
            text2.insert(line, col, str);
        }
        assertEquals(text, text2);
        for (int i = 0;i < text2.getLineCount();i++) {
            assertEquals("expected = " + text2.getColumnCount(i) + " actual = " + text.getColumnCount(i),text.getColumnCount(i), text2.getColumnCount(i));
        }
    }



    @Test(timeout = 10000)
    public void insertBigTextToEditor() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        if (Looper.myLooper() == null)
            Looper.prepare();
        editor = new CodeEditor(instrumentation.getTargetContext());
        editor.setEditorLanguage(new EmptyLanguage());
        Content.useBlock = false;
        Content text = new Content();
        Random random = new Random();
        int c = 0;
        Exception ex = null;
        //try {
            for (int i = 0; i < 10000; i++) {
                int line = random.nextInt(editor.getLineCount());
                text.insert(line, 0, str);
                editor.getText().insert(line, 0, str);
            }
        /*}catch (Exception e) {
            ex = e;
            c++;
            assertEquals(text, editor.getText());
        }
        assertEquals(ex == null ? "null" : ex.toString(),0, c);*/
    }

}
