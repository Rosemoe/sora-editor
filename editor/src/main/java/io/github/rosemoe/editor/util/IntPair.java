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
package io.github.rosemoe.editor.util;

/**
 * Pack two int into a long
 * Also unpack it
 */
public class IntPair {

    public static long pack(int first, int second) {
        return (((long) first) << 32L) + second;
    }

    public static int getSecond(long packedValue) {
        return (int) (packedValue << 32L >> 32L);
    }

    public static int getFirst(long packedValue) {
        return (int) (packedValue >> 32L);
    }

}
