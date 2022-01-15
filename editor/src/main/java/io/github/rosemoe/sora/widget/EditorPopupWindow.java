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
package io.github.rosemoe.sora.widget;

import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import java.util.Objects;

//TODO
public class EditorPopupWindow extends PopupWindow {

    /**
     * Update the position of this window when user scrolls the editor
     */
    public final static int FEATURE_SCROLL_AS_CONTENT = 1;

    /**
     * Allow the window to be displayed outside the view's rectangle.
     * Otherwise, the window's size will be adjusted to force it to display in the view.
     * If the space can't display it, it will get hidden.
     */
    public final static int FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED = 1 << 1;

    /**
     * Hide this window when the user scrolls fast. Such as the selection handle
     * is currently near the edge of screen.
     */
    public final static int FEATURE_HIDE_WHEN_FAST_SCROLL = 1 << 2;

    private final CodeEditor mEditor;
    private final int mFeatures;

    /**
     * Create a popup window for editor
     *
     * @param features Features to request
     * @see #FEATURE_SCROLL_AS_CONTENT
     * @see #FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED
     * @see #FEATURE_HIDE_WHEN_FAST_SCROLL
     */
    public EditorPopupWindow(@NonNull CodeEditor editor, int features) {
        mEditor = Objects.requireNonNull(editor);
        mFeatures = features;
    }

    /**
     * Get editor instance
     */
    @NonNull
    public CodeEditor getEditor() {
        return mEditor;
    }

    /**
     * Checks whether a single feature is enabled
     *
     * @see #FEATURE_SCROLL_AS_CONTENT
     * @see #FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED
     * @see #FEATURE_HIDE_WHEN_FAST_SCROLL
     */
    public boolean isFeatureEnabled(int feature) {
        if (Integer.bitCount(feature) != 1) {
            throw new IllegalArgumentException("Not a valid feature integer");
        }
        return (mFeatures & feature) != 0;
    }

}
