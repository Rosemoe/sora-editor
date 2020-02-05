package com.rose.editor.utils;

import android.content.Context;
import android.text.ClipboardManager;

public class ClipboardBase implements IClipboard {

    private ClipboardManager mClip;

    public ClipboardBase(Context context) {
        mClip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public CharSequence getTextFromClipboard() {
        return (mClip != null ? mClip.getText() : null);
    }

    @Override
    public void setTextToClipboard(CharSequence text) {
        if(mClip != null) {
            mClip.setText(text);
        }
    }
}
