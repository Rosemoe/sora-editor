/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
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
package io.github.rosemoe.sora.widget;

import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.data.CompletionItem;

/**
 * Adapter to display results
 *
 * @author Rose
 */
@SuppressWarnings("CanBeFinal")
class DefaultCompletionItemAdapter extends EditorCompletionAdapter {

    @Override
    public int getItemHeight() {
        // 45 dp
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getContext().getResources().getDisplayMetrics());
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent, boolean isCurrentCursorPosition) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.default_completion_result_item, parent, false);
        }
        CompletionItem item = getItem(pos);
        TextView tv = view.findViewById(R.id.result_item_label);
        var label = Spannable.Factory.getInstance().newSpannable(item.label);
        var prefix = getPrefix();
        var index = TextUtils.indexOf(item.label.toLowerCase(Locale.ROOT), prefix.toLowerCase(Locale.ROOT));
        if (index != -1) {
            label.setSpan(new ForegroundColorSpan(0xffec407a), index, index + prefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tv.setText(label);
        tv = view.findViewById(R.id.result_item_desc);
        tv.setText(item.desc);
        view.setTag(pos);
        if (isCurrentCursorPosition) {
            view.setBackgroundColor(0xffdddddd);
        } else {
            view.setBackgroundColor(0xffffffff);
        }
        ImageView iv = view.findViewById(R.id.result_item_image);
        iv.setImageDrawable(item.icon);
        return view;
    }

}
