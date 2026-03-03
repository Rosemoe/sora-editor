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

import android.text.GetChars;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.rosemoe.sora.annotations.UnsupportedUserUsage;
import io.github.rosemoe.sora.text.bidi.BidiRequirementChecker;
import io.github.rosemoe.sora.text.bidi.TextBidi;
import io.github.rosemoe.sora.text.string.StringLatin1;
import io.github.rosemoe.sora.text.string.StringUTF16;
import io.github.rosemoe.sora.util.ShareableData;

public class ContentLine implements CharSequence, GetChars, BidiRequirementChecker, ShareableData<ContentLine> {

    private static final byte LATIN1 = 0;
    private static final byte UTF16 = 1;

    private byte[] value;
    private byte coder;
    private int length;

    private int rtlAffectingCount;
    private LineSeparator lineSeparator;
    private transient AtomicInteger refCount;

    public ContentLine() {
        this(true);
    }

    public ContentLine(@Nullable CharSequence text) {
        this(true);
        insert(0, text);
    }

    public ContentLine(@NonNull ContentLine src) {
        length = src.length;
        coder = src.coder;
        value = coder == LATIN1 ? new byte[src.length + 16] : new byte[StringUTF16.bytesForChars(src.length + 16)];
        rtlAffectingCount = src.rtlAffectingCount;
        lineSeparator = src.lineSeparator;
        if (coder == LATIN1) {
            StringLatin1.copyChars(src.value, 0, value, 0, length);
        } else {
            StringUTF16.copyChars(src.value, 0, value, 0, length);
        }
    }

    public ContentLine(int size) {
        length = 0;
        coder = LATIN1;
        value = new byte[size];
    }

    private ContentLine(boolean initialize) {
        if (initialize) {
            length = 0;
            coder = LATIN1;
            value = new byte[32];
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index > length) {
            throw new StringIndexOutOfBoundsException("index = " + index + ", length = " + length);
        }
    }

    private void ensureCapacity(int capacity) {
        if (charCapacity() < capacity) {
            int oldCapacity = charCapacity();
            int newLength = oldCapacity * 2 < capacity ? capacity + 2 : oldCapacity * 2;
            byte[] newValue = coder == LATIN1 ? new byte[newLength] : new byte[StringUTF16.bytesForChars(newLength)];
            if (coder == LATIN1) {
                StringLatin1.copyChars(value, 0, newValue, 0, length);
            } else {
                StringUTF16.copyChars(value, 0, newValue, 0, length);
            }
            value = newValue;
        }
    }

    private int charCapacity() {
        return coder == LATIN1 ? value.length : value.length >> 1;
    }

    private char charAtInternal(int index) {
        return coder == LATIN1 ? StringLatin1.getChar(value, index) : StringUTF16.getChar(value, index);
    }

    private void putCharInternal(int index, char c) {
        if (coder == LATIN1) {
            StringLatin1.putChar(value, index, c);
        } else {
            StringUTF16.putChar(value, index, c);
        }
    }

    private void copyCharsInternal(byte[] src, int srcBegin, byte[] dst, int dstBegin, int len) {
        if (coder == LATIN1) {
            StringLatin1.copyChars(src, srcBegin, dst, dstBegin, len);
        } else {
            StringUTF16.copyChars(src, srcBegin, dst, dstBegin, len);
        }
    }

    private static boolean requiresUTF16(@NonNull CharSequence s, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!StringLatin1.canEncode(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void toUTF16IfNeeded(int capacity) {
        if (coder == LATIN1) {
            int oldCapacity = charCapacity();
            int newCapacity = oldCapacity * 2 < capacity ? capacity + 2 : oldCapacity * 2;
            value = StringLatin1.inflateToUTF16(value, length, newCapacity);
            coder = UTF16;
        }
    }

    /**
     * Inserts the specified {@code CharSequence} into this sequence.
     * <p>
     * The characters of the {@code CharSequence} argument are inserted,
     * in order, into this sequence at the indicated offset, moving up
     * any characters originally above that position and increasing the length
     * of this sequence by the length of the argument s.
     * <p>
     * The result of this method is exactly the same as if it were an
     * invocation of this object's
     * {@link #insert(int, CharSequence, int, int) insert}(dstOffset, s, 0, s.length())
     * method.
     *
     * <p>If {@code s} is {@code null}, then the four characters
     * {@code "null"} are inserted into this sequence.
     *
     * @param dstOffset the offset.
     * @param s         the sequence to be inserted
     * @return a reference to this object.
     * @throws IndexOutOfBoundsException if the offset is invalid.
     */
    @NonNull
    public ContentLine insert(int dstOffset, @Nullable CharSequence s) {
        if (s == null)
            s = "null";
        return this.insert(dstOffset, s, 0, s.length());
    }

    /**
     * Inserts a subsequence of the specified {@code CharSequence} into
     * this sequence.
     * <p>
     * The subsequence of the argument {@code s} specified by
     * {@code start} and {@code end} are inserted,
     * in order, into this sequence at the specified destination offset, moving
     * up any characters originally above that position. The length of this
     * sequence is increased by {@code end - start}.
     * <p>
     * The character at index <i>k</i> in this sequence becomes equal to:
     * <ul>
     * <li>the character at index <i>k</i> in this sequence, if
     * <i>k</i> is less than {@code dstOffset}
     * <li>the character at index <i>k</i>{@code +start-dstOffset} in
     * the argument {@code s}, if <i>k</i> is greater than or equal to
     * {@code dstOffset} but is less than {@code dstOffset+end-start}
     * <li>the character at index <i>k</i>{@code -(end-start)} in this
     * sequence, if <i>k</i> is greater than or equal to
     * {@code dstOffset+end-start}
     * </ul><p>
     * The {@code dstOffset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     * <p>The start argument must be nonnegative, and not greater than
     * {@code end}.
     * <p>The end argument must be greater than or equal to
     * {@code start}, and less than or equal to the length of s.
     *
     * <p>If {@code s} is {@code null}, then this method inserts
     * characters as if the s parameter was a sequence containing the four
     * characters {@code "null"}.
     *
     * @param dstOffset the offset in this sequence.
     * @param s         the sequence to be inserted.
     * @param start     the starting index of the subsequence to be inserted.
     * @param end       the end index of the subsequence to be inserted.
     * @return a reference to this object.
     * @throws IndexOutOfBoundsException if {@code dstOffset}
     *                                   is negative or greater than {@code this.length()}, or
     *                                   {@code start} or {@code end} are negative, or
     *                                   {@code start} is greater than {@code end} or
     *                                   {@code end} is greater than {@code s.length()}
     */
    @NonNull
    public ContentLine insert(int dstOffset, @Nullable CharSequence s,
                              int start, int end) {
        if (s == null)
            s = "null";
        if ((dstOffset < 0) || (dstOffset > this.length()))
            throw new IndexOutOfBoundsException("dstOffset " + dstOffset);
        if ((start < 0) || (end < 0) || (start > end) || (end > s.length()))
            throw new IndexOutOfBoundsException(
                    "start " + start + ", end " + end + ", s.length() "
                            + s.length());
        int len = end - start;
        if (coder == LATIN1 && requiresUTF16(s, start, end)) {
            toUTF16IfNeeded(length + len);
        }
        ensureCapacity(length + len);
        copyCharsInternal(value, dstOffset, value, dstOffset + len, length - dstOffset);
        for (int i = start; i < end; i++) {
            var ch = s.charAt(i);
            putCharInternal(dstOffset++, ch);
            if (TextBidi.couldAffectRtl(ch)) {
                rtlAffectingCount++;
            }
        }
        length += len;
        return this;
    }

    @NonNull
    public ContentLine insert(int offset, char c) {
        if (coder == LATIN1 && !StringLatin1.canEncode(c)) {
            toUTF16IfNeeded(length + 1);
        }
        ensureCapacity(length + 1);
        if (offset < length) {
            copyCharsInternal(value, offset, value, offset + 1, length - offset);
        }
        if (TextBidi.couldAffectRtl(c)) {
            rtlAffectingCount++;
        }
        putCharInternal(offset, c);
        length += 1;
        return this;
    }

    /**
     * Removes the characters in a substring of this sequence.
     * The substring begins at the specified {@code start} and extends to
     * the character at index {@code end - 1} or to the end of the
     * sequence if no such character exists. If
     * {@code start} is equal to {@code end}, no changes are made.
     *
     * @param start The beginning index, inclusive.
     * @param end   The ending index, exclusive.
     * @return This object.
     * @throws StringIndexOutOfBoundsException if {@code start}
     *                                         is negative, greater than {@code length()}, or
     *                                         greater than {@code end}.
     */
    @NonNull
    public ContentLine delete(int start, int end) {
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (end > length)
            end = length;
        if (start > end)
            throw new StringIndexOutOfBoundsException();
        int len = end - start;
        if (len > 0) {
            for (int i = start; i < end; i++) {
                if (TextBidi.couldAffectRtl(charAtInternal(i))) {
                    rtlAffectingCount--;
                }
            }
            copyCharsInternal(value, start + len, value, start, length - end);
            length -= len;
        }
        return this;
    }

    /**
     * Check if any character in the text affects RTL state
     */
    public boolean mayNeedBidi() {
        return rtlAffectingCount > 0;
    }

    /**
     * Append the given text
     *
     * @return This object.
     */
    @NonNull
    public ContentLine append(CharSequence text) {
        return this.insert(length, text);
    }

    @Override
    public int length() {
        return length;
    }

    /**
     * {@inheritDoc}
     * <strong>Index is unchecked for performance</strong>
     */
    @Override
    @UnsupportedUserUsage
    public char charAt(int index) {
        // checkIndex(index);
        if (index >= length) {
            var separator = getLineSeparator();
            return separator.getLength() > 0 ? getLineSeparator().getContent().charAt(index - length) : '\n';
        }
        return charAtInternal(index);
    }

    @Override
    @NonNull
    public ContentLine subSequence(int start, int end) {
        checkIndex(start);
        checkIndex(end);
        if (end < start) {
            throw new StringIndexOutOfBoundsException("start is greater than end");
        }
        var res = new ContentLine(false);
        res.coder = coder;
        if (res.coder == LATIN1) {
            res.value = new byte[end - start + 16];
            StringLatin1.copyChars(value, start, res.value, 0, end - start);
        } else {
            res.value = new byte[StringUTF16.bytesForChars(end - start + 16)];
            StringUTF16.copyChars(value, start, res.value, 0, end - start);
        }
        res.length = end - start;

        // Compute new value when required
        if (rtlAffectingCount > 0) {
            for (int i = 0; i < res.length; i++) {
                if (TextBidi.couldAffectRtl(res.charAt(i))) {
                    res.rtlAffectingCount++;
                }
            }
        }
        return res;
    }

    /**
     * A convenient method to append text to a StringBuilder
     */
    public void appendTo(@NonNull StringBuilder sb) {
        if (coder == LATIN1) {
            StringLatin1.appendTo(value, length, sb);
        } else {
            StringUTF16.appendTo(value, length, sb);
        }
    }

    @Override
    @NonNull
    public String toString() {
        return coder == LATIN1 ? StringLatin1.newString(value, length) : StringUTF16.newString(value, length);
    }

    /**
     * Unlike {@link #toString()}, string generated by this method adds an extra LF character
     * at the end of the result {@link String}.
     */
    @NonNull
    public String toStringWithNewline() {
        if (charCapacity() == length) {
            ensureCapacity(length + 1);
        }
        putCharInternal(length, '\n');
        return coder == LATIN1 ? StringLatin1.newString(value, length + 1) : StringUTF16.newString(value, length + 1);
    }

    @Override
    public void getChars(int srcBegin, int srcEnd, @NonNull char[] dst, int dstBegin) {
        if (srcBegin < 0)
            throw new StringIndexOutOfBoundsException(srcBegin);
        if ((srcEnd < 0) || (srcEnd > length))
            throw new StringIndexOutOfBoundsException(srcEnd);
        if (srcBegin > srcEnd)
            throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
        if (coder == LATIN1) {
            StringLatin1.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }

    public void setLineSeparator(@Nullable LineSeparator separator) {
        this.lineSeparator = separator;
    }

    @NonNull
    public LineSeparator getLineSeparator() {
        if (lineSeparator == null) {
            return LineSeparator.NONE;
        }
        return lineSeparator;
    }

    /**
     * Copy the object.
     * The new instance can be safely modified without affecting the original instance.
     */
    @NonNull
    public ContentLine copy() {
        var clone = new ContentLine(false);
        clone.length = length;
        clone.coder = coder;
        clone.value = new byte[value.length];
        System.arraycopy(value, 0, clone.value, 0, value.length);
        clone.rtlAffectingCount = rtlAffectingCount;
        clone.lineSeparator = lineSeparator;
        return clone;
    }

    @Override
    public void retain() {
        if (refCount == null) {
            refCount = new AtomicInteger(2);
            return;
        }
        refCount.incrementAndGet();
    }

    @Override
    public void release() {
        if (refCount == null) {
            return;
        }
        int count = refCount.decrementAndGet();
        if (count < 0) {
            throw new IllegalStateException("illegal operation. There is no active owner");
        }
    }

    @Override
    public boolean isMutable() {
        return refCount == null || refCount.get() == 1;
    }

    @Override
    public ContentLine toMutable() {
        if (isMutable()) {
            return this;
        }
        return copy();
    }
}
