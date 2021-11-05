/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
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
package io.github.rosemoe.sora.app;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import io.github.rosemoe.sora.interfaces.EditorLanguage;
import io.github.rosemoe.sora.langs.EmptyLanguage;
import io.github.rosemoe.sora.langs.css3.CSS3Language;
import io.github.rosemoe.sora.langs.desc.CDescription;
import io.github.rosemoe.sora.langs.desc.CppDescription;
import io.github.rosemoe.sora.langs.desc.JavaScriptDescription;
import io.github.rosemoe.sora.langs.html.HTMLLanguage;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.langs.python.PythonLanguage;
import io.github.rosemoe.sora.langs.textmate.theme.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.universal.UniversalLanguage;
import io.github.rosemoe.sora.textmate.core.internal.theme.reader.ThemeReader;
import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;
import io.github.rosemoe.sora.utils.CrashHandler;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.EditorColorScheme;
import io.github.rosemoe.sora.widget.SymbolInputView;
import io.github.rosemoe.sora.widget.schemes.HTMLScheme;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse;
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub;
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX;
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019;

public class MainActivity extends AppCompatActivity {

    private CodeEditor editor;
    private LinearLayout panel;
    private EditText search, replace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler.INSTANCE.init(this);
        setContentView(R.layout.activity_main);

        editor = findViewById(R.id.editor);
        panel = findViewById(R.id.search_panel);
        search = findViewById(R.id.search_editor);
        replace = findViewById(R.id.replace_editor);

        SymbolInputView inputView = findViewById(R.id.symbol_input);
        inputView.bindEditor(editor);
        inputView.addSymbols(new String[]{"->", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"},
                new String[]{"\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"});

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                editor.getSearcher().search(editable.toString());
            }
        });
        editor.setTypefaceText(Typeface.createFromAsset(getAssets(), "JetBrainsMono-Regular.ttf"));
        editor.setEditorLanguage(new JavaLanguage());
        // The font we use does not have the issue
        editor.setLigatureEnabled(true);
        editor.setNonPrintablePaintingFlags(CodeEditor.FLAG_DRAW_WHITESPACE_LEADING | CodeEditor.FLAG_DRAW_LINE_SEPARATOR);
        new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("sample.txt")));
                String line;
                StringBuilder text = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    text.append(line).append('\n');
                }
                runOnUiThread(() -> editor.setText(text));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private final ActivityResultLauncher<String> loadTMLLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        try {
            if (result == null) return;
            //TextMateLanguage only support TextMateColorScheme
            EditorColorScheme editorColorScheme = editor.getColorScheme();
            if (!(editorColorScheme instanceof TextMateColorScheme)) {
                IRawTheme iRawTheme = ThemeReader.readThemeSync("QuietLight.tmTheme", getAssets().open("textmate/QuietLight.tmTheme"));
                editorColorScheme = TextMateColorScheme.create(iRawTheme);
                editor.setColorScheme(editorColorScheme);
            }


            EditorLanguage language = TextMateLanguage.create(
                    result.getPath()
                    , getContentResolver().openInputStream(result)
                    , ((TextMateColorScheme) editorColorScheme).getRawTheme());

            editor.setEditorLanguage(language);
        } catch (Exception e) {
            e.printStackTrace();
        }

    });
    private final ActivityResultLauncher<String> loadTMTLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        try {
            if (result == null) return;
            IRawTheme iRawTheme = ThemeReader.readThemeSync(
                    result.getPath()
                    , getContentResolver().openInputStream(result));
            TextMateColorScheme colorScheme = TextMateColorScheme.create(iRawTheme);
            editor.setColorScheme(colorScheme);

            EditorLanguage language = editor.getEditorLanguage();
            if (language instanceof TextMateLanguage) {
                TextMateLanguage textMateLanguage = (TextMateLanguage) language;
                textMateLanguage.updateTheme(iRawTheme);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        var id = item.getItemId();
        if (id == R.id.text_undo) {
            editor.undo();
        } else if (id == R.id.text_redo) {
            editor.redo();
        } else if (id == R.id.goto_end) {
            editor.setSelection(editor.getText().getLineCount() - 1, editor.getText().getColumnCount(editor.getText().getLineCount() - 1));
        } else if (id == R.id.move_up) {
            editor.moveSelectionUp();
        } else if (id == R.id.move_down) {
            editor.moveSelectionDown();
        } else if (id == R.id.home) {
            editor.moveSelectionHome();
        } else if (id == R.id.end) {
            editor.moveSelectionEnd();
        } else if (id == R.id.move_left) {
            editor.moveSelectionLeft();
        } else if (id == R.id.move_right) {
            editor.moveSelectionRight();
        } else if (id == R.id.magnifier) {
            editor.setMagnifierEnabled(!editor.isMagnifierEnabled());
            item.setChecked(editor.isMagnifierEnabled());
        } else if (id == R.id.code_format) {
            editor.formatCodeAsync();
        } else if (id == R.id.switch_language) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.switch_language)
                    .setSingleChoiceItems(new String[]{"C", "C++", "Java", "JavaScript", "HTML", "Python", "CSS3", "TextMate Java", "TextMate Kotlin", "TM Language from file", "None"}, -1, (dialog, which) -> {
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
                                editor.setEditorLanguage(new HTMLLanguage());
                                editor.setColorScheme(new HTMLScheme());
                                break;
                            case 5:
                                editor.setEditorLanguage(new PythonLanguage());
                                break;
                            case 6:
                                editor.setEditorLanguage(new CSS3Language());
                                break;
                            case 7:

                                try {
                                    //TextMateLanguage only support TextMateColorScheme
                                    EditorColorScheme editorColorScheme = editor.getColorScheme();
                                    if (!(editorColorScheme instanceof TextMateColorScheme)) {
                                        IRawTheme iRawTheme = ThemeReader.readThemeSync("QuietLight.tmTheme", getAssets().open("textmate/QuietLight.tmTheme"));
                                        editorColorScheme = TextMateColorScheme.create(iRawTheme);
                                        editor.setColorScheme(editorColorScheme);
                                    }


                                    EditorLanguage language = TextMateLanguage.create(
                                            "java.tmLanguage.json"
                                            , getAssets().open("textmate/java/syntaxes/java.tmLanguage.json")
                                            , new InputStreamReader(getAssets().open("textmate/java/language-configuration.json"))
                                            , ((TextMateColorScheme) editorColorScheme).getRawTheme());


                                    editor.setEditorLanguage(language);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                break;
                            case 8:

                                try {
                                    //TextMateLanguage only support TextMateColorScheme
                                    EditorColorScheme editorColorScheme = editor.getColorScheme();
                                    if (!(editorColorScheme instanceof TextMateColorScheme)) {
                                        IRawTheme iRawTheme = ThemeReader.readThemeSync("QuietLight.tmTheme", getAssets().open("textmate/QuietLight.tmTheme"));
                                        editorColorScheme = TextMateColorScheme.create(iRawTheme);
                                        editor.setColorScheme(editorColorScheme);
                                    }


                                    EditorLanguage language = TextMateLanguage.create(
                                            "Kotlin.tmLanguage"
                                            , getAssets().open("textmate/kotlin/syntaxes/Kotlin.tmLanguage")
                                            , new InputStreamReader(getAssets().open("textmate/kotlin/language-configuration.json"))
                                            , ((TextMateColorScheme) editorColorScheme).getRawTheme());


                                    editor.setEditorLanguage(language);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                break;
                            case 9:
                                loadTMLLauncher.launch("*/*");
                                break;
                            default:
                                editor.setEditorLanguage(new EmptyLanguage());
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else if (id == R.id.search_panel_st) {
            if (panel.getVisibility() == View.GONE) {
                replace.setText("");
                search.setText("");
                editor.getSearcher().stopSearch();
                panel.setVisibility(View.VISIBLE);
                item.setChecked(true);
            } else {
                panel.setVisibility(View.GONE);
                editor.getSearcher().stopSearch();
                item.setChecked(false);
            }
        } else if (id == R.id.search_am) {
            replace.setText("");
            search.setText("");
            editor.getSearcher().stopSearch();
            editor.beginSearchMode();
        } else if (id == R.id.switch_colors) {
            var themes = new String[]{"Default", "GitHub", "Eclipse",
                    "Darcula", "VS2019", "NotepadXX", "HTML", "QuietLight for TM", "Darcula for TM", "Abyss for TM", "TM theme from file"};
            new AlertDialog.Builder(this)
                    .setTitle(R.string.color_scheme)
                    .setSingleChoiceItems(themes, -1, (dialog, which) -> {
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
                            case 6:
                                editor.setColorScheme(new HTMLScheme());
                                break;
                            case 7:
                                try {
                                    IRawTheme iRawTheme = ThemeReader.readThemeSync("QuietLight.tmTheme", getAssets().open("textmate/QuietLight.tmTheme"));
                                    TextMateColorScheme colorScheme = TextMateColorScheme.create(iRawTheme);
                                    editor.setColorScheme(colorScheme);

                                    EditorLanguage language = editor.getEditorLanguage();
                                    if (language instanceof TextMateLanguage) {
                                        TextMateLanguage textMateLanguage = (TextMateLanguage) language;
                                        textMateLanguage.updateTheme(iRawTheme);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 8:
                                try {
                                    IRawTheme iRawTheme = ThemeReader.readThemeSync("darcula.json", getAssets().open("textmate/darcula.json"));
                                    TextMateColorScheme colorScheme = TextMateColorScheme.create(iRawTheme);
                                    editor.setColorScheme(colorScheme);

                                    EditorLanguage language = editor.getEditorLanguage();
                                    if (language instanceof TextMateLanguage) {
                                        TextMateLanguage textMateLanguage = (TextMateLanguage) language;
                                        textMateLanguage.updateTheme(iRawTheme);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 9:
                                try {
                                    IRawTheme iRawTheme = ThemeReader.readThemeSync("abyss-color-theme.json", getAssets().open("textmate/abyss-color-theme.json"));
                                    TextMateColorScheme colorScheme = TextMateColorScheme.create(iRawTheme);
                                    editor.setColorScheme(colorScheme);

                                    EditorLanguage language = editor.getEditorLanguage();
                                    if (language instanceof TextMateLanguage) {
                                        TextMateLanguage textMateLanguage = (TextMateLanguage) language;
                                        textMateLanguage.updateTheme(iRawTheme);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 10:
                                loadTMTLauncher.launch("*/*");
                                break;
                        }
                        editor.doAnalyze();
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else if (id == R.id.text_wordwrap) {
            item.setChecked(!item.isChecked());
            editor.setWordwrap(item.isChecked());
        } else if (id == R.id.open_logs) {
            FileInputStream fis = null;
            try {
                fis = openFileInput("crash-journal.log");
                var br = new BufferedReader(new InputStreamReader(fis));
                var sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                Toast.makeText(this, "Succeeded", Toast.LENGTH_SHORT).show();
                editor.setText(sb);
            } catch (Exception e) {
                Toast.makeText(this, "Failed:" + e, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (id == R.id.clear_logs) {
            FileOutputStream fos = null;
            try {
                fos = openFileOutput("crash-journal.log", MODE_PRIVATE);
                Toast.makeText(this, "Succeeded", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed:" + e, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (id == R.id.open_debug_logs) {
            //ignored
            //editor.setText(Logs.getLogs());
        } else if (id == R.id.editor_line_number) {
            editor.setLineNumberEnabled(!editor.isLineNumberEnabled());
            item.setChecked(editor.isLineNumberEnabled());
        } else if (id == R.id.pin_line_number) {
            editor.setPinLineNumber(!editor.isLineNumberPinned());
            item.setChecked(editor.isLineNumberPinned());
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
