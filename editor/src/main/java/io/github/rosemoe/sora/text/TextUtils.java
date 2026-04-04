/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.text;

import android.icu.lang.UCharacter;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.rosemoe.sora.util.IntPair;

/**
 * Utility class for texts
 */
public class TextUtils {

    private static final int LINE_FEED = 0x0A;
    private static final int CARRIAGE_RETURN = 0x0D;

    /**
     * Counts the number of whitespaces at the start of the given {@link CharSequence}.
     *
     * @param text     The text to count the spaces in.
     * @return A long packed with the number of spaces and tabs at the start of the line.
     * Use {@link IntPair#getFirst(long)} to get the number of spaces and {@link IntPair#getSecond(long)}
     * for the number of tabs.
     */
    public static long countLeadingSpacesAndTabs(@NonNull CharSequence text) {
        Objects.requireNonNull(text);

        int p = 0, spaces = 0, tabs = 0;
        char c;
        while (p < text.length() && isWhitespace((c = text.charAt(p)))) {
            if (c == '\t') {
                tabs += 1;
            } else {
                spaces += 1;
            }
            ++p;
        }

        return IntPair.pack(spaces, tabs);
    }

    /**
     * Compute leading space count
     *
     * @param tabWidth Tab is considered in {@code tabWidth} spaces
     */
    public static int countLeadingSpaceCount(@NonNull CharSequence text, int tabWidth) {
        final var result = countLeadingSpacesAndTabs(text);
        return IntPair.getFirst(result) + (tabWidth * IntPair.getSecond(result));
    }

    /**
     * Create indent space
     *
     * @return Generated space string
     */
    public static String createIndent(int indentSize, int tabWidth, boolean useTab) {
        indentSize = Math.max(0, indentSize);
        int tab = 0;
        int space;
        if (useTab) {
            tab = indentSize / tabWidth;
            space = indentSize % tabWidth;
        } else {
            space = indentSize;
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < tab; i++) {
            s.append('\t');
        }
        for (int i = 0; i < space; i++) {
            s.append(' ');
        }
        return s.toString();
    }

    public static int indexOf(@NonNull CharSequence text, @NonNull CharSequence pattern, boolean ignoreCase, int fromIndex) {
        var max = text.length() - pattern.length();
        var len = pattern.length();
        label:
        for (int i = fromIndex; i <= max; i++) {
            // Compare
            for (int j = 0; j < len; j++) {
                char s = text.charAt(i + j);
                char p = pattern.charAt(j);
                if (!(s == p || (ignoreCase && Character.toLowerCase(s) == Character.toLowerCase(p)))) {
                    continue label;
                }
            }
            return i;
        }
        return -1;
    }

    public static int lastIndexOf(@NonNull CharSequence text, @NonNull CharSequence pattern, boolean ignoreCase, int fromIndex) {
        var len = pattern.length();
        fromIndex = Math.min(fromIndex, text.length() - len);
        label:
        for (int i = fromIndex; i >= 0; i--) {
            // Compare
            for (int j = 0; j < len; j++) {
                char s = text.charAt(i + j);
                char p = pattern.charAt(j);
                if (!(s == p || (ignoreCase && Character.toLowerCase(s) == Character.toLowerCase(p)))) {
                    continue label;
                }
            }
            return i;
        }
        return -1;
    }

    public static boolean startsWith(@NonNull CharSequence text, @NonNull CharSequence pattern, boolean ignoreCase) {
        if (text.length() < pattern.length()) {
            return false;
        }
        var len = pattern.length();
        for (int i = 0; i < len; i++) {
            char s = text.charAt(i);
            char p = pattern.charAt(i);
            if (!((s == p) || (ignoreCase && Character.toLowerCase(s) == Character.toLowerCase(p)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWhitespace(char ch) {
        return ch == '\t' || ch == ' ';
    }

    public static String padStart(String src, char padChar, int length) {
        if (src.length() >= length) {
            return src;
        }
        var sb = new StringBuilder(length);
        for (int i = 0; i < length - src.length(); i++) {
            sb.append(padChar);
        }
        sb.append(src);
        return sb.toString();
    }

    /**
     * Find where leading spaces end and trailing spaces start
     *
     * @param line The line to search
     */
    public static long findLeadingAndTrailingWhitespacePos(CharSequence line) {
        return findLeadingAndTrailingWhitespacePos(line, 0, line.length());
    }

    /**
     * Find where leading spaces end and trailing spaces start
     *
     * @param line  The line to search
     * @param start Range start (inclusive)
     * @param end   Range end (exclusive)
     */
    public static long findLeadingAndTrailingWhitespacePos(CharSequence line, int start, int end) {
        int leading = start;
        int trailing = end;
        while (leading < end && isWhitespace(line.charAt(leading))) {
            leading++;
        }
        // Skip for space-filled line
        if (leading != end) {
            while (trailing > 0 && isWhitespace(line.charAt(trailing - 1))) {
                trailing--;
            }
        }
        return IntPair.pack(leading, trailing);
    }

    public static CharSequence trimToSize(@Nullable CharSequence text, @IntRange(from = 1) int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be bigger than 0");
        if (text == null || text.length() <= size) return text;
        if (Character.isHighSurrogate(text.charAt(size - 1))
                && Character.isLowSurrogate(text.charAt(size))) {
            size = size - 1;
        }
        return text.subSequence(0, size);
    }


    // From AOSP, BaseKeyListener.java

    /**
     * Returns the start offset to be deleted by a backspace key from the given offset.
     */
    public static int getOffsetForBackspaceKey(@NonNull CharSequence text, int offset) {
        if (offset <= 1) {
            return 0;
        }

        // Initial state
        final int STATE_START = 0;

        // The offset is immediately before line feed.
        final int STATE_LF = 1;

        // The offset is immediately before a KEYCAP.
        final int STATE_BEFORE_KEYCAP = 2;
        // The offset is immediately before a variation selector and a KEYCAP.
        final int STATE_BEFORE_VS_AND_KEYCAP = 3;

        // The offset is immediately before an emoji modifier.
        final int STATE_BEFORE_EMOJI_MODIFIER = 4;
        // The offset is immediately before a variation selector and an emoji modifier.
        final int STATE_BEFORE_VS_AND_EMOJI_MODIFIER = 5;

        // The offset is immediately before a variation selector.
        final int STATE_BEFORE_VS = 6;

        // The offset is immediately before an emoji.
        final int STATE_BEFORE_EMOJI = 7;
        // The offset is immediately before a ZWJ that were seen before a ZWJ emoji.
        final int STATE_BEFORE_ZWJ = 8;
        // The offset is immediately before a variation selector and a ZWJ that were seen before a
        // ZWJ emoji.
        final int STATE_BEFORE_VS_AND_ZWJ = 9;

        // The number of following RIS code points is odd.
        final int STATE_ODD_NUMBERED_RIS = 10;
        // The number of following RIS code points is even.
        final int STATE_EVEN_NUMBERED_RIS = 11;

        // The offset is in emoji tag sequence.
        final int STATE_IN_TAG_SEQUENCE = 12;

        // The state machine has been stopped.
        final int STATE_FINISHED = 13;

        int deleteCharCount = 0;  // Char count to be deleted by backspace.
        int lastSeenVSCharCount = 0;  // Char count of previous variation selector.

        int state = STATE_START;

        int tmpOffset = offset;
        do {
            final int codePoint = Character.codePointBefore(text, tmpOffset);
            tmpOffset -= Character.charCount(codePoint);

            switch (state) {
                case STATE_START:
                    deleteCharCount = Character.charCount(codePoint);
                    if (codePoint == LINE_FEED) {
                        state = STATE_LF;
                    } else if (AndroidEmoji.isVariationSelector(codePoint)) {
                        state = STATE_BEFORE_VS;
                    } else if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        state = STATE_ODD_NUMBERED_RIS;
                    } else if (AndroidEmoji.isEmojiModifier(codePoint)) {
                        state = STATE_BEFORE_EMOJI_MODIFIER;
                    } else if (codePoint == AndroidEmoji.COMBINING_ENCLOSING_KEYCAP) {
                        state = STATE_BEFORE_KEYCAP;
                    } else if (AndroidEmoji.isEmoji(codePoint)) {
                        state = STATE_BEFORE_EMOJI;
                    } else if (codePoint == AndroidEmoji.CANCEL_TAG) {
                        state = STATE_IN_TAG_SEQUENCE;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_LF:
                    if (codePoint == CARRIAGE_RETURN) {
                        ++deleteCharCount;
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_ODD_NUMBERED_RIS:
                    if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount += 2; /* Char count of RIS */
                        state = STATE_EVEN_NUMBERED_RIS;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_EVEN_NUMBERED_RIS:
                    if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount -= 2; /* Char count of RIS */
                        state = STATE_ODD_NUMBERED_RIS;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_KEYCAP:
                    if (AndroidEmoji.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_KEYCAP;
                        break;
                    }

                    if (AndroidEmoji.isKeycapBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS_AND_KEYCAP:
                    if (AndroidEmoji.isKeycapBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_EMOJI_MODIFIER:
                    if (AndroidEmoji.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_EMOJI_MODIFIER;
                        break;
                    } else if (AndroidEmoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_BEFORE_EMOJI;
                        break;
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS_AND_EMOJI_MODIFIER:
                    if (AndroidEmoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_VS:
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_BEFORE_EMOJI;
                        break;
                    }

                    if (!AndroidEmoji.isVariationSelector(codePoint) &&
                            UCharacter.getCombiningClass(codePoint) == 0) {
                        deleteCharCount += Character.charCount(codePoint);
                    }
                    state = STATE_FINISHED;
                    break;
                case STATE_BEFORE_EMOJI:
                    if (codePoint == AndroidEmoji.ZERO_WIDTH_JOINER) {
                        state = STATE_BEFORE_ZWJ;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_ZWJ:
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint) + 1;  // +1 for ZWJ.
                        state = AndroidEmoji.isEmojiModifier(codePoint) ?
                                STATE_BEFORE_EMOJI_MODIFIER : STATE_BEFORE_EMOJI;
                    } else if (AndroidEmoji.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint);
                        state = STATE_BEFORE_VS_AND_ZWJ;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_BEFORE_VS_AND_ZWJ:
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        // +1 for ZWJ.
                        deleteCharCount += lastSeenVSCharCount + 1 + Character.charCount(codePoint);
                        lastSeenVSCharCount = 0;
                        state = STATE_BEFORE_EMOJI;
                    } else {
                        state = STATE_FINISHED;
                    }
                    break;
                case STATE_IN_TAG_SEQUENCE:
                    if (AndroidEmoji.isTagSpecChar(codePoint)) {
                        deleteCharCount += 2; /* Char count of emoji tag spec character. */
                        // Keep the same state.
                    } else if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint);
                        state = STATE_FINISHED;
                    } else {
                        // Couldn't find tag_base character. Delete the last tag_term character.
                        deleteCharCount = 2;  // for U+E007F
                        state = STATE_FINISHED;
                    }
                    // TODO: Need handle emoji variation selectors. Issue 35224297
                    break;
                default:
                    throw new IllegalArgumentException("state " + state + " is unknown");
            }
        } while (tmpOffset > 0 && state != STATE_FINISHED);

        return offset - deleteCharCount;
    }
}
