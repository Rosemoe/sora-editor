package com.rose.editor.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import com.rose.editor.langs.s5droid.S5droidLanguage;
import com.rose.editor.model.NavigationLabel;
import com.rose.editor.utils.CrashHandler;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RoseEditor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler.getInstance().init(this);
        setContentView(R.layout.activity_main);
        editor = (RoseEditor) findViewById(R.id.editor);
        editor.setEditorLanguage(new S5droidLanguage());
        editor.setText("/*\n * Test\n*/\n//test\n方法 测试方法_求和(参数1 为 整数型,参数2 为 整数型) 为 整数型\n   (返回 参数1 + 参数2);\n结束 方法");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        SubMenu sub = menu.addSubMenu(0,9999,0,"Cursor");
        //sub.add(0,10,0,"Go to line");
        sub.add(0,2,0,"Go to End");
        sub.add(0,3,0,"Move Up");
        sub.add(0,4,0,"Move Down");
        sub.add(0,5,0,"Home");
        sub.add(0,6,0,"End");
        sub.add(0,7,0,"Move Left");
        sub.add(0,8,0,"Move Right");
        sub = menu.addSubMenu(0,99999,0,"ComposeText");
        sub.add(0,0,0,"Undo");
        sub.add(0,1,0,"Redo");
        menu.add(0,9,0,"Navigation");
        menu.add(0,10,0,"Format");
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
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
                        Toast.makeText(this, "Naviagtion not prepared", Toast.LENGTH_SHORT).show();
                    } else {
                        CharSequence[] items = new CharSequence[labels.size()];
                        for (int i = 0; i < labels.size(); i++) {
                            items[i] = labels.get(i).label;
                        }
                        new AlertDialog.Builder(this)
                                .setTitle("Navigation")
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
                    .setPositiveButton("Ok",null)
                    .show();
        }
        Toast.makeText(this,"Action finished in " + (System.nanoTime() - st) / 1e6 + " ms",Toast.LENGTH_SHORT).show();

        return super.onOptionsItemSelected(item);
    }
}
