/*
 *   Copyright 2020 Rose2073
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
package com.rose.editor.langs.internal;

import java.util.Arrays;

/**
 * @author Rose
 * Get whether Identifier part/start quickly
 */
public class MyCharacter {

    /**
     * Compressed bit set for isJavaIdentifierStart()
     */
    private static int[] state_start;

    /**
     * Compressed bit set for isJavaIdentifierPart()
     */
    private static int[] state_part;

    /**
     * Get bit in compressed bit set
     * @param values Compressed bit set
     * @param bitIndex Target index
     * @return Boolean value at the index
     */
    private static boolean get(int[] values,int bitIndex) {
        return ((values[bitIndex / 32] & (1 << (bitIndex % 32))) != 0);
    }

    /**
     * Make the given position's bit true
     * @param values Compressed bit set
     * @param bitIndex Index of bit
     */
    private static void set(int[] values,int bitIndex) {
        values[bitIndex / 32] |= (1 << (bitIndex % 32));
    }

    /**
     * Init maps
     */
    public static void initMap() {
        if(state_start != null) {
            return;
        }
        state_part = new int[2048];
        state_start = new int[2048];
        Arrays.fill(state_part,0);
        Arrays.fill(state_start,0);
        for(int i = 0;i <= 65535;i++) {
            if(Character.isJavaIdentifierPart((char) i)) {
                set(state_part,i);
            }
            if(Character.isJavaIdentifierStart((char) i)) {
                set(state_start,i);
            }
        }
    }

    /**
     * @see Character#isJavaIdentifierPart(char)
     * @param key Character
     * @return Whether a identifier part
     */
    public static boolean isJavaIdentifierPart(int key) {
        return get(state_part,key);
    }

    /**
     * @see Character#isJavaIdentifierStart(char)
     * @param key Character
     * @return Whether a identifier start
     */
    public static boolean isJavaIdentifierStart(int key) {
        return get(state_start,key);
    }

}

