package com.rose.editor.langs.internal;

import java.util.Arrays;

/**
 * @author Rose
 * Get whether Identifier part/start quickly
 *
 * 暴力出奇迹,打表得省一
 * 暴力出奇迹,打表得省一
 * 暴力出奇迹,打表得省一
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

    private static boolean get(int[] values,int bitIndex) {
        return ((values[bitIndex / 32] & (1 << (bitIndex % 32))) != 0);
    }

    private static void set(int[] values,int bitIndex) {
        values[bitIndex / 32] |= (1 << (bitIndex % 32));
    }

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

    public static boolean isJavaIdentifierPart(int key) {
        return get(state_part,key);
    }

    public static boolean isJavaIdentifierStart(int key) {
        return get(state_start,key);
    }

}

