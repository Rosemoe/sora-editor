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
package io.github.rosemoe.sora.lang.format;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;

/**
 * Format content for editor
 */
public interface Formatter {


    /**
     * Format the given content from {@code start} position to {@code end} position
     *
     * Format the content directly, and call {@link FormatResultReceiver} to receive the formatted content from the editor when the formatting is complete
     * *
     * @param text the content to format
     */
    void format(@NonNull Content text);

    /**
     * Format the given content from {@code start} position to {@code end} position
     *
     * Format the content directly, and call {@link FormatResultReceiver} to receive the formatted content from the editor when the formatting is complete
     *
     * @param text  the content to format
     * @param start the start position of the content to format
     * @param end   the end position of the content to format
     */
    void formatRegion(@NonNull Content text, @NonNull CharPosition start, @NonNull CharPosition end);

    void setReceiver(@Nullable FormatResultReceiver receiver);

    /**
     * Whether the current formatter is running
     */
    boolean isRunning();

    interface FormatResultReceiver {
        /**
         * Called when the formatting is complete
         *
         * @param applyContent the formatted text
         */
        void onFormatSucceed(@NonNull CharSequence applyContent);

        /**
         * Called when the formatting is failed
         *
         * @param throwable the throwable that caused formatting failed
         */
        void onFormatFail(@Nullable Throwable throwable);

    }


    /**
     * Destroy the formatter. Release any resources held.
     * Make sure that you will not call the receiver anymore.
     */
    void destroy();

}
