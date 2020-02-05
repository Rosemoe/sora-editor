package com.rose.editor.utils;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardIceCream implements IClipboard {

    private ClipboardManager mClip;

    @TargetApi(15)
    public ClipboardIceCream(Context context) {
        mClip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    @TargetApi(15)
    public CharSequence getTextFromClipboard() {
        if(mClip == null) {
            return null;
        }
        if(mClip.hasPrimaryClip()) {
            ClipData data = mClip.getPrimaryClip();
            if(data != null && data.getItemCount() > 0) {
                return data.getItemAt(0).getText();
            }
        }
        return null;
    }

    @Override
    @TargetApi(15)
    public void setTextToClipboard(CharSequence text) {
        if(mClip != null) {
            mClip.setPrimaryClip(ClipData.newPlainText("Text",text));
        }
    }
}
