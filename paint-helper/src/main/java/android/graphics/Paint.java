/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2021  Rosemoe
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
package android.graphics;

/**
 * Stub class
 */
public class Paint {

    /*
     * Android API 21 - 28
     *
     * getTextRunAdvances is made public since API 29
     * getTextRunCursor changed signature and is made public since API 29
     */

    /**
     * Flag for getTextRunAdvances indicating left-to-right run direction.
     */
    public static final int DIRECTION_LTR = 0;

    /**
     * Flag for getTextRunAdvances indicating right-to-left run direction.
     */
    public static final int DIRECTION_RTL = 1;

    /**
     * Option for getTextRunCursor to compute the valid cursor after
     * offset or the limit of the context, whichever is less.
     */
    public static final int CURSOR_AFTER = 0;

    /**
     * Option for getTextRunCursor to compute the valid cursor at or after
     * the offset or the limit of the context, whichever is less.
     */
    public static final int CURSOR_AT_OR_AFTER = 1;

    /**
     * Option for getTextRunCursor to compute the valid cursor before
     * offset or the start of the context, whichever is greater.
     */
    public static final int CURSOR_BEFORE = 2;

    /**
     * Option for getTextRunCursor to compute the valid cursor at or before
     * offset or the start of the context, whichever is greater.
     */
    public static final int CURSOR_AT_OR_BEFORE = 3;

    /**
     * Option for getTextRunCursor to return offset if the cursor at offset
     * is valid, or -1 if it isn't.
     */
    public static final int CURSOR_AT = 4;

    public float getTextRunAdvances(char[] chars, int index, int count,
                                    int contextIndex, int contextCount, boolean isRtl, float[] advances,
                                    int advancesIndex) {
        throw new UnsupportedOperationException("stub");
    }

    public float getTextRunAdvances(CharSequence text, int start, int end,
                                    int contextStart, int contextEnd, boolean isRtl, float[] advances,
                                    int advancesIndex) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Returns the total advance width for the characters in the run
     * between start and end, and if advances is not null, the advance
     * assigned to each of these characters (java chars).
     *
     * <p>The trailing surrogate in a valid surrogate pair is assigned
     * an advance of 0.  Thus the number of returned advances is
     * always equal to count, not to the number of unicode codepoints
     * represented by the run.
     *
     * <p>In the case of conjuncts or combining marks, the total
     * advance is assigned to the first logical character, and the
     * following characters are assigned an advance of 0.
     *
     * <p>This generates the sum of the advances of glyphs for
     * characters in a reordered cluster as the width of the first
     * logical character in the cluster, and 0 for the widths of all
     * other characters in the cluster.  In effect, such clusters are
     * treated like conjuncts.
     *
     * <p>The shaping bounds limit the amount of context available
     * outside start and end that can be used for shaping analysis.
     * These bounds typically reflect changes in bidi level or font
     * metrics across which shaping does not occur.
     *
     * @param text the text to measure. Cannot be null.
     * @param start the index of the first character to measure
     * @param end the index past the last character to measure
     * @param contextStart the index of the first character to use for shaping context,
     * must be <= start
     * @param contextEnd the index past the last character to use for shaping context,
     * must be >= end
     * @param isRtl whether the run is in RTL direction
     * @param advances array to receive the advances, must have room for all advances,
     * can be null if only total advance is needed
     * @param advancesIndex the position in advances at which to put the
     * advance corresponding to the character at start
     * @return the total advance
     */
    public float getTextRunAdvances(String text, int start, int end, int contextStart,
                                    int contextEnd, boolean isRtl, float[] advances, int advancesIndex) {
        throw new UnsupportedOperationException("stub");
    }

    public int getTextRunCursor(char[] text, int contextStart, int contextLength,
                                int dir, int offset, int cursorOpt) {
        throw new UnsupportedOperationException("stub");
    }

    public int getTextRunCursor(CharSequence text, int contextStart,
                                int contextEnd, int dir, int offset, int cursorOpt) {
        throw new UnsupportedOperationException("stub");
    }

    public int getTextRunCursor(String text, int contextStart, int contextEnd,
                                int dir, int offset, int cursorOpt) {
        throw new UnsupportedOperationException("stub");
    }

    /*
     * Sine Android API 29
     */

    public int getTextRunCursor(char[] text, int contextStart, int contextLength,
                                boolean isRtl, int offset, int cursorOpt) {
        throw new UnsupportedOperationException("stub");
    }

    public int getTextRunCursor(CharSequence text, int contextStart,
                                int contextEnd, boolean isRtl, int offset, int cursorOpt) {
        throw new UnsupportedOperationException("stub");
    }

    public int getTextRunCursor(String text, int contextStart, int contextEnd,
                                boolean isRtl, int offset, int cursorOpt) {
        throw new UnsupportedOperationException("stub");
    }




}
