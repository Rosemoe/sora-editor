/*
 *   Copyright 2020 Rosemoe
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
package com.rose.editor.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import io.github.rosemoe.editor.langs.EmptyLanguage;
import io.github.rosemoe.editor.langs.desc.CDescription;
import io.github.rosemoe.editor.langs.desc.CppDescription;
import io.github.rosemoe.editor.langs.desc.JavaScriptDescription;
import io.github.rosemoe.editor.langs.s5droid.S5droidAutoComplete;
import io.github.rosemoe.editor.langs.s5droid.S5droidLanguage;
import io.github.rosemoe.editor.langs.universal.UniversalLanguage;
import io.github.rosemoe.editor.struct.NavigationLabel;
import io.github.rosemoe.editor.utils.CrashHandler;

import java.util.List;

import io.github.rosemoe.editor.langs.java.JavaLanguage;
import io.github.rosemoe.editor.widget.CodeEditor;
import io.github.rosemoe.editor.widget.EditorColorScheme;
import io.github.rosemoe.editor.widget.schemes.SchemeDarcula;
import io.github.rosemoe.editor.widget.schemes.SchemeEclipse;
import io.github.rosemoe.editor.widget.schemes.SchemeGitHub;
import io.github.rosemoe.editor.widget.schemes.SchemeNotepadXX;
import io.github.rosemoe.editor.widget.schemes.SchemeVS2019;

public class MainActivity extends Activity {

    private CodeEditor editor;
    private LinearLayout panel;
    private EditText search, replace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler.INSTANCE.init(this);
        setContentView(R.layout.activity_main);
        if (getActionBar() != null) {
            Editable title = Editable.Factory.getInstance().newEditable(getString(R.string.app_name));
            title.setSpan(new ForegroundColorSpan(0xffffffff), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            getActionBar().setTitle(title);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_permission_title)
                    .setMessage(R.string.dialog_permission_content)
                    .setPositiveButton(R.string.dialog_permission_permit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 9998);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(false)
                    .show();
        }
        S5droidAutoComplete.init(this);
        editor = findViewById(R.id.editor);
        panel = findViewById(R.id.search_panel);
        search = findViewById(R.id.search_editor);
        replace = findViewById(R.id.replace_editor);
        editor.setOverScrollEnabled(false);
        editor.setEditorLanguage(new JavaLanguage());
        editor.setColorScheme(new SchemeDarcula());
        editor.setText("/**\n * Demo\n */\n@SuppressWarnings(/**/\"unused\")\n" +
                "public class Main {\n\n\tpublic static void main(String[] args) {\n\t\t" +
                "// Comment\n\t\tSystem.out.println(\"Hello\");\n\t}\n\n}\n");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.text_undo:
                editor.undo();
                break;
            case R.id.text_redo:
                editor.redo();
                break;
            case R.id.goto_end:
                editor.setSelection(editor.getText().getLineCount() - 1, editor.getText().getColumnCount(editor.getText().getLineCount() - 1));
                break;
            case R.id.move_up:
                editor.moveSelectionUp();
                break;
            case R.id.move_down:
                editor.moveSelectionDown();
                break;
            case R.id.home:
                editor.moveSelectionHome();
                break;
            case R.id.end:
                editor.moveSelectionEnd();
                break;
            case R.id.move_left:
                editor.moveSelectionLeft();
                break;
            case R.id.move_right:
                editor.moveSelectionRight();
                break;
            case R.id.code_navigation: {
                final List<NavigationLabel> labels = editor.getTextAnalyzeResult().getNavigation();
                if (labels == null) {
                    Toast.makeText(this, R.string.navi_err_msg, Toast.LENGTH_SHORT).show();
                } else {
                    CharSequence[] items = new CharSequence[labels.size()];
                    for (int i = 0; i < labels.size(); i++) {
                        items[i] = labels.get(i).label;
                    }
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.code_navi)
                            .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface p1, int p2) {
                                    editor.jumpToLine(labels.get(p2).line);
                                    p1.dismiss();
                                }

                            })
                            .setPositiveButton(android.R.string.cancel, null)
                            .show();
                }
                break;
            }
            case R.id.code_format:
                editor.formatCodeAsync();
                break;
            case R.id.switch_language:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.switch_language)
                        .setSingleChoiceItems(new String[]{"C", "C++", "Java", "JavaScript", "S5droid", "None"}, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        editor.setEditorLanguage(new UniversalLanguage(new CDescription()));
                                        break;
                                    case 1:
                                        editor.setEditorLanguage(new UniversalLanguage(new CppDescription()));
                                        break;
                                    case 2:
                                        editor.setEditorLanguage(new JavaLanguage());
                                        break;
                                    case 3:
                                        editor.setEditorLanguage(new UniversalLanguage(new JavaScriptDescription()));
                                        break;
                                    case 4:
                                        editor.setEditorLanguage(new S5droidLanguage());
                                        break;
                                    case 5:
                                        editor.setEditorLanguage(new EmptyLanguage());
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
            case R.id.search_panel_st:
                if (panel.getVisibility() == View.GONE) {
                    replace.setText("");
                    search.setText("");
                    editor.getSearcher().stopSearch();
                    panel.setVisibility(View.VISIBLE);
                } else {
                    panel.setVisibility(View.GONE);
                    editor.getSearcher().stopSearch();
                }
                break;
            case R.id.search_am:
                editor.beginSearchMode();
                break;
            case R.id.switch_colors:
                String[] themes = new String[]{"Default", "GitHub", "Eclipse",
                        "Darcula", "VS2019", "NotepadXX"};
                new AlertDialog.Builder(this)
                        .setTitle(R.string.color_scheme)
                        .setSingleChoiceItems(themes, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        editor.setColorScheme(new EditorColorScheme());
                                        break;
                                    case 1:
                                        editor.setColorScheme(new SchemeGitHub());
                                        break;
                                    case 2:
                                        editor.setColorScheme(new SchemeEclipse());
                                        break;
                                    case 3:
                                        editor.setColorScheme(new SchemeDarcula());
                                        break;
                                    case 4:
                                        editor.setColorScheme(new SchemeVS2019());
                                        break;
                                    case 5:
                                        editor.setColorScheme(new SchemeNotepadXX());
                                        break;
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
            case R.id.text_wordwrap:
                item.setChecked(!item.isChecked());
                editor.setWordwrap(item.isChecked());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void gotoNext(View view) {
        try {
            editor.getSearcher().gotoNext();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void gotoLast(View view) {
        try {
            editor.getSearcher().gotoLast();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void replace(View view) {
        try {
            editor.getSearcher().replaceThis(replace.getText().toString());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void replaceAll(View view) {
        try {
            editor.getSearcher().replaceAll(replace.getText().toString());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
