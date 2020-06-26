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
package com.rose.editor.android;

import android.util.SparseIntArray;

/**
 * This class manages the color props of editor
 * @author Rose
 */
public final class ColorScheme {
    //-----------------Highlight colors-----------

    public static final int ANNOTATION = 28;
    public static final int FUNCTION_NAME = 27;
    public static final int IDENTIFIER_NAME = 26;
    public static final int IDENTIFIER_VAR = 25;
    public static final int LITERAL = 24;
    public static final int OPERATOR = 23;
    public static final int COMMENT = 22;
    public static final int KEYWORD = 21;

    //-------------View colors---------------------

    public static final int MATCHED_TEXT_BACKGROUND = 29;
    public static final int AUTO_COMP_PANEL_CORNER = 20;
    public static final int AUTO_COMP_PANEL_BG = 19;
    public static final int LINE_BLOCK_LABEL = 18;
    public static final int LINE_NUMBER_PANEL_TEXT = 17;
    public static final int LINE_NUMBER_PANEL = 16;
    public static final int BLOCK_LINE_CURRENT = 15;
    public static final int BLOCK_LINE = 14;
    public static final int SCROLL_BAR_TRACK = 13;
    public static final int SCROLL_BAR_THUMB_PRESSED = 12;
    public static final int SCROLL_BAR_THUMB = 11;
    public static final int UNDERLINE = 10;
    public static final int CURRENT_LINE = 9;
    public static final int SELECTION_HANDLE = 8;
    public static final int SELECTION_INSERT = 7;
    public static final int SELECTED_TEXT_BACKGROUND = 6;
    public static final int TEXT_NORMAL = 5;
    public static final int WHOLE_BACKGROUND = 4;
    public static final int LINE_NUMBER_BACKGROUND = 3;
    public static final int LINE_NUMBER = 2;
    public static final int LINE_DIVIDER = 1;

    /**
     * Min pre-defined color id
     */
    private static final int START_COLOR_INDEX = 1;

    /**
     * Max pre-defined color id
     */
    private static final int END_COLOR_INDEX = 29;

    /**
     * Host editor object
     */
    private CodeEditor mEditor;

    /**
     * Real color saver
     */
    private SparseIntArray mColors;

    /**
     * Create a new ColorScheme for the given editor
     * @param editor Host editor
     */
    ColorScheme(CodeEditor editor) {
        mEditor = editor;
        if(editor == null){
            throw new IllegalArgumentException();
        }
        mColors = new SparseIntArray();
        applyDefault();
    }

    /**
     * Apply default colors
     */
    public void applyDefault() {
        for(int i = START_COLOR_INDEX;i <= END_COLOR_INDEX;i++){
            applyDefault(i);
        }
    }

    /**
     * Apply default color for the given type
     * @param type The type
     */
    public void applyDefault(int type) {
        int color;
        switch(type) {
            case LINE_DIVIDER:
                color = 0xff3f51bf;
                break;
            case LINE_NUMBER:
                color = 0xff444444;
                break;
            case LINE_NUMBER_BACKGROUND:
                color = 0xffeeeeee;
                break;
            case WHOLE_BACKGROUND:
                color = 0;
                break;
            case TEXT_NORMAL:
                color = 0xff222222;
                break;
            case SELECTION_INSERT:
            case UNDERLINE:
                color = 0xff000000;
                break;
            case SELECTION_HANDLE:
            case ANNOTATION:
                color = 0xffec407a;
                break;
            case CURRENT_LINE:
                color = 0x33ec407a;
                break;
            case SELECTED_TEXT_BACKGROUND:
                color = 0x303f51b5;
                break;
            case KEYWORD:
                color = 0xeeee0000;
                break;
            case COMMENT:
                color = 0xffaaaaaa;
                break;
            case OPERATOR:
                color = 0xff219167;
                break;
            case LITERAL:
                color = 0xdd0096ff;
                break;
            case SCROLL_BAR_THUMB:
                color = 0xff2196f3;
                break;
            case SCROLL_BAR_THUMB_PRESSED:
                color = 0xaaec407a;
                break;
            case BLOCK_LINE:
                color = 0xffdddddd;
                break;
            case SCROLL_BAR_TRACK:
                color = 0xeeeeeeee;
                break;
            case LINE_NUMBER_PANEL:
                color = 0xdd000000;
                break;
            case LINE_NUMBER_PANEL_TEXT:
            case AUTO_COMP_PANEL_BG:
            case AUTO_COMP_PANEL_CORNER:
                color = 0xffffffff;
                break;
            case BLOCK_LINE_CURRENT:
                color = 0xff999999;
                break;
            case LINE_BLOCK_LABEL:
                color = 0x88dddddd;
                break;
            case IDENTIFIER_VAR:
                color = 0xffe91e63;
                break;
            case IDENTIFIER_NAME:
                color = 0xffff9800;
                break;
            case FUNCTION_NAME:
                color = 0xffaaaaff;
                break;
            case MATCHED_TEXT_BACKGROUND:
                color = 0xaaffff00;
                break;
            default:
                throw new IllegalArgumentException("Unexpected type:" + type);
        }
        putColor(type, color);
    }

    /**
     * Apply a new color for the given type
     * @param type The type
     * @param color New color
     */
    public void putColor(int type,int color) {
        //Do not change if the old value is the same as new value
        //  due to avoid unnecessary invalidate() calls
        int old = getColor(type);
        if(old == color){
            return;
        }

        mColors.put(type,color);

        //Notify the editor
        mEditor.onColorUpdated(type);
    }

    /**
     * Get color by type
     * @param type The type
     * @return The color for type
     */
    public int getColor(int type) {
        return mColors.get(type);
    }

}
