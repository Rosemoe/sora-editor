/*
 *   Copyright 2020-2021 Rosemoe
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
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
