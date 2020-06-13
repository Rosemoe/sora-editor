package com.rose.editor.text;
import com.rose.editor.interfaces.EditorLanguage;

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
            CharSequence chars = ((mText instanceof Content) ? (((Content)mText).toStringBuilder()) : new StringBuilder(mText));
            result = mLanguage.format(chars);
        } catch (Throwable e) {
            if(mReceiver != null) {
                mReceiver.onFormatFail(e);
            }
        }
        if(mReceiver != null) {
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
