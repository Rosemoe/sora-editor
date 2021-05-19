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
package io.github.rosemoe.editor.widget;

import android.annotation.SuppressLint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;

import io.github.rosemoe.editor.R;

public class TextActionPopupWindow extends TextComposeBasePopup implements View.OnClickListener, CodeEditor.EditorTextActionPresenter{
    private final CodeEditor mEditor;
    private final MaterialButton mPasteBtn;
    private final MaterialButton mSelectAll;
    private final LinearLayout mContainer;

    private float mDpUnit = 0f;
    private int popupHeightInDp = 60;
    /**
     * Create a panel for editor
     *
     * @param editor Target editor
     */
    public TextActionPopupWindow(CodeEditor editor) {
        super(editor);
        mEditor = editor;
        mDpUnit = mEditor.getDpUnit();
        popHeightPx = (int) (popupHeightInDp * mDpUnit);
        // Since popup window does provide decor view, we have to pass null to this method
        @SuppressLint("InflateParams")
        View root = LayoutInflater.from(editor.getContext()).inflate(R.layout.text_compose_popup_window, null);
        mSelectAll = root.findViewById(R.id.tcpw_material_button_select_all);
        mContainer = root.findViewById(R.id.text_compose_panel);
        MaterialButton cut = root.findViewById(R.id.tcpw_material_button_cut);
        MaterialButton copy = root.findViewById(R.id.tcpw_material_button_copy);
        mPasteBtn = root.findViewById(R.id.tcpw_material_button_paste);
        mSelectAll.setOnClickListener(this);
        cut.setOnClickListener(this);
        copy.setOnClickListener(this);
        mPasteBtn.setOnClickListener(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(mDpUnit * 8);
        gd.setStroke(1, 0xff808080);
        gd.setColor(0xffffffff);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            root.setBackground(gd);
        } else {
            root.setBackgroundDrawable(gd);
        }

        setContentView(root);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onSelectedTextClicked(MotionEvent event) {

    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onBeginTextSelect() {

    }

    @Override
    public void onExit() {

    }

    @Override
    public boolean shouldShowCursor() {
        return false;
    }
}
