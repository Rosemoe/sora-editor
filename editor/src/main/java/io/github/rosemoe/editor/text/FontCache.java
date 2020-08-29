/*
 *   Copyright 2020 Rosemoe
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.github.rosemoe.editor.text;

import android.graphics.Paint;
import java.util.Arrays;

/**
  * Cache to measure text quickly
  * This is very useful when text is long
  * Use this to make editor 20x faster than before
  *
  * @author Rose
  */
public class FontCache {
    
    private final float[] cache;
    
    public FontCache() {
        cache = new float[65536];
    }
    
    /**
      * Clear caches of font
      */
    public void clearCache() {
        Arrays.fill(cache, 0);
    }
    
    /**
      * Measure a single character
      */
    public float measureChar(char ch, Paint p) {
        float width = cache[(int) ch];
        if (width == 0) {
            width = p.measureText(new char[] {ch}, 0, 1);
            cache[(int) ch] = width;
        }
        return width;
    }
    
    /*
     * Measure text
     */
    public float measureText(char[] chars, int start, int end, Paint p) {
        float width = 0f;
        for (int i = start;i < end;i++) {
            char ch = chars[i];
            if (isEmoji(ch) && i + 1 < end) {
                width += p.measureText(new char[] {ch, chars[++i]}, 0, 2);
            } else {
                width += measureChar(ch, p);
            }
        }
        return width;
    }
    
    /**
      * Measure text
      */
    public float measureText(CharSequence str, int start, int end, Paint p) {
        float width = 0f;
        for (int i = start;i < end;i++) {
            char ch = str.charAt(i);
            if (isEmoji(ch) && i + 1 < end) {
                width += p.measureText(new char[]{ch, str.charAt(++i)}, 0, 2);
            } else {
                width += measureChar(ch, p);
            }
        }
        return width;
    }
    
    private static boolean isEmoji(char ch) {
        return ch == 0xd83c || ch == 0xd83d;
    }
    
}
