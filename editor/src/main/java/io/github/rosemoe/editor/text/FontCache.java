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

import android.util.SparseArray;
import android.graphics.Paint;

/**
  * Cache to measure text quickly
  * This is very useful when text is long
  * 
  *
  * @author Rose
  */
public class FontCache {
    
    private SparseArray<Float> cache;
    
    public FontCache() {
        cache = new SparseArray<>();
    }
    
    /**
      * Clear caches
      */
    public void clearCache() {
        cache.clear();
    }
    
    public float measureChar(char ch, Paint p) {
        Float width = cache.get(ch);
        if (width == null) {
            width = p.measureText(new char[]{ch}, 0, 1);
            cache.put(ch, width);
        }
        return width;
    }
    
    public float measureText(char[] chars, int start, int end, Paint p) {
        float width = 0f;
        for (int i = start;i < end;i++) {
            char ch = chars[i];
            if (isEmoji(ch)) {
                if (i + 1 < end) {
                    width += p.measureText(new char[]{ch, chars[++i]}, 0, 2);
                } else {
                    width += measureChar(ch, p);
                }
            } else {
                width += measureChar(ch, p);
            }
        }
        return width;
    }
    
    public float measureText(String str, int start, int end, Paint p) {
        float width = 0f;
        for (int i = start;i < end;i++) {
            char ch = str.charAt(i);
            if (isEmoji(ch)) {
                if (i + 1 < end) {
                    width += p.measureText(new char[]{ch, str.charAt(++i)}, 0, 2);
                } else {
                    width += measureChar(ch, p);
                }
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
