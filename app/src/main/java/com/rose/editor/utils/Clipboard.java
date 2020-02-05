package com.rose.editor.utils;

import android.content.Context;
import android.os.Build;

public class Clipboard {

    public static IClipboard getClipboard(Context context) {
        //Deprecated after this level
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return new ClipboardIceCream(context);
        }
        return new ClipboardBase(context);
    }

}
