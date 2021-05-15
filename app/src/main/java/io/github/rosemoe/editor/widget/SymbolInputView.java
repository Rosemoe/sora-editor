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

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;

public class SymbolInputView extends LinearLayout {


    public SymbolInputView(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        setOrientation(HORIZONTAL);
    }

    public SymbolInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE);
        setOrientation(HORIZONTAL);
    }

    public SymbolInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.WHITE);
        setOrientation(HORIZONTAL);
    }

    public SymbolInputView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setBackgroundColor(Color.WHITE);
        setOrientation(HORIZONTAL);
    }

    private SymbolChannel channel;

    public void bindEditor(CodeEditor editor) {
        channel = editor.createNewSymbolChannel();
    }

    public void removeSymbols() {
        removeAllViews();
    }

    public void addSymbols(String[] display, final String[] insertText) {
        int count = Math.max(display.length, insertText.length);
        for (int i = 0; i < count; i++) {
            Button btn = new Button(getContext(), null, android.R.attr.buttonStyleSmall);
            btn.setText(display[i]);
            btn.setBackground(new ColorDrawable(0));
            addView(btn, new LinearLayout.LayoutParams(-2, -1));
            int finalI = i;
            btn.setOnClickListener((view) -> {
                channel.insertSymbol(insertText[finalI], 1);
            });
        }
    }

}
