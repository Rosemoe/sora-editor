
package io.github.rosemoe.sora.widget.schemes;

import io.github.rosemoe.sora.widget.EditorColorScheme;

public class SchemeAndroidXml extends EditorColorScheme {

    @Override
    public void applyDefault() {
        super.applyDefault();
        setColor(ANNOTATION, 0xffbbb529);
        setColor(FUNCTION_NAME, 0xFF02C4FF);
        setColor(IDENTIFIER_NAME, 0xFFE5C31A);
        setColor(IDENTIFIER_VAR, 0xFFA4CD45);
        setColor(LITERAL, 0xFF058A3F);
        setColor(OPERATOR, 0xFF00E66F);
        setColor(COMMENT, 0xFFB360F8);
        setColor(KEYWORD, 0xFFFF0059);
        setColor(WHOLE_BACKGROUND, 0xFF000000);
        setColor(TEXT_NORMAL, 0xffffffff);
        setColor(LINE_NUMBER_BACKGROUND, 0xFF000000);
        setColor(LINE_NUMBER, 0xFFD1171D);
        setColor(LINE_DIVIDER, 0xFFD1171D);
        setColor(SCROLL_BAR_THUMB, 0xffa6a6a6);
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xff565656);
        setColor(SELECTED_TEXT_BACKGROUND, 0xff3676b8);
        setColor(MATCHED_TEXT_BACKGROUND, 0xff32593d);
        setColor(CURRENT_LINE, 0x4ADA6264);
        setColor(SELECTION_INSERT, 0xffffffff);
        setColor(SELECTION_HANDLE, 0xffffffff);
        setColor(BLOCK_LINE, 0xD5FD2A2D);
        setColor(BLOCK_LINE_CURRENT, 0xFF00D4FF);
       /// setColor(DEX , 0xFFFF00A6 );
        setColor(NON_PRINTABLE_CHAR, 0xFF4582D7);
    }

}
