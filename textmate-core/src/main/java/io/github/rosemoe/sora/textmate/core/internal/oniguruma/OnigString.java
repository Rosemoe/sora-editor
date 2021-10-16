/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/atom/node-oniguruma
 * Initial copyright Copyright (c) 2013 GitHub Inc.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - GitHub Inc.: Initial code, written in JavaScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 * - Fabio Zadrozny <fabiofz@gmail.com> - Convert uniqueId to Object (for identity compare)
 * - Fabio Zadrozny <fabiofz@gmail.com> - Utilities to convert between utf-8 and utf-16
 */

package io.github.rosemoe.sora.textmate.core.internal.oniguruma;

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
            ;
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
