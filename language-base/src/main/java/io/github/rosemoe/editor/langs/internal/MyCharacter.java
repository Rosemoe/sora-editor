/*
 *   Copyright 2020-2021 Rosemoe
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
package io.github.rosemoe.editor.langs.internal;

import java.util.Arrays;

/**
 * @author Rose
 * Get whether Identifier part/start quickly
 */
public class MyCharacter {

    static {
        initMapInternal();
    }

    /**
     * Compressed bit set for isJavaIdentifierStart()
     */
    private static int[] bitsIsStart;

    /**
     * Compressed bit set for isJavaIdentifierPart()
     */
    private static int[] bitsIsPart;

    /**
     * Get bit in compressed bit set
     *
     * @param values   Compressed bit set
     * @param bitIndex Target index
     * @return Boolean value at the index
     */
    private static boolean get(int[] values, int bitIndex) {
        return ((values[bitIndex / 32] & (1 << (bitIndex % 32))) != 0);
    }

    /**
     * Make the given position's bit true
     *
     * @param values   Compressed bit set
     * @param bitIndex Index of bit
     */
    private static void set(int[] values, int bitIndex) {
        values[bitIndex / 32] |= (1 << (bitIndex % 32));
    }

    /**
     * Init maps
     *
     * @deprecated The class will be initialized automatically
     */
    @Deprecated
    public static void initMap() {
        // Empty
    }

    /**
     * Init maps
     */
    private static void initMapInternal() {
        if (bitsIsStart != null) {
            return;
        }
        bitsIsPart = new int[2048];
        bitsIsStart = new int[2048];
        Arrays.fill(bitsIsPart, 0);
        Arrays.fill(bitsIsStart, 0);
        for (int i = 0; i <= 65535; i++) {
            if (Character.isJavaIdentifierPart((char) i)) {
                set(bitsIsPart, i);
            }
            if (Character.isJavaIdentifierStart((char) i)) {
                set(bitsIsStart, i);
            }
        }
    }

    /**
     * @param key Character
     * @return Whether a identifier part
     * @see Character#isJavaIdentifierPart(char)
     */
    public static boolean isJavaIdentifierPart(int key) {
        return get(bitsIsPart, key);
    }

    /**
     * @param key Character
     * @return Whether a identifier start
     * @see Character#isJavaIdentifierStart(char)
     */
    public static boolean isJavaIdentifierStart(int key) {
        return get(bitsIsStart, key);
    }

}

