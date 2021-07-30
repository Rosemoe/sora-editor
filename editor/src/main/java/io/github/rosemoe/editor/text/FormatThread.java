/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.text;

import io.github.rosemoe.editor.interfaces.EditorLanguage;

public class FormatThread extends Thread {

    private CharSequence mText;

    private EditorLanguage mLanguage;

    private FormatResultReceiver mReceiver;

    public FormatThread(CharSequence text, EditorLanguage language, FormatResultReceiver receiver) {
        mText = text;
        mLanguage = language;
        mReceiver = receiver;
    }

    @Override
    public void run() {
        CharSequence result = null;
        try {
            CharSequence chars = ((mText instanceof Content) ? (((Content) mText).toStringBuilder()) : new StringBuilder(mText));
            result = mLanguage.format(chars);
        } catch (Throwable e) {
            if (mReceiver != null) {
                mReceiver.onFormatFail(e);
            }
        }
        if (mReceiver != null) {
            mReceiver.onFormatSucceed(mText, result);
        }
        mReceiver = null;
        mLanguage = null;
        mText = null;
    }

    public interface FormatResultReceiver {

        void onFormatSucceed(CharSequence originalText, CharSequence newText);

        void onFormatFail(Throwable throwable);

    }

}
