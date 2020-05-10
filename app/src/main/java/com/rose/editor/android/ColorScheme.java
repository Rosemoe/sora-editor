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
    //Add you colors above(starts from 30) and change END_COLOR_INDEX below
    //-------------------------------------------
    public static final int AUTO_COMP_PANEL_CORNER = 24;
    public static final int AUTO_COMP_PANEL_BG = 23;
    public static final int LINE_BLOCK_LABEL = 22;
    public static final int BLOCK_LINE_CURRENT = 21;
    public static final int LINE_NUMBER_PANEL_TEXT = 20;
    public static final int LINE_NUMBER_PANEL = 19;
    public static final int SCROLL_BAR_TRACK = 18;
    public static final int BLOCK_LINE = 17;
    public static final int SCROLL_BAR_THUMB_DOWN = 16;
    public static final int SCROLL_BAR_THUMB = 15;
    //----------------------------------
    /*
     * These are highlight colors
     */
    public static final int ANNOTATION = 28;
    public static final int FUNCTION_NAME = 29;
    /**
     * This is a special color.
     * This color tells the editor it is a hex color expression.
     * And editor will try to recognize the hex color expression and draw an
     * underline for this expression.
     * Language used it now is only S5droid.
     * To adapt more languages please modify RoseEditor.java
     */
    public static final int HEX_COLOR = 27;
    public static final int IDENTIFIER_NAME = 26;
    public static final int IDENTIFIER_VAR = 25;
    public static final int LITERAL = 14;
    public static final int OPERATOR = 13;
    public static final int COMMENT = 12;
    public static final int KEYWORD = 11;
    //----------------------------------
    public static final int SELECTED_TEXT_BACKGROUND = 10;
    public static final int UNDERLINE = 9;
    public static final int CURRENT_LINE = 8;
    public static final int SELECTION_HANDLE = 7;
    public static final int SELECTION_INSERT = 6;
    public static final int TEXT_NORMAL = 5;
    public static final int WHOLE_BACKGROUND = 4;
    public static final int LINE_NUMBER_BACKGROUND = 3;
    public static final int LINE_NUMBER = 2;
    public static final int LINE_DIVIDER = 1;

    /**
     * Min color id
     * NOTE:color id must start with 1
     */
    private static final int START_COLOR_INDEX = 1;
    /**
     * Max color id
     * NOTE:color id must be a series such as 1,2,3,4,5
     */
    private static final int END_COLOR_INDEX = 29;

    /**
     * The default colors for each color id
     */
    private static final int[] DEFAULT_COLORS = new int[]{
            0,
            0xff3f51bf,
            0xff444444,
            0xffeeeeee,
            0,
            0xff222222,
            0xff000000,
            0xffec407a,
            0x33ec407a,
            0xff000000,
            0x883f51b5,
            0xffb71c1c,
            0xff9e9e9e,
            0xdd0096ff,
            0xff2196f3,
            0xaaec407a,
            0xffec407a,
            0xfff44336,
            0xeeeeeeee,
            0xdd000000,
            0xffffffff,
            0xff009688,
            0xccdddddd,
            0xffffffff,
            0xffec407a,
            0xffe91e63,
            0xffff9800,
            0xff4caf50,
            0xffec407a,
            0xffaaaaff
    };

    /**
     * Host editor object
     */
    private RoseEditor mEditor;

    /**
     * Real color saver
     */
    private SparseIntArray mColors;

    /**
     * Create a new ColorScheme for the given editor
     * @param editor Host editor
     */
    ColorScheme(RoseEditor editor){
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
    public void applyDefault(){
        for(int i = START_COLOR_INDEX;i <= END_COLOR_INDEX;i++){
            applyDefault(i);
        }
    }

    /**
     * Apply default color for the given type
     * @param type The type
     */
    public void applyDefault(int type){
        if(type >= START_COLOR_INDEX && type <= END_COLOR_INDEX){
            putColor(type,DEFAULT_COLORS[type]);
        }else{
            throw new IllegalArgumentException("Unexpected type:" + type);
        }
    }

    /**
     * Apply a new color for the given type
     * @param type The type
     * @param color New color
     */
    public void putColor(int type,int color){
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
    public int getColor(int type){
        return mColors.get(type);
    }

}
