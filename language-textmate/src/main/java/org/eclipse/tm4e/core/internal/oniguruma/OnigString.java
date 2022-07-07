/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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

package org.eclipse.tm4e.core.internal.oniguruma;

import org.jcodings.specific.UTF8Encoding;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Oniguruma string.
 *
 * @see https://github.com/atom/node-oniguruma/blob/master/src/onig-string.cc
 *
 */
public class OnigString {

    public final String string;
    public final byte[] utf8_value;

    private int[] charsPosFromBytePos;
    private boolean computedOffsets;


    public OnigString(String str) {
        this.string = str;
        this.utf8_value = str.getBytes(StandardCharsets.UTF_8);
    }

    public int convertUtf16OffsetToUtf8(int posInChars) {
        if (!computedOffsets) {
            computeOffsets();
        }
        if (charsPosFromBytePos == null) {
            // Same conditions as code below, but taking into account that the
            // bytes and chars len are the same.
            if (posInChars < 0 || this.utf8_value.length == 0 || posInChars > this.utf8_value.length) {
                throw new ArrayIndexOutOfBoundsException(posInChars);
            }
            return posInChars;
        }

        int[] charsLenInBytes = charsPosFromBytePos;
        if (posInChars < 0 || charsLenInBytes.length == 0) {
            throw new ArrayIndexOutOfBoundsException(posInChars);
        }
        if (posInChars == 0) {
            return 0;
        }

        int last = charsLenInBytes[charsLenInBytes.length - 1];
        if (last < posInChars) {
            if (last == posInChars - 1) {
                return charsLenInBytes.length;
            } else {
                throw new ArrayIndexOutOfBoundsException(posInChars);
            }
        }

        int index = Arrays.binarySearch(charsLenInBytes, posInChars);
        while (index > 0) {
            if (charsLenInBytes[index - 1] == posInChars) {
                index--;
            } else {
                break;
            }
        }
        return index;
    }

    public int convertUtf8OffsetToUtf16(int posInBytes) {
        if (!computedOffsets) {
            computeOffsets();
        }
        if (charsPosFromBytePos == null) {
            return posInBytes;
        }
        if (posInBytes < 0) {
            return posInBytes;
        }
        if (posInBytes >= charsPosFromBytePos.length) {
            //One off can happen when finding the end of a regexp (it's the right boundary).
            return charsPosFromBytePos[posInBytes - 1] + 1;
        }
        return charsPosFromBytePos[posInBytes];
    }

    private void computeOffsets() {
        if (this.utf8_value.length != this.string.length()) {
            charsPosFromBytePos = new int[this.utf8_value.length];
            int bytesLen = 0;
            int charsLen = 0;
            int length = this.utf8_value.length;
            for (int i = 0; i < length; ) {
                int codeLen = UTF8Encoding.INSTANCE.length(this.utf8_value, i, length);
                for (int i1 = 0; i1 < codeLen; i1++) {
                    charsPosFromBytePos[bytesLen + i1] = charsLen;
                }
                bytesLen += codeLen;
                i += codeLen;
                charsLen += 1;
            }
            if (bytesLen != this.utf8_value.length) {
                throw new AssertionError(bytesLen + " != " + this.utf8_value.length);
            }
        }
        computedOffsets = true;
    }
}
