/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.text.string;

public final class StringLatin1 {

    private StringLatin1() {
    }

    public static boolean canEncode(char c) {
        return c <= 0x00FF;
    }

    public static char getChar(byte[] value, int index) {
        return (char) (value[index] & 0xFF);
    }

    public static void putChar(byte[] value, int index, char c) {
        value[index] = (byte) c;
    }

    public static void copyChars(byte[] src, int srcBegin, byte[] dst, int dstBegin, int len) {
        System.arraycopy(src, srcBegin, dst, dstBegin, len);
    }

    public static void getChars(byte[] value, int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        for (int i = srcBegin; i < srcEnd; i++) {
            dst[dstBegin++] = getChar(value, i);
        }
    }

    public static void appendTo(byte[] value, int length, StringBuilder sb) {
        for (int i = 0; i < length; i++) {
            sb.append(getChar(value, i));
        }
    }

    public static String newString(byte[] value, int length) {
        var chars = new char[length];
        getChars(value, 0, length, chars, 0);
        return new String(chars);
    }

    public static byte[] inflateToUTF16(byte[] value, int length, int newCapacity) {
        var utf16 = new byte[StringUTF16.bytesForChars(newCapacity)];
        for (int i = 0; i < length; i++) {
            StringUTF16.putChar(utf16, i, getChar(value, i));
        }
        return utf16;
    }
}
