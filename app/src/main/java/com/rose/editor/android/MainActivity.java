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
import com.rose.editor.langs.s5droid.S5droidLanguage;
import com.rose.editor.simpleclass.NavigationLabel;
import com.rose.editor.utils.CrashHandler;

import java.util.List;
import com.rose.editor.langs.java.JavaLanguage;

public class MainActivity extends Activity {

    private RoseEditor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler.getInstance().init(this);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission_group.STORAGE}, 9998);
        }
        S5droidAutoComplete.init(this);
        editor = (RoseEditor) findViewById(R.id.editor);
        editor.setEditorLanguage(new JavaLanguage());
        editor.setText("public static class");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        SubMenu sub = menu.addSubMenu(0,9999,0,"光标");
        //sub.add(0,10,0,"Go to line");
        sub.add(0,2,0,"跳转到最后");
        sub.add(0,3,0,"上移");
        sub.add(0,4,0,"下移");
        sub.add(0,5,0,"行头");
        sub.add(0,6,0,"行尾");
        sub.add(0,7,0,"左移");
        sub.add(0,8,0,"右移");
        sub = menu.addSubMenu(0,99999,0,"文本操作");
        sub.add(0,0,0,"撤销");
        sub.add(0,1,0,"重做");
        menu.add(0,9,0,"代码导航");
        menu.add(0,10,0,"格式化");
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
                        Toast.makeText(this, "导航未准备完成", Toast.LENGTH_SHORT).show();
                    } else {
                        CharSequence[] items = new CharSequence[labels.size()];
                        for (int i = 0; i < labels.size(); i++) {
                            items[i] = labels.get(i).label;
                        }
                        new AlertDialog.Builder(this)
                                .setTitle("代码导航")
                                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface p1, int p2) {
                                        editor.jumpToLine(labels.get(p2).line);
                                        p1.dismiss();
                                    }

                                })
                                .setPositiveButton("关闭", null)
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
                    .setTitle("错误")
                    .setMessage(sb)
                    .setPositiveButton("好",null)
                    .show();
        }
        Toast.makeText(this,"操作在" + (System.nanoTime() - st) / 1e6 + " ms 内完成.",Toast.LENGTH_SHORT).show();

        return super.onOptionsItemSelected(item);
    }
}
