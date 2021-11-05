package io.github.rosemoe.sora.widget.schemes;

import io.github.rosemoe.sora.widget.EditorColorScheme;

public class SchemePurple extends EditorColorScheme {
   public void applyDefault() {
      super.applyDefault();
      setColor(ANNOTATION, 0xFFFF6EAF);
        setColor(FUNCTION_NAME, 0xFF2FA5FC);
        setColor(IDENTIFIER_NAME, 0xFF2FA5FC);
        setColor(IDENTIFIER_VAR, 0xFF0677BE);
        setColor(LITERAL, 0xFF0AFC5B);
        setColor(OPERATOR, 0xff08fff3);
        setColor(COMMENT, 0xff05ff1a); // yellow
        setColor(KEYWORD, 0xFFE0590B);
        setColor(WHOLE_BACKGROUND, 0xFF000027);
        setColor(TEXT_NORMAL, 0xFF68F9FF);
        setColor(LINE_NUMBER_BACKGROUND, 0xFF000027);
        setColor(LINE_NUMBER, 0xFF00C3F1);
        setColor(LINE_DIVIDER, 0xFFFF5F00);
        setColor(SCROLL_BAR_THUMB, 0xFF00002C);
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xFF00002C);
        setColor(SELECTED_TEXT_BACKGROUND, 0xFF005E78);
		// search result highlight color
        setColor(MATCHED_TEXT_BACKGROUND, 0xFFFFC200);
        setColor(CURRENT_LINE, 0xFF0075C9);
        setColor(SELECTION_INSERT, 0xFF6AFFFF);
        setColor(SELECTION_HANDLE, 0xFF6AFFFF);
        setColor(BLOCK_LINE, 0xFF836BEB);
        setColor(BLOCK_LINE_CURRENT, 0xff5accc6);
        setColor(NON_PRINTABLE_CHAR, 0xFF000028);
		
		
   }
}
