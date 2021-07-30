/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

import io.github.rosemoe.editor.struct.CompletionItem;

/**
 * A class to make custom adapter for auto-completion window
 * @see EditorCompletionAdapter#getItemHeight()
 * @see EditorCompletionAdapter#getView(int, View, ViewGroup, boolean)
 */
public abstract class EditorCompletionAdapter extends BaseAdapter {

    private EditorAutoCompleteWindow mWindow;
    private List<CompletionItem> mItems;

    /**
     * Called by {@link EditorAutoCompleteWindow} to attach some arguments
     */
    public void attachAttributes(EditorAutoCompleteWindow window, List<CompletionItem> items) {
        mWindow = window;
        mItems = items;
    }

    @Override
    public CompletionItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent, position == mWindow.getCurrentPosition());
    }

    /**
     * Get context from editor
     */
    protected Context getContext() {
        return mWindow.getContext();
    }

    /**
     * Implementation of this class should provide exact height of its item
     */
    public abstract int getItemHeight();

    /**
     * @see BaseAdapter#getView(int, View, ViewGroup)
     * @param isCurrentCursorPosition Is the {@param position} currently selected
     */
    protected abstract View getView(int position, View convertView, ViewGroup parent, boolean isCurrentCursorPosition);

}
