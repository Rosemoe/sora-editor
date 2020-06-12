/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.rose.editor.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import com.rose.editor.langs.s5droid.S5droidAutoComplete;
import com.rose.editor.struct.NavigationLabel;
import com.rose.editor.utils.CrashHandler;

import java.util.List;
import com.rose.editor.langs.java.JavaLanguage;

public class MainActivity extends Activity {

    private CodeEditor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler.getInstance().init(this);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 9998);
        }
        S5droidAutoComplete.init(this);
        editor = (CodeEditor) findViewById(R.id.editor);
        editor.setEditorLanguage(new JavaLanguage());
        editor.setText("public class Main {\n\n\tpublic static void main(String[] args) {\n\t\t\n\t}\n\n}");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        SubMenu sub = menu.addSubMenu(0,9999,0,"Cursor Actions");
        //sub.add(0,10,0,"Go to line");
        sub.add(0,2,0,"Go To End");
        sub.add(0,3,0,"Move Up");
        sub.add(0,4,0,"Move Down");
        sub.add(0,5,0,"Home");
        sub.add(0,6,0,"End");
        sub.add(0,7,0,"Move Left");
        sub.add(0,8,0,"Move Right");
        sub = menu.addSubMenu(0,99999,0,"Text Actions");
        sub.add(0,0,0,"Undo");
        sub.add(0,1,0,"Redo");
        sub.add(0,11,0,"Copy");
        sub.add(0,12,0,"Paste");
        menu.add(0,9,0,"Code Navigation");
        menu.add(0,10,0,"Format");
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        long st = System.nanoTime();
        try {
            switch (item.getItemId()) {
                case 0:
                    editor.undo();
                    break;
                case 1:
                    editor.redo();
                    break;
                case 2:
                    editor.setSelection(editor.getText().getLineCount() - 1, editor.getText().getColumnCount(editor.getText().getLineCount() - 1));
                    break;
                case 3:
                    editor.moveSelectionUp();
                    break;
                case 4:
                    editor.moveSelectionDown();
                    break;
                case 5:
                    editor.moveSelectionHome();
                    break;
                case 6:
                    editor.moveSelectionEnd();
                    break;
                case 7:
                    editor.moveSelectionLeft();
                    break;
                case 8:
                    editor.moveSelectionRight();
                    break;
                case 9: {
                    final List<NavigationLabel> labels = editor.getTextColor().getNavigation();
                    if (labels == null) {
                        Toast.makeText(this, "Code navigation not prepared or unsupported", Toast.LENGTH_SHORT).show();
                    } else {
                        CharSequence[] items = new CharSequence[labels.size()];
                        for (int i = 0; i < labels.size(); i++) {
                            items[i] = labels.get(i).label;
                        }
                        new AlertDialog.Builder(this)
                                .setTitle("Code navigation")
                                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface p1, int p2) {
                                        editor.jumpToLine(labels.get(p2).line);
                                        p1.dismiss();
                                    }

                                })
                                .setPositiveButton("Cancel", null)
                                .show();
                    }
                    break;
                }
                case 10:
                    editor.formatCode();
                    break;
                case 11:
                    editor.copyText();
                    break;
                case 12:
                    editor.pasteText();
                    break;
            }
        }catch(Exception t){
            StringBuilder sb = new StringBuilder();
            sb.append(t.toString());
            for(Object o : t.getStackTrace()){
                sb.append('\n').append(o);
            }
            new AlertDialog.Builder(this)
                    .setTitle("Error occured!")
                    .setMessage(sb)
                    .setPositiveButton("Cancel",null)
                    .show();
        }
        Toast.makeText(this,"Action done in " + (System.nanoTime() - st) / 1e6 + " ms.",Toast.LENGTH_SHORT).show();

        return super.onOptionsItemSelected(item);
    }
}
