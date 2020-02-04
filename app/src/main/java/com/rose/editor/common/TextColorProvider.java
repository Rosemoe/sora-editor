package com.rose.editor.common;


import com.rose.editor.android.ColorScheme;
import com.rose.editor.interfaces.CodeAnalyzer;
import com.rose.editor.model.BlockLine;
import com.rose.editor.model.NavigationLabel;
import com.rose.editor.model.Span;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a manager of analyzing text
 * @author Rose
 */
public class TextColorProvider {

    private TextColors mColors;
    private Callback mCallback;
    private AnalyzeThread mT;
    private CodeAnalyzer codeAnalyzer;
    private Allocator allocator = new Allocator();

    /**
     * Create a new manager for the given codeAnalyzer
     * @param codeAnalyzer0 Target codeAnalyzer
     */
    public TextColorProvider(CodeAnalyzer codeAnalyzer0){
        if(codeAnalyzer0 == null){
            throw new IllegalArgumentException();
        }
        mColors = new TextColors(allocator);
        mColors.addNormalIfNull();
        codeAnalyzer = codeAnalyzer0;
    }

    /**
     * Set callback of analysis
     * @param cb New callback
     */
    public void setCallback(Callback cb){
        mCallback = cb;
    }

    /**
     * Analyze the given text
     * @param origin The source text
     */
    public void analyze(Content origin){
        final AnalyzeThread mT = this.mT;
        if(mT == null){
            this.mT = new AnalyzeThread(codeAnalyzer,origin);
            this.mT.start();
        }else{
            if(mT.isAlive()){
                mT.restartWith(origin);
            }else{
                this.mT = new AnalyzeThread(codeAnalyzer,origin);
                this.mT.start();
            }
        }
    }

    /**
     * Get analysis result
     * @return Result of analysis
     */
    public TextColors getColors(){
        return mColors;
    }

    /**
     * Debug:Start time
     */
    public long st;

    /**
     * AnalyzeThread to control
     */
    public class AnalyzeThread extends Thread{

        private boolean waiting = false;
        private Content content;
        private CodeAnalyzer codeAnalyzer;

        /**
         * Create a new thread
         * @param a The CodeAnalyzer to call
         * @param content The Content to analyze
         */
        public AnalyzeThread(CodeAnalyzer a, Content content){
            codeAnalyzer = a;
            this.content = content;
        }

        /**
         * A delegate for token stream loop
         * To make it stop in time
         */
        public class Delegate{

            public boolean shouldReAnalyze(){
                return waiting;
            }

        }

        @Override
        public void run() {
            TextColors colors = new TextColors(allocator);
            Delegate d = new Delegate();
            st = System.currentTimeMillis();
            do{
                waiting = false;
                StringBuilder c = content.toStringBuilder();
                codeAnalyzer.analyze(c,colors,d);
                if(waiting){
                    colors.mSpans.clear();
                    colors.mLast = null;
                    colors.mLines.clear();
                    colors.mSuppressSwitch = Integer.MAX_VALUE;
                    colors.mLabels = null;
                    colors.mExtra = null;
                }
            }while(waiting);

            List<Span> garbages = mColors.getSpans();
            mColors = colors;
            colors.addNormalIfNull();
            try {
                if (mCallback != null)
                    mCallback.onAnalysisDone(TextColorProvider.this, colors);
                allocator.addSource(garbages);
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        /**
         * New content has been sent
         * Notify us to restart
         * @param content New source
         */
        public synchronized void restartWith(Content content){
            waiting = true;
            this.content = content;
        }

    }

    public interface Callback{

        void onAnalysisDone(TextColorProvider provider,TextColors colors);

    }

    /**
     * The result of analysis
     */
    public static class TextColors{

        private List<Span> mSpans;
        private List<BlockLine> mLines;
        private List<NavigationLabel> mLabels;
        private Allocator mAllocator;
        private Span mLast;
        private int mSuppressSwitch = Integer.MAX_VALUE;
        public Object mExtra;

        /**
         * Create a new result with the given Allocator
         * @param al Allocator to use
         */
        public TextColors(Allocator al){
            mAllocator = al;
            mLast = null;
            mSpans = new ArrayList<>(8192);
            mLines = new ArrayList<>(1024);
        }

        /**
         * Add a new span if required (colorId is different from last)
         * @param start Start index
         * @param colorId Type
         * @param c Target content
         */
        public void addIfNeeded(int start,int colorId,Content c){
            if(mLast == null){
                mSpans.add(mLast = mAllocator.obtain(start,colorId).wrap(c));
                return;
            }
            if(mLast.colorId != colorId){
                mSpans.add(mLast = mAllocator.obtain(start,colorId).wrap(c));
            }
        }

        /**
         * Add a new span if required (colorId is different from last)
         * @param start Start index
         * @param line Line
         * @param column Column
         * @param colorId Type
         */
        public void addIfNeeded(int start,int line,int column,int colorId) {
            if(mLast == null){
                mSpans.add(mLast = mAllocator.obtain(start,line,column,colorId));
                return;
            }
            if(mLast.colorId != colorId){
                mSpans.add(mLast = mAllocator.obtain(start,line,column,colorId));
            }
        }

        /**
         * Add a new span directly
         * @param start Start index
         * @param line Line
         * @param column Column
         * @param colorId Type
         */
        public void add(int start,int line,int column,int colorId) {
            mSpans.add(mLast = mAllocator.obtain(start,line,column,colorId));
        }

        /**
         * Get a new BlockLine object
         * <strong>It fields maybe not initialized with zero</strong>
         * @return An idle BlockLine
         */
        public BlockLine obtainNewBlock() {
            return mAllocator.next2();
        }

        /**
         * Add a new code block info
         * @param block Info of code block
         */
        public void addBlockLine(BlockLine block) {
            mLines.add(block);
        }

        /**
         * Get list of code blocks
         * @return code blocks
         */
        public List<BlockLine> getBlocks() {
            return mLines;
        }

        /**
         * Get list of span
         * @return spans
         */
        public List<Span> getSpans(){
            return mSpans;
        }

        /**
         * Ensure the list not empty
         */
        public void addNormalIfNull(){
            if(mLast == null){
                mSpans.add(mLast = mAllocator.obtain(0, ColorScheme.TEXT_NORMAL).wrap());
            }
        }

        /**
         * Set code navigation list
         * @param navigation New navigation list
         */
        public void setNavigation(List<NavigationLabel> navigation){
            mLabels = navigation;
        }

        /**
         * Get code navigation list
         * @return Current navigation list
         */
        public List<NavigationLabel> getNavigation(){
            return mLabels;
        }

        /**
         * Get a NavigationLabel with given arguments
         * @param line The line of NavigationLabel
         * @param label The description of NavagationLabel
         * @return Created NavigationLabel
         */
        public NavigationLabel obtainLabel(int line,String label) {
            return mAllocator.next3(line,label);
        }

        /**
         * Set suppress switch for editor
         * What is 'suppress switch' ?:
         * Suppress switch is a switch size for code block line drawing
         * and for the process to find out which code block the cursor is in.
         * Because the code blocks are not saved by the order of both start line and
         * end line,we are unable to know exactly when we should stop the process.
         * So without a suppress switch,it will cost a large of time to search code
         * blocks.So I added this switch.
         * A suppress switch is the code block count in the first layer code block
         * (as well as its sub code blocks).
         * If you are unsure,do not set it.
         * The default value if Integer.MAX_VALUE
         * @param suppressSwitch Suppress switch
         */
        public void setSuppressSwitch(int suppressSwitch) {
            mSuppressSwitch = suppressSwitch;
        }

        /**
         * Returns suppress switch
         * @see TextColors#setSuppressSwitch(int)
         * @return suppress switch
         */
        public int getSuppressSwitch() {
            return mSuppressSwitch;
        }

    }

}

