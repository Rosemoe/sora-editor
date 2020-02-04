package com.rose.editor.langs.internal;

/**
 * @author Rose
 * Get whether Identifier part/start quickly
 *
 * 暴力出奇迹,打表得省一
 * 暴力出奇迹,打表得省一
 * 暴力出奇迹,打表得省一
 */
public class MyCharacter {

    public static boolean[] state1,state2;

    public static boolean[] state3,state4;

    public static void initMap() {
        if(state1 != null) {
            return;
        }

        state1 = new boolean[35000];
        state2 = new boolean[65536 - 35000];
        for(int i = 0;i < 35000;i++) {
            state1[i] = Character.isJavaIdentifierPart((char)i);
        }
        for(int i = 35000;i <= 65535;i++) {
            state2[i - 35000] = Character.isJavaIdentifierPart((char)i);
        }

        state3 = new boolean[35000];
        state4 = new boolean[65536 - 35000];
        for(int i = 0;i < 35000;i++) {
            state3[i] = Character.isJavaIdentifierStart((char)i);
        }
        for(int i = 35000;i <= 65535;i++) {
            state4[i - 35000] = Character.isJavaIdentifierStart((char)i);
        }
    }

    public static boolean isJavaIdentifierPart(int key) {
        if(key < 35000) {
            return state1[key];
        }else {
            return state2[key - 35000];
        }
    }

    public static boolean isJavaIdentifierStart(int key) {
        if(key < 35000) {
            return state3[key];
        }else {
            return state4[key - 35000];
        }
    }

}

