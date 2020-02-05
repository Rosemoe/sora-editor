package com.rose.editor.android;

import android.view.View;
import android.widget.Button;
import android.view.LayoutInflater;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.content.res.Resources;

/**
 * This will show when selecting text
 * @author Rose
 */
public class TextComposePanel extends BasePanel implements View.OnClickListener
{
    private RoseEditor mEditor;
    private Button selectAll,cut,copy,paste;

    /**
     * Create a panel for the given editor
     * @param editor Target editor
     */
    public TextComposePanel(RoseEditor editor) {
        super(editor);
        mEditor = editor;
        View root = LayoutInflater.from(editor.getContext()).inflate(R.layout.text_compose_panel,null);
        selectAll = (Button) root.findViewById(R.id.panel_btn_select_all);
        cut =  (Button)root.findViewById(R.id.panel_btn_cut);
        copy = (Button) root.findViewById(R.id.panel_btn_copy);
        paste = (Button) root.findViewById(R.id.panel_btn_paste);
        selectAll.setOnClickListener(this);
        cut.setOnClickListener(this);
        copy.setOnClickListener(this);
        paste.setOnClickListener(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(5);
        gd.setColor(0xffffffff);
        root.setBackgroundDrawable(gd);
        setContentView(root);
    }

    /**
     * Update the state of paste button
     */
    private void updateBtnState() {
        if(mEditor.hasClip()) {
            paste.setEnabled(true);
        }else{
            paste.setEnabled(false);
        }
    }

    @Override
    public void show(){
        updateBtnState();
        if(Build.VERSION.SDK_INT >= 21) {
            setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,Resources.getSystem().getDisplayMetrics()));
        }
        super.show();
    }

    @Override
    public void onClick(View p1) {
        switch(p1.getId()) {
            case R.id.panel_btn_select_all:
                mEditor.selectAll();
                break;
            case R.id.panel_btn_cut:
                mEditor.copyText();
                if(mEditor.getCursor().isSelected()) {
                    mEditor.getCursor().onDeleteKeyPressed();
                }
                break;
            case R.id.panel_btn_paste:
                mEditor.pasteText();
                mEditor.setSelection(mEditor.getCursor().getRightLine(),mEditor.getCursor().getRightColumn());
                break;
            case R.id.panel_btn_copy:
                mEditor.copyText();
                mEditor.setSelection(mEditor.getCursor().getRightLine(),mEditor.getCursor().getRightColumn());
                break;
        }
        hide();
    }

}

