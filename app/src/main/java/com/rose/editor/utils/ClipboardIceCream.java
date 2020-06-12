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
package com.rose.editor.utils;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardIceCream implements IClipboard {

    private final ClipboardManager mClip;

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
