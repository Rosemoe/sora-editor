/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.rose.editor.android;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.rose.editor.text.Cursor;
import com.rose.editor.text.TextAnalyzer;
import com.rose.editor.interfaces.AutoCompleteProvider;
import com.rose.editor.struct.ResultItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Auto complete panel for editing code quicker
 * @author Rose
 */
public class AutoCompletePanel extends BasePanel
{
    private CodeEditor mEditor;
    private LinearLayout mLayout;
    private ListView mListView;
    private TextView mTip;
    private ProgressBar mPb;
    private GradientDrawable mBg;

    private int mCurrent = 0;
    private long mRequestTime;
    private String mLastPrefix;
    private MatchThread mThread;
    private AutoCompleteProvider mProvider;

    private final static String TIP = "Loading...";
    public final static Comparator<ResultItem> RES_COMP = new Comparator<ResultItem>(){

        @Override
        public int compare(ResultItem p1, ResultItem p2)
        {
            return p1.label.compareTo(p2.label);
        }


    };

    /**
     * Create a panel instance for the given editor
     * @param editor Target editor
     */
    public AutoCompletePanel(CodeEditor editor) {
        super(editor);
        mEditor = editor;
        mLayout = new LinearLayout(mEditor.getContext());
        mLayout.setGravity(Gravity.CENTER);
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setPadding(5, 5, 5, 5);
        mListView = new ListView(mEditor.getContext());
        mLayout.addView(mListView, new LinearLayout.LayoutParams(-1, -1));
        mPb = new ProgressBar(mEditor.getContext());
        mPb.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        mLayout.addView(mPb);
        mTip = new TextView(mEditor.getContext());
        mTip.setText(TIP);
        mTip.setGravity(Gravity.CENTER);
        mTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        mTip.setTextColor(0xff000000);
        mLayout.addView(mTip);
        ((LinearLayout.LayoutParams)mPb.getLayoutParams()).bottomMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, Resources.getSystem().getDisplayMetrics());
        setContentView(mLayout);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(4);
        mLayout.setBackgroundDrawable(gd);
        mBg = gd;
        applyColor();
        setLoading(true);
        mListView.setOnItemClickListener(new ListView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
            {
                try{
                    select(p3);
                }catch(Exception e) {
                    Toast.makeText(mEditor.getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
            }


        });
    }

    /**
     * Set a auto completion items provider
     * @param p New provider.can not be null
     */
    public void setProvider(AutoCompleteProvider p){
        mProvider = p;
    }

    /**
     * Apply colors for self
     */
    public void applyColor() {
        ColorScheme colors = mEditor.getColorScheme();
        mBg.setStroke(1, colors.getColor(ColorScheme.AUTO_COMP_PANEL_CORNER));
        mBg.setColor(colors.getColor(ColorScheme.AUTO_COMP_PANEL_BG));
    }

    /**
     * Change layout to loading/idle
     * @param state Whether loading
     */
    public void setLoading(boolean state) {
        mTip.setVisibility(/*state ? View.VISIBLE : */View.GONE);
        mPb.setVisibility(/*state ? View.VISIBLE : */View.GONE);
        mListView.setVisibility(/*(!state) ? */View.VISIBLE/* : View.GONE*/);
    }

    /**
     * Move selection down
     */
    public void moveDown() {
        if(mCurrent + 1 >= mListView.getAdapter().getCount()) {
            return;
        }
        mCurrent++;
        ((ItemAdapter) mListView.getAdapter()).notifyDataSetChanged();
        ensurePosition();
    }

    /**
     * Move selection up
     */
    public void moveUp() {
        if(mCurrent - 1 < 0) {
            return;
        }
        mCurrent--;
        ((ItemAdapter) mListView.getAdapter()).notifyDataSetChanged();
        ensurePosition();
    }

    /**
     * Make current selection visible
     */
    private void ensurePosition() {
        mListView.smoothScrollToPosition(mCurrent);
    }

    /**
     * Select current position
     */
    public void select() {
        select(mCurrent);
    }

    /**
     * Select the given position
     * @param pos Index of auto complete item
     */
    public void select(int pos) {
        ResultItem item = ((ItemAdapter) mListView.getAdapter()).getItem(pos);
        Cursor cursor = mEditor.getCursor();
        if(!cursor.isSelected()) {
            mEditor.getText().delete(cursor.getLeftLine(), cursor.getLeftColumn() - mLastPrefix.length(), cursor.getLeftLine(), cursor.getLeftColumn());
            cursor.onCommitText(item.commit);
            if((item.mask & ResultItem.MASK_SHIFT_LEFT_TWICE) != 0){
                mEditor.moveSelectionLeft();
                mEditor.moveSelectionLeft();
            }
            if((item.mask & ResultItem.MASK_SHIFT_LEFT_ONCE) != 0) {
                mEditor.moveSelectionLeft();
            }
            mEditor.cursorChangeExternal();
        }
        hide();
    }

    /**
     * Set prefix for auto complete analysis
     * @param prefix The user's input code's prefix
     */
    public void setPrefix(String prefix) {
        setLoading(true);
        mLastPrefix = prefix;
        mRequestTime = System.currentTimeMillis();
        mThread = new MatchThread(mRequestTime, prefix);
        mThread.start();
    }

    /**
     * Get prefix set
     * @return The previous prefix
     */
    public String getPrefix() {
        return mLastPrefix;
    }

    /**
     * Display result of analysis
     * @param results Items of analysis
     * @param requestTime The time that this thread starts
     */
    private void displayResults(final List<ResultItem> results, long requestTime) {
        if(mRequestTime != requestTime) {
            return;
        }
        mEditor.post(new Runnable() {
            @Override
            public void run() {
                setLoading(false);
                if(results == null || results.isEmpty()) {
                    hide();
                    return;
                }
                mCurrent = 0;
                mListView.setAdapter(new ItemAdapter(results));
            }
        });
    }

    /**
     * Adapter to display results
     * @author Rose
     */
    private class ItemAdapter extends BaseAdapter {

        private List<ResultItem> mItems;

        private BitmapDrawable[] bmps;

        public ItemAdapter(List<ResultItem> items) {
            mItems = items;
            bmps = new BitmapDrawable[2];
            BitmapDrawable src = (BitmapDrawable)mEditor.getContext().getResources().getDrawable(R.mipmap.box_red);
            Bitmap bmp = src.getBitmap();
            bmps[0] = new BitmapDrawable(mEditor.getContext().getResources(),bmp);
            bmps[0].setColorFilter(0xff009688, PorterDuff.Mode.SRC_ATOP);
            bmps[1] = new BitmapDrawable(mEditor.getContext().getResources(),bmp);
            bmps[1].setColorFilter(0xffec4071, PorterDuff.Mode.SRC_ATOP);

        }

        @Override
        public int getCount(){
            return mItems.size();
        }

        @Override
        public ResultItem getItem(int pos){
            return mItems.get(pos);
        }

        @Override
        public long getItemId(int pos){
            return getItem(pos).hashCode();
        }

        @Override
        @SuppressWarnings("all") /*to clear redundant cast warnings*/
        public View getView(int pos, View view, ViewGroup parent){
            if(view == null) {
                view = LayoutInflater.from(mEditor.getContext()).inflate(R.layout.result_item, parent, false);
            }
            ResultItem item = getItem(pos);
            TextView tv = (TextView)view.findViewById(R.id.result_item_label);
            tv.setText(item.label);
            tv = (TextView) view.findViewById(R.id.result_item_desc);
            tv.setText(item.desc);
            view.setTag(pos);
            if(mCurrent == pos) {
                view.setBackgroundColor(0xffdddddd);
            }else{
                view.setBackgroundColor(0xffffffff);
            }
            ImageView iv = (ImageView) view.findViewById(R.id.result_item_image);
            iv.setImageDrawable(bmps[item.type]);

            return view;
        }

    }

    /**
     * Analysis thread
     * @author Rose
     */
    private class MatchThread extends Thread {

        private long mTime;
        private String mPrefix;
        private boolean mInner;
        private TextAnalyzer.TextColors mColors;
        private int mLine;
        private final AutoCompleteProvider mLocalProvider = mProvider;

        public MatchThread(long requestTime, String prefix) {
            mTime = requestTime;
            mPrefix = prefix;
            mColors = mEditor.getTextColor();
            mLine = mEditor.getCursor().getLeftLine();
            mInner = (!mEditor.isHighlightCurrentBlock()) || (mEditor.getBlockIndex() != -1);
        }

        @Override
        public void run() {
            try {
                displayResults(mLocalProvider.getAutoCompleteItems(mPrefix, mInner, mColors, mLine), mTime);
            }catch (Exception e) {
                e.printStackTrace();
                displayResults(new ArrayList<ResultItem>(),mTime);
            }
        }


    }

}

